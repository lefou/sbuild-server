import org.sbuild._

@version("0.7.9013")
class TestProject(implicit _project: Project) {

  Target("phony:hello") help "Greet me" exec {
    println("Hello you")
  }

  Target("phony:hello-slow") exec {
    Thread.sleep(5000)
    println("Hello you")
  }

}
