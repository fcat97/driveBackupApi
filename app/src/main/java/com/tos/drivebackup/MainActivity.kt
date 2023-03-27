package com.tos.drivebackup

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import com.google.android.material.color.DynamicColors
import com.google.android.material.textfield.TextInputEditText
import com.tos.drivebackup.drive_backup.DriveBackupUtils
import media.uqab.libdrivebackup.GoogleDriveBackupManager
import java.io.File
import java.io.FileReader

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val CLIENT_ID_WEB = "109876320882-22lsfuhmi4cpjhiolke0eb5ngg61rhve.apps.googleusercontent.com"
        private const val CLIENT_ID_ANDROID = "109876320882-llmhgsaqpablpupq0b443unnmjtiidek.apps.googleusercontent.com"
        private const val MIME_TYPE: String = "application/json"
    }
    private val terminalOutputLiveData = MutableLiveData("")
    
    private lateinit var rootFolderButton: Button
    private lateinit var sendButton: Button
    private lateinit var fetchButton: Button
    private lateinit var deleteButton: Button
    private lateinit var downloadButton: Button
    private lateinit var downloadDemoBackupButton: Button
    private lateinit var createDemoBackupButton: Button
    private lateinit var clearTerminalButton: Button
    private lateinit var editText: TextInputEditText
    private lateinit var terminal: TextView
    private lateinit var emailTextView: TextView
    private lateinit var signOutButton: Button
    private lateinit var signInButton: Button

    private val googleDriveBackupManager = GoogleDriveBackupManager(
        appID = BuildConfig.APPLICATION_ID,
        activity = this,
        credentialID = CLIENT_ID_WEB
    )

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        printToTerminal("disabled for now")
        /*val file = it.toFile(this) ?: return@registerForActivityResult

        googleDriveBackupManager.uploadFile(file, MIME_TYPE) { path ->
            printToTerminal("[output]: \n$path")
        }*/
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivitiesIfAvailable(this.application)
        setContentView(R.layout.main_activity)

        rootFolderButton = findViewById(R.id.createFolderButton)
        sendButton = findViewById(R.id.sendButton)
        fetchButton = findViewById(R.id.fetchButton)
        downloadButton = findViewById(R.id.downloadButton)
        downloadDemoBackupButton = findViewById(R.id.downloadDemoButton)
        createDemoBackupButton = findViewById(R.id.createDemoBackupButton)
        clearTerminalButton = findViewById(R.id.clearTerminalButton)
        deleteButton = findViewById(R.id.deleteButton)
        editText = findViewById(R.id.edit_text)
        terminal = findViewById(R.id.terminal)
        emailTextView = findViewById(R.id.emailTextView)
        signOutButton = findViewById(R.id.signOutButton)
        signInButton = findViewById(R.id.signInButton)

        createDemoBackupButton.setOnClickListener { createDemoBackup() }
        downloadDemoBackupButton.setOnClickListener { downloadDemoBackup() }

        rootFolderButton.setOnClickListener {
            googleDriveBackupManager.createRootFolder {
                printToTerminal(it)
            }
        }
        sendButton.setOnClickListener { startSendFlow() }
        fetchButton.setOnClickListener { fetchFiles() }
        downloadButton.setOnClickListener { downloadFile() }
        deleteButton.setOnClickListener {
            val fileID = editText.text.toString()
            if (fileID.isEmpty() || fileID == "null") return@setOnClickListener

            googleDriveBackupManager.deleteFile(fileID) {
                Toast.makeText(this@MainActivity, "file deleted", Toast.LENGTH_SHORT).show()
                fetchFiles()
            }
        }
        signInButton.setOnClickListener { singIn() }
        signOutButton.setOnClickListener { signOut() }
        clearTerminalButton.setOnClickListener { terminalOutputLiveData.value = "" }

        terminalOutputLiveData.observe(this) { terminal.text = it }

        getCurrentEmail()
        printToTerminal("[Welcome to drive backup]:")
    }

    private fun startSendFlow() {
        filePicker.launch(arrayOf(MIME_TYPE))
    }

    private fun fetchFiles() {
        googleDriveBackupManager.getFiles {
            printToTerminal("[backups]: \n" + it.joinToString(separator = "\n"))
        }
    }

    private fun downloadFile() {
        val fileID = editText.text.toString()
        if (fileID.isEmpty() || fileID == "null") return

        googleDriveBackupManager.getFile(
            fileID = fileID,
            onFailed = { printToTerminal(it.message) }
        ) { info ->
            val outputFile = File(getExternalFilesDir(null), info.name)

            googleDriveBackupManager.downloadFile(
                fileID = fileID,
                outputFile = outputFile,
                onFailed = {
                    printToTerminal(it.message)
                    Log.e(TAG, "onCreate: download filed", it)
                },
                onDownload = { f ->
                    printToTerminal("[downloaded]:\n${FileReader(f).readText()}")
                }
            )
        }
    }

    private fun createDemoBackup() {
        DriveBackupUtils.backupToDrive(this, googleDriveBackupManager) {
            printToTerminal(it)
        }
    }

    private fun downloadDemoBackup() {
        DriveBackupUtils.restoreBackup(this, googleDriveBackupManager) {
            printToTerminal(it)
        }
    }

    private fun getCurrentEmail() {
        googleDriveBackupManager.getCurrentUser(
            onFailed = {
                Log.d(TAG, "setCurrentUser failed ${it.message}")
            },
            currentUser = {
                emailTextView.text = "${it.name}\n${it.email}"
            }
        )
    }

    private fun singIn() {
        googleDriveBackupManager.signIn(
            onFailed = null,
            onSuccess = {
                printToTerminal(it.toString())
            }
        )
    }

    private fun signOut() {
        googleDriveBackupManager.signOut(
            onFailed = {
                printToTerminal("signOut failed ${it.message}")
                Log.d(TAG, "signOut failed ${it.message}")
            },
            onSuccess = {
                printToTerminal("signOut successful")
                Log.d(TAG, "signOut successful")
            }
        )
    }
    
    private fun printToTerminal(msg: String?) {
        val newOutput = msg + "\n\n" + (terminalOutputLiveData.value ?: "")
        terminalOutputLiveData.value = newOutput
    }
}
