package com.wehkart.repository

import com.wehkart.domain.CatalogProduct

/**
  * Repository like structure for catalog products.
  * Abstracts the underlying implementation.
  */
trait ProductsRepository {

  def initialProducts: Set[CatalogProduct]
}