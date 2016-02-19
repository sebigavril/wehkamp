package com.wehkamp

import scala.concurrent.ExecutionContext
import javax.inject.{Inject, Provider}
import akka.actor.ActorRef
import com.wehkamp.AppModule.BasketActorProvider
import com.wehkart.{ExecutionContexts, ActorContext}
import com.wehkart.service.BasketService
import play.api.{Configuration, Environment}
import play.api.inject._
import service.CatalogService

class AppModule extends Module {

  implicit private val ec = new ExecutionContexts().ec

  def bindings(environment: Environment, configuration: Configuration) =
    Seq(
      bind[ExecutionContexts].toInstance(new ExecutionContexts),

      bind[CatalogService].toSelf,
      bind[BasketService].toSelf,

      bind[ActorRef].qualifiedWith("catalogActor").toInstance(ActorContext.catalogActor),
      bind[ActorRef].qualifiedWith("basketActor").toProvider[BasketActorProvider])
}

object AppModule {

  implicit private val ec = new ExecutionContexts().ec

  class BasketActorProvider @Inject() extends Provider[ActorRef] {
    override def get() = ActorContext.basketActor(1)
  }
}
