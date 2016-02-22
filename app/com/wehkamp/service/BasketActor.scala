package com.wehkamp.service

import scala.concurrent.{ExecutionContext, Await, Future}
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{PoisonPill, Actor, ActorRef, Props}
import akka.pattern.ask
import com.wehkamp.ActorConstants.{duration, timeout}
import com.wehkamp.ActorProtocol._
import com.wehkamp.domain.ShoppingProduct

/**
  * The basket that is attributed to a certain user.
  *
  */
private class BasketActor(
  catalog: ActorRef)(
  implicit  ec: ExecutionContext) extends Actor {

  override def receive: Receive = {
    case Add(items, productId, amount)    => add(items, productId, amount)
    case Remove(items, productId, amount) => remove(items, productId, amount)
  }

  /**
    * Since there is only one catalog, the different basket actors will need to share it.
    * Even though a synchronization is not explicit, this like will take more or less time to execute
    * depending on the number of basket actors that are accessing the catalog.
    *
    * A better approach is suggested in [[com.wehkamp.service.CatalogActor]]
    */
  private def add(items: Set[BasketProduct], productId: String, amount: Long) = {
    if (amount > 0) addValidAmount(items, productId, amount)
    else            sendAndReturn(InvalidAmount, items)
  }

  private def addValidAmount(items: Set[BasketProduct], productId: String, amount: Long) = {
    val productsInCatalog = (catalog ? ListAll).map(_.asInstanceOf[Set[ShoppingProduct]])

    val res = productsInCatalog.flatMap {
      _.find(_.id == productId) match {
        case Some(product)  => addProductIfStillAvailable(items, product, amount)
        case None           => Future.successful(sendAndReturn(OutOfStock, items))
      }
    }
    Await.result(res, duration)
  }

  private def addProductIfStillAvailable(items: Set[BasketProduct], product: ShoppingProduct, amount: Long) = {
    val catalogResponse = catalog ? RemoveCatalog(product.id, amount)
    catalogResponse.map {
      case OutOfStock     => sendAndReturn(OutOfStock, items)
      case StockNotEnough => sendAndReturn(StockNotEnough, items)
      case Done(_)        => addProduct(items, product, amount)
    }
  }

  private def addProduct(items: Set[BasketProduct], product: ShoppingProduct, amount: Long) = {
    def addNew(p: ShoppingProduct) =
      items + BasketProduct(p.id, amount)

    def addExisting(p: BasketProduct) =
      items.filterNot(_.id == p.id) + BasketProduct(p.id, p.amount + amount)

    items.find(_.id == product.id) match {
      case Some(existingProduct)  => sendAndReturn(Done(addExisting(existingProduct)), addExisting(existingProduct))
      case None                   => sendAndReturn(Done(addNew(product)), addNew(product))
    }
  }

  private def remove(items: Set[BasketProduct], productId: String, amount: Long) = {
    if (amount > 0) removeValidAmount(items, productId, amount)
    else            sendAndReturn(InvalidAmount, items)
  }

  private def removeValidAmount(items: Set[BasketProduct], productId: String, amount: Long) = {
    items.find(_.id == productId) match {
      case Some(p)  => removeExistingProduct(items, productId, amount, p.amount)
      case None     => sendAndReturn(ProductNotInBasket, items)
    }
  }

  private def removeExistingProduct(items: Set[BasketProduct], productId: String, amount: Long, currentAmount: Long) = {
    def findProduct                       = items.find(_.id == productId)
    def allExcludingProduct               = items.filterNot(_.id == productId)
    def removeAmount(p: BasketProduct)  = BasketProduct(p.id, p.amount - amount)

    catalog ! AddCatalog(productId, amount)

    val finalItems =
      if (amount >= currentAmount)  allExcludingProduct
      else                          allExcludingProduct ++ findProduct.map(removeAmount)
    sendAndReturn(Done(finalItems), finalItems)
  }

  private def removeAll(items: Set[ShoppingProduct]) = {
    items.foreach(p => catalog ! AddCatalog(p.id, p.amount))
    sendAndReturn(Done(Set.empty), Set.empty)
  }

  private def sendAndReturn(msg: ActorMessage,items: Set[BasketProduct]) = {
    sender() ! msg
    self ! Stop     // My work is done.
    items
  }
}


object BasketActor {
  def props(catalog: ActorRef)(implicit ec: ExecutionContext) = Props(new BasketActor(catalog))
}

case class BasketProduct(
  id: String,
  amount: Long)