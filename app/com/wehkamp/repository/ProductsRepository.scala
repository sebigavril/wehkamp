package com.wehkamp.repository

import com.wehkamp.domain.ShoppingProduct

/**
  * Repository like structure for catalog products.
  * Abstracts the underlying implementation.
  */
trait ProductsRepository {

  def initialProducts: Set[ShoppingProduct]
}