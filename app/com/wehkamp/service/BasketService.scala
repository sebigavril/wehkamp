package com.wehkamp.service

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import javax.inject.Inject
import akka.pattern.ask
import com.wehkamp.BasketActorFactory
import com.wehkamp.domain.ShoppingProduct
import com.wehkamp.service.BasketServiceProtocol._
import com.wehkamp.ActorProtocol.Basket
import com.wehkamp.ActorProtocol
import com.wehkamp.ActorConstants.timeout


/**
  * Middleware intended to abstract the communication with the underlying actors
  */
class BasketService @Inject() (
  basketActorFactory: BasketActorFactory)(
  implicit ec: ExecutionContext) {

  def put(products: Set[BasketProduct], productId: String, howMany: Long): Future[BasketServiceResponse] = {
    (basketActorFactory.get() ? Basket.Add(products, productId, howMany))
      .map {
        case Basket.Done(p)        => Put(p)
        case ActorProtocol.OutOfStock     => OutOfStock
        case ActorProtocol.StockNotEnough => StockNotEnough
        case ActorProtocol.InvalidAmount  => InvalidAmount
        case _                            => InternalError
      }
  }

  def remove(products: Set[BasketProduct], productId: String, howMany: Long): Future[BasketServiceResponse] = {
    (basketActorFactory.get() ? Basket.Remove(products, productId, howMany))
      .map {
        case Basket.Done(p)            => Deleted(p)
        case ActorProtocol.ProductNotInBasket => NotInBasket
        case ActorProtocol.InvalidAmount      => InvalidAmount
        case _                                => InternalError
      }
  }
}

/**
  * Abstracts the underlying messages that are shared between the actors.
  */
object BasketServiceProtocol {
  sealed trait BasketServiceResponse

  case class Put(products: Set[BasketProduct])        extends BasketServiceResponse
  case class Deleted(products: Set[BasketProduct])    extends BasketServiceResponse
  case object Emptied                                 extends BasketServiceResponse
  case object OutOfStock                              extends BasketServiceResponse
  case object StockNotEnough                          extends BasketServiceResponse
  case object InvalidAmount                           extends BasketServiceResponse
  case object NotInBasket                             extends BasketServiceResponse
  case object InternalError                           extends BasketServiceResponse
}
