package com.wehkart.service

import scala.concurrent.Await
import akka.actor.ActorSystem
import com.wehkart.ActorConstants.duration
import com.wehkart.service.BasketServiceProtocol.{InvalidAmount, Deleted}
import com.wehkart.{ExecutionContexts, ActorContextBaseSpec}
import com.wehkart.TestUtils.id
import com.wehkart.domain.ShoppingProduct
import com.wehkart.repository.InMemoryProducts._
import org.scalatest.{EitherValues, WordSpecLike, MustMatchers}
import com.wehkart.TestUtils.expectProducts

class BasketServiceSpec(_system: ActorSystem)
  extends ActorContextBaseSpec(_system)
  with WordSpecLike
  with MustMatchers
  with EitherValues {

  implicit private val ec = new ExecutionContexts().ec
  private val basketService = new BasketService

  val iPadId = id(iPad)
  val iPhoneId = id(iPhone)

  "a BasketService" must {

    "list all products" in {
      Await.result(basketService.list(1), duration) mustEqual Set.empty
    }

    "add multiple products to basket" in {
      val res = for {
        _   <- basketService.add(1, iPadId, 2)
        _   <- basketService.add(1, iPhoneId, 10)
        all <- basketService.list(1)
      } yield all

      val expected = Set(ShoppingProduct(iPad, 2), ShoppingProduct(iPhone, 10))
      expectProducts(Await.result(res, duration).right.value, expected)
    }

    "remove products from basket" in {
      val res = for {
        _       <- basketService.add(1, iPadId, 2)
        _       <- basketService.add(1, iPhoneId, 10)
        success <- basketService.remove(1, iPhoneId, 1)
        fail    <- basketService.remove(1, iPhoneId, 0)
      } yield (success, fail)

      Await.result(res.map(_._1), duration) mustEqual Deleted
      Await.result(res.map(_._2), duration) mustEqual InvalidAmount
    }
  }
}
