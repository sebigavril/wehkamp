package com.wehkart.repository

import com.wehkart.domain.CatalogProduct
import org.scalatest.{MustMatchers, WordSpecLike}
import InMemoryProducts._


class InMemoryRepositorySpec extends WordSpecLike with MustMatchers {

  "InMemoryRepository" must {
    "return correct products" in {
      InMemoryProducts.initialProducts mustEqual Set(
        CatalogProduct(iPad,      100),
        CatalogProduct(iPhone,    90),
        CatalogProduct(galaxyS,   200),
        CatalogProduct(candy, 1))
    }
  }
}
