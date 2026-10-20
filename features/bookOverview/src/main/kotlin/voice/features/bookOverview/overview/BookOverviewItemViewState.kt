package voice.features.bookOverview.overview

import android.provider.DocumentsContract
import androidx.compose.runtime.Immutable
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.audiobookshelf.download.WaitingFor
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.logging.api.Logger
import voice.core.ui.formatTime

@Immutable
data class BookOverviewItemViewState(
  val name: String,
  val author: String?,
  val cover: String?,
  val progress: Float,
  val id: BookId,
  val remainingTime: String,
  /** A server book that isn't downloaded while the server can't be reached. */
  val unavailable: Boolean = false,
  val download: DownloadBadge? = null,
  /** Audio file used to show the current volume's embedded cover in the library. */
  val volumeAudioUri: String? = null,
)

@Immutable
sealed interface DownloadBadge {
  data class Running(
    val progress: Float,
    val waitingFor: WaitingFor?,
  ) : DownloadBadge

  data object Failed : DownloadBadge
}

internal fun BookDownloadState?.badge(): DownloadBadge? = when (this) {
  is BookDownloadState.Downloading -> DownloadBadge.Running(progress, waitingFor)
  BookDownloadState.Failed -> DownloadBadge.Failed
  is BookDownloadState.Downloaded, BookDownloadState.NotDownloaded, null -> null
}

internal fun Book.toItemViewState(
  unavailable: Boolean = false,
  download: DownloadBadge? = null,
) = BookOverviewItemViewState(
  name = libraryDisplayName(),
  author = content.metadata.author,
  cover = content.coverUrl,
  volumeAudioUri = if (!id.isRemote && chapters.size > 1) currentChapter.id.value else null,
  id = id,
  progress = progress(),
  remainingTime = formatTime(duration - position),
  unavailable = unavailable,
  download = download,
)

private fun Book.progress(): Float {
  val globalPosition = position
  val totalDuration = duration
  val progress = globalPosition.toFloat() / totalDuration.toFloat()
  if (progress < 0F) {
    Logger.w("Negative progress: position=$globalPosition, duration=$totalDuration")
  }
  return progress.coerceIn(0F, 1F)
}

/** The library can show a series folder without changing playback metadata. */
private fun Book.libraryDisplayName(): String {
  if (id.isRemote || chapters.size < 2 || chapters.first().id.value == id.value) return content.name
  val uri = id.toUri()
  if (uri.scheme != "content" || "document" !in uri.pathSegments) return content.name
  val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return content.name
  // External storage IDs contain a path, e.g. primary:Books/Series.
  // Opaque IDs from other document providers should keep the original name.
  val folderName = documentId.substringAfterLast('/').trim()
  return folderName.takeIf { '/' in documentId && it.isNotBlank() } ?: content.name
}
