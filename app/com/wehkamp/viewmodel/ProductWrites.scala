package com.wehkamp.viewmodel

import com.wehkamp.domain._
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes._
import play.api.libs.json._

object ProductWrites {

  implicit def shoppingProductWrites: Writes[ShoppingProduct] =
    (
      (__ \ 'id).write[Long] ~
        (__ \ 'product).write[ProductLike] ~
        (__ \ 'amount).write[Long]) (shoppingProduct => (
      shoppingProduct.id,
      shoppingProduct.product,
      shoppingProduct.amount))

  implicit def basketProductWrites: Writes[BasketProduct] =
    (
      (__ \ 'id).write[Long] ~
        (__ \ 'amount).write[Long]) (basketProduct => (
      basketProduct.id,
      basketProduct.amount))


  implicit private[viewmodel] def productLikeWriter: Writes[ProductLike] =
    (
      (__ \ 'name).write[String] ~
        (__ \ 'description).write[String] ~
        (__ \ 'price).write[Double] ~
        (__ \ 'cores).writeNullable[Int] ~
        (__ \ 'network).writeNullable[String]) (productLike => (
      productLike.name,
      productLike.description,
      productLike.price,
      smartLike(productLike),
      phoneLike(productLike)))


  private def smartLike(productLike: ProductLike) = productLike match {
    case p: SmartLike => Some(p.cores)
    case _ => None
  }

  private def phoneLike(productLike: ProductLike) = productLike match {
    case p: PhoneLike => Some(p.network)
    case _ => None
  }
}
