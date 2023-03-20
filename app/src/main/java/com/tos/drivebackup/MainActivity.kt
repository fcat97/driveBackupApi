package com.tos.drivebackup

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import media.uqab.libdrivebackup.GoogleDriveBackupManager
import media.uqab.libdrivebackup.util.Helper.toFile
import java.io.File

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val CLIENT_ID_WEB = "109876320882-22lsfuhmi4cpjhiolke0eb5ngg61rhve.apps.googleusercontent.com"
        private const val CLIENT_ID_ANDROID = "109876320882-llmhgsaqpablpupq0b443unnmjtiidek.apps.googleusercontent.com"
        private const val MIME_TYPE: String = "application/json"
        val terminalOutput = MutableLiveData("")
    }

    private lateinit var rootFolderButton: Button
    private lateinit var sendButton: Button
    private lateinit var fetchButton: Button
    private lateinit var deleteButton: Button
    private lateinit var downloadButton: Button
    private lateinit var editText: EditText
    private lateinit var terminal: TextView

    private val googleDriveBackupManager = GoogleDriveBackupManager(BuildConfig.APPLICATION_ID, this, CLIENT_ID_WEB)

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        val file = it.toFile(this) ?: return@registerForActivityResult

        googleDriveBackupManager.uploadFile(file, MIME_TYPE) { path ->
            terminalOutput.postValue("[output]: \n$path")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        rootFolderButton = findViewById(R.id.createFolderButton)
        sendButton = findViewById(R.id.sendButton)
        fetchButton = findViewById(R.id.fetchButton)
        downloadButton = findViewById(R.id.downloadButton)
        deleteButton = findViewById(R.id.deleteButton)
        editText = findViewById(R.id.edit_text)
        terminal = findViewById(R.id.terminal)

        rootFolderButton.setOnClickListener {
            googleDriveBackupManager.createRootFolder {
                terminalOutput.postValue(it)
            }
        }
        sendButton.setOnClickListener { startSendFlow() }
        fetchButton.setOnClickListener { fetchFiles() }
        downloadButton.setOnClickListener {
            val fileID = editText.text.toString()
            if (fileID.isEmpty() || fileID == "null") return@setOnClickListener

            val outputFile = File(getExternalFilesDir(null), "backup.json")
            googleDriveBackupManager.downloadFile(
                fileID = fileID,
                outputFile = outputFile,
                onFailed = {
                    Log.e(TAG, "onCreate: download filed", it)
                },
                onDownload = {
                    terminalOutput.postValue(
                        "[output]: \n" + it.name + "-->" + it.path
                    )
                    Log.d(TAG, "onCreate: ${it.exists()}")
                }
            )

        }
        deleteButton.setOnClickListener {
            val fileID = editText.text.toString()
            if (fileID.isEmpty() || fileID == "null") return@setOnClickListener

            googleDriveBackupManager.deleteFile(fileID) {
                Toast.makeText(this@MainActivity, "file deleted", Toast.LENGTH_SHORT).show()
                fetchFiles()
            }
        }

        terminalOutput.observe(this) { terminal.text = it }
    }

    private fun fetchFiles() {
        googleDriveBackupManager.getBackupIDs {
            terminalOutput.postValue("[output]: \n" + it.joinToString(separator = "\n") {f -> f.name + f.extension + "(${it.size} bytes)" + "--->" + f.fileID + " " + f.webLink} )
        }
    }

    private fun startSendFlow() {
        filePicker.launch(arrayOf(MIME_TYPE))
    }
}
