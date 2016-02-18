package com.wehkart.domain

import java.util.UUID

/**
  * Entity that represents a products that is placed in a basket
 *
  * @param id      Uniquely identifies a product in the basket.
  *                The case class [[com.wehkart.domain.PlainProduct]] can be used for the same purpose,
  *                but passing by an id is easier (and is also specified in the requirements).
  * @param product The product in the basket
  * @param amount  The amount of products of this type in the basket
  */
case class BasketProduct(
  id: String,
  product: ProductLike,
  amount: Long)

object BasketProduct {
  def apply(product: ProductLike, count: Long) =
    new BasketProduct(
      UUID.randomUUID().toString,
      product,
      count)
}