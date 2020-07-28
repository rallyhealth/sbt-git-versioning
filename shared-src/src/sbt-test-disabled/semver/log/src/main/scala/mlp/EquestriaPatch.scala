package mlp

/**
  * Adds only private fields/methods and changes implementation.
  */
class EquestriaPatch {

  /** No change */
  val twilightSparkle: Int = 256

  /** No change */
  protected val rarity: Int = 1024

  /** Changed private field type from Int to Long */
  private val flutterShy: Long = 128L

  /** Added private field */
  private val psychoShy: String = s"$flutterShy's daughter"

  /** No change */
  def rainbowDash(): Double = twilightSparkle * 1.20

  /** No change */
  protected def appleJack(): Int = rarity / 2

  /** Changed private method return type from Int to Long */
  private def pinkiePie(): Long = twilightSparkle + flutterShy

  /** Added private method */
  private def rampage(): String = s"${pinkiePie()}'s insanity ${appleJack()}'s strength"

  /** Changed public method implementation; signature stayed the same */
  def princessCelestia(): Int = flutterShy.toInt + pinkiePie().toInt
}
