package media.uqab.libdrivebackup.useCase

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.model.File

/**
 * Class to get detail information about a single file
 */
object GetFile {
    /**
     * Get Drive's file detail
     *
     * @param fileID id to query
     * @param credentials [GoogleAccountCredential]
     */
    fun getFile(
        fileID: String,
        credentials: GoogleAccountCredential
    ): File {
        return getService(credentials).files()
            .get(fileID)
            .execute()
    }
}