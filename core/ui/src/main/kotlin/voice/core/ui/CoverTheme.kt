@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme

/**
 * Seeds are cached (see [CoverSeedColors]), so a cover that was analyzed before themes the screen
 * from its very first frame. Until a seed is known the surrounding theme is used unchanged, and the
 * switch animates. Light, dark and pure black follow the surrounding theme.
 */
@Composable
fun CoverTheme(
  cover: String?,
  currentAudioUri: String? = null,
  content: @Composable () -> Unit,
) {
  val representativeSeed = rememberCoverSeedColor(cover)
  val colors = LocalCoverSeedColors.current
  // Read the already cached volume color during composition, before the first frame.
  // Starting at null would briefly show the representative (first-volume) cover color.
  val cachedVolumeSeed = currentAudioUri?.let { colors?.cached("embedded:$it") }?.takeIf { it.isSpecified }
  val volumeSeed by produceState<Color?>(initialValue = cachedVolumeSeed, currentAudioUri, colors) {
    value = cachedVolumeSeed
    if (currentAudioUri != null && colors != null && value == null) {
      value = colors.loadEmbeddedArtwork(currentAudioUri)
    }
  }
  val seed = volumeSeed ?: representativeSeed
  val baseScheme = MaterialTheme.colorScheme
  val targetScheme = if (seed != null) {
    val dark = baseScheme.surface.luminance() < 0.5F
    val pureBlack = baseScheme.isPureBlack
    remember(seed, dark, pureBlack) {
      coverColorScheme(seed, dark).let { if (pureBlack) it.withPureBlack() else it }
    }
  } else {
    baseScheme
  }
  MaterialExpressiveTheme(
    colorScheme = animateColorScheme(targetScheme),
    motionScheme = MaterialTheme.motionScheme,
    shapes = MaterialTheme.shapes,
    typography = MaterialTheme.typography,
    content = content,
  )
}

/** The colors a cover with the theme [seed] paints, light or [dark]. */
fun coverColorScheme(
  seed: Color,
  dark: Boolean,
): ColorScheme = dynamicColorScheme(
  seedColor = seed,
  isDark = dark,
  style = PaletteStyle.Vibrant,
  specVersion = ColorSpec.SpecVersion.SPEC_2025,
)

/** The theme seed for [cover], or null while unknown or if the cover has no usable color. */
@Composable
fun rememberCoverSeedColor(cover: String?): Color? {
  val seedColors = LocalCoverSeedColors.current
  val seed by produceState(initialValue = cover?.let { seedColors?.cached(it) }, cover, seedColors) {
    value = cover?.let { seedColors?.cached(it) }
    if (cover != null && seedColors != null) {
      value = seedColors.load(cover)
    }
  }
  return seed?.takeIf { it.isSpecified }
}
