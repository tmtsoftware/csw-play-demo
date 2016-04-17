package modules

import com.google.inject.AbstractModule
import csw.services.loc.LocationService

// Initialize the location service on start (Hopefully before the ActorSystem is created)
trait Init {}

class InitLocationService extends Init {
  initialize() // running initialization in constructor
  def initialize() = {
    LocationService.initInterface()
  }
}

class InitModule extends AbstractModule {
  def configure() = {
    bind(classOf[Init])
      .to(classOf[InitLocationService]).asEagerSingleton()
  }
}
