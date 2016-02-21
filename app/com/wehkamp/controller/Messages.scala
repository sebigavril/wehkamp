package com.wehkamp.controller


object Messages {

  val ProductOutOfStock     = "Sorry, we're out of stock for this product"
  val ProductNotEnoughStock = "Sorry, we don't have enough stock for this product"
  val ProductInvalidAmount  = (amount: Long) => s"You're trying to add an invalid number of products: $amount"

  val ProductNotInBasket = "You're trying to delete a product that is not your basket"

  val GenericError = "Oups! We're sorry, something went terribly wrong..."
}
