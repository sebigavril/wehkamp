package com.wehkamp

import scala.concurrent.Await
import java.util.concurrent.atomic.AtomicLong
import akka.actor.{ActorSystem, ActorRef}
import akka.pattern.ask
import com.wehkamp.ActorConstants.{duration, timeout}
import com.wehkamp.ActorProtocol.ListAll
import com.wehkamp.domain.{ProductLike, ShoppingProduct}
import com.wehkamp.repository.InMemoryProducts
import org.scalatest.MustMatchers

object TestUtils extends MustMatchers {

  implicit val ec = new ExecutionContexts().ec

  val actorSystem = ActorSystem("TestActorSystem")
  val actorContext = new ActorContext(actorSystem)


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

object WithNextId {

  private val atomicInt = new AtomicLong(1)

  def apply[A](fn: Long => A) = fn(atomicInt.incrementAndGet)
}
