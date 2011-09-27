package sample

import akka.actor.Actor.actorOf
import akka.actor.Actor
import akka.actor.ActorRef
import akka.dispatch.Dispatchers
import akka.routing.CyclicIterator
import akka.routing.Routing

object Backend {

  case class TranslationRequest(text: String)
  case class TranslationResponse(text: String, words: Int)

  private val backendDispatcher = Dispatchers.newExecutorBasedEventDrivenDispatcher("backend-dispatcher")
    .setCorePoolSize(4)
    .build

  val translationService = loadBalanced(10, actorOf[TranslationService])

  private def loadBalanced(poolSize: Int, actor: ⇒ ActorRef): ActorRef = {
    val workers = Vector.fill(poolSize)(actor.start())
    Routing.loadBalancerActor(CyclicIterator(workers)).start()
  }

  class TranslationService extends Actor {
    self.dispatcher = backendDispatcher

    val translator = loadBalanced(4, actorOf[Translator])
    val counter = loadBalanced(4, actorOf[Counter])

    def receive = {
      case TranslationRequest(text) ⇒
        val future1 = (translator ? text)
        val translatedText = future1.get.asInstanceOf[String]
        val future2 = (counter ? text)
        val words = future2.get.asInstanceOf[Int]

        self.channel ! TranslationResponse(translatedText, words)
    }
  }

  class Translator extends Actor {
    self.dispatcher = backendDispatcher

    def receive = {
      case x: String ⇒
        Thread.sleep(100)
        val result = x.toUpperCase
        self.channel ! result
    }
  }

  class Counter extends Actor {
    self.dispatcher = backendDispatcher

    def receive = {
      case x: String ⇒
        Thread.sleep(100)
        val result = x.split(" ").length
        self.channel ! result
    }
  }

}