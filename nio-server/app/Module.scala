import com.google.inject.AbstractModule

class Module extends AbstractModule {

  override def configure() = {
    println("Starting Nio !")

    bind(classOf[Starter]).asEagerSingleton()
  }

}
