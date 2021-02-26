
name := "reactivemongo-commons"

organization := "beerbarrel"

version := "0.1"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "1.0.2-play28",
  "com.typesafe.play" %% "play" % "2.8.7",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

scalacOptions in Test ++= Seq("-Yrangepos")