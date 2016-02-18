package com.wehkart.service

import scala.concurrent.Await
import scala.concurrent.duration._
import java.util.UUID
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.wehkart.ActorProtocol._
import com.wehkart.domain.{PlainProduct, BasketProduct}
import com.wehkart.repository.InMemoryProducts
import com.wehkart.repository.InMemoryProducts._
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

class BasketActorSpec(_system: ActorSystem) extends TestKit(_system)
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
      val basket = buildBasket

      expectProducts(basket, Set.empty)
    }

    "add one product" in {
      val basket = buildBasket
      basket ! Add(iPad, 1)

      expectMsg(Done)
      expectProducts(basket, Set(BasketProduct(iPad, 1)))
    }

    "add same product twice" in {
      val basket = buildBasket
      basket ! Add(iPad, 1)
      basket ! Add(iPad, 1)

      expectMsg(Done)
      expectMsg(Done)
      expectProducts(basket, Set(BasketProduct(iPad, 2)))
    }

    "return error when asked to add zero products" in {
      val basket = buildBasket
      basket ! Add(iPad, 0)

      expectMsg(NumberLowerOrEqualToZero)
      expectProducts(basket, Set.empty)
    }

    "return error when asked to add a negative number of products" in {
      val basket = buildBasket
      basket ! Add(iPad, -1)

      expectMsg(NumberLowerOrEqualToZero)
      expectProducts(basket, Set.empty)
    }

    "return not enough stock when asked to add a number of products greater than the stock" in {
      val basket = buildBasket
      basket ! Add(iPad, 1000)

      expectMsg(StockNotEnough)
      expectProducts(basket, Set.empty)
    }

    "return out of stock when asked to add a product that has stock 0" in {
      val basket = buildBasket
      basket ! Add(candy, 1)
      basket ! Add(candy, 1)

      expectMsg(Done)
      expectMsg(OutOfStock)
      expectProducts(basket, Set(BasketProduct(candy, 1)))
    }

    "remove one product" in {
      val basket = buildBasket
      basket ! Add(iPad, 2)
      basket ! Remove(iPad, 1)

      expectMsg(Done)
      expectMsg(Done)
      expectProducts(basket, Set(BasketProduct(iPad, 1)))
    }

    "remove all products" in {
      val basket = buildBasket
      basket ! Add(iPad, 1)
      basket ! Add(iPhone, 2)
      basket ! Remove(iPad, 1)
      basket ! Remove(iPhone, 99)

      expectMsg(Done)
      expectMsg(Done)
      expectMsg(Done)
      expectMsg(Done)
      expectProducts(basket, Set.empty)
    }

    "return error when asked to remove a negative number of products" in {
      val basket = buildBasket
      basket ! Add(iPad, 1)
      basket ! Remove(iPad, -1)

      expectMsg(Done)
      expectMsg(NumberLowerOrEqualToZero)
      expectProducts(basket, Set(BasketProduct(iPad, 1)))
    }

    "do nothing when asked to remove a product that does not exist" in {
      val basket = buildBasket
      basket ! Remove(iPad, 0)

      expectMsg(ProductNotInBasket)
      expectProducts(basket, Set.empty)
    }
  }

  private lazy val catalogActor = system.actorOf(CatalogActor.props(InMemoryProducts), "Catalog")

  private def buildBasket = {
    TestActorRef(BasketActor.props(1, catalogActor), s"basket-${UUID.randomUUID()}")
  }

  private def expectProducts(actor: ActorRef, expected: Set[BasketProduct]) = {
    actor ! ListAll
    expectMsgPF() { case res: Set[BasketProduct] =>
      res.map(_.product) mustEqual expected.map(_.product)
      res.map(_.amount) mustEqual expected.map(_.amount)
    }
  }
}
