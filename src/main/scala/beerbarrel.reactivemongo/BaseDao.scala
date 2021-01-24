package beerbarrel.reactivemongo

import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONBoolean, BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseDao[T](val reactiveMongoApi: ReactiveMongoApi)(implicit val ec: ExecutionContext) {

  protected val collectionName: String

  protected def collection: Future[BSONCollection] = reactiveMongoApi.database.map(_.collection[BSONCollection](collectionName))

  private val maxDocs = -1 // Fixme this is not a good idear with huge collections. Rethink how to stream the data here.

  def create(model: T)(implicit writer: BSONDocumentWriter[T]): Future[T] = {
    collection.flatMap(_.insert.one(model).map(_ => model))
  }

  private def findByInternal(query: BSONDocument)(implicit reader: BSONDocumentReader[T]): Future[Seq[T]] = {
    collection.flatMap(
      _.find(query)
        .cursor[T]()
        .collect[Seq](maxDocs, Cursor.FailOnError[Seq[T]]())
    )
  }

  private def findOneByInternal(query: BSONDocument)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = {
    collection.flatMap(_.find(query).one[T])
  }

  protected def findByBoolean(field: String, value: Boolean)(implicit reader: BSONDocumentReader[T]): Future[Seq[T]] = {
    findByInternal(BSONDocument(field -> BSONBoolean(value)))
  }

  def getAll()(implicit reader: BSONDocumentReader[T]): Future[Seq[T]] = {
    findByInternal(BSONDocument.empty)
  }

  protected def findByString(field: String, id: String)(implicit reader: BSONDocumentReader[T]): Future[Seq[T]] = {
    // TODO this could be handled in a better way i think.
    val query = BSONDocument(field -> id)
    findByInternal(query)
  }

  protected def findById(id: String)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = {
    findOneByInternal(idQuery(id))
  }

  def delete(id: String)(implicit reader: BSONDocumentReader[T]): Future[Boolean] = {
    collection.flatMap(_.delete.one(idQuery(id)).map(_.n >= 1))
  }

  /** Hack find a better way to do this but don't spread the reactive mongo stuff around
   * @return
   */
  def generateObjectId(): String = {
    BSONObjectID.generate().stringify
  }

  private def idQuery(id: String) = BSONDocument("_id" -> id)
}
