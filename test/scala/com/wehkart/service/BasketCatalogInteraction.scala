package scala.com.wehkart.service

import scala.concurrent.Await
import com.wehkamp.BasketActorFactory
import com.wehkart.ActorConstants.duration
import com.wehkart.TestUtils._
import com.wehkart.domain.ShoppingProduct
import com.wehkart.repository.InMemoryProducts._
import com.wehkart.service.BasketService
import com.wehkart.{ActorContextBaseSpec, WithNextId}
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}
import service.CatalogService

class BasketCatalogInteraction extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers
  with OptionValues {

  private val basketService = new BasketService(new BasketActorFactory(actorContext))
  private val catalogService = new CatalogService(actorContext.catalogActor)

  private val iPadId = id(iPad)
  private val iPhoneId = id(iPhone)
  private val galaxySId = id(galaxyS)

  "a Basket and Catalog" must {
    "add to basket and remove from catalog" in WithNextId { userId =>
      val res = for {
        _           <- basketService.add(userId, iPadId, 10)
        catalogRes  <- catalogService.list
      } yield catalogRes

      Await.result(res, duration).find(_.id == iPadId).value mustEqual ShoppingProduct(iPadId, iPad, 90)
    }

    "remove from basket and add to catalog" in WithNextId { userId =>
      val res = for {
        _           <- basketService.add(userId, iPhoneId, 50)
        _           <- basketService.remove(userId, iPhoneId, 40)
        catalogRes  <- catalogService.list
      } yield catalogRes

      Await.result(res, duration).find(_.id == iPhoneId).value mustEqual ShoppingProduct(iPhoneId, iPhone, 80)
    }
  }
}
