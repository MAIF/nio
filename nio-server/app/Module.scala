import com.codahale.metrics.MetricRegistry
import com.google.inject.AbstractModule

class Module extends AbstractModule {

  override def configure() = {
    println("Starting Nio !")

    bind(classOf[Starter]).asEagerSingleton()

    val metrics: MetricRegistry = new MetricRegistry()

    bind(classOf[MetricRegistry]).toInstance(metrics)
  }

}
