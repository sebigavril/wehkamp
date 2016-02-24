package com.wehkamp

import TestUtils.ec
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.wehkamp.controller.{BasketController, CatalogController}
import com.wehkamp.service.{BasketService, CatalogService}
import org.scalatest._

import scala.concurrent.Await

abstract class ActorContextBaseSpec(_system: ActorSystem)
  extends TestKit(_system)
    with ImplicitSender
    with Suite
    with BeforeAndAfterAll {

  def this() = this(WithNextId{ id => ActorSystem("TestActorSystem")})

  val actorContext = (id: Long) => new ActorContext(_system, id.toString)

  override def afterAll: Unit = {
    system.terminate()
    Await.result(system.whenTerminated, ActorConstants.duration)
  }

  protected def basket(id: Long) = actorContext(id).basketActor()
  protected def basketService(id: Long) = new BasketService(basket(id), catalog(id))
  protected def basketController(id: Long) = new BasketController(basketService(id))

  protected def catalog(id: Long) = actorContext(id).catalogActor
  protected def catalogService(id: Long) = new CatalogService(catalog(id))
  protected def catalogController(id: Long) = new CatalogController(catalogService(id))

}