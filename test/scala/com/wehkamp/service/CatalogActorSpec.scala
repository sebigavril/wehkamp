package com.wehkamp.service

import akka.pattern.ask
import com.wehkamp.ActorProtocol.Catalog._
import com.wehkamp.ActorConstants.timeout
import com.wehkamp.TestUtils.expectProducts
import com.wehkamp.domain.{PlainProduct, ShoppingProduct}
import com.wehkamp.repository.InMemoryProducts
import com.wehkamp.repository.InMemoryProducts._
import com.wehkamp.{ActorContextBaseSpec, WithNextId}
import org.scalatest.{MustMatchers, WordSpecLike}

class CatalogActorSpec extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers {

  "a Catalog" must {

    "list all products" in WithNextId { id =>
      val catalog = buildCatalog(id)

      expectProducts(catalog, initialProducts)
    }

    "remove one item from the catalog" in WithNextId { id =>
      val catalog = buildCatalog(id)
      catalog ? Remove(iPad.id, 1)

      expectProducts(catalog, initialProducts.map {
        case ShoppingProduct(_, PlainProduct("iPad", _, _), _) => iPad.copy(amount = 99)
        case cp => cp
      })
    }

    "remove all items of a kind from the catalog" in WithNextId { id =>
      val catalog = buildCatalog(id)
      (1 to 100).par.foreach(_ => catalog ! Remove(iPad.id, 1))

      expectProducts(catalog, initialProducts.map {
        case ShoppingProduct(_, PlainProduct("iPad", _, _), _) => iPad.copy(amount = 0)
        case cp => cp
      })
    }

    "remove all items of a kind except one from the catalog" in WithNextId { id =>
      val catalog = buildCatalog(id)
      (1 to 99).par.foreach(_ => catalog ! Remove(iPad.id, 1))

      expectProducts(catalog, initialProducts.map {
        case ShoppingProduct(_, PlainProduct("iPad", _, _), _) => iPad.copy(amount = 1)
        case cp => cp
      })
    }
  }

  private def buildCatalog(id: Long) = system.actorOf(CatalogActor.props(InMemoryProducts), s"Catalog-$id")
}
