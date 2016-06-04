name := "stock_prediction"
version := "1.0"
scalaVersion := "2.11.7"
 
libraryDependencies ++= {
  val smileVersion = "1.1.0"
  val breezeVersion = "0.12"
  val playVersion = "2.5.0"
  val reactivemongoVersion = "0.11.11"

  Seq(
    /* Java */
    "com.github.haifengl" % "smile-core" % smileVersion,
    "com.github.haifengl" % "smile-plot" % smileVersion,

    /* Scala */
    "org.scala-lang" % "scala-swing" % "2.11.0-M7",
    "org.scalanlp" %% "breeze" % breezeVersion,
    "org.scalanlp" %% "breeze-natives" % breezeVersion,
    "org.scala-saddle" %% "saddle-core" % "1.3.4",
    "com.typesafe.play" %% "play-json" % playVersion,
    "com.typesafe.play" %% "play-ws" % playVersion,
    "org.reactivemongo" %% "reactivemongo" % reactivemongoVersion,
    "org.reactivemongo" %% "play2-reactivemongo" % reactivemongoVersion
  )
}
