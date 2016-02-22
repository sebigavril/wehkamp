package com.wehkamp.service

import scala.concurrent.{ExecutionContext, Await, Future}
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import com.wehkamp.ActorConstants.{duration, timeout}
import com.wehkamp.ActorProtocol.Catalog
import com.wehkamp.ActorProtocol.Basket._
import com.wehkamp.ActorProtocol._
import com.wehkamp.domain.{BasketProduct, ShoppingProduct}

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
  private def add(items: Set[BasketProduct], productId: Long, amount: Long) = {
    if (amount > 0) addValidAmount(items, productId, amount)
    else            sendAndReturn(InvalidAmount, items)
  }

  private def addValidAmount(items: Set[BasketProduct], productId: Long, amount: Long) = {
    val productsInCatalog = (catalog ? Catalog.ListAll).map(_.asInstanceOf[Set[ShoppingProduct]])

    val res = productsInCatalog.flatMap {
      _.find(_.id == productId) match {
        case None                     => Future.successful(sendAndReturn(OutOfStock, items))
        case Some(p) if p.amount == 0 => Future.successful(sendAndReturn(OutOfStock, items))
        case Some(p)                  => addIfStillAvailable(items, p, amount)
      }
    }
    Await.result(res, duration)
  }

  private def addIfStillAvailable(items: Set[BasketProduct], catalogProduct: ShoppingProduct, amount: Long) = {
    items.find(_.id == catalogProduct.id) match {
      case Some(p) if p.amount == amount  => Future.successful(sendAndReturn(Done(items), items))
      case Some(p)                        => addIfNotAlreadyIn(items, catalogProduct, amount - p.amount)
      case None                           => addIfNotAlreadyIn(items, catalogProduct, amount)
    }
  }

  private def addIfNotAlreadyIn(items: Set[BasketProduct], catalogProduct: ShoppingProduct, amount: Long) = {
    val catalogResponse = catalog ? Catalog.Remove(catalogProduct.id, amount)
    catalogResponse.map {
      case InvalidAmount  => sendAndReturn(InvalidAmount, items)
      case OutOfStock     => sendAndReturn(OutOfStock, items)
      case StockNotEnough => sendAndReturn(StockNotEnough, items)
      case Catalog.Done   => addProduct(items, catalogProduct, amount)
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

  private def remove(items: Set[BasketProduct], productId: Long, amount: Long) = {
    if (amount > 0) removeValidAmount(items, productId, amount)
    else            sendAndReturn(InvalidAmount, items)
  }

  private def removeValidAmount(items: Set[BasketProduct], productId: Long, amount: Long) = {
    items.find(_.id == productId) match {
      case Some(p)  => removeExistingProduct(items, productId, amount, p.amount)
      case None     => sendAndReturn(ProductNotInBasket, items)
    }
  }

  private def removeExistingProduct(items: Set[BasketProduct], productId: Long, amount: Long, currentAmount: Long) = {
    def findProduct                       = items.find(_.id == productId)
    def allExcludingProduct               = items.filterNot(_.id == productId)
    def removeAmount(p: BasketProduct)  = BasketProduct(p.id, p.amount - amount)

    catalog ! Catalog.Add(productId, amount)

    val finalItems =
      if (amount >= currentAmount)  allExcludingProduct
      else                          allExcludingProduct ++ findProduct.map(removeAmount)
    sendAndReturn(Done(finalItems), finalItems)
  }

  private def sendAndReturn(msg: ActorMessage, items: Set[BasketProduct]) = {
    sender() ! msg
    self ! Stop     // My work is done.
    items
  }
}


object BasketActor {
  def props(catalog: ActorRef)(implicit ec: ExecutionContext) = Props(new BasketActor(catalog))
}

