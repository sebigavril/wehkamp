package com.wehkamp.service

import scala.concurrent.Await
import com.wehkamp.ActorConstants.duration
import com.wehkamp.ActorContextBaseSpec
import com.wehkamp.TestUtils._
import com.wehkamp.service.BasketServiceProtocol._
import org.scalatest.{EitherValues, MustMatchers, WordSpecLike}

class BasketServiceSpec extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers
  with EitherValues {

  private val basketService = new BasketService(actorContext(1).basketActor(), actorContext(1).catalogActor)

  "a BasketService" must {

    "add multiple products to basket" in {
      val res = for {
        res1 <- basketService.put(Set.empty, iPad.id, 2).map(_.asInstanceOf[Put])
        res2 <- basketService.put(Set(iPad.copy(amount = 2)), iPhone.id, 10).map(_.asInstanceOf[Put])
      } yield (res1, res2)

      Await.result(res, duration)._1.products.map(_.toBasket) mustEqual Set(iPad.copy(amount = 2))
      Await.result(res, duration)._2.products.map(_.toBasket) mustEqual Set(iPad.copy(amount = 2), iPhone.copy(amount = 10))
    }

    "return invalid amount when trying to put an amount that is smaller than the current amount" in {
      val res = for {
        res1 <- basketService.put(Set(iPad.copy(amount = 2)), iPad.id, 2).map(_.asInstanceOf[Put])
        res2 <- basketService.put(Set(iPad.copy(amount = 2)), iPad.id, 1).map(_.asInstanceOf[InvalidAmount.type])
      } yield (res1, res2)

      Await.result(res, duration)._1.products.map(_.toBasket) mustEqual Set(iPad.copy(amount = 2))
      Await.result(res, duration)._2 mustEqual InvalidAmount
    }

    "remove products from basket" in {
      val res = for {
        _       <- basketService.put(Set.empty, iPad.id, 2)
        _       <- basketService.put(Set(iPad.copy(amount = 2)), iPhone.id, 10)
        remove  <- basketService.remove(Set(iPad.copy(amount = 2), iPhone.copy(amount = 10)), iPhone.id, 1).map(_.asInstanceOf[Deleted])
      } yield remove

      Await.result(res, duration).products.map(_.toBasket) mustEqual Set(iPad.copy(amount = 2), iPhone.copy(amount = 9))
    }

    "add in an idempotent manner" in {
      val res = for {
        res1 <- basketService.put(Set.empty, iPad.id, 2).map(_.asInstanceOf[Put])
        res2 <- basketService.put(Set.empty, iPad.id, 2).map(_.asInstanceOf[Put])
      } yield (res1, res2)

      Await.result(res, duration)._1.products.map(_.toBasket) mustEqual Set(iPad.copy(amount = 2))
      Await.result(res, duration)._2.products.map(_.toBasket) mustEqual Set(iPad.copy(amount = 2))
    }
  }
}
