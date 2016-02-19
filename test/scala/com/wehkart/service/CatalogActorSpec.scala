package com.wehkart.service

import java.util.UUID
import akka.actor.ActorSystem
import com.wehkart.ActorProtocol._
import com.wehkart.ActorContextBaseSpec
import com.wehkart.TestUtils.{expectProducts, id}
import com.wehkart.domain.{PlainProduct, ShoppingProduct}
import com.wehkart.repository.InMemoryProducts
import com.wehkart.repository.InMemoryProducts._
import org.scalatest.{WordSpecLike, MustMatchers}

class CatalogActorSpec(_system: ActorSystem)
  extends ActorContextBaseSpec(_system)
    with WordSpecLike
    with MustMatchers {

  def this() = this(ActorSystem("BasketSpec"))

  val iPadId = id(iPad)

  "a Catalog" must {

    "list all products" in {
      val catalog = buildCatalog

      expectProducts(catalog, initialProducts)
    }

    "remove one item from the catalog" in {
      val catalog = buildCatalog
      catalog ! Remove(iPadId, 1)

      expectMsg(Done)
      expectProducts(catalog, initialProducts.map {
        case ShoppingProduct(_, PlainProduct("iPad", _, _), _) => ShoppingProduct(iPad, 99L)
        case cp => cp
      })
    }

    "remove all items of a kind from the catalog" in {
      val catalog = buildCatalog
      (1 to 100).par.foreach(_ => catalog ! Remove(iPadId, 1))

      (1 to 100).par.foreach(_ => expectMsg(Done))
      expectProducts(catalog, initialProducts.filterNot(_.product == iPad))
    }

    "remove all items of a kind except one from the catalog" in {
      val catalog = buildCatalog
      (1 to 99).par.foreach(_ => catalog ! Remove(iPadId, 1))

      (1 to 99).par.foreach(_ => expectMsg(Done))
      expectProducts(catalog, initialProducts.map {
        case ShoppingProduct(_, PlainProduct("iPad", _, _), _) => ShoppingProduct(iPad, 1L)
        case cp => cp
      })
    }
  }

  private def buildCatalog = system.actorOf(CatalogActor.props(InMemoryProducts), s"Catalog-${UUID.randomUUID()}")
}
