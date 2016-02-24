package com.wehkamp.service

import akka.actor.{Actor, Props}
import com.wehkamp.ActorProtocol.Catalog._
import com.wehkamp.ActorProtocol._
import com.wehkamp.domain.{BasketProduct, ShoppingProduct}
import com.wehkamp.repository.ProductsRepository

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
    * The client will always check if there are enough products in stock (handling [[com.wehkamp.ActorProtocol.Catalog.Add]]).
    * But when he want's to add items to the basket, the stocks are not updated (handling [[com.wehkamp.ActorProtocol.Catalog.Remove]]).
    * Removing from the stock should happen only when the client hits the "buy" button.
    * If in the meantime, the stock is gone, he will see a message stating this.
    * This is how I would advice on implementing this scenario.
    */
  private def active(items: Set[ShoppingProduct]): Receive = {
    case ListAll                    => sender() ! items
    case List(basketProds)          => sender() ! list(basketProds, items)
    case Add(productId, amount)     => context become active(add(items, productId, amount))
    case Remove(productId, amount)  => context become active(remove(items, productId, amount))
  }

  private def list(basketProds: Set[BasketProduct], items: Set[ShoppingProduct]) =
    basketProds.flatMap { p => items.find(_.id == p.id).map(_.copy(amount = p.amount)) }

  private def add(items: Set[ShoppingProduct], productId: Long, amount: Long) =
    if (amount > 0) sendAndReturn(Done, addValidAmount(items, productId, amount))
    else            sendAndReturn(InvalidAmount, items)


  private def addValidAmount(items: Set[ShoppingProduct], productId: Long, amount: Long) = {
    items.find(_.id == productId) match {
      case Some(p)  => items.filterNot(_.id == productId) + ShoppingProduct.from(p, Some(amount + p.amount))
      case None     => sendAndReturn(OutOfStock, items)
    }
  }

  private def remove(items: Set[ShoppingProduct], productId: Long, amount: Long) = {
    if (amount > 0) removeValidAmount(items, productId, amount)
    else            sendAndReturn(InvalidAmount, items)
  }

  private def removeValidAmount(items: Set[ShoppingProduct], productId: Long, amount: Long) = {
    items.find(_.id == productId) match {
      case Some(ShoppingProduct(_, _, currentAmount)) => removeProduct(items, productId, amount, currentAmount)
      case None                                       => sendAndReturn(OutOfStock, items)
    }
  }

  private def removeProduct(items: Set[ShoppingProduct], productId: Long, amount: Long, currentCount: Long) = {
    def find            = items.find(_.id == productId)
    def outOfStock      = find.map(_.amount == 0).getOrElse(true)
    def notEnoughStock  = find.map(_.amount < amount).getOrElse(true)
    def removeExisting  = items.filterNot(_.id == productId) ++ find.map(removeAmount)
    def removeAmount(p: ShoppingProduct) = ShoppingProduct.from(p, Some(p.amount - amount))

    if (outOfStock)                   sendAndReturn(OutOfStock, items)
    else if (notEnoughStock)          sendAndReturn(StockNotEnough, items)
    else if (currentCount >= amount)  sendAndReturn(Done, removeExisting)
    else                              sendAndReturn(Done, items)
  }

  private def sendAndReturn(msg: ActorMessage, items: Set[ShoppingProduct]) = {
    sender() ! msg
    items
  }
}

object CatalogActor {
  def props(productRepo: ProductsRepository) =
    Props(new CatalogActor(productRepo))
}