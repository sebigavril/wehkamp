package com.wehkart.service

import scala.concurrent.Await
import scala.concurrent.duration._
import java.util.UUID
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.wehkart.ActorProtocol._
import com.wehkart.domain.{CatalogProduct, PlainProduct}
import com.wehkart.repository.InMemoryProducts
import com.wehkart.repository.InMemoryProducts._
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

class CatalogActorSpec(_system: ActorSystem) extends TestKit(_system)
  with ImplicitSender
  with WordSpecLike
  with MustMatchers
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("BasketSpec"))

  override def afterAll: Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 10.seconds)
  }

  "a Basket" must {

    "list all products" in {
      val catalog = buildCatalog

      expectProducts(catalog, InMemoryProducts.initialProducts)
    }

    "remove one item from the catalog" in {
      val catalog = buildCatalog
      catalog ! Remove(iPad, 1)

      expectMsg(Done)
      expectProducts(catalog, InMemoryProducts.initialProducts.map {
        case CatalogProduct(PlainProduct("iPad", _, _), _) => CatalogProduct(iPad, 99L)
        case cp => cp
      })
    }

    "remove all items of a kind from the catalog" in {
      val catalog = buildCatalog
      (1 to 100).par.foreach(_ => catalog ! Remove(iPad, 1))

      (1 to 100).par.foreach(_ => expectMsg(Done))
      expectProducts(catalog, InMemoryProducts.initialProducts.filterNot(_.product == iPad))
    }

    "remove all items of a kind except one from the catalog" in {
      val catalog = buildCatalog
      (1 to 99).par.foreach(_ => catalog ! Remove(iPad, 1))

      (1 to 99).par.foreach(_ => expectMsg(Done))
      expectProducts(catalog, InMemoryProducts.initialProducts.map {
        case CatalogProduct(PlainProduct("iPad", _, _), _) => CatalogProduct(iPad, 1L)
        case cp => cp
      })
    }
  }

  private def buildCatalog = system.actorOf(CatalogActor.props(InMemoryProducts), s"catalog-${UUID.randomUUID()}")

  private def expectProducts(actor: ActorRef, expected: Set[CatalogProduct]) = {
    actor ! ListAll
    expectMsg(expected)
  }
}
