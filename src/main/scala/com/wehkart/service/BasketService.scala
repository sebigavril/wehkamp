package com.wehkart.service

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.pattern.ask
import akka.util.Timeout
import com.wehkart.domain.{ProductLike, BasketProduct}
import com.wehkart.repository.ProductsRepository
import com.wehkart.ActorProtocol._
import com.wehkart.{domain, ActorContext}


class BasketService(implicit ec: ExecutionContext) {

  private implicit val timeout = Timeout(10 seconds)

  def list(userId: Long): Future[Set[BasketProduct]] = {
    ActorContext.basketActor(userId)
      .flatMap(_ ? ListAll)
      .map( _.asInstanceOf[Set[BasketProduct]])
  }

  def add(userId: Long, product: ProductLike, howMany: Long): Future[ActorMessage] = {
    ActorContext.basketActor(userId)
      .flatMap(_ ? Add(product, howMany))
      .map(_.asInstanceOf[ActorMessage])
  }

  def remove(userId: Long, product: ProductLike, howMany: Long): Future[ActorMessage] = {
    ActorContext.basketActor(userId)
      .flatMap(_ ? Remove(product, howMany))
      .map(_.asInstanceOf[ActorMessage])
  }

}
