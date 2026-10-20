@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons
import voice.features.playbackScreen.BookPlayViewState

@Composable
internal fun PlaybackTopBar(
  viewState: BookPlayViewState,
  onSleepTimerClick: () -> Unit,
  onSpeedChangeClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onBookmarkLongClick: () -> Unit,
  onSkipSilenceClick: () -> Unit,
  onVolumeBoostClick: () -> Unit,
  onCloseClick: () -> Unit,
  onBookActionsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CloseButton(onClick = onCloseClick)
    Spacer(Modifier.weight(1F))
    ActionToolbar(
      sleepTimerState = viewState.sleepTimerState,
      playbackSpeed = viewState.playbackSpeed,
      skipSilence = viewState.skipSilence,
      volumeBoostActive = viewState.volumeBoostActive,
      compact = true,
      onSleepTimerClick = onSleepTimerClick,
      onSpeedClick = onSpeedChangeClick,
      onBookmarkClick = onBookmarkClick,
      onBookmarkLongClick = onBookmarkLongClick,
      onSkipSilenceClick = onSkipSilenceClick,
      onVolumeBoostClick = onVolumeBoostClick,
    )
    Spacer(Modifier.weight(1F))
    BookActionsButton(onClick = onBookActionsClick)
  }
}

@Composable
internal fun ChaptersButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  TopBarButton(
    icon = VoiceIcons.ViewList,
    contentDescription = stringResource(R.string.playback_chapters_title),
    onClick = onClick,
    modifier = modifier,
  )
}

@Composable
internal fun BookActionsButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  TopBarButton(
    icon = VoiceIcons.MoreVert,
    contentDescription = stringResource(R.string.common_action_more),
    onClick = onClick,
    modifier = modifier,
  )
}

@Composable
internal fun CloseButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  TopBarButton(
    icon = VoiceIcons.KeyboardArrowDown,
    contentDescription = stringResource(R.string.common_action_close),
    onClick = onClick,
    modifier = modifier,
  )
}

@Composable
private fun TopBarButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilledTonalIconButton(
    onClick = onClick,
    modifier = modifier.size(44.dp),
    shape = CircleShape,
    colors = topBarButtonColors(),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
    )
  }
}

@Composable
private fun topBarButtonColors(): IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(
  containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9F),
)
