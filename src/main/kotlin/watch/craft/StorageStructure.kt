package watch.craft

import com.google.common.annotations.VisibleForTesting
import watch.craft.network.CachingRetriever
import watch.craft.network.FailingRetriever
import watch.craft.network.NetworkRetriever
import watch.craft.network.NetworkRetriever.Config
import watch.craft.network.Retriever
import watch.craft.storage.*
import watch.craft.utils.ZONE_OFFSET
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StorageStructure(
  dateString: String? = null,
  private val forceDownload: Boolean = false,
  private val localStore: ObjectStore = LocalObjectStore(LOCAL_STORAGE_DIR),
  private val remoteStore: ObjectStore = GcsObjectStore(GCS_BUCKET)
) {
  private val live = dateString == null

  private val instant = if (live) {
    Instant.now()
  } else {
    LocalDate.parse(dateString).atStartOfDay(ZONE_OFFSET).toInstant()
  }

  val results = remoteStore
    .run { if (runningOnCanonicalCiBranch) this else readOnly() }
    .frontedBy(localStore)
    .resolve(RESULTS_DIRNAME)

  @VisibleForTesting
  suspend fun downloads(id: String) = remoteStore
    .frontedBy(localStore)
    .resolve("${DOWNLOADS_DIR}/${id}")
    .targetDir()

  val createRetriever: suspend (String, Boolean) -> Retriever = { id, failOn404 ->
    CachingRetriever(
      downloads(id),
      if (live) {
        NetworkRetriever(Config(id = id, failOn404 = failOn404))
      } else {
        FailingRetriever()
      }
    )
  }

  private suspend fun ObjectStore.targetDir(): ObjectStore {
    val today = DATE_FORMAT.format(instant)

    val latest = list()
      .sorted()
      .lastOrNull { it.startsWith(today) }

    val subdir = if (latest == null) {
      today
    } else {
      val parts = latest.split("--")
      val idx = if (parts.size < 2) 0 else parts.last().toInt()
      val idxAdjusted = idx + if (forceDownload) 1 else 0

      if (idxAdjusted == 0) {
        today
      } else {
        "${today}--${idxAdjusted.toString().padStart(3, '0')}"
      }
    }

    return resolve(subdir)
  }

  companion object {
    const val DOWNLOADS_DIR = "downloads"
    const val RESULTS_DIRNAME = "results"

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("YYYY-MM-dd").withZone(ZONE_OFFSET)
  }
}
