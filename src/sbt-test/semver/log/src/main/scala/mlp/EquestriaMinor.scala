package mlp

/**
  * Adds protected fields/methods and changes implementation.
  */
class EquestriaMinor {

  /** No change */
  val twilightSparkle: Int = 256

  /** Added public field */
  val starlightGlimmer: Double = twilightSparkle * 0.90

  /** No change */
  protected val rarity: Int = 1024

  /** No change */
  private val flutterShy: Int = 128

  /** Added protected field */
  protected val velvetRemedy: Double = (rarity * 0.70) + (flutterShy * 0.30)

  /** No change */
  def rainbowDash(): Double = twilightSparkle * 1.20

  /** No change */
  protected def appleJack(): Int = rarity / 2

  /** Added protected method */
  protected def calamity(): Double = (appleJack() * 0.50) + (rainbowDash() * 0.40)

  /** No change */
  private def pinkiePie(): Int = twilightSparkle + flutterShy

  /** Changed public method implementation; signature stayed the same */
  def princessCelestia(): Int = twilightSparkle.toInt + pinkiePie()

  /** Added public method */
  def princessLuna(): Double = princessCelestia() + starlightGlimmer
}
