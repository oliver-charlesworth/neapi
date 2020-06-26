package choliver.neapi.storage

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import java.io.FileNotFoundException

class GcsBacker(private val bucketName: String) : Backer {
  private val storage = StorageOptions.getDefaultInstance().service

  override fun write(key: String, content: ByteArray) {
    storage.create(blobInfo(key), content)
  }

  override fun read(key: String) = try {
    storage.readAllBytes(blobId(key))!!
  } catch (e: StorageException) {
    if (e.code == 404) {
      throw FileNotFoundException()
    } else {
      throw e
    }
  }

  private fun blobInfo(key: String) = BlobInfo.newBuilder(blobId(key)).build()

  private fun blobId(key: String) = BlobId.of(bucketName, key)
}
