lazy val commonSettings = Seq(
  organization := "net.psforever",
  version := "1.0.2-SNAPSHOT",
  scalaVersion := "2.11.8",
  scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8", "-language:postfixOps"),
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  libraryDependencies ++= Seq(
    "org.ini4j"                  % "ini4j"         % "0.5.4"
  )
)

lazy val psloginPackSettings = packAutoSettings ++ Seq(
  packArchivePrefix := "pslogin",
  packExtraClasspath := Map("ps-login" -> Seq("${PROG_HOME}/config")),
  packResourceDir += (baseDirectory.value / "config" -> "config")
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(psloginPackSettings: _*).
  aggregate(pslogin)

lazy val pslogin = (project in file("pslogin")).
  settings(commonSettings: _*).
  settings(
    name := "pslogin"
  )
