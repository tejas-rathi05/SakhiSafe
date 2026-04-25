package com.heysafe.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = FirebaseApp.getInstance()
        Log.d("HeySafe", "Firebase initialized: ${app.name} / ${app.options.projectId}")
        setContent { HeySafeApp() }
    }
}
