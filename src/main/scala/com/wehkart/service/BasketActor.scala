package com.wehkart.service

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.wehkart.ActorProtocol._
import com.wehkart.ExecutionContexts.ctx
import com.wehkart.domain.{BasketProduct, ProductLike}

/**
  * The basket that is attributed to a certain user.
  *
  * @param userId The id of the user that owns this basket.
  */
private class BasketActor(userId: Long, catalog: ActorRef) extends Actor {

  implicit val timeout = Timeout(10 seconds)

  override def receive: Receive = active(Set.empty)

  private def active(items: Set[BasketProduct]): Receive = {
    case ListAll                 => sender() ! items
    case Add(product, amount)    => context become active(add(items, product, amount))
    case Remove(product, amount) => context become active(remove(items, product, amount))
  }

  private def add(items: Set[BasketProduct], product: ProductLike, amount: Long) = {
    amount match {
      /**
        * Since there is only one catalog, the different basket actors will need to share it.
        * Even though a synchronization is not explicit, this like will take more or less time to execute
        * depending on the number of basket actors that are accessing the catalog.
        *
        * A better approach is suggested in [[com.wehkart.service.CatalogActor]]
        */
      case n if n > 0   => Await.result(addSome(items, product, amount), timeout.duration)
      case n if n <= 0  => sendAndReturn(NumberLowerOrEqualToZero, items)
    }
  }

  private def addSome(items: Set[BasketProduct], product: ProductLike, amount: Long) = {
    def findProduct = items.find(_.product == product)
    def addToExistingProduct(p: BasketProduct) =
      items.filterNot(_.product == product) + BasketProduct(product, amount + p.amount)
    def addNewProduct = items + BasketProduct(product, amount)

    val catalogResponse = catalog ? Remove(product, amount)
    catalogResponse.map {
      case OutOfStock               => sendAndReturn(OutOfStock, items)
      case StockNotEnough           => sendAndReturn(StockNotEnough, items)
      case NumberLowerOrEqualToZero => sendAndReturn(NumberLowerOrEqualToZero, items)
      case Done                     =>
        val updatedItems = findProduct match {
          case Some(p)  => addToExistingProduct(p)
          case None     => addNewProduct
        }
        sendAndReturn(Done, updatedItems)
    }
  }

  private def remove(items: Set[BasketProduct], product: ProductLike, amount: Long) = {
    items.find(_.product == product) match {
      case Some(p) if amount > 0  => removeSome(items, product, amount, p.amount)
      case Some(_) if amount <= 0 => sendAndReturn(NumberLowerOrEqualToZero, items)
      case None                   => sendAndReturn(ProductNotInBasket, items)
    }
  }

  private def removeSome(items: Set[BasketProduct], product: ProductLike, amount: Long, currentCount: Long) = {
    def findProduct = items.find(_.product == product)
    def allExcludingProduct = items.filterNot(_.product == product)

    val finalItems =
      if (currentCount <= amount) allExcludingProduct
      else allExcludingProduct ++ findProduct.map(old => BasketProduct(old.product, old.amount - amount))
    sendAndReturn(Done, finalItems)
  }

  private def sendAndReturn(msg: ActorMessage,items: Set[BasketProduct]) = {
    sender() ! msg
    items
  }
}


object BasketActor {
  def props(userId: Long, catalog: ActorRef) =
    Props(new BasketActor(userId, catalog))
}
