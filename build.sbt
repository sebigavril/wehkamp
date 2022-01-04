name := "wehkamp"
version := "1.0"
scalaVersion := "2.11.7"


lazy val wehkamp = (project in file("."))
  .enablePlugins(PlayScala)


lazy val akkaVersion = "2.4.1"
lazy val playVersion = "2.4.6"

resolvers ++=  Seq(
  "Typesafe Snapshots"     at "http://repo.typesafe.com/typesafe/snapshots/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/")

libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-core" % "2.12.0",
  "com.typesafe.akka" %  "akka-actor_2.11"  % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"       % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit"     % akkaVersion,
  "com.typesafe.play" %% "play-ws"          % playVersion,
  "com.typesafe.play" %% "play-json"        % playVersion,
  "org.scalatest"     %% "scalatest"        % "2.2.4"       % Test,
  "com.typesafe.play" %% "play-test"        % playVersion   % Test)
