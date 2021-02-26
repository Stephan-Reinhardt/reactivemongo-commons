package beerbarrel.reactivemongo

import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseDao[T](val reactiveMongoApi: ReactiveMongoApi)(implicit val ec: ExecutionContext) {

  val collectionName: String

  protected def collection: Future[BSONCollection] =
    reactiveMongoApi.database.map(_.collection[BSONCollection](collectionName))

  private val maxDocs = -1

  def create(model: T)(implicit writer: BSONDocumentWriter[T]): Future[T] =
    collection.flatMap(_.insert.one(model).map(_ => model))


  private def findByInternal(query: BSONDocument)(implicit reader: BSONDocumentReader[T]): Future[Seq[T]] =
    collection.flatMap(
      _.find(query)
        .cursor[T]()
        .collect[Seq](maxDocs, Cursor.FailOnError[Seq[T]]())
    )

  def all()(implicit reader: BSONDocumentReader[T]): Future[Seq[T]] =
    findByInternal(BSONDocument.empty)

  def delete(id: String)(implicit reader: BSONDocumentReader[T]): Future[Boolean] =
    collection.flatMap(_.delete.one(idQuery(id)).map(_.n >= 1))

  def generateObjectId(): String =
    BSONObjectID.generate().stringify

  private def idQuery(id: String) = BSONDocument("_id" -> id)

}
