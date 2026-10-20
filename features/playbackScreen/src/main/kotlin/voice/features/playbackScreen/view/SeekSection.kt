@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import voice.core.ui.formatTime
import voice.core.ui.icons.VoiceIcons
import voice.features.playbackScreen.BookPlayViewState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import voice.core.strings.R as StringsR

private val HorizontalInset = 12.dp

/**
 * Seek bar for the current chapter: a wave that ripples while audio plays and flattens when paused
 * or grabbed. Dragging pops up a bubble with the target time. Tap the right label to toggle
 * between remaining and total time. After a big jump, a pill between the labels offers the way back.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun SeekSection(
  playedTime: Duration,
  duration: Duration,
  playing: Boolean,
  clock: () -> Float,
  onSeek: (Duration) -> Unit,
  jumpBack: BookPlayViewState.JumpBackViewState?,
  onJumpBack: (id: Long) -> Unit,
  onJumpBackExpire: (id: Long) -> Unit,
  modifier: Modifier = Modifier,
  unavailable: Boolean = false,
) {
  var dragFraction by remember { mutableStateOf<Float?>(null) }
  var pendingFraction by remember { mutableStateOf<Float?>(null) }
  val progress = (playedTime / duration).toFloat().coerceIn(0F, 1F)
  LaunchedEffect(pendingFraction) {
    if (pendingFraction != null) {
      delay(1500.milliseconds)
      pendingFraction = null
    }
  }
  LaunchedEffect(progress) {
    val pending = pendingFraction
    if (pending != null && abs(pending - progress) < 0.01F) {
      pendingFraction = null
    }
  }
  val shownFraction = dragFraction ?: pendingFraction ?: progress
  val shownTime = duration * shownFraction.toDouble()
  val dragging = dragFraction != null
  var showRemaining by rememberSaveable { mutableStateOf(true) }

  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "章",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(24.dp),
      )
      WavySeekBar(
        fraction = shownFraction,
        dragging = dragging,
        playing = playing,
        clock = clock,
        bubbleText = formatTime(shownTime.inWholeMilliseconds, duration.inWholeMilliseconds),
        onDrag = { dragFraction = it },
        onSeek = { fraction ->
          dragFraction = null
          pendingFraction = fraction
          onSeek(duration * fraction.toDouble())
        },
        modifier = Modifier.weight(1F),
      )
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val elapsedColor by animateColorAsState(
        targetValue = if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "elapsedColor",
      )
      Text(
        text = formatTime(shownTime.inWholeMilliseconds, duration.inWholeMilliseconds),
        style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
        color = elapsedColor,
      )
      // the pill is a bit taller than the labels, it may overlap the space around the row
      Box(
        modifier = Modifier
          .weight(1F)
          .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            layout(constraints.maxWidth, 0) {
              placeable.place((constraints.maxWidth - placeable.width) / 2, -placeable.height / 2)
            }
          },
      ) {
        // a jump that expired while the player was closed shows no pill, so it doesn't hide this one
        val jumpBackVisibleFor = rememberJumpBackVisibleFor()
        if (unavailable && (jumpBack == null || jumpBack.elapsed >= jumpBackVisibleFor)) {
          OfflinePill(Modifier.padding(horizontal = 4.dp))
        } else {
          JumpBackPill(
            jumpBack = jumpBack,
            onClick = onJumpBack,
            onExpire = onJumpBackExpire,
            modifier = Modifier.padding(horizontal = 4.dp),
          )
        }
      }
      Text(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable { showRemaining = !showRemaining }
          .padding(horizontal = 4.dp, vertical = 2.dp),
        text = if (showRemaining) {
          "−" + formatTime((duration - shownTime).inWholeMilliseconds, duration.inWholeMilliseconds)
        } else {
          formatTime(duration.inWholeMilliseconds, duration.inWholeMilliseconds)
        },
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun WavySeekBar(
  fraction: Float,
  dragging: Boolean,
  playing: Boolean,
  clock: () -> Float,
  bubbleText: String,
  onDrag: (Float) -> Unit,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val haptics = LocalHapticFeedback.current
  val thumbDiameter = 12.dp
  val waveStrength by animateFloatAsState(
    targetValue = if (playing && !dragging) 1F else 0F,
    label = "chapterWaveStrength",
  )
  val bubbleScale by animateFloatAsState(
    targetValue = if (dragging) 1F else 0F,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMedium),
    label = "bubbleScale",
  )
  val currentFraction by rememberUpdatedState(fraction)
  val currentOnDrag by rememberUpdatedState(onDrag)
  val currentOnSeek by rememberUpdatedState(onSeek)
  var lastDragFraction by remember { mutableFloatStateOf(0F) }

  Layout(
    modifier = modifier,
    content = {
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(32.dp)
          .semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(current = fraction, range = 0F..1F)
            setProgress { target ->
              currentOnSeek(target.coerceIn(0F, 1F))
              true
            }
          }
          .pointerInput(Unit) {
            detectTapGestures { offset ->
              val inset = HorizontalInset.toPx()
              currentOnSeek(((offset.x - inset) / (size.width - 2 * inset)).coerceIn(0F, 1F))
            }
          }
          .pointerInput(Unit) {
            val inset = HorizontalInset.toPx()
            fun fractionAt(x: Float) = ((x - inset) / (size.width - 2 * inset)).coerceIn(0F, 1F)
            detectHorizontalDragGestures(
              onDragStart = { offset ->
                lastDragFraction = fractionAt(offset.x)
                currentOnDrag(lastDragFraction)
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
              },
              onDragEnd = {
                currentOnSeek(lastDragFraction)
                haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
              },
              onDragCancel = { currentOnSeek(lastDragFraction) },
              onHorizontalDrag = { change, _ ->
                change.consume()
                lastDragFraction = fractionAt(change.position.x)
                currentOnDrag(lastDragFraction)
              },
            )
          }
          .drawWithCache {
            onDrawBehind {
              drawWavyTrack(
                fraction = currentFraction,
                thumbWidth = thumbDiameter.toPx(),
                thumbHeight = thumbDiameter.toPx(),
                activeColor = colors.primary,
                inactiveColor = colors.secondaryContainer,
                waveStrength = waveStrength,
                wavePhase = clock(),
              )
            }
          },
      )
      Box(
        modifier = Modifier
          .graphicsLayer {
            scaleX = bubbleScale
            scaleY = bubbleScale
            alpha = bubbleScale.coerceIn(0F, 1F)
            transformOrigin = TransformOrigin(0.5F, 1F)
          }
          .background(colors.primary, RoundedCornerShape(percent = 50))
          .padding(horizontal = 14.dp, vertical = 6.dp),
      ) {
        Text(
          text = bubbleText,
          style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
          color = colors.onPrimary,
        )
      }
    },
  ) { measurables, constraints ->
    val bar = measurables[0].measure(constraints)
    val bubble = measurables[1].measure(Constraints())
    // Keep the 32dp interactive hit area but overlap its unused vertical margins.
    val visualHeight = 18.dp.roundToPx()
    val barOffset = (bar.height - visualHeight) / 2
    layout(bar.width, visualHeight) {
      bar.place(0, -barOffset)
      val inset = HorizontalInset.toPx()
      val thumbX = inset + (bar.width - 2 * inset) * currentFraction
      val x = (thumbX - bubble.width / 2F).roundToInt()
        .coerceIn(0, (bar.width - bubble.width).coerceAtLeast(0))
      bubble.place(x, -bubble.height + 2.dp.roundToPx() - barOffset)
    }
  }
}

private fun DrawScope.drawWavyTrack(
  fraction: Float,
  thumbWidth: Float,
  thumbHeight: Float,
  activeColor: Color,
  inactiveColor: Color,
  showThumb: Boolean = true,
  waveStrength: Float = 0F,
  wavePhase: Float = 0F,
) {
  val inset = HorizontalInset.toPx()
  val strokeWidth = 5.dp.toPx()
  val gap = if (showThumb) 5.dp.toPx() else 0F
  val centerY = size.height / 2
  val end = size.width - inset
  val thumbX = inset + (end - inset) * fraction
  val activeEnd = thumbX - (if (showThumb) thumbWidth / 2 + gap else 0F)
  if (activeEnd > inset) {
    if (waveStrength > 0.001F) {
      val path = Path()
      val amplitude = 3.dp.toPx() * waveStrength
      val wavelength = 36.dp.toPx()
      val phase = wavePhase * 2F * PI.toFloat()
      var x = inset
      path.moveTo(x, centerY)
      while (x < activeEnd) {
        x = (x + 3.dp.toPx()).coerceAtMost(activeEnd)
        val wave = sin((x - inset) / wavelength * 2F * PI.toFloat() - phase)
        path.lineTo(x, centerY + amplitude * wave)
      }
      drawPath(path = path, color = activeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
    } else {
      drawLine(
        color = activeColor,
        start = Offset(inset, centerY),
        end = Offset(activeEnd, centerY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
      )
    }
  }

  val inactiveStart = thumbX + (if (showThumb) thumbWidth / 2 + gap else 0F)
  if (inactiveStart < end) {
    drawLine(
      color = inactiveColor,
      start = Offset(inactiveStart, centerY),
      end = Offset(end, centerY),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round,
    )
    if (showThumb) drawCircle(color = activeColor, radius = 1.5.dp.toPx(), center = Offset(end, centerY))
  }

  if (showThumb) {
    drawCircle(
      color = activeColor,
      radius = thumbWidth / 2,
      center = Offset(thumbX, centerY),
    )
  }
}

/**
 * Takes the place of the jump back pill while the book can't stream.
 */
@Composable
private fun OfflinePill(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceContainerHighest)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = VoiceIcons.CloudOff,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(16.dp),
    )
    Spacer(Modifier.width(6.dp))
    Text(
      text = stringResource(StringsR.string.library_book_offline),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
  }
}

/**
 * Read-only progress for the current volume. Uses the chapter seek track's
 * exact geometry without pointer handlers or seeking.
 */
@Composable
internal fun VolumeProgressSection(
  playedTime: Duration,
  duration: Duration,
  playbackSpeed: Float,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val fraction = (playedTime / duration).toFloat().coerceIn(0F, 1F)
  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "巻",
        style = MaterialTheme.typography.labelLarge,
        color = colors.onSurfaceVariant,
        modifier = Modifier.width(24.dp),
      )
      Canvas(
        modifier = Modifier
          .weight(1F)
          .height(18.dp),
      ) {
        drawWavyTrack(
          fraction = fraction,
          thumbWidth = 5.dp.toPx(),
          thumbHeight = 0F,
          activeColor = colors.primary,
          inactiveColor = colors.secondaryContainer,
          showThumb = false,
        )
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = formatTime(playedTime.inWholeMilliseconds, duration.inWholeMilliseconds),
        style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
        color = colors.onSurfaceVariant,
      )
      Spacer(Modifier.weight(1F))
      Text(
        text = "−" + formatTime(((duration - playedTime) / playbackSpeed.coerceAtLeast(0.1F).toDouble()).inWholeMilliseconds),
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
        color = colors.onSurfaceVariant,
      )
    }
  }
}
