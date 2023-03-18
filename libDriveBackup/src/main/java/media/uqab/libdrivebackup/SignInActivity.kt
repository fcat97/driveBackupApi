package media.uqab.libdrivebackup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import media.uqab.libdrivebackup.Helper.toFile
import java.io.File
import java.security.AccessControlContext
import java.util.*


open class SignInActivity: ComponentActivity() {
    private val CLIENT_ID_WEB = "109876320882-22lsfuhmi4cpjhiolke0eb5ngg61rhve.apps.googleusercontent.com"

    companion object {
        private const val TAG = "SignInActivity"
        private const val RC_SIGN_IN = 1098765423

        /**
         * Key to pass [File] path if it is an [Operation.UPLOAD_FILE].
         */
        const val EXTRA_FILE_PATH = "SignInActivity.file_path"

        /**
         * Key to pass an [Operation].
         */
        const val EXTRA_OPERATION = "SignInActivity.file_path"

        val terminalOutput = MutableLiveData("")
    }

    private lateinit var filePath: String
    private lateinit var sendButton: Button
    private lateinit var fetchButton: Button
    private lateinit var terminal: TextView

    private var operation: Operation? = null
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        filePath = it.toFile(this)?.path ?: return@registerForActivityResult

        operation = Operation.UPLOAD_FILE
        requestAccountAccess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        sendButton = findViewById(R.id.sendButton)
        fetchButton = findViewById(R.id.fetchButton)
        terminal = findViewById(R.id.terminal)

        sendButton.setOnClickListener { startSendFlow() }
        fetchButton.setOnClickListener { startFetchFlow() }

        terminalOutput.observe(this) {
            terminal.text = it
        }
    }

    private fun startFetchFlow() {
        operation = Operation.READ_FILE_LIST
        requestAccountAccess()
    }

    private fun startSendFlow() {
        filePicker.launch(arrayOf("application/json"))
    }

    private fun requestAccountAccess() {
        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(CLIENT_ID_WEB)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()

        val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(this, gso)

        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "onActivityResult: ${data?.extras}")

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "handleSignInResult: \n" + """
                ${account.account}
                ${account.id}
                ${account.givenName}
            """.trimIndent())
            // Signed in successfully, show authenticated UI.

            val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(
                this,
                Collections.singleton(DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccountName = account.email

            Log.d(TAG, "handleSignInResult: $credential")

            when(operation) {
                Operation.UPLOAD_FILE -> {
                    val file = File(filePath)
                    Log.d(TAG, "handleSignInResult: ${file.path} ${file.extension}")
                    if (file.extension != "json") return

                    Thread {
                        try {
                            UploadAppData.uploadAppData(credential, file, "application/json")
                        } catch (e: Exception) {
                            Log.d(TAG, "handleSignInResult: ${e.message}")
                        }
                    }.start()
                }
                Operation.READ_FILE_LIST -> {
                    Thread {
                        try {
                            ListAppData.listAppData(credential)
                        } catch (e: Exception) {
                            Log.d(TAG, "handleSignInResult: ${e.message}")
                        }
                    }.start()
                }

                else -> {
                    Log.d(TAG, "handleSignInResult: no $operation")
                }
            }
        } catch (e: Exception) {
            // The ApiException status code indicates the detailed failure reason.

            when(e) {
                is ApiException -> Log.e(TAG, "handleSignInResult: ${e.status}", e)
                else -> Log.e(TAG, "signInResult:failed $e")
            }

            e.printStackTrace()
        }
    }
}