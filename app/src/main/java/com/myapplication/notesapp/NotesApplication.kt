package com.myapplication.notesapp

import android.app.Application
import com.google.firebase.FirebaseApp

class NotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}