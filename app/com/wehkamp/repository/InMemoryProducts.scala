package com.wehkamp.repository

import com.wehkamp.domain._

/**
  * Just a place to keep products instances...
  */
object InMemoryProducts extends ProductsRepository {

  override val initialProducts = Set(iPad, iPhone, galaxyS, candy)

  lazy val iPad     = ShoppingProduct(_iPad,    100)
  lazy val iPhone   = ShoppingProduct(_iPhone,  90)
  lazy val galaxyS  = ShoppingProduct(_galaxyS, 200)
  lazy val candy    = ShoppingProduct(_candy,   1)

  private lazy val _iPad = new PlainProduct("iPad", "makes you look cool", 999.0) with SmartLike {
    override val cores = 4
  }
  private lazy val _iPhone = new PlainProduct("iPhone", "also makes you look cool", 999.0) with SmartLike with PhoneLike {
    override val cores = 2
    override val network = "Vodafone"
  }
  private lazy val _galaxyS = new PlainProduct("Samsung Galaxy S", "pretty good", 700.0) with SmartLike with PhoneLike {
    override val cores = 2
    override val network = "Orange"
  }
  private lazy val _candy = new PlainProduct("Candy", "just candy", 10.0)
}