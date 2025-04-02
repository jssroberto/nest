package itson.appsmoviles.nest.domain.model.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.domain.model.entity.Expense
import itson.appsmoviles.nest.domain.model.enums.Category
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime


class ExpenseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun getMovementsFromFirebase(): List<Expense> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
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
    ) {
        val userId = auth.currentUser?.uid ?: return
        val newExpenseRef = database.child("usuarios").child(userId).child("gastos").push()

        val expense = mapOf(
            "amount" to amount,
            "description" to description,
            "date" to date,
            "category" to category,
            "paymentMethod" to paymentMethod
        )

        try {

            newExpenseRef.setValue(expense).await()
            onSuccess()
        } catch (e: Exception) {

            onFailure(e)
        }
    }

}