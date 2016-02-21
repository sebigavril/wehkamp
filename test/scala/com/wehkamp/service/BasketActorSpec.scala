package com.wehkamp.service

import scala.concurrent.Await
import akka.pattern.ask
import com.wehkamp.ActorConstants.duration
import com.wehkamp.ActorProtocol._
import com.wehkamp.TestUtils.{expectProducts, id, _}
import com.wehkamp.domain.ShoppingProduct
import com.wehkamp.repository.InMemoryProducts._
import com.wehkamp.{ActorContextBaseSpec, WithNextId}
import org.scalatest.{MustMatchers, WordSpecLike}
import com.wehkamp.ActorConstants.timeout

class BasketActorSpec extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers {

  private val iPadId = id(iPad)
  private val iPhoneId = id(iPhone)
  private val candyId = id(candy)

  "a Basket" must {

    "list all products" in WithNextId { userId =>
      val basket = buildBasket(userId)

      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "add one product" in WithNextId { userId =>
      val basket = buildBasket(userId)
      basket ! Add(iPadId, 1)

      expectProducts(basket, Set(ShoppingProduct(iPad, 1)))
    }

    "add same product twice" in WithNextId { userId =>
      val basket = buildBasket(userId)
      basket ! Add(iPadId, 1)
      basket ! Add(iPadId, 1)

      expectProducts(basket, Set(ShoppingProduct(iPad, 2)))
    }

    "return error when asked to add zero products" in WithNextId { userId =>
      val basket = buildBasket(userId)
      val res = basket ? Add(iPadId, 0)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "return error when asked to add a negative number of products" in WithNextId { userId =>
      val basket = buildBasket(userId)
      val res = basket ? Add(iPadId, -1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "return not enough stock when asked to add a number of products greater than the stock" in WithNextId { userId =>
      val basket = buildBasket(userId)
      val res = basket ? Add(iPadId, 1000)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual StockNotEnough
      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "return out of stock when asked to add a product that has stock 0" in WithNextId { userId =>
      val basket = buildBasket(userId)
      val res1 = basket ? Add(candyId, 1)
      val actual1 = Await.result(res1, duration).asInstanceOf[ActorMessage]
      val res2 = basket ? Add(candyId, 1)
      val actual2 = Await.result(res2, duration).asInstanceOf[ActorMessage]

      actual1 mustEqual Done
      actual2 mustEqual OutOfStock
      expectProducts(basket, Set(ShoppingProduct(candy, 1)))
    }

    "remove one product" in WithNextId { userId =>
      val basket = buildBasket(userId)
      basket ! Add(iPadId, 2)
      basket ! Remove(iPadId, 1)

      expectProducts(basket, Set(ShoppingProduct(iPad, 1)))
    }

    "remove all products" in WithNextId { userId =>
      val basket = buildBasket(userId)
      basket ! Add(iPadId, 1)
      basket ! Add(iPhoneId, 2)
      basket ! Remove(iPadId, 1)
      basket ! Remove(iPhoneId, 99)

      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "return error when asked to remove a negative number of products" in WithNextId { userId =>
      val basket = buildBasket(userId)
      basket ! Add(iPadId, 1)
      val res = basket ? Add(iPadId, -1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
      expectProducts(basket, Set(ShoppingProduct(iPad, 1)))
    }

    "do nothing when asked to remove a product that does not exist" in WithNextId { userId =>
      val basket = buildBasket(userId)
      val res = basket ? Remove(iPadId, 1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual ProductNotInBasket
      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "remove all" in WithNextId { userId =>
      val basket = buildBasket(userId)
      basket ! Add(iPadId, 1)
      basket ! Add(iPhoneId, 1)
      val res = basket ? RemoveAll
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual Done
      expectProducts(basket, Set.empty[ShoppingProduct])
    }
  }

  private def buildBasket(userId: Long) = actorContext.basketActor(userId)
}
