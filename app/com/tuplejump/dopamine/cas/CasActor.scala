package com.tuplejump.dopamine.cas

import akka.actor.{Actor, ActorLogging}
import com.datastax.driver.core._
import scala.collection.JavaConversions._

import play.api.libs.json._
import java.math.BigInteger
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import scala.Some
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject

object CasDefaults {
  val host = List("localhost")
  val port = Some(9042)
  val username = None
  val password = None
}

/* messages */
sealed trait Message

case class Connect(contactPoints: List[String] = CasDefaults.host,
                   port: Option[Int] = CasDefaults.port,
                   username: Option[String] = CasDefaults.username,
                   password: Option[String] = None) extends Message

case class Query(msgId: String, cql: String, opts: Option[Map[String, String]] = None) extends Message

case class Result(msgId: String, response: JsValue) extends Message

case class Disconnect(msgId: String)

/* Data */
sealed trait Data

case class Connection(session: Session)

class CasActor extends Actor with ActorLogging {

  def receive = {
    case Connect(contactPoints, port, username, password) => {
      val cluster = {
        val builder = Cluster.builder().addContactPoints(contactPoints: _*)
        builder.withPort(port.get)

        (username, password) match {
          case (Some(u), Some(p)) => {
            builder.withCredentials(u, p)
          }

          case _ => {
            log.debug("No user/password")
          }
        }

        builder.build()
      }

      val session = cluster.connect()

      context.become {
        case Query(id, query, opt) => {
          val result = session.execute(query)
          log.info(result.toString)
          val response: JsArray = resultToJson(result)

          log.info(response.toString())
          sender ! Result(id, response)
        }

        case Disconnect => {
          session.shutdown()
          context.unbecome()
        }
      }
    }
  }


  private def resultToJson(result: ResultSet): JsArray = {
    var response = Json.arr()

    result.iterator() map rowToJson foreach {
      jsRow =>
        response = response :+ jsRow
    }
    response
  }

  private def rowToJson(row: Row): JsObject = {
    var responseRow = Json.obj()
    val coldef = row.getColumnDefinitions()
    coldef foreach {
      d =>
        import DataType.Name._
        import java.math.{BigDecimal => BigDec}
        implicit def bigInteger2bigDecimal(b: BigInteger) = new BigDecimal(new BigDec(b))
        d.getType.getName match {

          case ASCII | VARCHAR | TEXT => responseRow = responseRow + (d.getName -> JsString(row.getString(d.getName)))

          case BIGINT | DataType.Name.COUNTER => responseRow = responseRow + (d.getName -> JsNumber(row.getLong(d.getName)))

          case INT => responseRow = responseRow + (d.getName -> JsNumber(row.getInt(d.getName)))

          case BLOB => responseRow = responseRow + (d.getName -> JsString("SPECIAL:BLOB"))

          case BOOLEAN => responseRow = responseRow + (d.getName -> JsBoolean(row.getBool(d.getName)))

          case DECIMAL => responseRow = responseRow + (d.getName -> JsNumber(row.getDecimal(d.getName)))

          case DOUBLE => responseRow = responseRow + (d.getName -> JsNumber(row.getDouble(d.getName)))

          case FLOAT => responseRow = responseRow + (d.getName -> JsNumber(row.getFloat(d.getName)))

          case INET => responseRow = responseRow + (d.getName -> JsString(row.getInet(d.getName).toString))

          case TIMESTAMP => responseRow = responseRow + (d.getName -> JsString(row.getDate(d.getName).toString))

          case UUID => responseRow = responseRow + (d.getName -> JsString(row.getUUID(d.getName).toString))

          case VARINT => responseRow = responseRow + (d.getName -> JsNumber(row.getVarint(d.getName)))

          case TIMEUUID => responseRow = responseRow + (d.getName -> JsString(row.getUUID(d.getName).toString))

          case LIST => responseRow = responseRow + (d.getName -> JsString(row.getList(d.getName, d.getType.getTypeArguments.head.asJavaClass()).toString))

          case SET => responseRow = responseRow + (d.getName -> JsString(row.getSet(d.getName, d.getType.getTypeArguments.head.asJavaClass()).toString))

          case MAP => responseRow = responseRow + (d.getName -> JsString(row.getMap(d.getName, d.getType.getTypeArguments.head.asJavaClass(), d.getType.getTypeArguments.last.asJavaClass()).toString))

          case CUSTOM => responseRow = responseRow + (d.getName -> JsString("[[%s]]".format(d.getType.getCustomTypeClassName))) //TODO: Read the byteBuffer and try to serialize if it has implicit bb => cc
        }
    }
    log.info(responseRow.toString())
    responseRow

  }
}

