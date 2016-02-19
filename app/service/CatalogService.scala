package service

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import javax.inject.{Named, Inject}
import akka.actor.ActorRef
import akka.pattern.ask
import com.wehkart.ActorConstants.timeout
import com.wehkart.ActorProtocol._
import com.wehkart.domain.{ProductLike, ShoppingProduct}


class CatalogService @Inject() (@Named("catalogActor") catalogActor: ActorRef)(implicit ec: ExecutionContext) {

  def list: Future[Set[ShoppingProduct]] = {
    (catalogActor ? ListAll)
      .map(_.asInstanceOf[Set[ShoppingProduct]])
  }

  def add(product: ProductLike, howMany: Long): Future[ActorMessage] = ???
}
