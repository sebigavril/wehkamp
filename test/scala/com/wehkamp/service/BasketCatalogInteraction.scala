package com.wehkamp.service

import com.wehkamp.ActorConstants.duration
import com.wehkamp.TestUtils.ec
import com.wehkamp.repository.InMemoryProducts._
import com.wehkamp.{ActorContextBaseSpec, WithNextId}
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}

import scala.concurrent.Await

class BasketCatalogInteraction extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers
  with OptionValues {

  "a Basket and Catalog" must {
    "add to basket and remove from catalog" in WithNextId { id =>
      val res = for {
        _           <- basketService(id).put(Set.empty, iPad.id, 10)
        catalogRes  <- catalogService(id).list
      } yield catalogRes

      Await.result(res, duration).find(_.id == iPad.id).value mustEqual iPad.copy(amount = 90)
    }

    "remove from basket and add to catalog" in WithNextId { id =>
      val res = for {
        _           <- basketService(id).put(Set.empty, iPhone.id, 50)
        _           <- basketService(id).remove(Set(iPhone.copy(amount = 50).toBasket), iPhone.id, 40)
        catalogRes  <- catalogService(id).list
      } yield catalogRes

      Await.result(res, duration).find(_.id == iPhone.id).value mustEqual iPhone.copy(amount = 80)
    }
  }
}
