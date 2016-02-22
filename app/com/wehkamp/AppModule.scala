package com.wehkamp

import akka.actor.{ActorRef, ActorSystem}
import com.wehkamp.ActorConstants.actorSystemName
import com.wehkamp.service.{BasketService, CatalogService}
import play.api.inject._
import play.api.{Configuration, Environment}

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
      bind[ActorRef].qualifiedWith("basketActor").toInstance(actorContext.basketActor()))
}