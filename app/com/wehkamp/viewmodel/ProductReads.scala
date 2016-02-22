package com.wehkamp.viewmodel

import java.util.UUID
import com.wehkamp.domain.{PlainProduct, ProductLike, ShoppingProduct}
import com.wehkamp.service.BasketProduct
import play.api.libs.functional.syntax._
import play.api.libs.json._


object ProductReads {

  implicit def shoppingProductReads: Reads[ShoppingProduct] = (
    (__ \ 'id).readNullable[String] and
      (__ \ 'product).read[ProductLike] and
      (__ \ 'amount).read[Long]) ((id, product, amount) =>
    new ShoppingProduct(id.getOrElse(UUID.randomUUID().toString), product, amount))

  implicit def basketProductReads: Reads[BasketProduct] = (
    (__ \ 'id).read[String] and
      (__ \ 'amount).read[Long]) ((id, amount) =>
    new BasketProduct(id, amount))

  implicit private [viewmodel] def productLikeReads: Reads[ProductLike] = (
    (__ \ 'name).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'price).read[Double]) (PlainProduct)

}