package common

import java.nio.file.{FileSystems, Files, Path, Paths}
import java.util.Collections

import play.api.Environment

import scala.io.Source

object file {
  import collection.JavaConverters._

  def readResource(path: String)(implicit env: Environment): Vector[String] =
    Source.fromInputStream(env.resourceAsStream(path).get).getLines().toVector

  // http://stackoverflow.com/questions/1429172/how-do-i-list-the-files-inside-a-jar-file/28057735#28057735
  def readAllResources(directoryPath: String)(implicit env: Environment): Vector[(Path, Vector[String])] = {
    val uri = env.resource(directoryPath).get.toURI

    val dirPath =
      if (uri.getScheme == "jar") {
        println("Read resources in jar file")
        FileSystems.newFileSystem(uri, Collections.emptyMap[String, Object]()).getPath(directoryPath)
      }
      else {
        println("Read resources in file system")
        Paths.get(uri)
      }

    Files.newDirectoryStream(dirPath).asScala.toVector.map(path => (path, Source.fromFile(path.toFile).getLines().toVector))
  }
}
