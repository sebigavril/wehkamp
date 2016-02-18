name := "wehkart"

version := "1.0"

scalaVersion := "2.11.7"


lazy val akkaVersion = "2.4.1"
lazy val playVersion = "2.4.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %  "akka-actor_2.11"  % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit"     % akkaVersion,
  "com.typesafe.play" %% "play-ws"          % playVersion,
  "com.typesafe.play" %% "play-json"        % playVersion,
  "org.scalatest"     %% "scalatest"        % "2.2.4"       % Test)