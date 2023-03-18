package com.tos.drivebackup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import media.uqab.libdrivebackup.SignInActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, SignInActivity::class.java))
    }
}
