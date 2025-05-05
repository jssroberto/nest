package itson.appsmoviles.nest.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.data.model.Income
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
        val userId = auth.currentUser?.uid
        if (userId == null) {
            withContext(Dispatchers.Main) { onFailure(Exception("User not authenticated")) }
            return@withContext
        }

        val incomesRef = database.child("users").child(userId).child("movements").child("incomes")
        val newIncomeRef = incomesRef.push()

        val incomeMap = mapOf(
            "description" to income.description,
            "amount" to income.amount,
            "date" to income.date
        )

        try {
            newIncomeRef.setValue(incomeMap).await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            Log.e("IncomeRepository", "Error adding income", e)
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }

    suspend fun getAllIncomes(): List<Income> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext emptyList()
        val incomesRef = database.child("users").child(userId).child("movements").child("incomes")

        return@withContext try {
            val snapshot = incomesRef.get().await()
            Log.d(
                "IncomeRepository",
                "getAllIncomes: Fetched ${snapshot.childrenCount} raw incomes."
            )
            snapshot.children.mapNotNull { dataSnapshot ->
                dataSnapshot.getValue(Income::class.java)?.copy(id = dataSnapshot.key ?: "")
            }
        } catch (e: Exception) {
            Log.e("IncomeRepository", "Error getting all incomes from Firebase", e)
            emptyList()
        }
    }

    suspend fun updateIncome(
        income: Income,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            withContext(Dispatchers.Main) { onFailure(Exception("User not authenticated")) }
            return@withContext
        }

        val incomeId = income.id
        if (incomeId.isBlank()) {
            val error = IllegalArgumentException("Income must have a valid ID to be updated")
            withContext(Dispatchers.Main) { onFailure(error) }
            return@withContext
        }

        val incomeRef = database.child("users").child(userId).child("movements").child("incomes")
            .child(incomeId)

        val updatedIncomeMap = mapOf(
            "description" to income.description,
            "amount" to income.amount,
            "date" to income.date
        )

        try {
            incomeRef.updateChildren(updatedIncomeMap).await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            Log.e("IncomeRepository", "Error updating income with ID: $incomeId", e)
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }

    suspend fun deleteIncome(
        incomeId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            withContext(Dispatchers.Main) { onFailure(Exception("User not authenticated")) }
            return@withContext
        }

        if (incomeId.isBlank()) {
            val error = IllegalArgumentException("Income ID cannot be blank for deletion")
            withContext(Dispatchers.Main) { onFailure(error) }
            return@withContext
        }

        val incomeRef = database.child("users").child(userId).child("movements").child("incomes")
            .child(incomeId)

        try {
            incomeRef.removeValue().await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            Log.e("IncomeRepository", "Error deleting income with ID: $incomeId", e)
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }
}
