package models

import akka.actor.{Props, Actor, ActorLogging}
import com.datastax.driver.core.ResultSet
import scala.util.{Failure, Success, Try}

case class ConnectToCassandra(db: CassandraDB)

case class DBQuery(query: String, queryId: Long)

case class QueryResult(resultSet: ResultSet, queryId: Long)

case class Disconnect()

case class ErrorWithDetails(status: String, queryId: Long, msg: String)

class DBActor extends Actor with ActorLogging {

  def receive: Actor.Receive = {
    case ConnectToCassandra(db: CassandraDB) =>
      val mayBeSession = Try(db.getSession)
      mayBeSession match {
        case Success(session) =>
          sender ! "Connected"
          context.become({
            case DBQuery(query, queryId) =>
              val mayBeResult = Try(session.execute(query))
              mayBeResult match {
                case Success(resultSet) =>
                  sender ! new QueryResult(resultSet, queryId)
                case Failure(e) =>
                  log.info(e.getMessage + " for " + query + " with id " + queryId)
                  sender ! new ErrorWithDetails("QueryError", queryId, e.getMessage)
              }
            case Disconnect() =>
              session.shutdown()
              log.info("disconnected session")
              context.unbecome()
              sender ! "Disconnected"
          })
          log.info("created session")
        case Failure(f) =>
          sender ! new ErrorWithDetails("Connection Failure", -1, f.getMessage)
          log.info(f.getMessage)
      }
  }
}

object DBActor {
  def props: Props = Props(classOf[DBActor])
}
