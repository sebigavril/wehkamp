package com.wehkamp.service

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import javax.inject.{Named, Inject}
import akka.actor.ActorRef
import akka.pattern.ask
import com.wehkamp.ActorConstants.timeout
import com.wehkamp.ActorProtocol.{Catalog, Basket}
import com.wehkamp.ActorProtocol
import com.wehkamp.domain.{ShoppingProduct, BasketProduct}
import com.wehkamp.service.BasketServiceProtocol._


/**
  * Middleware intended to abstract the communication with the underlying actors
  */
class BasketService @Inject() (
  @Named("basketActor") basketActor: ActorRef,
  @Named("catalogActor") catalogActor: ActorRef)(
  implicit ec: ExecutionContext) {

  def put(products: Set[BasketProduct], productId: Long, howMany: Long): Future[BasketServiceResponse] = {
    (basketActor ? Basket.Add(products, productId, howMany))
      .flatMap {
        case Basket.Done(basketProds)     => toShopping(basketProds, shoppingProds => Put(shoppingProds))
        case ActorProtocol.OutOfStock     => Future.successful(OutOfStock)
        case ActorProtocol.StockNotEnough => Future.successful(StockNotEnough)
        case ActorProtocol.InvalidAmount  => Future.successful(InvalidAmount)
        case _                            => Future.successful(InternalError)
      }
  }

  def remove(products: Set[BasketProduct], productId: Long, howMany: Long): Future[BasketServiceResponse] = {
    (basketActor ? Basket.Remove(products, productId, howMany))
      .flatMap {
        case Basket.Done(basketProds)         => toShopping(basketProds, shoppingProds => Deleted(shoppingProds))
        case ActorProtocol.ProductNotInBasket => Future.successful(NotInBasket)
        case ActorProtocol.InvalidAmount      => Future.successful(InvalidAmount)
        case _                                => Future.successful(InternalError)
      }
  }

  private def toShopping(newProducts: Set[BasketProduct], f: Set[ShoppingProduct] => BasketServiceResponse) =
    (catalogActor ? Catalog.List(newProducts))
      .map {
        case p: Set[_] => f(p.map(_.asInstanceOf[ShoppingProduct]))
        case _ => InternalError
      }
}

/**
  * Abstracts the underlying messages that are shared between the actors.
  */
object BasketServiceProtocol {
  sealed trait BasketServiceResponse

  case class Put(products: Set[ShoppingProduct])      extends BasketServiceResponse
  case class Deleted(products: Set[ShoppingProduct])  extends BasketServiceResponse
  case object Emptied                                 extends BasketServiceResponse
  case object OutOfStock                              extends BasketServiceResponse
  case object StockNotEnough                          extends BasketServiceResponse
  case object InvalidAmount                           extends BasketServiceResponse
  case object NotInBasket                             extends BasketServiceResponse
  case object InternalError                           extends BasketServiceResponse
}
