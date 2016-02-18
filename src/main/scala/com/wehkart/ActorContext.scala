package com.wehkart

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import akka.actor._
import com.wehkart.domain.ProductLike
import com.wehkart.repository.InMemoryProducts
import com.wehkart.service.{BasketActor, CatalogActor}

/**
  * Singleton responsible for starting the actors and their context
  */
object ActorContext {

  private lazy val actorSystemName = "ShoppingActorSystem"
  private lazy val system = ActorSystem(actorSystemName)

  /**
    * Singleton - don't want to have multiple catalogs
    */
  private val catalogActor = system.actorOf(CatalogActor.props(InMemoryProducts), "Catalog")

  def basketActor(userId: Long)(implicit ec: ExecutionContext): Future[ActorRef] = {
    val name = s"Basket-$userId"
    val path = s"akka://$actorSystemName/user/$name"

    system
      .actorSelection(path)
      .resolveOne(10 seconds)
      .recover { case _: ActorNotFound => system.actorOf(BasketActor.props(userId, catalogActor), name) }
  }
}

object ActorProtocol {

  sealed trait ActorMessage

  //commands
  case object ListAll                                    extends ActorMessage
  case class Add(product: ProductLike, howMany: Long)    extends ActorMessage
  // should probably remove by id?
  case class Remove(product: ProductLike, howMany: Long) extends ActorMessage

  //ok
  case object Done extends ActorMessage

  //errors
  case object NumberLowerOrEqualToZero  extends ActorMessage
  case object OutOfStock                extends ActorMessage
  case object StockNotEnough            extends ActorMessage
  case object ProductNotInBasket        extends ActorMessage
}

