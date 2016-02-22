package com.wehkamp

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import java.util.UUID
import javax.inject.Inject
import akka.actor._
import akka.util.Timeout
import com.wehkamp.repository.InMemoryProducts
import com.wehkamp.service.{BasketActor, BasketProduct, CatalogActor}

/**
  * Singleton responsible for starting the actors and their context
  */
class ActorContext @Inject() (system: ActorSystem) {

  implicit private val ec = new ExecutionContexts().ec

  /**
    * Singleton - don't want to have multiple catalogs
    */
  val catalogActor = {
    Await.result(
      createIfNotExists(path("Catalog"), system.actorOf(CatalogActor.props(InMemoryProducts), "Catalog")),
      ActorConstants.duration)
  }

  /**
    * Basket actor is stateless so every time someone asks for an actor, just create one
    */
  def basketActor(): ActorRef =
    system.actorOf(BasketActor.props(catalogActor), s"Basket-${UUID.randomUUID()}")

  private def createIfNotExists(path: String, default: => ActorRef) =
    system
      .actorSelection(path)
      .resolveOne(10 seconds)
      .recover { case _: ActorNotFound => default }

  private def path(name: String) = s"akka://${system.name}/user/$name"
}

object ActorProtocol {

  sealed trait ActorMessage

  //commands
  case object ListAll                                          extends ActorMessage
  case class  AddCatalog(productId: String, howMany: Long)     extends ActorMessage
  case class  RemoveCatalog(productId: String, howMany: Long)  extends ActorMessage

  case class  Add(products: Set[BasketProduct], productId: String, howMany: Long)     extends ActorMessage
  case class  Remove(products: Set[BasketProduct], productId: String, howMany: Long)  extends ActorMessage

  //ok
  case class Done(products: Set[BasketProduct]) extends ActorMessage

  //errors
  case object InvalidAmount       extends ActorMessage
  case object OutOfStock          extends ActorMessage
  case object StockNotEnough      extends ActorMessage
  case object ProductNotInBasket  extends ActorMessage
}

object ActorConstants {

  val actorSystemName = "ShoppingActorSystem"

  val duration = 2 seconds
  implicit val timeout = Timeout(duration)
}

