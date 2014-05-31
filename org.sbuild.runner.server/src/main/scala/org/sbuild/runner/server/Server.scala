package org.sbuild.runner.server

import akka.actor.ActorSystem
import spray.routing.SimpleRoutingApp


object Server extends App with SimpleRoutingApp {

  implicit val actorSystem = ActorSystem()

  startServer(interface = "localhost", port = 8080) {
    post {
      path("run") {
        complete {
          "Nothing done"
        }
      }
    }
  }
}