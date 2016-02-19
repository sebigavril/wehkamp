package com.wehkart.service

import scala.concurrent.{ExecutionContext, Await, Future}
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import com.wehkart.ActorConstants.{duration, timeout}
import com.wehkart.ActorProtocol._
import com.wehkart.domain.ShoppingProduct

/**
  * The basket that is attributed to a certain user.
  *
  * @param userId The id of the user that owns this basket.
  */
private class BasketActor(
  userId: Long,
  catalog: ActorRef)(
  implicit  ec: ExecutionContext) extends Actor {

  override def receive: Receive = active(Set.empty)

  private def active(items: Set[ShoppingProduct]): Receive = {
    case ListAll                   => sender() ! items
    case Add(productId, amount)    => context become active(add(items, productId, amount))
    case Remove(productId, amount) => context become active(remove(items, productId, amount))
    case RemoveAll                 => context become active(removeAll)
  }

  /**
    * Since there is only one catalog, the different basket actors will need to share it.
    * Even though a synchronization is not explicit, this like will take more or less time to execute
    * depending on the number of basket actors that are accessing the catalog.
    *
    * A better approach is suggested in [[com.wehkart.service.CatalogActor]]
    */
  private def add(items: Set[ShoppingProduct], productId: String, amount: Long) = {
    if (amount > 0) addValidAmount(items, productId, amount)
    else            sendAndReturn(InvalidAmount, items)
  }

  private def addValidAmount(items: Set[ShoppingProduct], productId: String, amount: Long) = {
    val productsInCatalog = (catalog ? ListAll).map(_.asInstanceOf[Set[ShoppingProduct]])

    val res = productsInCatalog.flatMap {
      _.find(_.id == productId) match {
        case Some(product)  => addProductIfStillAvailable(items, product, amount)
        case None           => Future.successful(sendAndReturn(OutOfStock, items))
      }
    }
    Await.result(res, duration)
  }

  private def addProductIfStillAvailable(items: Set[ShoppingProduct], product: ShoppingProduct, amount: Long) = {
    val catalogResponse = catalog ? Remove(product.id, amount)
    catalogResponse.map {
      case OutOfStock     => sendAndReturn(OutOfStock, items)
      case StockNotEnough => sendAndReturn(StockNotEnough, items)
      case Done           => addProduct(items, product, amount)
    }
  }

  private def addProduct(items: Set[ShoppingProduct], product: ShoppingProduct, amount: Long) = {
    def addNew(p: ShoppingProduct) =
      items + ShoppingProduct.from(p, Some(amount))

    def addExisting(p: ShoppingProduct) =
      items.filterNot(_.id == p.id) + ShoppingProduct.from(p, Some(p.amount + amount))

    items.find(_.id == product.id) match {
      case Some(existingProduct)  => sendAndReturn(Done, addExisting(existingProduct))
      case None                   => sendAndReturn(Done, addNew(product))
    }
  }

  private def remove(items: Set[ShoppingProduct], productId: String, amount: Long) = {
    if (amount > 0) removeValidAmount(items, productId, amount)
    else            sendAndReturn(InvalidAmount, items)
  }

  private def removeValidAmount(items: Set[ShoppingProduct], productId: String, amount: Long) = {
    items.find(_.id == productId) match {
      case Some(p)  => removeExistingProduct(items, productId, amount, p.amount)
      case None     => sendAndReturn(ProductNotInBasket, items)
    }
  }

  private def removeExistingProduct(items: Set[ShoppingProduct], productId: String, amount: Long, currentAmount: Long) = {
    def findProduct                       = items.find(_.id == productId)
    def allExcludingProduct               = items.filterNot(_.id == productId)
    def removeAmount(p: ShoppingProduct)  = ShoppingProduct.from(p, Some(p.amount - amount))

    val finalItems =
      if (amount >= currentAmount)  allExcludingProduct
      else                          allExcludingProduct ++ findProduct.map(removeAmount)
    sendAndReturn(Done, finalItems)
  }

  private def removeAll = sendAndReturn(Done, Set.empty)

  private def sendAndReturn(msg: ActorMessage,items: Set[ShoppingProduct]) = {
    sender() ! msg
    items
  }
}


object BasketActor {
  def props(userId: Long, catalog: ActorRef)(implicit ec: ExecutionContext) =
    Props(new BasketActor(userId, catalog))
}
