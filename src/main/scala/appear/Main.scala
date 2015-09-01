package appear

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import appear.rest.RestServiceActor
import spray.can.Http

object SpaceRest extends App {

  // create an actor system for application
  implicit val system = ActorSystem("appear-space-program")

  // create and start rest service actor
  val restService = system.actorOf(Props[RestServiceActor], "rest-endpoint")

  // start HTTP server with rest service actor as a handler
  IO(Http) ! Http.Bind(restService, "0.0.0.0", 9000)
}