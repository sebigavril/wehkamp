package com.wehkart.service

import akka.actor.{Actor, Props}
import com.wehkart.ActorProtocol._
import com.wehkart.domain.{CatalogProduct, ProductLike}
import com.wehkart.repository.ProductsRepository

private class CatalogActor(productsRepo: ProductsRepository) extends Actor {

  override def receive: Receive = active(productsRepo.initialProducts)

  /**
    * From the requirements:
    * "Every product has a certain stock which is updated when products are added of deleted in the shopping-basket."
    *
    * This design is not scalable. Every time a client adds something to the basket, a shared state (the catalog) has to be updated.
    * This will happen even if the client never buys anything.
    *
    * A better approach, in my opinion, is to allow for eventual consistency.
    * The client will always check if there are enough products in stock (handling [[com.wehkart.ActorProtocol.Add]]).
    * But when he want's to add items to the basket, the stocks are not updated (handling [[com.wehkart.ActorProtocol.Remove]]).
    * Removing from the stock should happen only when the client hits the "buy" button.
    * If in the meantime, the stock is gone, he will see a message stating this.
    * This is how I would advice on implementing this scenario.
    */
  private def active(items: Set[CatalogProduct]): Receive = {
    case ListAll                 => sender() ! items
    case Add(product, amount)    => context become active(add(items, product, amount))
    case Remove(product, amount) => context become active(remove(items, product, amount))
  }

  private def add(items: Set[CatalogProduct], product: ProductLike, amount: Long) =
    amount match {
      case n if n > 0   => addSome(items, product, amount)
      case n if n <= 0  => sendAndReturn(NumberLowerOrEqualToZero, items)
    }

  private def addSome(items: Set[CatalogProduct], product: ProductLike, amount: Long) = {
    items.find(_.product == product) match {
      case Some(p)  => items.filterNot(_.product == product) + CatalogProduct(product, amount + p.amount)
      case None     => items + CatalogProduct(product, amount)
    }
  }

  private def remove(items: Set[CatalogProduct], product: ProductLike, amount: Long) = {
    items.find(_.product == product) match {
      case Some(CatalogProduct(_, currCnt)) if currCnt > 0  => removeSome(items, product, amount, currCnt)
      case Some(_) if amount <= 0                           => sendAndReturn(NumberLowerOrEqualToZero, items)
      case None                                             => sendAndReturn(OutOfStock, items)
    }
  }

  private def removeSome(items: Set[CatalogProduct], product: ProductLike, amount: Long, currentCount: Long) = {
    def findProduct = items.find(_.product == product)
    def outOfStock = findProduct.map(_.amount == 0).getOrElse(true)
    def notEnoughStock = findProduct.map(_.amount < amount).getOrElse(true)
    def removeAll = items.filterNot(_.product == product)
    def removeFromExisting = {
      val newProduct = findProduct.map(old => old.copy(amount =  old.amount - amount))
      items.filterNot(_.product == product) ++ newProduct
    }

    if(outOfStock)                    sendAndReturn(OutOfStock, items)
    else if(notEnoughStock)           sendAndReturn(StockNotEnough, items)
    else if (currentCount > amount)   sendAndReturn(Done, removeFromExisting)
    else if (currentCount == amount)  sendAndReturn(Done, removeAll)
    else sendAndReturn(Done, items)
  }

  private def sendAndReturn(msg: ActorMessage, items: Set[CatalogProduct]) = {
    sender() ! msg
    items
  }
}

object CatalogActor {
  def props(productRepo: ProductsRepository) =
    Props(new CatalogActor(productRepo))
}