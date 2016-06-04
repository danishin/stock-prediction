import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.{AbstractModule, Guice}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient
import stock.StockLearner

import scala.concurrent.Await

class AppModule extends AbstractModule {
  def configure(): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    bind(classOf[WSClient]).toInstance(AhcWSClient())
  }
}


object Application {
  import concurrent.duration._

  def main(args: Array[String]): Unit = {
    require(args.nonEmpty, "You must provide tickers to predict for")

    val injector = Guice.createInjector(new AppModule)

    val learner = injector.getInstance(classOf[StockLearner])

    val prediction = learner.predict(args)

    val result = Await.result(prediction, 20.seconds)

    println(result)
  }
}
