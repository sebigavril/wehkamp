package com.wehkamp.service

import scala.concurrent.Await
import com.wehkamp.ActorConstants.duration
import com.wehkamp.{ActorContextBaseSpec, BasketActorFactory}
import com.wehkamp.TestUtils._
import com.wehkamp.service.BasketServiceProtocol._
import org.scalatest.{EitherValues, MustMatchers, WordSpecLike}

class BasketServiceSpec extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers
  with EitherValues {

  private val basketService = new BasketService(new BasketActorFactory(actorContext))

  "a BasketService" must {

    "add multiple products to basket" in {
      val res = for {
        res1 <- basketService.put(Set.empty, iPad.id, 2).map(_.asInstanceOf[Put])
        res2 <- basketService.put(Set(iPad.copy(amount = 2)), iPhone.id, 10).map(_.asInstanceOf[Put])
      } yield (res1, res2)

      Await.result(res, duration)._1.products mustEqual Set(iPad.copy(amount = 2))
      Await.result(res, duration)._2.products mustEqual Set(iPad.copy(amount = 2), iPhone.copy(amount = 10))
    }

    "remove products from basket" in {
      val res = for {
        _       <- basketService.put(Set.empty, iPad.id, 2)
        _       <- basketService.put(Set(iPad.copy(amount = 2)), iPhone.id, 10)
        remove  <- basketService.remove(Set(iPad.copy(amount = 2), iPhone.copy(amount = 10)), iPhone.id, 1).map(_.asInstanceOf[Deleted])
      } yield remove

      Await.result(res, duration).products mustEqual Set(iPad.copy(amount = 2), iPhone.copy(amount = 9))
    }

    "add must be idempotent" in {
      val res = for {
        res1 <- basketService.put(Set.empty, iPad.id, 2).map(_.asInstanceOf[Put])
        res2 <- basketService.put(Set.empty, iPad.id, 2).map(_.asInstanceOf[Put])
      } yield (res1, res2)

      Await.result(res, duration)._1.products mustEqual Set(iPad.copy(amount = 2))
      Await.result(res, duration)._2.products mustEqual Set(iPad.copy(amount = 2))
    }
  }
}
