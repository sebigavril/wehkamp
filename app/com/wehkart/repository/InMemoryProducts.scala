package com.wehkart.repository

import com.wehkart.domain._

/**
  * Just a place to keep products instances...
  */
object InMemoryProducts extends ProductsRepository {

  override val initialProducts = Set(
    ShoppingProduct(iPad,    100),
    ShoppingProduct(iPhone,  90),
    ShoppingProduct(galaxyS, 200),
    ShoppingProduct(candy,   1))

  lazy val iPad = new PlainProduct("iPad", "makes you look cool", 999.0) with SmartLike {
    override val cores = 4
  }
  lazy val iPhone = new PlainProduct("iPhone", "also makes you look cool", 999.0) with SmartLike with PhoneLike {
    override val cores = 2
    override val network = "Vodafone"
  }
  lazy val galaxyS = new PlainProduct("Samsung Galaxy S", "pretty good", 700.0) with SmartLike with PhoneLike {
    override val cores = 2
    override val network = "Orange"
  }
  lazy val candy = new PlainProduct("Candy", "just candy", 10.0)
}