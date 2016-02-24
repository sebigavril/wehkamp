package com.wehkamp.domain

/**
  * Entity representing the products in a clients basket.
  * Since a client can theoretically have all products in the catalog in his basket,
  * this entity only contains the minimum set of information needed.
  */
case class BasketProduct(
  id: Long,
  amount: Long)
