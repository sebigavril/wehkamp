package com.wehkamp.domain

import java.util.concurrent.atomic.AtomicLong

/**
  * Entity that represents a products that can be shopped.
  * It can be used either in the catalog or in the basket.
  *
  * @param id      Uniquely identifies a product in the basket.
  *                The case class [[com.wehkamp.domain.PlainProduct]] can be used for the same purpose,
  *                but passing by an id is easier (and is also specified in the requirements).
  * @param product The product in the basket
  * @param amount  The amount of products of this type in the basket
  */
case class ShoppingProduct(
  id: Long,
  product: ProductLike,
  amount: Long) {

  def toBasket = BasketProduct(id, amount)
}

object ShoppingProduct {

  val incrementer = new AtomicLong

  def apply(p: ProductLike, count: Long) =
    new ShoppingProduct(
      incrementer.incrementAndGet(),
      p,
      count)

  def from(p: ShoppingProduct, amount: Option[Long] = None) =
    new ShoppingProduct(
      p.id,
      p.product,
      amount.getOrElse(p.amount)
    )
}