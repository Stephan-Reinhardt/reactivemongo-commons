
name := "reactivemongo-commons"

organization := "beerbarrel"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "1.0.2-play28",
  "com.typesafe.play" %% "play" % "2.8.7",
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.9" % "test",
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.9" % "test"
)