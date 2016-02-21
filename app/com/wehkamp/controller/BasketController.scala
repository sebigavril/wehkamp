package com.wehkamp.controller

import scala.concurrent.ExecutionContext
import javax.inject.Inject
import com.wehkamp.controller.Messages._
import com.wehkamp.service.BasketService
import com.wehkamp.service.BasketServiceProtocol._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

class BasketController @Inject() (
  basketService: BasketService)(
  implicit ec: ExecutionContext) extends Controller {

  def list(userId: Long) = Action.async {
    import com.wehkamp.viewmodel.BasketWrites.basketProductWriter
    basketService
      .list(userId)
      .map{
        case Right(products)      => Ok(Json.toJson(products))
        case Left(InternalError)  => InternalServerError(Json.toJson(GenericError))}
  }

  def put(userId: Long, productId: String, amount: Long) = Action.async {
    implicit request =>
      basketService
        .put(userId, productId, amount)
        .map {
          case Put          => Created
          case OutOfStock     => NotFound(Json.toJson(ProductOutOfStock))
          case StockNotEnough => NotFound(Json.toJson(ProductNotEnoughStock))
          case InvalidAmount  => BadRequest(Json.toJson(ProductInvalidAmount(amount)))
          case _              => InternalServerError(Json.toJson(GenericError))
        }
  }

  def delete(userId: Long, productId: String, amount: Long) = Action.async {
    implicit request =>
      basketService
        .remove(userId, productId, amount)
        .map {
          case Deleted        => Ok
          case NotInBasket    => NotFound(Json.toJson(ProductNotInBasket))
          case InvalidAmount  => BadRequest(Json.toJson(ProductInvalidAmount(amount)))
          case _              => InternalServerError(Json.toJson(GenericError))
        }
  }
}
