package sample

import akka.actor.Actor.actorOf
import akka.actor.Actor
import akka.actor.ActorRef
import akka.dispatch.Dispatchers
import akka.http.Endpoint
import akka.http.Get
import akka.http.RequestMethod
import akka.http.RootEndpoint
import akka.routing.CyclicIterator
import akka.routing.Routing
import sample.Backend.TranslationRequest
import sample.Backend.TranslationResponse
import sample.Backend.translationService

object Frontend {

  object EndpointURI {
    val Translate = "/translate"
  }

  private val frontendDispatcher = Dispatchers.newExecutorBasedEventDrivenDispatcher("frontend-dispatcher")
    .setCorePoolSize(4)
    .build

  private def loadBalanced(poolSize: Int, actor: ⇒ ActorRef): ActorRef = {
    val workers = Vector.fill(poolSize)(actor.start())
    Routing.loadBalancerActor(CyclicIterator(workers)).start()
  }

  class WebEndpoint(root: ActorRef) extends Actor with Endpoint {
    import EndpointURI._
    self.dispatcher = frontendDispatcher

    val translate = loadBalanced(4, actorOf[TranslateHandler])

    override def preStart() = {
      root ! Endpoint.Attach(hook, provide)
    }

    def receive = handleHttpRequest

    def hook(uri: String): Boolean = {
      uri.startsWith(Translate)
    }

    def provide(uri: String): ActorRef = {
      if (uri.startsWith(Translate)) translate
      else throw new IllegalArgumentException("Unkown URI: " + uri)
    }
  }

  class TranslateHandler extends Actor {
    self.dispatcher = frontendDispatcher

    def receive = {
      case get: Get ⇒
        val text = get.request.getParameter("text")
        val TranslationResponse(translatedText, words) = (translationService ? TranslationRequest(text)).
          get.asInstanceOf[TranslationResponse]
        get.OK("Translated %s words to: %s".format(words, translatedText))
      case other: RequestMethod ⇒
        other.NotAllowed("Invalid method for this endpoint.")
    }
  }

}

class Boot {
  val root = actorOf[RootEndpoint].start()
  actorOf(new Frontend.WebEndpoint(root)).start()
}