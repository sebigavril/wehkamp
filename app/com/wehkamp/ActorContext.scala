package com.wehkamp

import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

import akka.actor._
import akka.util.Timeout
import com.wehkamp.ActorConstants.atomicInt
import com.wehkamp.domain.BasketProduct
import com.wehkamp.repository.InMemoryProducts
import com.wehkamp.service.{BasketActor, CatalogActor}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Singleton responsible for starting the actors and their context
  */
class ActorContext @Inject() (
  system: ActorSystem,
  suffix: String = "") {

  implicit private val ec = new ExecutionContexts().ec

  /**
    * Singleton - don't want to have multiple catalogs in the same context
    */
  val catalogActor = {
    val name = s"Catalog${if (suffix == "") "" else "-" + suffix}"
    Await.result(
      createIfNotExists(path(name), system.actorOf(CatalogActor.props(InMemoryProducts), name)),
      ActorConstants.duration)
  }

  /**
    * Basket actor is stateless so every time someone asks for an actor, just create one
    */
  def basketActor(): ActorRef =
    system.actorOf(
      BasketActor.props(catalogActor),
      s"Basket-${atomicInt.incrementAndGet()}")

  private def createIfNotExists(path: String, default: => ActorRef) =
    system
      .actorSelection(path)
      .resolveOne(10 seconds)
      .recover { case _: ActorNotFound => default }

  private def path(name: String) = s"akka://${system.name}/user/$name"
}

object ActorProtocol {

  sealed trait ActorMessage

  object Catalog {
    //commands
    case object ListAll                                 extends ActorMessage
    case class  List(basketProds: Set[BasketProduct])   extends ActorMessage
    case class  Add(productId: Long, howMany: Long)     extends ActorMessage
    case class  Remove(productId: Long, howMany: Long)  extends ActorMessage

    //ok
    case object Done  extends ActorMessage
  }

  object Basket {
    //commands
    case class Add(products: Set[BasketProduct], productId: Long, howMany: Long)    extends ActorMessage
    case class Remove(products: Set[BasketProduct], productId: Long, howMany: Long) extends ActorMessage

    //ok
    case class Done(products: Set[BasketProduct]) extends ActorMessage
  }

  //errors
  case object InvalidAmount       extends ActorMessage
  case object OutOfStock          extends ActorMessage
  case object StockNotEnough      extends ActorMessage
  case object ProductNotInBasket  extends ActorMessage
}

object ActorConstants {

  val actorSystemName = "ShoppingActorSystem"

  val atomicInt = new AtomicLong(1)

  val duration = 2 seconds
  implicit val timeout = Timeout(duration)
}

