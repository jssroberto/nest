package itson.appsmoviles.nest

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class NestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}