package com.wehkart.domain

/**
  * Mix this in to create product like objects
  */
trait ProductLike {
  def name: String
  def description: String
  def price: Double
}


sealed trait ProductExtras

/**
  * Mix this in to create phone like objects
  */
trait PhoneLike extends ProductExtras {
  def network: String
}

/**
  * Mix this in to create smart like objects
  */
trait SmartLike extends ProductExtras {
  def cores: Int
}
