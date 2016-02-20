package com.wehkart.service

import scala.concurrent.Await
import com.wehkamp.BasketActorFactory
import com.wehkart.ActorConstants.duration
import com.wehkart.TestUtils.{id, _}
import com.wehkart.domain.ShoppingProduct
import com.wehkart.repository.InMemoryProducts._
import com.wehkart.service.BasketServiceProtocol.{Deleted, Emptied, InvalidAmount}
import com.wehkart.{ActorContextBaseSpec, WithNextId}
import org.scalatest.{EitherValues, MustMatchers, WordSpecLike}

class BasketServiceSpec extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers
  with EitherValues {

  private val basketService = new BasketService(new BasketActorFactory(actorContext))

  private val iPadId = id(iPad)
  private val iPhoneId = id(iPhone)

  "a BasketService" must {

    "list all products" in WithNextId { userId =>
      Await.result(basketService.list(userId), duration).right.value mustEqual Set.empty
    }

    "add multiple products to basket" in WithNextId { userId =>
      val res = for {
        _   <- basketService.put(userId, iPadId, 2)
        _   <- basketService.put(userId, iPhoneId, 10)
        all <- basketService.list(userId)
      } yield all

      val expected = Set(ShoppingProduct(iPad, 2), ShoppingProduct(iPhone, 10))
      expectProducts(Await.result(res, duration).right.value, expected)
    }

    "remove products from basket" in WithNextId { userId =>
      val res = for {
        _       <- basketService.put(userId, iPadId, 2)
        _       <- basketService.put(userId, iPhoneId, 10)
        success <- basketService.remove(userId, iPhoneId, 1)
        fail    <- basketService.remove(userId, iPhoneId, 0)
      } yield (success, fail)

      Await.result(res.map(_._1), duration) mustEqual Deleted
      Await.result(res.map(_._2), duration) mustEqual InvalidAmount
    }

    "remove all products from basket" in WithNextId { userId =>
      val res = for {
        _       <- basketService.put(userId, iPadId, 2)
        _       <- basketService.put(userId, iPhoneId, 10)
        remove  <- basketService.removeAll(userId)
        list    <- basketService.list(userId)
      } yield (remove, list)

      val removeRes = res.map(_._1)
      val listRes = res.map(_._2)

      Await.result(removeRes, duration) mustEqual Emptied
      Await.result(listRes, duration).right.value mustEqual Set.empty
    }
  }
}
