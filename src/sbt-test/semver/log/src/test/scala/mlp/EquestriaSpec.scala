package mlp

import org.scalatest.FunSuite

class EquestriaSpec extends FunSuite {

  test("sanity test") {
    val equestria = new Equestria()
    assert(equestria.hashCode().toString.nonEmpty)
  }
}
