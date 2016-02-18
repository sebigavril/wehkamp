package com.wehkart.domain

/**
  * Algebraic data type representing a product in a catalog.
  */
case class CatalogProduct(
   product: ProductLike,
   amount: Long)
