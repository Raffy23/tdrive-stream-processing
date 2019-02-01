lazy val taxiJobRunner = project.in(file("TaxiJobRunner")).dependsOn(RootProject(file("taxi-processor"))).settings(
  // we set all provided dependencies to none, so that they are included in the classpath of mainRunner
  libraryDependencies := (libraryDependencies in RootProject(file("taxi-processor"))).value.map{
    module => module.configurations match {
      case Some("provided") => module.withConfigurations(None)
      case _ => module
    }
  }
)