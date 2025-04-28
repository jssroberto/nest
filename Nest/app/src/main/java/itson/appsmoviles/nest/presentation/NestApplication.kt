package itson.appsmoviles.nest.presentation

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class NestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase Realtime Database for caching
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}