package com.wehkamp.repository

import com.wehkamp.domain.ShoppingProduct
import org.scalatest.{MustMatchers, WordSpecLike}
import InMemoryProducts._


class InMemoryRepositorySpec extends WordSpecLike with MustMatchers {

  "an InMemoryRepository" must {
    "return correct products" in {
      InMemoryProducts.initialProducts.map(p => (p.product, p.amount)) mustEqual
        Set(
          ShoppingProduct(iPad,      100),
          ShoppingProduct(iPhone,    90),
          ShoppingProduct(galaxyS,   200),
          ShoppingProduct(candy, 1)).map(p => (p.product, p.amount))
    }
  }
}
