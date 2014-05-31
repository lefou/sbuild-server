package org.sbuild.runner.server

import akka.actor.ActorSystem
import spray.routing.SimpleRoutingApp
import spray.httpx.unmarshalling.Unmarshaller
import spray.http.HttpEntity


object Server extends App with SimpleRoutingApp {

    implicit val actorSystem = ActorSystem()

    private val serverPort = 8080

    startServer(interface = "0.0.0.0", port = serverPort) {
        post {
            path("echo") {
                entity(as[String]) {
                    parameters =>
                        println(parameters.split("&").toList)
                        complete {
                            s"http://localhost:$serverPort/echo/1234"
                        }

                }

            }
        } ~
            get {
                path("echo" / Rest) {
                    id =>
                        complete {
                            s"log for run of $id"
                        }
                }
            }
    }
}