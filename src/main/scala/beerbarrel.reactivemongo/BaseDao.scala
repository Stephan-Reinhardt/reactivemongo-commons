package beerbarrel.reactivemongo

import beerbarrel.play.datetime.JodaImplicits
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONBoolean, BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseDao[T](val reactiveMongoApi: ReactiveMongoApi)(implicit val ec: ExecutionContext) extends JodaImplicits {

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

  def findOneBy(field: String, id: Any)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = {
    val query = buildQuery(field, id)
    findOneByInternal(query)
  }

  private def buildQuery(field: String, value: Any): BSONDocument = {
    value match {
      case v: String => BSONDocument(field -> v)
      case v: Int => BSONDocument(field -> v)
      case v: Long => BSONDocument(field -> v)
      case v: Double => BSONDocument(field -> v)
      case v: BigDecimal => BSONDocument(field -> v)
      case v: Float => BSONDocument(field -> v)
      case v: Boolean => BSONDocument(field -> v)
      case _ => throw new IllegalStateException(s"sType of ${value.getClass.toString} is not mapped.")
    }
  }

  def findBy(field: String, id: Any)(implicit reader: BSONDocumentReader[T]): Future[Seq[T]] = {
    val query = buildQuery(field, id)
    findByInternal(query)
  }

  def getAll()(implicit reader: BSONDocumentReader[T]): Future[Seq[T]] = {
    findByInternal(BSONDocument.empty)
  }

  def findById(id: String)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = {
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
