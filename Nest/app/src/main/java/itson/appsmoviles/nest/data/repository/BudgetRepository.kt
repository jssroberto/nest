package itson.appsmoviles.nest.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException

class BudgetRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // TODO: Replace placeholder with actual Firebase database call
     suspend fun fetchBudget(userId: String): Double {
        return try {
            // Example: Fetching a single value
            // val snapshot = database.child("users").child(userId).child("profile").child("budget").get().await()
            // snapshot.getValue(Double::class.java) ?: 150.0 // Default if null or not found
            150.0
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error fetching budget", e)
            throw IOException("Error fetching budget", e)
        }
    }
}