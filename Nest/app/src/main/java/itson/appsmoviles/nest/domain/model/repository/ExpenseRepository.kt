package itson.appsmoviles.nest.domain.model.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.domain.model.entity.Expense
import itson.appsmoviles.nest.domain.model.enums.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime


class ExpenseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun getMovementsFromFirebase(): List<Expense> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext emptyList()
        return@withContext try {
            val snapshot = database.child("usuarios").child(userId).child("gastos").get().await()
            Log.d("ExpenseRepository", "Firebase snapshot children count: ${snapshot.childrenCount}")

            snapshot.children.mapNotNull { gastoSnapshot ->
                val expense = gastoSnapshot.getValue(Expense::class.java)
                expense?.copy(id = gastoSnapshot.key ?: "")
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error obteniendo datos de Firebase", e)
            emptyList()
        }
    }

    suspend fun addExpense(
        amount: Double,
        description: String,
        category: Category,
        paymentMethod: String,
        date: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext onFailure(Exception("User not authenticated"))
        val newExpenseRef = database.child("usuarios").child(userId).child("gastos").push()

        val expense = mapOf(
            "amount" to amount,
            "description" to description,
            "date" to date,
            "category" to category.name,
            "paymentMethod" to paymentMethod
        )

        try {
            newExpenseRef.setValue(expense).await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }

    suspend fun updateExpense(
        expenseId: String,
        amount: Double,
        description: String,
        category: Category,
        paymentMethod: String,
        date: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            withContext(Dispatchers.Main) { onFailure(Exception("Invalid user ID")) }
            return@withContext
        }

        if (expenseId.isEmpty()) {
            withContext(Dispatchers.Main) { onFailure(Exception("Invalid expense ID")) }
            return@withContext
        }

        val expenseRef = database.child("usuarios").child(userId).child("gastos").child(expenseId)

        val updatedExpense = mapOf(
            "amount" to amount,
            "description" to description,
            "category" to category.name,
            "paymentMethod" to paymentMethod,
            "date" to date
        )

        try {
            expenseRef.updateChildren(updatedExpense).await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            Log.e("Firebase", "Error actualizando gasto: ${e.message}")
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }
}
