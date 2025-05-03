package itson.appsmoviles.nest.data.repository

import android.R.attr.category
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class IncomeRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun addIncome(
        amount: Double,
        date: Long,
        description: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext onFailure(Exception("User not authenticated"))

        val newIncomeRef = database.child("users").child(userId).child("incomes").push()

        val income = mapOf(
            "amount" to amount,
            "date" to date,
            "description" to description
        )

        try {
            newIncomeRef.setValue(income).await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }
}
