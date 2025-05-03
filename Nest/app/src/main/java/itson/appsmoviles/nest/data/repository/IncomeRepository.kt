package itson.appsmoviles.nest.data.repository

import android.R.attr.category
import android.R.attr.description
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.data.model.Income
import itson.appsmoviles.nest.ui.add.income.AddIncomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class IncomeRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun addIncome(
        income: Income,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext onFailure(Exception("User not authenticated"))

        val newIncomeRef = database.child("users").child(userId).child("incomes").push()

        val income = mapOf(
            "description" to income.description,
            "amount" to income.amount,
            "date" to income.date,
        )

        try {
            newIncomeRef.setValue(income).await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }
}
