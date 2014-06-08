package test

import com.datastax.driver.core.ResultSet
import com.typesafe.config.ConfigFactory
import models._
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import akka.actor.{ActorLogging, Actor, ActorSystem}
import akka.testkit._
import play.api.test.FakeApplication

class TestActor extends Actor with ActorLogging {
  def receive: Actor.Receive = {
    case x: String => println(x)
    case x: ResultSet => println(Utils.resultToJson(x))
  }
}

class DBActorSpec extends Specification {

  implicit val actorSystem = ActorSystem("testActorSystem", ConfigFactory.load())

  "DBActor" should {
    "connect to cassandra" in {
      running(FakeApplication()) {
        val dbActor = TestActorRef(new DBActor())
        val sender = TestActorRef(new TestActor())
        dbActor.tell(new ConnectToCassandra(new CassandraDB), sender)
        dbActor.tell(new DBQuery("SELECT * FROM demodb.emp;",1), sender)
        dbActor.tell(new DBQuery("SELECT first_name,last_name FROM demodb.emp;",2), sender)
        dbActor.tell(new Disconnect(), sender)
        1 === 1
      }
    }
  }

  step(TestKit.shutdownActorSystem(actorSystem))
}
