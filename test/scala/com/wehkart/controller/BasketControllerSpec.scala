package com.wehkart.controller

import akka.actor.ActorSystem
import com.wehkart.{ExecutionContexts, ActorContextBaseSpec}
import com.wehkart.TestUtils.{expectProducts, id}
import com.wehkart.domain.ShoppingProduct
import com.wehkart.repository.InMemoryProducts.{iPad, candy}
import com.wehkart.service.BasketService
import com.wehkart.viewmodel.BasketReads.basketProductReads
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}
import play.api.http.MimeTypes._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import play.test.WithApplication

class BasketControllerSpec(_system: ActorSystem)
  extends ActorContextBaseSpec(_system)
    with PlayRunners
    with WordSpecLike
    with MustMatchers
    with OptionValues {

  def this() = this(ActorSystem(s"BasketControllerSpec"))

  private val iPadId = id(iPad)
  private val candyId = id(candy)

  "BasketController" must {
    "list all products in a basket" in new WithApplication {
      new BasketService().add(1, iPadId, 10)
      val result = basketController.list(1).apply(FakeRequest(GET, "/api/shoppingbasket/:userId"))
      val products = Json.fromJson[Set[ShoppingProduct]](contentAsJson(result)).asOpt.value

      status(result) mustEqual OK
      contentType(result) mustEqual Some(JSON)
      expectProducts(products, Set(ShoppingProduct(iPad, 10)))
    }

    "add one product in a basket" in new WithApplication {
      val addResult = basketController.add(1, iPadId, 1).apply(FakeRequest(POST, "/api/shoppingbasket/:userId"))
      status(addResult) mustEqual CREATED
      contentType(addResult) mustEqual None

      val getResult = basketController.list(1).apply(FakeRequest(GET, "/api/shoppingbasket/:userId"))
      val products = Json.fromJson[Set[ShoppingProduct]](contentAsJson(getResult)).asOpt.value

      status(getResult) mustEqual OK
      contentType(getResult) mustEqual Some(JSON)
      expectProducts(products, Set(ShoppingProduct(iPad, 11)))
    }

    "not add an inexistent product in a basket" in new WithApplication {
      val addResult = basketController.add(1, "DUMMY_ID", 1).apply(FakeRequest(POST, "/api/shoppingbasket/:userId"))
      status(addResult) mustEqual NOT_FOUND
      contentType(addResult) mustEqual Some(JSON)
      contentAsString(addResult) mustEqual """"Sorry, we're out of stock for this product""""
    }

    "not add more products than in stock in a basket" in new WithApplication {
      val addResult = basketController.add(1, iPadId, Int.MaxValue).apply(FakeRequest(POST, "/api/shoppingbasket/:userId"))
      status(addResult) mustEqual NOT_FOUND
      contentType(addResult) mustEqual Some(JSON)
      contentAsString(addResult) mustEqual """"Sorry, we don't have enough stock for this product""""
    }

    "not add a negative number of products in a basket" in new WithApplication {
      val addResult = basketController.add(1, iPadId, -1).apply(FakeRequest(POST, "/api/shoppingbasket/:userId"))
      status(addResult) mustEqual BAD_REQUEST
      contentType(addResult) mustEqual Some(JSON)
      contentAsString(addResult) mustEqual """"You're trying to add an invalid number of products: -1""""
    }

    "delete one product from the basket" in new WithApplication {
      val res = for {
        _       <- basketController.add(1, candyId, 1).apply(FakeRequest(POST, "/api/shoppingbasket/:userId"))
        delete  <- basketController.delete(1, candyId, 1).apply(FakeRequest(DELETE, "/api/shoppingbasket/:userId"))
        get     <- basketController.list(1).apply(FakeRequest(GET, "/api/shoppingbasket/:userId"))
      } yield (delete, get)

      val deleteRes = res.map(_._1)
      val getRes = res.map(_._2)

      status(deleteRes) mustEqual OK
      contentType(deleteRes) mustEqual None
      val products = Json.fromJson[Set[ShoppingProduct]](contentAsJson(getRes)).asOpt.value
      expectProducts(products.filter(_.id == candyId), Set.empty[ShoppingProduct])
    }

    "not delete a product that is not in the basket" in new WithApplication {
      val deleteResult = basketController.delete(1, candyId, 1).apply(FakeRequest(DELETE, "/api/shoppingbasket/:userId"))

      status(deleteResult) mustEqual NOT_FOUND
      contentType(deleteResult) mustEqual Some(JSON)
      contentAsString(deleteResult) mustEqual """"You're trying to delete a product that is not your basket""""
    }

    "not delete a negative number of products" in new WithApplication {
      val deleteResult = basketController.delete(1, iPadId, -1).apply(FakeRequest(DELETE, "/api/shoppingbasket/:userId"))

      status(deleteResult) mustEqual BAD_REQUEST
      contentType(deleteResult) mustEqual Some(JSON)
      contentAsString(deleteResult) mustEqual """"You're trying to add an invalid number of products: -1""""
    }
  }

  implicit private val ec = new ExecutionContexts().ec
  private def basketController = new BasketController(new BasketService())

}
