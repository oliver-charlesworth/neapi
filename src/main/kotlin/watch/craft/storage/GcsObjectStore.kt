package watch.craft.storage

import com.google.cloud.http.HttpTransportOptions
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Bucket.BlobTargetOption.doesNotExist
import com.google.cloud.storage.Storage
import com.google.cloud.storage.Storage.BlobListOption.currentDirectory
import com.google.cloud.storage.Storage.BlobListOption.prefix
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import com.google.common.base.Throwables.getRootCause
import watch.craft.FatalScraperException
import watch.craft.executor.onIoThread
import java.net.SocketTimeoutException

class GcsObjectStore(
  bucketName: String,
  storage: Storage = createGcsService()
) : ObjectStore {
  private val bucket = storage.get(bucketName)

  override suspend fun write(key: String, content: ByteArray) {
    try {
      withTimeoutRetries { create(key, content, doesNotExist()) }
    } catch (e: StorageException) {
      if (e.code == 412) {
        throw FileExistsException(key)
      } else {
        throw FatalScraperException("Error writing to GCS: ${key}", e)
      }
    }
  }

  override suspend fun read(key: String) = try {
    withTimeoutRetries { get(key)?.getContent() }
      ?: throw FileDoesntExistException(key)
  } catch (e: StorageException) {
    throw FatalScraperException("Error reading from GCS: ${key}", e)
  }

  override suspend fun list(key: String) = try {
    val prefix = key.normaliseAsPrefix()
    withTimeoutRetries { list(prefix(prefix), currentDirectory()).iterateAll() }
      .map { it.name.removePrefix(prefix).removeSuffix("/") }
  } catch (e: StorageException) {
    if (e.code == 404) {
      throw FileDoesntExistException(key)
    } else {
      throw FatalScraperException("Error reading from GCS: ${key}", e)
    }
  }

  // TODO - needs to be cancellable
  // Retry settings for GCS client do not seem to cause it to retry on timeout, so handle this manually.
  private suspend fun <R> withTimeoutRetries(block: Bucket.() -> R): R {
    var exception: StorageException? = null
    repeat(MAX_RETRIES) {
      try {
        return onIoThread { block(bucket) }
      } catch (e: StorageException) {
        if (getRootCause(e) is SocketTimeoutException) {
          exception = e
        } else {
          throw e
        }
      }
    }
    throw exception!!
  }

  private fun String.normaliseAsPrefix() = when {
    this in listOf("", "/") -> ""   // Root is slightly inconsistent
    endsWith("/") -> this
    else -> "${this}/"
  }

  companion object {
    fun createGcsService() = StorageOptions.newBuilder().apply {
      setTransportOptions(HttpTransportOptions.newBuilder().apply {
        setConnectTimeout(TIMEOUT_MILLIS)
        setReadTimeout(TIMEOUT_MILLIS)
      }.build())
    }.build().service!!

    private const val TIMEOUT_MILLIS = 10_000
    private const val MAX_RETRIES = 5
  }


}
