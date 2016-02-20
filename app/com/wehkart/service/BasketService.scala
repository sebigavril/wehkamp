package com.wehkart.service

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import javax.inject.Inject
import akka.pattern.ask
import com.wehkamp.BasketActorFactory
import com.wehkart.domain.ShoppingProduct
import com.wehkart.service.BasketServiceProtocol._
import com.wehkart.ActorProtocol
import com.wehkart.ActorConstants.timeout


/**
  * Middleware intended to abstract the communication with the underlying actors
  */
class BasketService @Inject() (
  basketActorFactory: BasketActorFactory)(
  implicit ec: ExecutionContext) {

  def list(userId: Long): Future[Either[InternalError.type, Set[ShoppingProduct]]] = {
    (basketActorFactory.get(userId) ? ActorProtocol.ListAll)
      .map {
        case s: Set[_]  => Right(s.map(_.asInstanceOf[ShoppingProduct]))
        case _          => Left(InternalError)
      }
  }

  def put(userId: Long, productId: String, howMany: Long): Future[BasketServiceResponse] = {
    (basketActorFactory.get(userId) ? ActorProtocol.Add(productId, howMany))
      .map {
        case ActorProtocol.Done                     => Put
        case ActorProtocol.OutOfStock               => OutOfStock
        case ActorProtocol.StockNotEnough           => StockNotEnough
        case ActorProtocol.InvalidAmount => InvalidAmount
        case _                                      => InternalError
      }
  }

  def remove(userId: Long, productId: String, howMany: Long): Future[BasketServiceResponse] = {
    (basketActorFactory.get(userId) ? ActorProtocol.Remove(productId, howMany))
      .map {
        case ActorProtocol.Done                     => Deleted
        case ActorProtocol.ProductNotInBasket       => NotInBasket
        case ActorProtocol.InvalidAmount => InvalidAmount
        case _                                      => InternalError
      }
  }

  def removeAll(userId: Long): Future[BasketServiceResponse] = {
    (basketActorFactory.get(userId) ? ActorProtocol.RemoveAll)
      .map{
        case ActorProtocol.Done => Emptied
        case _                  => InternalError
      }
  }
}

/**
  * Abstracts the underlying messages that are shared between the actors.
  */
object BasketServiceProtocol {
  sealed trait BasketServiceResponse

  case object Put           extends BasketServiceResponse
  case object Deleted         extends BasketServiceResponse
  case object Emptied         extends BasketServiceResponse
  case object OutOfStock      extends BasketServiceResponse
  case object StockNotEnough  extends BasketServiceResponse
  case object InvalidAmount   extends BasketServiceResponse
  case object NotInBasket     extends BasketServiceResponse
  case object InternalError   extends BasketServiceResponse
}
