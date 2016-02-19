logLevel := Level.Info

resolvers ++=  Seq(
  "Typesafe Snapshots"     at "http://repo.typesafe.com/typesafe/snapshots/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea"    % "1.6.0-SNAPSHOT")
addSbtPlugin("com.typesafe.play"    % "sbt-plugin"  % "2.4.6")