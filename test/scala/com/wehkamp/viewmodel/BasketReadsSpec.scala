package com.wehkamp.viewmodel

import com.wehkamp.domain.ShoppingProduct
import com.wehkamp.repository.InMemoryProducts.iPad
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}
import play.api.libs.json.Json

class BasketReadsSpec extends WordSpecLike with MustMatchers with OptionValues {

  "a BasketReads" must {
    "read a ProductLike object " in {
      import com.wehkamp.viewmodel.ProductReads.productLikeReads
      val json = Json.parse(
        """
          | {
          |   "name":"iPad",
          |   "description":"makes you look cool",
          |   "price":999
          | }
          | """.stripMargin)
      val productLike = Json.fromJson(json).asOpt.value

      productLike.name mustEqual iPad.product.name
      productLike.description mustEqual iPad.product.description
      productLike.price mustEqual iPad.product.price
    }

    "read a BasketProduct object " in {
      import com.wehkamp.viewmodel.ProductReads.shoppingProductReads
      val json = Json.parse(
        """
          | {
          |   "id":"1",
          |   "product": {
          |       "name":"iPad",
          |       "description":"makes you look cool",
          |       "price":999
          |    },
          |    "amount":2
          | }
          | """.stripMargin)

      val basketProduct = Json.fromJson[ShoppingProduct](json).asOpt.value

      basketProduct.id mustEqual "1"
      basketProduct.product.name mustEqual iPad.product.name
      basketProduct.amount mustEqual 2
    }
  }

}
