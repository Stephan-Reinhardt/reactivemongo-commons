package beerbarrel.play.datetime

import org.joda.time.DateTime
import reactivemongo.api.bson.{BSONDateTime, BSONReader, BSONString, BSONValue, BSONWriter}

import scala.util.Try

trait JodaImplicits extends JodaReads with JodaWrites {

  implicit object DateTimeReader extends BSONReader[DateTime] {
    override def readTry(bson: BSONValue): Try[DateTime] = {
      bson match {
        case dateTime: BSONDateTime => Try(new DateTime(dateTime.value))
        case _                      => bson.asTry[BSONString].map(x => DateTime.parse(x.value))
      }
    }
  }

  implicit object DateTimeWriter extends BSONWriter[DateTime] {
    override def writeTry(t: DateTime): Try[BSONValue] = Try(BSONDateTime(t.getMillis))
  }

}
