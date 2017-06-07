package mlp

/**
  * Main method just to ensure that the compiler doesn't try to eliminate the [[Equestria]] class in some sort of
  * compiler optimization.
  */
object Main {

  def main(args: Array[String]): Unit = {
    val equestria = new Equestria()
    println(equestria.hashCode)
  }
}
