package com.wehkart.controller

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import com.wehkart.ActorProtocol.{Done, InvalidAmount}
import com.wehkart.controller.Messages.ProductInvalidAmount
import com.wehkart.domain.ShoppingProduct
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, Controller}
import service.CatalogService

class CatalogController @Inject() (
  catalogService: CatalogService)(
  implicit ec: ExecutionContext) extends Controller {

  def list() = Action.async {
    import com.wehkart.viewmodel.BasketWrites.basketProductWriter
    catalogService
      .list
      .map(products => Ok(Json.toJson(products)))
  }

  def add(amount: Int) = Action.async(parse.tolerantJson) {
    implicit request =>
      import com.wehkart.viewmodel.BasketReads.basketProductReads
      request.body.validate[ShoppingProduct].fold(
        errors => {
          Future.successful(BadRequest(JsError.toJson(errors)))
        },
        basketProduct => {
          catalogService
            .add(basketProduct.product, basketProduct.amount)
            .map {
              case Done                     => Created
              case InvalidAmount => BadRequest(Json.toJson(ProductInvalidAmount(amount)))
            }
        }
      )
  }
}
