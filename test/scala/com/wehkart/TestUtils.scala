package com.wehkart

import scala.concurrent.Await
import akka.actor.ActorRef
import akka.pattern.ask
import com.wehkart.ActorConstants.{duration, timeout}
import com.wehkart.ActorProtocol.ListAll
import com.wehkart.domain.{ProductLike, ShoppingProduct}
import com.wehkart.repository.InMemoryProducts
import org.scalatest.MustMatchers

object TestUtils extends MustMatchers {

  def id(p: ProductLike): String = InMemoryProducts.initialProducts.find(_.product == p).get.id

  def expectProducts(actor: ActorRef, expected: Set[ShoppingProduct]): Unit = {
    val res = actor ? ListAll
    val actual = Await.result(res, duration).asInstanceOf[Set[ShoppingProduct]]

    expectProducts(actual, expected)
  }

  def expectProducts(actual: Set[ShoppingProduct], expected: Set[ShoppingProduct]): Unit = {
    actual.map(_.product) mustEqual expected.map(_.product)
    actual.map(_.amount) mustEqual expected.map(_.amount)
  }
}
