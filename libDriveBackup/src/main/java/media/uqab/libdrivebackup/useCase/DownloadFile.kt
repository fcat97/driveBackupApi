package media.uqab.libdrivebackup.useCase

import androidx.annotation.WorkerThread
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Class to download file from the application data folder.
 */
object DownloadFile {
    /**
     * Download a file from the application data folder.
     *
     * @param credential [GoogleAccountCredential]
     * @param fileID google drive's file id to download
     * @return downloaded files.
     * @throws [IOException], [IllegalStateException], [GoogleJsonResponseException]
     */
    @Throws(IOException::class, IllegalStateException::class, GoogleJsonResponseException::class)
    @WorkerThread
    fun downloadFile(credential: GoogleAccountCredential, fileID: String): ByteArrayOutputStream {
        val outputStream: ByteArrayOutputStream = ByteArrayOutputStream()

        getService(credential).files().get(fileID)
            .executeMediaAndDownloadTo(outputStream)

        return outputStream
    }
}