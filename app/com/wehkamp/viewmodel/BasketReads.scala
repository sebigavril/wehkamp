package com.wehkamp.viewmodel

import java.util.UUID
import com.wehkamp.domain.{PlainProduct, ProductLike, ShoppingProduct}
import play.api.libs.functional.syntax._
import play.api.libs.json._


object BasketReads {

  implicit def basketProductReads: Reads[ShoppingProduct] = (
    (__ \ 'id).readNullable[String] and
      (__ \ 'product).read[ProductLike] and
      (__ \ 'amount).read[Long]) ((id, product, amount) =>
    new ShoppingProduct(id.getOrElse(UUID.randomUUID().toString), product, amount))

  implicit private [viewmodel] def productReads: Reads[ProductLike] = (
    (__ \ 'name).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'price).read[Double]) (PlainProduct)

}
