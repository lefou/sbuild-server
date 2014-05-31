import de.tototec.sbuild._
import de.tototec.sbuild.ant._
import de.tototec.sbuild.ant.tasks._

@version("0.7.6")
@classpath("mvn:org.apache.ant:ant:1.8.4", "mvn:org.sbuild:org.sbuild.plugins.aether:0.1.0")
class SBuild(implicit _project: Project) {

  val sprayVersion = "1.3.1-20140423"
  val akkaVersion = "2.3.2"

  import org.sbuild.plugins.aether._
  Plugin[Aether]("aether") configure {
    _.copy(remoteRepos = Seq(Repository.Central, "spray::default::http://repo.spray.io")).
      addDeps("compile")(
        "org.scala-lang:scala-library:2.11.1",
        "org.scala-lang:scala-compiler:2.11.1",
        s"com.typesafe.akka:akka-actor_2.11:${akkaVersion}",
        s"io.spray:spray-routing_2.11:${sprayVersion}",
        s"io.spray:spray-client_2.11:${sprayVersion}",
        // "org.json4s:json4s-native_2.11:3.2.9",
        //        "com.typesafe:scalalogging-slf4j_2.10:1.1.0",
        "ch.qos.logback:logback-classic:1.0.13").
        addDeps("runtime")("compile")
  }

  // sbuild master
  val sbuildPath = Path(Prop("SBUILD_MASTER", "../sbuild"))
  Module(sbuildPath.getPath)

  val sbuildVersion = "0.7.9013"

  val namespace = "org.sbuild.runner.server"
  val jar = s"target/${namespace}-${sbuildVersion}.jar"
  val sourcesZip = s"target/${namespace}-${sbuildVersion}-sources.jar"

  val testJar = s"target/${namespace}-${sbuildVersion}-tests.jar"

  val scalaLibrary = "mvn:org.scala-lang:scala-library:2.11.1"
  val scalaXml = "mvn:org.scala-lang.modules:scala-xml_2.11:1.0.2"
  val scalaTest = "mvn:org.scalatest:scalatest_2.11:2.1.7"

  val compileCp = "aether:compile" ~
    (sbuildPath / s"org.sbuild/target/org.sbuild-${sbuildVersion}.jar")

  val testCp = compileCp ~
    scalaTest

  val compilerPath = scalaLibrary ~
    "mvn:org.scala-lang:scala-reflect:2.11.1" ~
    "mvn:org.scala-lang:scala-compiler:2.11.1"

  ExportDependencies("eclipse.classpath", testCp)

  Target("phony:all") dependsOn jar ~ sourcesZip ~ "test"

  Target("phony:clean").evictCache exec {
    AntDelete(dir = Path("target"))
  }

  Target("phony:compile").cacheable dependsOn compilerPath ~ compileCp ~
    "scan:src/main/scala" exec {

      val output = "target/classes"

      // compile scala files
      addons.scala.Scalac(
        compilerClasspath = compilerPath.files,
        classpath = compileCp.files,
        sources = "scan:src/main/scala".files,
        destDir = Path(output),
        unchecked = true, deprecation = true, debugInfo = "vars",
        fork = true)

    }

  Target(jar) dependsOn "compile" ~ "LICENSE.txt" ~ "scan:src/main/resources" exec { ctx: TargetContext =>
    new AntJar(destFile = ctx.targetFile.get, baseDir = Path("target/classes")) {
      if (Path("src/main/resources").exists) add(AntFileSet(dir = Path("src/main/resources")))
      add(AntFileSet(file = Path("LICENSE.txt")))
    }.execute
  }

  Target(testJar) dependsOn "testCompile" ~ "scan:target/test-classes" exec { ctx: TargetContext =>
    AntJar(destFile = ctx.targetFile.get, baseDir = Path("target/test-classes"))
  }

  Target(sourcesZip) dependsOn "scan:src/main" ~ "scan:LICENSE.txt" exec { ctx: TargetContext =>
    AntZip(destFile = ctx.targetFile.get, fileSets = Seq(
      AntFileSet(dir = Path("src/main/scala")),
      AntFileSet(file = Path("LICENSE.txt"))))
  }

  Target("phony:testCompile").cacheable dependsOn compilerPath ~ testCp ~ jar ~ "scan:src/test/scala" exec {
    if (!"scan:src/test/scala".files.isEmpty) {
      addons.scala.Scalac(
        compilerClasspath = compilerPath.files,
        classpath = testCp.files ++ jar.files,
        sources = "scan:src/test/scala".files,
        destDir = Path("target/test-classes"),
        deprecation = true, unchecked = true, debugInfo = "vars",
        fork = true)
    }
  }

  Target("phony:test") dependsOn testCp ~ jar ~ "testCompile" exec {
    //    addons.scalatest.ScalaTest(
    //      classpath = testCp.files ++ jar.files,
    //      runPath = Seq("target/test-classes"),
    //      //      reporter = "oF",
    //      standardOutputSettings = "G",
    //      xmlOutputDir = Path("target/test-output"),
    //      fork = true)

    val res = addons.support.ForkSupport.runJavaAndWait(
      classpath = testCp.files ++ jar.files,
      arguments = Array("org.scalatest.tools.Runner", "-p", Path("target/test-classes").getPath, "-oG", "-u", Path("target/test-output").getPath))
    if (res != 0) throw new RuntimeException("Some tests failed")

  }

  Target("phony:scaladoc").cacheable dependsOn compilerPath ~ compileCp ~ "scan:src/main/scala" exec {
    addons.scala.Scaladoc(
      scaladocClasspath = compilerPath.files,
      classpath = compileCp.files,
      sources = "scan:src/main/scala".files,
      destDir = Path("target/scaladoc"),
      deprecation = true, unchecked = true, implicits = true,
      docVersion = sbuildVersion,
      docTitle = s"SBuild Runner API Reference")
  }

  Target("phony:libs") dependsOn compileCp exec { ctx: TargetContext =>
    val libDir = Path("target/libs")
    libDir.mkdirs()
    ctx.fileDependencies.foreach { file => file.copyTo(libDir / file.getName) }
  }

  SchemeHandler("run", new SchemeHandler with SchemeResolverWithDependencies {
    def localPath(schemeContext: SchemeHandler.SchemeContext): String = s"phony:${schemeContext.fullName}"
    def dependsOn(schemeContext: SchemeHandler.SchemeContext): TargetRefs = "aether:runtime" ~ "compile"
    def resolve(schemeContext: SchemeHandler.SchemeContext, targetContext: TargetContext): Unit = {
      schemeContext.path.split(" ") match {
        case args @ Array(className) =>
          addons.support.ForkSupport.runJavaAndWait(
            classpath = "aether:runtime".files ++ Seq(Path("target/classes")),
            arguments = args)
        case _ => throw new RuntimeException("Unsupport path: " + schemeContext)
      }
    }
  })

}
