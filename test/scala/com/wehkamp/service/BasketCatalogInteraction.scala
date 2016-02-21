package com.wehkamp.service

import scala.concurrent.Await
import akka.actor.ActorSystem
import com.wehkamp.{ActorContext, BasketActorFactory, ActorContextBaseSpec, WithNextId}
import com.wehkamp.ActorConstants.duration
import com.wehkamp.TestUtils._
import com.wehkamp.domain.ShoppingProduct
import com.wehkamp.repository.InMemoryProducts._
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}

class BasketCatalogInteraction extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers
  with OptionValues {

  private val actorSystem = ActorSystem("InteractionActorSystem")
  private val actorContext = new ActorContext(actorSystem)
  private val basketService = new BasketService(new BasketActorFactory(actorContext))
  private val catalogService = new CatalogService(actorContext.catalogActor)

  private val iPadId = id(iPad)
  private val iPhoneId = id(iPhone)

  "a Basket and Catalog" must {
    "add to basket and remove from catalog" in WithNextId { userId =>
      val res = for {
        _           <- basketService.put(userId, iPadId, 10)
        catalogRes  <- catalogService.list
      } yield catalogRes

      Await.result(res, duration).find(_.id == iPadId).value mustEqual ShoppingProduct(iPadId, iPad, 90)
    }

    "remove from basket and add to catalog" in WithNextId { userId =>
      val res = for {
        _           <- basketService.put(userId, iPhoneId, 50)
        _           <- basketService.remove(userId, iPhoneId, 40)
        catalogRes  <- catalogService.list
      } yield catalogRes

      Await.result(res, duration).find(_.id == iPhoneId).value mustEqual ShoppingProduct(iPhoneId, iPhone, 80)
    }
  }
}
