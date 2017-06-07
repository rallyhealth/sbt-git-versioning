package mlp

/**
  * Removes, changes types, changes access modifiers.
  */
class EquestriaMajor {

  /** Changed public field type and value from Int to String */
  val twilightSparkle: String = "256"

  /** No change */
  protected val rarity: Int = 1024

  /** No change */
  private val flutterShy: Int = 128

  // Deleted public method
  // def rainbowDash(): Double = twilightSparkle * 1.20

  /** No change */
  protected def appleJack(): Int = rarity / 2

  /** Changed private method access to public */
  def pinkiePie(): Int = twilightSparkle.toInt + flutterShy

  /** Changed public method access to protected */
  protected def princessCelestia(): Int = flutterShy + pinkiePie()
}
