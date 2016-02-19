package com.wehkart.service

import com.wehkart.{WithNextId, ActorContextBaseSpec}
import com.wehkart.ActorProtocol._
import com.wehkart.TestUtils._
import com.wehkart.domain.{PlainProduct, ShoppingProduct}
import com.wehkart.repository.InMemoryProducts
import com.wehkart.repository.InMemoryProducts._
import org.scalatest.{MustMatchers, WordSpecLike}

class CatalogActorSpec extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers {

  private val iPadId = id(iPad)

  "a Catalog" must {

    "list all products" in WithNextId { id =>
      val catalog = buildCatalog(id)

      expectProducts(catalog, initialProducts)
    }

    "remove one item from the catalog" in WithNextId { id =>
      val catalog = buildCatalog(id)
      catalog ! Remove(iPadId, 1)

      expectMsg(Done)
      expectProducts(catalog, initialProducts.map {
        case ShoppingProduct(_, PlainProduct("iPad", _, _), _) => ShoppingProduct(iPad, 99L)
        case cp => cp
      })
    }

    "remove all items of a kind from the catalog" in WithNextId { id =>
      val catalog = buildCatalog(id)
      (1 to 100).par.foreach(_ => catalog ! Remove(iPadId, 1))

      expectProducts(catalog, initialProducts.map {
        case ShoppingProduct(_, PlainProduct("iPad", _, _), _) => ShoppingProduct(iPad, 0L)
        case cp => cp
      })
    }

    "remove all items of a kind except one from the catalog" in WithNextId { id =>
      val catalog = buildCatalog(id)
      (1 to 99).par.foreach(_ => catalog ! Remove(iPadId, 1))

      expectProducts(catalog, initialProducts.map {
        case ShoppingProduct(_, PlainProduct("iPad", _, _), _) => ShoppingProduct(iPad, 1L)
        case cp => cp
      })
    }
  }

  private def buildCatalog(id: Long) = system.actorOf(CatalogActor.props(InMemoryProducts), s"Catalog-$id")
}
