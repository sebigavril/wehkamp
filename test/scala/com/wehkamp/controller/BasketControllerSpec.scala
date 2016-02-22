package com.wehkamp.controller

import com.wehkamp.{WithNextId, ActorContextBaseSpec}
import com.wehkamp.TestUtils._
import com.wehkamp.domain.{BasketProduct, ShoppingProduct}
import com.wehkamp.repository.InMemoryProducts
import com.wehkamp.service.{BasketService, CatalogService}
import com.wehkamp.viewmodel.ProductReads._
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}
import play.api.http.MimeTypes._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import play.test.WithApplication

class BasketControllerSpec extends ActorContextBaseSpec
    with PlayRunners
    with WordSpecLike
    with MustMatchers
    with OptionValues {

  private val URL = "/api/shoppingbasket"

  "a BasketController" must {

    "add one product in a basket" in WithNextId { id =>
      new WithApplication {
        val addResult = basketController(id).put(iPad.id, 1)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
        status(addResult) mustEqual CREATED
        contentType(addResult) mustEqual Some(JSON)

        val products = Json.fromJson[Set[BasketProduct]](contentAsJson(addResult)).asOpt.value
        products mustEqual Set(BasketProduct(iPad.id, 1))
      }
    }

    "not add an inexistent product in a basket" in WithNextId { id =>
      new WithApplication {
        val addResult = basketController(id).put(12345, 1)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
        status(addResult) mustEqual NOT_FOUND
        contentType(addResult) mustEqual Some(JSON)
        contentAsString(addResult) mustEqual """"Sorry, we're out of stock for this product""""
      }
    }

    "not add more products than in stock in a basket" in WithNextId { id =>
      new WithApplication {
        val addResult = basketController(id).put(iPad.id, Int.MaxValue)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
        status(addResult) mustEqual NOT_FOUND
        contentType(addResult) mustEqual Some(JSON)
        contentAsString(addResult) mustEqual """"Sorry, we don't have enough stock for this product""""
      }
    }

    "not add a negative number of products in a basket" in WithNextId { id =>
      new WithApplication {
        val addResult = basketController(id).put(iPad.id, -1)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
        status(addResult) mustEqual BAD_REQUEST
        contentType(addResult) mustEqual Some(JSON)
        contentAsString(addResult) mustEqual """"You're trying to add an invalid number of products: -1""""
      }
    }

    "be idempotent for put" in WithNextId { id =>
      new WithApplication {
        val addResult1 = basketController(id).put(iPhone.id, 10)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
        val addResult2 = basketController(id).put(iPhone.id, 10)(FakeRequest(PUT, URL).withBody(basketProductJson(iPhone.id, 10)))
        val listResult = catalogController(id).list()(FakeRequest(GET, "api/catalog"))

        status(addResult1) mustEqual CREATED
        contentType(addResult1) mustEqual Some(JSON)
        status(addResult2) mustEqual CREATED
        contentType(addResult2) mustEqual Some(JSON)

        val products1 = Json.fromJson[Set[BasketProduct]](contentAsJson(addResult1)).asOpt.value
        val products2 = Json.fromJson[Set[BasketProduct]](contentAsJson(addResult2)).asOpt.value
        val listProducts = Json.fromJson[Set[ShoppingProduct]](contentAsJson(listResult)).asOpt.value
        products1 mustEqual Set(BasketProduct(iPhone.id, 10))
        products1 mustEqual Set(BasketProduct(iPhone.id, 10))
        expectProducts(
          listProducts.filter(_.id == iPhone.id),
          InMemoryProducts.initialProducts.filter(_.id == iPhone.id).map(_.copy(amount = 80)))
      }
    }

    "delete one product from the basket" in WithNextId { id =>
      new WithApplication {
        val res = for {
          _ <- basketController(id).put(iPad.id, 1)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
          delete <- basketController(id).delete(iPad.id, 1)(FakeRequest(DELETE, URL).withBody(basketProductJson(iPad.id, 1)))
        } yield delete

        status(res) mustEqual OK
        contentType(res) mustEqual Some(JSON)
        val products = Json.fromJson[Set[BasketProduct]](contentAsJson(res)).asOpt.value
        products.filter(_.id == iPad.id) mustEqual Set.empty[BasketProduct]
      }
    }

    "not delete a product that is not in the basket" in WithNextId { id =>
      new WithApplication {
        val deleteResult = basketController(id).delete(candy.id, 1)(FakeRequest(DELETE, URL).withBody(emptyBasketProductJson))

        status(deleteResult) mustEqual NOT_FOUND
        contentType(deleteResult) mustEqual Some(JSON)
        contentAsString(deleteResult) mustEqual """"You're trying to delete a product that is not your basket""""
      }
    }

    "not delete a negative number of products" in WithNextId { id =>
      new WithApplication {
        val deleteResult = basketController(id).delete(iPad.id, -1)(FakeRequest(DELETE, URL) withBody (emptyBasketProductJson))

        status(deleteResult) mustEqual BAD_REQUEST
        contentType(deleteResult) mustEqual Some(JSON)
        contentAsString(deleteResult) mustEqual """"You're trying to add an invalid number of products: -1""""
      }
    }
  }

  private def basketService(id: Long) = new BasketService(actorContext(id).basketActor(), actorContext(id).catalogActor)
  private def basketController(id: Long) = new BasketController(basketService(id))

  private def catalogService(id: Long) = new CatalogService(actorContext(id).catalogActor)
  private def catalogController(id: Long) = new CatalogController(catalogService(id))

  private def basketProductJson(id: Long, amount: Long = 1) = {
    import com.wehkamp.viewmodel.ProductWrites.basketProductWrites
    Json.toJson(Set(BasketProduct(id, amount)))
  }

  private def emptyBasketProductJson = {
    import com.wehkamp.viewmodel.ProductWrites.basketProductWrites
    Json.toJson(Set.empty[BasketProduct])
  }

}
