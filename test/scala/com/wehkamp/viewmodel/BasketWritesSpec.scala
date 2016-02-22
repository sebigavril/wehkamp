package com.wehkamp.viewmodel

import com.wehkamp.domain.{ShoppingProduct, ProductLike}
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.Json
import com.wehkamp.repository.InMemoryProducts.iPhone

class BasketWritesSpec extends WordSpecLike with MustMatchers {

  "a BasketWrites" must {
    "write a ProductLike object " in {
      import com.wehkamp.viewmodel.ProductWrites.productLikeWriter
      val product: ProductLike = iPhone.product
      val json = Json.toJson(product)

      (json \ "name").as[String] mustEqual "iPhone"
      (json \ "description").as[String] mustEqual "also makes you look cool"
      (json \ "price").as[Double] mustEqual 999.0
      (json \ "network").as[String] mustEqual "Vodafone"
      (json \ "cores").as[Int] mustEqual 2
    }

    "write a BasketProduct object " in {
      import com.wehkamp.viewmodel.ProductWrites.shoppingProductWriter
      import com.wehkamp.viewmodel.ProductReads.productLikeReads
      val json = Json.toJson(iPhone.copy(amount = 10))

      (json \ "id").as[String] mustEqual iPhone.id
      (json \ "product").as[ProductLike] mustEqual iPhone.product
      (json \ "amount").as[Long] mustEqual 10
    }
  }

}
