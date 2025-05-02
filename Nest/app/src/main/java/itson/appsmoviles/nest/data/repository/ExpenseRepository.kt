package itson.appsmoviles.nest.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.enums.CategoryType
import itson.appsmoviles.nest.data.enums.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExpenseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference


    suspend fun addExpense(
        amount: Double,
        description: String,
        categoryType: CategoryType,
        paymentMethod: String,
        date: Long,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext onFailure(Exception("User not authenticated"))
        val newExpenseRef = database.child("users").child(userId).child("expenses").push()

        val expense = mapOf(
            "amount" to amount,
            "description" to description,
            "date" to date,
            "category" to categoryType.name,
            "paymentMethod" to paymentMethod
        )

        try {
            newExpenseRef.setValue(expense).await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }

    // Método para actualizar un gasto
    suspend fun updateExpense(
        expenseId: String,
        amount: Double,
        description: String,
        categoryType: CategoryType,
        paymentMethod: PaymentMethod,
        date: Long
    ) {
        val userId = auth.currentUser?.uid ?: throw Exception("Invalid user ID")
        if (expenseId.isEmpty()) throw Exception("Invalid expense ID")

        val expenseRef = database.child("users").child(userId).child("expenses").child(expenseId)

        val updatedExpense = mapOf(
            "amount" to amount,
            "description" to description,
            "category" to categoryType.name,
            "paymentMethod" to paymentMethod.name,
            "date" to date
        )

        expenseRef.updateChildren(updatedExpense).await()
    }

    // Método para obtener todos los expenses
    suspend fun getMovementsFromFirebase(): List<Expense> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext emptyList()
        return@withContext try {
            val snapshot = database.child("users").child(userId).child("expenses").get().await()
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

    suspend fun getExpensesFiltered(category: String?, startDate: String?, endDate: String?): List<Expense> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = database.child("users").child(userId).child("expenses").get().await()

            snapshot.children.mapNotNull { gastoSnapshot ->
                gastoSnapshot.getValue(Expense::class.java)?.copy(id = gastoSnapshot.key ?: "")
            }.filter { expense ->
                val startMillis = startDate?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it)?.time
                }
                val endMillis = endDate?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it)?.time?.plus(86_399_999)
                }

                val inDateRange = (startMillis == null || expense.date >= startMillis) &&
                        (endMillis == null || expense.date <= endMillis)

                val inCategory = category == null || expense.category.name == category

                inDateRange && inCategory
            }

        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error filtrando expenses", e)
            emptyList()
        }
    }




    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getFilteredExpensesFromFirebase(
        startDate: LocalDate?,
        endDate: LocalDate?,
        categoryType: CategoryType? = null
    ): List<Expense> {
        val allExpenses = getMovementsFromFirebase()

        return allExpenses.filter { expense ->
            // Convert the Long timestamp to LocalDate
            val expenseDate = try {
                Instant.ofEpochMilli(expense.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } catch (e: Exception) {
                null
            }

            val dateMatches = (startDate == null || expenseDate == null || !expenseDate.isBefore(startDate)) &&
                    (endDate == null || expenseDate == null || !expenseDate.isAfter(endDate))

            val categoryMatches = categoryType == null || expense.category == categoryType

            dateMatches && categoryMatches
        }
    }




}
