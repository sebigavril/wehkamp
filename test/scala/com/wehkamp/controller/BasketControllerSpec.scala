package com.wehkamp.controller

import com.wehkamp.TestUtils._
import com.wehkamp.service.{BasketProduct, BasketService}
import com.wehkamp.viewmodel.ProductReads.basketProductReads
import com.wehkamp.{ActorContextBaseSpec, BasketActorFactory}
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

    "add one product in a basket" in new WithApplication {
      val addResult = basketController.put(iPad.id, 1)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
      status(addResult) mustEqual CREATED
      contentType(addResult) mustEqual Some(JSON)

      val products = Json.fromJson[Set[BasketProduct]](contentAsJson(addResult)).asOpt.value
      products mustEqual Set(BasketProduct(iPad.id, 1))
    }

    "not add an inexistent product in a basket" in new WithApplication {
      val addResult = basketController.put("DUMMY_ID", 1)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
      status(addResult) mustEqual NOT_FOUND
      contentType(addResult) mustEqual Some(JSON)
      contentAsString(addResult) mustEqual """"Sorry, we're out of stock for this product""""
    }

    "not add more products than in stock in a basket" in new WithApplication {
      val addResult = basketController.put(iPad.id, Int.MaxValue)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
      status(addResult) mustEqual NOT_FOUND
      contentType(addResult) mustEqual Some(JSON)
      contentAsString(addResult) mustEqual """"Sorry, we don't have enough stock for this product""""
    }

    "not add a negative number of products in a basket" in new WithApplication {
      val addResult = basketController.put(iPad.id, -1)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
      status(addResult) mustEqual BAD_REQUEST
      contentType(addResult) mustEqual Some(JSON)
      contentAsString(addResult) mustEqual """"You're trying to add an invalid number of products: -1""""
    }

    "delete one product from the basket" in new WithApplication {
      val res = for {
        _       <- basketController.put(iPad.id, 1)(FakeRequest(PUT, URL).withBody(emptyBasketProductJson))
        delete  <- basketController.delete(iPad.id, 1)(FakeRequest(DELETE, URL).withBody(basketProductJson(iPad.id, 1)))
      } yield delete

      status(res) mustEqual OK
      contentType(res) mustEqual Some(JSON)
      val products = Json.fromJson[Set[BasketProduct]](contentAsJson(res)).asOpt.value
      products.filter(_.id == iPad.id) mustEqual Set.empty[BasketProduct]
    }

    "not delete a product that is not in the basket" in new WithApplication {
      val deleteResult = basketController.delete(candy.id, 1)(FakeRequest(DELETE, URL).withBody(emptyBasketProductJson))

      status(deleteResult) mustEqual NOT_FOUND
      contentType(deleteResult) mustEqual Some(JSON)
      contentAsString(deleteResult) mustEqual """"You're trying to delete a product that is not your basket""""
    }

    "not delete a negative number of products" in new WithApplication {
      val deleteResult = basketController.delete(iPad.id, -1)(FakeRequest(DELETE, URL) withBody (emptyBasketProductJson))

      status(deleteResult) mustEqual BAD_REQUEST
      contentType(deleteResult) mustEqual Some(JSON)
      contentAsString(deleteResult) mustEqual """"You're trying to add an invalid number of products: -1""""
    }
  }

  private def basketService = new BasketService(new BasketActorFactory(actorContext))
  private def basketController = new BasketController(basketService)

  private def basketProductJson(id: String, amount: Long = 1) = {
    import com.wehkamp.viewmodel.ProductWrites.basketProductWriter
    Json.toJson(Set(BasketProduct(id, amount)))
  }

  private def emptyBasketProductJson = {
    import com.wehkamp.viewmodel.ProductWrites.basketProductWriter
    Json.toJson(Set.empty[BasketProduct])
  }

}
