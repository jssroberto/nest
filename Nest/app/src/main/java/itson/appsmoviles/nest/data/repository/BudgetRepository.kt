package itson.appsmoviles.nest.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.data.model.Budget
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
class BudgetRepository {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance()
        .getReference("users")
        .child(userId ?: "unknown")

    fun saveBudget(budget: Budget, onComplete: (() -> Unit)? = null) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BudgetRepository", "User is not authenticated. Please log in.")
            return
        }


        val dbRef: DatabaseReference = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)

        dbRef.child("budget").setValue(budget)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete?.invoke()
                    Log.d("BudgetRepository", "Budget saved successfully.")
                } else {
                    Log.e("BudgetRepository", "Failed to save budget", task.exception)

                }
            }
    }

    suspend fun getBudgetDataSuspend(): Budget? = suspendCoroutine { continuation ->
        dbRef.child("budget").get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue(Budget::class.java)
                continuation.resume(budget)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }



    fun getBudgetData(onDataReceived: (Budget) -> Unit) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BudgetRepository", "User is not authenticated. Please log in.")
            return
        }

        val dbRef: DatabaseReference = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)

        dbRef.child("budget").get().addOnSuccessListener { snapshot ->
            val budget = snapshot.getValue(Budget::class.java)
            if (budget != null) {
                onDataReceived(budget)
                Log.d("BudgetRepository", "Budget data loaded successfully.")
            } else {
                Log.e("BudgetRepository", "No budget data found")
            }
        }.addOnFailureListener { e ->
            Log.e("BudgetRepository", "Failed to load budget data", e)

        }
    }

}
