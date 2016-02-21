package com.wehkamp.service

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import javax.inject.{Named, Inject}
import akka.actor.ActorRef
import akka.pattern.ask
import com.wehkamp.ActorConstants.timeout
import com.wehkamp.ActorProtocol._
import com.wehkamp.domain.{ProductLike, ShoppingProduct}


class CatalogService @Inject() (@Named("catalogActor") catalogActor: ActorRef)(implicit ec: ExecutionContext) {

  def list: Future[Set[ShoppingProduct]] = {
    (catalogActor ? ListAll)
      .map(_.asInstanceOf[Set[ShoppingProduct]])
  }

  def add(product: ProductLike, howMany: Long): Future[ActorMessage] = ???
}
