package com.wehkart

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import akka.actor._
import akka.util.Timeout
import com.wehkart.repository.InMemoryProducts
import com.wehkart.service.{BasketActor, CatalogActor}

/**
  * Singleton responsible for starting the actors and their context
  */
object ActorContext {

  private lazy val actorSystemName = "ShoppingActorSystem"
  private lazy val system = ActorSystem(actorSystemName)

  implicit private val ec = new ExecutionContexts().ec

  /**
    * Singleton - don't want to have multiple catalogs
    */
  val catalogActor = system.actorOf(CatalogActor.props(InMemoryProducts), "Catalog")

  def basketActor(userId: Long)(implicit ec: ExecutionContext): ActorRef = {
    val name = s"Basket-$userId"
    val path = s"akka://$actorSystemName/user/$name"

    val actorRef = system
      .actorSelection(path)
      .resolveOne(10 seconds)
      .recover { case _: ActorNotFound => system.actorOf(BasketActor.props(userId, catalogActor), name) }

    Await.result(actorRef, ActorConstants.duration)
  }
}

object ActorProtocol {

  sealed trait ActorMessage

  //commands
  case object ListAll                                  extends ActorMessage
  case class  Add(productId: String, howMany: Long)    extends ActorMessage
  case class  Remove(productId: String, howMany: Long) extends ActorMessage
  case object RemoveAll                                extends ActorMessage

  //ok
  case object Done extends ActorMessage

  //errors
  case object InvalidAmount       extends ActorMessage
  case object OutOfStock          extends ActorMessage
  case object StockNotEnough      extends ActorMessage
  case object ProductNotInBasket  extends ActorMessage
}

object ActorConstants {
  val duration = 5 seconds
  implicit val timeout = Timeout(duration)
}

