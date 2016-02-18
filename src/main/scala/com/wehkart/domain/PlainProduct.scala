package com.wehkart.domain

/**
  * Algebraic data type representing a product.
  * A product is uniquely identified by the combination of its fields.
  */
case class PlainProduct(
  name: String,
  description: String,
  price: Double) extends ProductLike




