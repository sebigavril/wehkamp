package com.wehkamp.service

import scala.concurrent.Await
import akka.pattern.ask
import com.wehkamp.ActorConstants.{duration, timeout}
import com.wehkamp.{WithNextId, ActorContextBaseSpec}
import com.wehkamp.ActorProtocol.Basket._
import com.wehkamp.ActorProtocol._
import com.wehkamp.TestUtils._
import com.wehkamp.domain.BasketProduct
import org.scalatest.{MustMatchers, WordSpecLike}

class BasketActorSpec extends ActorContextBaseSpec
  with WordSpecLike
  with MustMatchers {

  "a Basket" must {

    "add one product" in WithNextId { id =>
      val res = Await.result(basket(id) ? Add(Set.empty, iPad.id, 1), duration).asInstanceOf[Done]

      res.products mustEqual Set(iPad.copy(amount = 1))
    }

    "not add same product twice" in WithNextId { id =>
      val res1 = Await.result(basket(id) ? Add(Set.empty, iPad.id, 1), duration).asInstanceOf[Done]
      val res2 = Await.result(basket(id) ? Add(Set(iPad.copy(amount = 1)), iPad.id, 1), duration).asInstanceOf[Done]

      res1.products mustEqual Set(iPad.copy(amount = 1))
      res2.products mustEqual Set(iPad.copy(amount = 1))
    }

    "return error when asked to add zero products" in WithNextId { id =>
      val res = basket(id) ? Add(Set.empty, iPad.id, 0)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
    }

    "return error when asked to add a negative number of products" in WithNextId { id =>
      val res = basket(id) ? Add(Set.empty, iPad.id, -1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
    }

    "return not enough stock when asked to add a number of products greater than the stock" in WithNextId { id =>
      val res = basket(id) ? Add(Set.empty, iPad.id, 1000)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual StockNotEnough
    }

    "return out of stock when asked to add a product that has stock 0" in WithNextId { id =>
      val res1 = basket(id) ? Add(Set.empty, candy.id, 1)
      val actual1 = Await.result(res1, duration).asInstanceOf[Done]
      val res2 = basket(id) ? Add(Set(candy.copy(amount = 1)), candy.id, 1)
      val actual2 = Await.result(res2, duration).asInstanceOf[ActorMessage]

      actual1.products mustEqual Set(candy.copy(amount = 1))
      actual2 mustEqual OutOfStock
    }

    "remove one product" in WithNextId { id =>
      basket(id) ! Add(Set.empty, iPad.id, 2)
      val res = Await.result(basket(id) ? Remove(Set(iPad.copy(amount = 2)), iPad.id, 1), duration).asInstanceOf[Done]

      res.products mustEqual Set(iPad.copy(amount = 1))
    }

    "remove all products" in WithNextId { id =>
      basket(id) ! Add(Set.empty, iPad.id, 1)
      basket(id) ! Add(Set(iPad.copy(amount = 1)), iPhone.id, 2)
      val res1 = Await.result(basket(id) ? Remove(Set(iPad.copy(amount = 1), iPhone.copy(amount = 2)), iPad.id, 1), duration).asInstanceOf[Done]
      val res2 = Await.result(basket(id) ? Remove(Set(iPhone.copy(amount = 2)), iPhone.id, 99), duration).asInstanceOf[Done]

      res1.products mustEqual Set(iPhone.copy(amount = 2))
      res2.products mustEqual Set.empty[BasketProduct]
    }

    "return error when asked to remove a negative number of products" in WithNextId { id =>
      basket(id) ! Add(Set.empty, iPad.id, 1)
      val res = basket(id) ? Add(Set(iPad.copy(amount = 1)), iPad.id, -1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
    }

    "do nothing when asked to remove a product that does not exist" in WithNextId { id =>
      val res = basket(id) ? Remove(Set.empty, iPad.id, 1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual ProductNotInBasket
    }
  }

  private def basket(id: Long) = actorContext(id).basketActor()
}
