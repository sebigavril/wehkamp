package com.wehkamp.service

import scala.concurrent.Await
import akka.pattern.ask
import com.wehkamp.ActorConstants.{duration, timeout}
import com.wehkamp.ActorContextBaseSpec
import com.wehkamp.ActorProtocol._
import com.wehkamp.TestUtils._
import com.wehkamp.repository.InMemoryProducts._
import org.scalatest.{MustMatchers, WordSpecLike}

class BasketActorSpec extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers {

  "a Basket" must {

    "add one product" in {
      val res = Await.result(basket ? Add(Set.empty, iPad.id, 1), duration).asInstanceOf[Done]

      res.products mustEqual Set(iPad.copy(amount = 1).toBasket)
    }

    "add same product twice" in {
      val res1 = Await.result(basket ? Add(Set.empty, iPad.id, 1), duration).asInstanceOf[Done]
      val res2 = Await.result(basket ? Add(Set(iPad.copy(amount = 1).toBasket), iPad.id, 1), duration).asInstanceOf[Done]

      res1.products mustEqual Set(iPad.copy(amount = 1).toBasket)
      res2.products mustEqual Set(iPad.copy(amount = 2).toBasket)
    }

    "return error when asked to add zero products" in {
      val res = basket ? Add(Set.empty, iPad.id, 0)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
    }

    "return error when asked to add a negative number of products" in {
      val res = basket ? Add(Set.empty, iPad.id, -1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
    }

    "return not enough stock when asked to add a number of products greater than the stock" in {
      val res = basket ? Add(Set.empty, iPad.id, 1000)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual StockNotEnough
    }

    "return out of stock when asked to add a product that has stock 0" in {
      val res1 = basket ? Add(Set.empty, candy.id, 1)
      val actual1 = Await.result(res1, duration).asInstanceOf[Done]
      val res2 = basket ? Add(Set(candy.copy(amount = 1).toBasket), candy.id, 1)
      val actual2 = Await.result(res2, duration).asInstanceOf[ActorMessage]

      actual1.products mustEqual Set(candy.copy(amount = 1).toBasket)
      actual2 mustEqual OutOfStock
    }

    "remove one product" in {
      basket ! Add(Set.empty, iPad.id, 2)
      val res = Await.result(basket ? Remove(Set(iPad.copy(amount = 2).toBasket), iPad.id, 1), duration).asInstanceOf[Done]

      res.products mustEqual Set(iPad.copy(amount = 1).toBasket)
    }

    "remove all products" in {
      basket ! Add(Set.empty, iPad.id, 1)
      basket ! Add(Set(iPad.copy(amount = 1).toBasket), iPhone.id, 2)
      val res1 = Await.result(basket ? Remove(Set(iPad.copy(amount = 1).toBasket, iPhone.copy(amount = 2).toBasket), iPad.id, 1), duration).asInstanceOf[Done]
      val res2 = Await.result(basket ? Remove(Set(iPhone.copy(amount = 2).toBasket), iPhone.id, 99), duration).asInstanceOf[Done]

      res1.products mustEqual Set(iPhone.copy(amount = 2).toBasket)
      res2.products mustEqual Set.empty[BasketProduct]
    }

    "return error when asked to remove a negative number of products" in {
      basket ! Add(Set.empty, iPad.id, 1)
      val res = basket ? Add(Set(iPad.copy(amount = 1).toBasket), iPad.id, -1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
    }

    "do nothing when asked to remove a product that does not exist" in {
      val res = basket ? Remove(Set.empty, iPad.id, 1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual ProductNotInBasket
    }
  }

  private def basket = actorContext.basketActor()
}
