package media.uqab.libdrivebackup.useCase

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import media.uqab.libdrivebackup.model.Constants

/**
 * Build a new authorized API client service.
 * @return [Drive] client
 **/
internal fun getService(credentials: GoogleAccountCredential): Drive {
    return Drive.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credentials
    ).setApplicationName(Constants.APP_NAME)
        .build()
}