package com.wehkart.viewmodel

import com.wehkart.domain.{ShoppingProduct, ProductLike}
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.Json
import com.wehkart.repository.InMemoryProducts.iPhone

class BasketWritesSpec extends WordSpecLike with MustMatchers {

  "BasketWrites" must {
    "write a ProductLike object " in {
      import com.wehkart.viewmodel.BasketWrites.productLikeWriter
      val product: ProductLike = iPhone
      val json = Json.toJson(product)

      (json \ "name").as[String] mustEqual "iPhone"
      (json \ "description").as[String] mustEqual "also makes you look cool"
      (json \ "price").as[Double] mustEqual 999.0
      (json \ "network").as[String] mustEqual "Vodafone"
      (json \ "cores").as[Int] mustEqual 2
    }

    "write a BasketProduct object " in {
      import com.wehkart.viewmodel.BasketWrites.basketProductWriter
      import com.wehkart.viewmodel.BasketReads.productReads
      val basketProduct = ShoppingProduct(iPhone, 10)
      val json = Json.toJson(basketProduct)

      (json \ "id").as[String] mustEqual basketProduct.id
      (json \ "product").as[ProductLike] mustEqual iPhone
      (json \ "amount").as[Long] mustEqual 10
    }
  }

}
