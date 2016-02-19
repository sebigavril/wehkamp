package com.wehkamp

import scala.concurrent.ExecutionContext
import javax.inject.Inject
import akka.actor.{ActorSystem, ActorRef}
import com.wehkart.service.BasketService
import com.wehkart.{ActorContext, ExecutionContexts}
import play.api.inject._
import play.api.{Configuration, Environment}
import service.CatalogService
import com.wehkart.ActorConstants.actorSystemName

class AppModule extends Module {

  implicit private val ec = new ExecutionContexts().ec

  private val system = ActorSystem(actorSystemName)
  private val actorContext = new ActorContext(system)

  def bindings(environment: Environment, configuration: Configuration) =
    Seq(
      bind[ExecutionContexts].toInstance(new ExecutionContexts),

      bind[CatalogService].toSelf,
      bind[BasketService].toSelf,

      bind[ActorContext].toInstance(actorContext),
      bind[ActorRef].qualifiedWith("catalogActor").toInstance(actorContext.catalogActor),
      bind[BasketActorFactory].toSelf)
}

class BasketActorFactory @Inject() (
  actorContext: ActorContext)(
  implicit ec: ExecutionContext) {

  def get(userId: Long) = actorContext.basketActor(userId)
}