package media.uqab.libdrivebackup

import android.content.Context
import android.net.Uri
import java.io.File

object Helper {
    fun Uri?.toFile(context: Context): File? = try {
        if (this == null) {
            null
        } else {
            FileUtils(context).copyToAppsDir(this, null)
        }
    } catch (e: Exception) {
        null
    }
}