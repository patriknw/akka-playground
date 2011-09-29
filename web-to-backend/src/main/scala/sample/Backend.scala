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

    val translator = actorOf[Translator].start()
    val counter = actorOf[Counter].start()

    def receive = {
      case TranslationRequest(text) ⇒
        val future1 = (translator ? text)
        val future2 = (counter ? text)

        val result1 = future1.as[String]
        val result2 = future2.as[Int]

        for (translatedText ← result1; words ← result2) {
          self.reply(TranslationResponse(translatedText, words))
        }
    }
  }

  class Translator extends Actor {
    self.dispatcher = backendDispatcher

    def receive = {
      case x: String ⇒
        // simulate work
        Thread.sleep(100)
        val result = x.toUpperCase
        self.reply(result)
    }
  }

  class Counter extends Actor {
    self.dispatcher = backendDispatcher

    def receive = {
      case x: String ⇒
        // simulate work
        Thread.sleep(100)
        val result = x.split(" ").length
        self.reply(result)
    }
  }

}