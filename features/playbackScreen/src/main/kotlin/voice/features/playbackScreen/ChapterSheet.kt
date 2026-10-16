@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import voice.core.ui.NowPlayingBars
import voice.core.strings.R as StringsR

@Composable
internal fun ChapterSheet(
  dialogState: BookPlayDialogViewState.SelectChapterDialog,
  playing: Boolean,
  onChapterClick: (number: Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val items = dialogState.items
  val selectedIndex = items.indexOfFirst { it.active }
  // Keep the original flat chapter numbers for seeking. Only the UI is grouped.
  val distinctVolumes = items.mapNotNull { it.volume }.distinct()
  val grouped = distinctVolumes.size > 1
  val groups = if (grouped) items.groupBy { it.volume.orEmpty() } else emptyMap()
  val currentVolume = items.getOrNull(selectedIndex)?.volume.orEmpty()
  val expanded = remember(dialogState) {
    mutableStateListOf<String>().apply { if (grouped) add(currentVolume) }
  }
  val visibleItems = if (grouped) buildList {
    groups.forEach { (volume, chapters) ->
      add(VolumeRow.Header(volume))
      if (volume in expanded) chapters.forEach { add(VolumeRow.Chapter(it)) }
    }
  } else items.map { VolumeRow.Chapter(it) }
  val activeVisibleIndex = visibleItems.indexOfFirst { it is VolumeRow.Chapter && it.item.active }
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberBottomSheetState(
      initialValue = Hidden,
      enabledValues = setOf(Hidden, Expanded),
    ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        modifier = Modifier.weight(1F),
        text = stringResource(StringsR.string.playback_chapters_title),
        style = MaterialTheme.typography.headlineSmallEmphasized,
      )
      if (selectedIndex >= 0) {
        Text(
          text = "${selectedIndex + 1} / ${items.size}",
          style = MaterialTheme.typography.labelLargeEmphasized,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
    if (grouped) {
      Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        TextButton(onClick = { expanded.clear(); expanded.addAll(groups.keys) }) { Text("すべて開く") }
        TextButton(onClick = { expanded.clear() }) { Text("すべて閉じる") }
      }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (activeVisibleIndex - 1).coerceAtLeast(0))
    LazyColumn(
      state = listState,
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      itemsIndexed(visibleItems, key = { _, row -> when(row) {
        is VolumeRow.Header -> "volume:${row.title}"
        is VolumeRow.Chapter -> "chapter:${row.item.number}"
      } }) { _, row ->
        when(row) {
          is VolumeRow.Header -> Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (row.title == currentVolume) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(12.dp),
            onClick = { if (row.title in expanded) expanded.remove(row.title) else expanded.add(row.title) },
          ) {
            Text(
              text = "${if (row.title in expanded) "▼" else "▶"}  ${row.title}${if (row.title == currentVolume) "  ●" else ""}",
              modifier = Modifier.padding(16.dp),
              style = MaterialTheme.typography.titleMedium,
            )
          }
          is VolumeRow.Chapter -> ChapterItem(
            chapter = row.item,
            played = selectedIndex >= 0 && row.item.number < items[selectedIndex].number,
            playing = playing,
            shape = RoundedCornerShape(8.dp),
            onClick = { onChapterClick(row.item.number) },
          )
        }
      }
    }
  }
}

@Composable
private fun ChapterItem(
  chapter: BookPlayDialogViewState.SelectChapterDialog.ItemViewState,
  played: Boolean,
  playing: Boolean,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val currentDescription = stringResource(StringsR.string.playback_chapter_current_content_description)
  Surface(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .semantics {
        selected = chapter.active
        if (chapter.active) contentDescription = currentDescription
      },
    shape = shape,
    color = if (chapter.active) colors.primaryContainer else colors.surfaceContainerHigh,
    contentColor = if (chapter.active) colors.onPrimaryContainer else colors.onSurface,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(
            color = if (chapter.active) colors.primary else colors.secondaryContainer,
            shape = if (chapter.active) MaterialShapes.Cookie9Sided.toShape() else CircleShape,
          ),
        contentAlignment = Alignment.Center,
      ) {
        if (chapter.active) {
          NowPlayingBars(playing = playing, color = colors.onPrimary)
        } else {
          Text(
            text = chapter.number.toString(),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = colors.onSecondaryContainer,
          )
        }
      }
      Spacer(Modifier.width(16.dp))
      Text(
        modifier = Modifier.weight(1F),
        text = chapter.name,
        style = if (chapter.active) MaterialTheme.typography.titleMediumEmphasized else MaterialTheme.typography.bodyLarge,
        color = if (played) colors.onSurfaceVariant else Color.Unspecified,
        maxLines = 2,
      )
      Spacer(Modifier.width(12.dp))
      Text(
        text = chapter.time,
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        color = colors.onSurfaceVariant,
      )
    }
  }
}

private sealed interface VolumeRow {
  data class Header(val title: String) : VolumeRow
  data class Chapter(val item: BookPlayDialogViewState.SelectChapterDialog.ItemViewState) : VolumeRow
}
