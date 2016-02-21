package com.wehkamp.controller

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import com.wehkamp.ActorProtocol.{Done, InvalidAmount}
import com.wehkamp.controller.Messages.{GenericError, ProductInvalidAmount}
import com.wehkamp.domain.ShoppingProduct
import com.wehkamp.service.CatalogService
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, Controller}

class CatalogController @Inject() (
  catalogService: CatalogService)(
  implicit ec: ExecutionContext) extends Controller {

  def list() = Action.async {
    import com.wehkamp.viewmodel.BasketWrites.basketProductWriter
    catalogService
      .list
      .map(products => Ok(Json.toJson(products)))
  }

  def add(amount: Int) = Action.async(parse.tolerantJson) {
    implicit request =>
      import com.wehkamp.viewmodel.BasketReads.basketProductReads
      request.body.validate[ShoppingProduct].fold(
        errors => {
          Future.successful(BadRequest(JsError.toJson(errors)))
        },
        basketProduct => {
          catalogService
            .add(basketProduct.product, basketProduct.amount)
            .map {
              case Done           => Created
              case InvalidAmount  => BadRequest(Json.toJson(ProductInvalidAmount(amount)))
              case _              => InternalServerError(Json.toJson(GenericError))
            }
        }
      )
  }
}
