package itson.appsmoviles.nest.domain.model.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.domain.model.entity.Expense
import itson.appsmoviles.nest.domain.model.enums.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExpenseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference


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
        val newExpenseRef = database.child("users").child(userId).child("expenses").push()

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

    // Método para actualizar un gasto
    suspend fun updateExpense(
        expenseId: String,
        amount: Double,
        description: String,
        category: Category,
        paymentMethod: String,
        date: String
    ) {
        val userId = auth.currentUser?.uid ?: throw Exception("Invalid user ID")
        if (expenseId.isEmpty()) throw Exception("Invalid expense ID")

        val expenseRef = database.child("users").child(userId).child("expenses").child(expenseId)

        val updatedExpense = mapOf(
            "amount" to amount,
            "description" to description,
            "category" to category.name,
            "paymentMethod" to paymentMethod,
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
                val expense = gastoSnapshot.getValue(Expense::class.java)?.copy(id = gastoSnapshot.key ?: "")
                expense
            }.filter { expense ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val expenseDate = dateFormat.parse(expense.date)

                val start = startDate?.let { dateFormat.parse(it) }
                val end = endDate?.let { dateFormat.parse(it) }

                val inDateRange = (start == null || (expenseDate != null && !expenseDate.before(start))) &&
                        (end == null || (expenseDate != null && !expenseDate.after(end)))

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
        category: Category? = null
    ): List<Expense> {
        val allExpenses = getMovementsFromFirebase()

        // Formato de la fecha que se usa en la base de datos
        val formatter = DateTimeFormatter.ofPattern("d/M/yyyy")

        return allExpenses.filter { expense ->

            // Convertir la fecha almacenada como String a LocalDate
            val expenseDate = try {
                LocalDate.parse(expense.date, formatter)  // Convierte la fecha del gasto a LocalDate
            } catch (e: Exception) {
                null  // Si ocurre un error al parsear la fecha, dejamos expenseDate como null
            }

            // Verificar si la fecha del gasto está dentro del rango proporcionado
            val dateMatches = (startDate == null || expenseDate == null || !expenseDate.isBefore(startDate)) &&
                    (endDate == null || expenseDate == null || !expenseDate.isAfter(endDate))

            // Filtrar también por categoría si es necesario
            val categoryMatches = category == null || expense.category == category

            dateMatches && categoryMatches
        }
    }



}
