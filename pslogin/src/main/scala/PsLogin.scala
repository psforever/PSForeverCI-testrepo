object PsLogin 
  def main(args : Array[String]) : Unit = {
    if (args.length >= 1) {
      args(0) match {
        case "good" =>
          println("Running")
          Thread.sleep(30*1000)
          println("Stopping good")
          System.exit(0)
        case "bad" =>
          println("Running")
          Thread.sleep(30*1000)
          println("Stopping bad")
          System.exit(1)
        case "forever" =>
          println("Running")
          var time = 0;

          while (true) {
            Thread.sleep(1*1000)
            time += 1;
            println(time)
          }

          println("Ending")
          System.exit(0)
      }
    } else {
      println("Not enough arguments")
      // insufficient arguments
      System.exit(1)
    }
  }
}
