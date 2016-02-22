package com.wehkamp.viewmodel

import java.util.UUID
import com.wehkamp.domain.{BasketProduct, PlainProduct, ProductLike, ShoppingProduct}
import play.api.libs.functional.syntax._
import play.api.libs.json._


object ProductReads {

  implicit def shoppingProductReads: Reads[ShoppingProduct] = (
    (__ \ 'id).readNullable[Long] and
      (__ \ 'product).read[ProductLike] and
      (__ \ 'amount).read[Long]) ((id, product, amount) =>
    id.map {i => ShoppingProduct(i, product, amount)}.getOrElse(ShoppingProduct(product, amount)))

  implicit def basketProductReads: Reads[BasketProduct] = (
    (__ \ 'id).read[Long] and
      (__ \ 'amount).read[Long]) ((id, amount) =>
    new BasketProduct(id, amount))

  implicit private [viewmodel] def productLikeReads: Reads[ProductLike] = (
    (__ \ 'name).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'price).read[Double]) (PlainProduct)

}
