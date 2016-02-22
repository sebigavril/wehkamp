package com.wehkamp.controller

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import com.wehkamp.controller.Messages._
import com.wehkamp.domain.BasketProduct
import com.wehkamp.service.BasketServiceProtocol._
import com.wehkamp.service.BasketService
import com.wehkamp.viewmodel.ProductReads.basketProductReads
import com.wehkamp.viewmodel.ProductWrites.shoppingProductWrites
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, Controller}

class BasketController @Inject() (
  basketService: BasketService)(
  implicit ec: ExecutionContext) extends Controller {

  def put(productId: Long, amount: Long) = Action.async(parse.tolerantJson) {
    implicit request =>
      request.body.validate[Set[BasketProduct]].fold(
        errors => {
          Future.successful(BadRequest(JsError.toJson(errors)))
        },
        basketProducts => {
          basketService
            .put(basketProducts, productId, amount)
            .map {
              case Put(products)  => Created(Json.toJson(products))
              case OutOfStock     => NotFound(Json.toJson(ProductOutOfStock))
              case StockNotEnough => NotFound(Json.toJson(ProductNotEnoughStock))
              case InvalidAmount  => BadRequest(Json.toJson(ProductInvalidAmount(amount)))
              case _              => InternalServerError(Json.toJson(GenericError))
            }
        }
      )
  }

  def delete(productId: Long, amount: Long) = Action.async(parse.tolerantJson) {
    implicit request =>
      request.body.validate[Set[BasketProduct]].fold(
        errors => {
          Future.successful(BadRequest(JsError.toJson(errors)))
        },
        basketProducts => {
          basketService
            .remove(basketProducts, productId, amount)
            .map {
              case Deleted(products)  => Ok(Json.toJson(products))
              case NotInBasket        => NotFound(Json.toJson(ProductNotInBasket))
              case InvalidAmount      => BadRequest(Json.toJson(ProductInvalidAmount(amount)))
              case _                  => InternalServerError(Json.toJson(GenericError))
            }
        }
      )
  }
}
