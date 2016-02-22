package com.wehkamp.service

import scala.concurrent.Await
import akka.actor.ActorSystem
import com.wehkamp.ActorConstants.duration
import com.wehkamp.TestUtils.{expectProducts, ec}
import com.wehkamp.repository.InMemoryProducts._
import com.wehkamp.{ActorContext, ActorContextBaseSpec, BasketActorFactory}
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}

class BasketCatalogInteraction extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers
  with OptionValues {

  private val actorSystem = ActorSystem("InteractionActorSystem")
  private val actorContext = new ActorContext(actorSystem)
  private val basketService = new BasketService(new BasketActorFactory(actorContext))
  private val catalogService = new CatalogService(actorContext.catalogActor)

  "a Basket and Catalog" must {
    "add to basket and remove from catalog" in {
      val res = for {
        _           <- basketService.put(Set.empty, iPad.id, 10)
        catalogRes  <- catalogService.list
      } yield catalogRes

      Await.result(res, duration).find(_.id == iPad.id).value mustEqual iPad.copy(amount = 90)
    }

    "remove from basket and add to catalog" in {
      val res = for {
        _           <- basketService.put(Set.empty, iPhone.id, 50)
        _           <- basketService.remove(Set(iPhone.copy(amount = 50).toBasket), iPhone.id, 40)
        catalogRes  <- catalogService.list
      } yield catalogRes

      Await.result(res, duration).find(_.id == iPhone.id).value mustEqual iPhone.copy(amount = 80)
    }
  }
}
