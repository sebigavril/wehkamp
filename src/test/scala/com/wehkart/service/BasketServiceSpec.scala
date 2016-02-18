package com.wehkart.service

import scala.concurrent.Await
import scala.concurrent.duration._
import com.wehkart.ActorProtocol.{Done, NumberLowerOrEqualToZero}
import com.wehkart.ExecutionContexts.ctx
import com.wehkart.domain.BasketProduct
import com.wehkart.repository.InMemoryProducts._
import org.scalatest.{MustMatchers, WordSpecLike}

class BasketServiceSpec extends WordSpecLike with MustMatchers {

  private val timeout = 2 seconds
  private val basketService = new BasketService

  "a BasketService" must {

    "list all products" in {
      Await.result(basketService.list(1), timeout) mustEqual Set.empty
    }

    "add multiple products to basket" in {
      val res = for {
        _   <- basketService.add(1, iPad, 2)
        _   <- basketService.add(1, iPhone, 10)
        all <- basketService.list(1)
      } yield all

      val expected = Set(BasketProduct(iPad, 2), BasketProduct(iPhone, 10))
      Await.result(res, timeout).map(_.amount) mustEqual expected.map(_.amount)
      Await.result(res, timeout).map(_.product) mustEqual expected.map(_.product)
    }

    "remove products from basket" in {
      val res = for {
        _       <- basketService.add(1, iPad, 2)
        _       <- basketService.add(1, iPhone, 10)
        success <- basketService.remove(1, iPhone, 1)
        fail    <- basketService.remove(1, iPhone, 0)
      } yield (success, fail)

      Await.result(res.map(_._1), timeout) mustEqual Done
      Await.result(res.map(_._2), timeout) mustEqual NumberLowerOrEqualToZero
    }
  }
}
