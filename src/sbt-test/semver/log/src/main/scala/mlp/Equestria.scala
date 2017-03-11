package mlp

/**
  * The original code that we started with. Changes that force patch, minor,
  * and major updates will be different files that are copies of this with changes.
  */
class Equestria {

  val twilightSparkle: Int = 256

  protected val rarity: Int = 1024

  private val flutterShy: Int = 128

  def rainbowDash(): Double = twilightSparkle * 1.20

  protected def appleJack(): Int = rarity / 2

  private def pinkiePie(): Int = twilightSparkle + flutterShy

  def princessCelestia(): Int = flutterShy + pinkiePie()
}
