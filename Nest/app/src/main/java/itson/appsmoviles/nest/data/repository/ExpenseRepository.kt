package itson.appsmoviles.nest.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.ui.home.HomeOverviewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException

class ExpenseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun getOverviewData(): HomeOverviewState? = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("ExpenseRepository", "getOverviewData: User not authenticated")
            return@withContext null
        }
        return@withContext try {
            val userName = auth.currentUser?.displayName ?: "User"
            val totalIncome = fetchTotalForNode(userId, "incomes")
            val totalExpenses = fetchTotalForNode(userId, "expenses")
            val budget = fetchBudget(userId)

            HomeOverviewState(
                userName = userName,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                budget = budget
            )
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error fetching overview data", e)
            null
        }
    }

    private suspend fun fetchTotalForNode(userId: String, nodeName: String): Double {
        return try {
            val path = database.child("users").child(userId).child("movements").child(nodeName)
            val snapshot = path.get().await()
            if (!snapshot.exists()) return 0.0

            snapshot.children.sumOf { itemSnapshot ->
                getAmountFromSnapshot(itemSnapshot) ?: 0.0
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error fetching total for $nodeName", e)
            throw IOException("Error fetching total for $nodeName", e)
        }
    }

    private fun getAmountFromSnapshot(snapshot: DataSnapshot): Double? {
        return snapshot.child("amount").getValue(Double::class.java)
            ?: snapshot.child("amount").getValue(Long::class.java)?.toDouble()
    }

    // TODO: Replace placeholder with actual Firebase database call
    private suspend fun fetchBudget(userId: String): Double {
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

    suspend fun addExpense(
        expense: Expense,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            withContext(Dispatchers.Main) { onFailure(Exception("User not authenticated")) }
            return@withContext
        }
        val expensesRef = database.child("users").child(userId).child("movements").child("expenses")
        val newExpenseRef = expensesRef.push()

        val expenseMap = mapOf(
            "description" to expense.description,
            "amount" to expense.amount,
            "date" to expense.date,
            "category" to expense.category.name,
            "paymentMethod" to expense.paymentMethod.name
        )

        try {
            newExpenseRef.setValue(expenseMap).await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error adding expense", e)
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }

    suspend fun updateExpense(
        expense: Expense,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            withContext(Dispatchers.Main) { onFailure(Exception("User not authenticated")) }
            return@withContext
        }

        val expenseId = expense.id
        if (expenseId.isBlank()) {
            val error = IllegalArgumentException("Expense must have a valid ID to be updated")
            withContext(Dispatchers.Main) { onFailure(error) }
            return@withContext
        }

        val expenseRef = database.child("users").child(userId).child("movements").child("expenses")
            .child(expenseId)

        val updatedExpenseMap = mapOf(
            "description" to expense.description,
            "amount" to expense.amount,
            "date" to expense.date,
            "category" to expense.category.name,
            "paymentMethod" to expense.paymentMethod.name
        )

        try {
            expenseRef.updateChildren(updatedExpenseMap).await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error updating expense with ID: $expenseId", e)
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }


    suspend fun deleteExpense(
        expenseId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            withContext(Dispatchers.Main) { onFailure(Exception("User not authenticated")) }
            return@withContext
        }

        if (expenseId.isBlank()) {
            val error = IllegalArgumentException("Expense ID cannot be blank for deletion")
            withContext(Dispatchers.Main) { onFailure(error) }
            return@withContext
        }
        val expenseRef = database.child("users").child(userId).child("movements").child("expenses")
            .child(expenseId)

        try {
            expenseRef.removeValue().await()
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error deleting expense with ID: $expenseId", e)
            withContext(Dispatchers.Main) { onFailure(e) }
        }
    }


    suspend fun getAllExpenses(): List<Expense> = withContext(Dispatchers.IO) {
        val userId =
            auth.currentUser?.uid ?: return@withContext emptyList() // Return early if no user

        return@withContext try {
            val snapshot =
                database.child("users").child(userId).child("movements").child("expenses")
                    .get().await()
            Log.d(
                "ExpenseRepository",
                "getAllExpenses: Fetched ${snapshot.childrenCount} raw expenses."
            )

            snapshot.children.mapNotNull { dataSnapshot ->
                dataSnapshot.getValue(Expense::class.java)?.copy(id = dataSnapshot.key ?: "")
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error getting all expenses from Firebase", e)
            emptyList()
        }
    }


    suspend fun getExpensesFiltered(
        startDate: Long? = null,
        endDate: Long? = null,
        category: CategoryType? = null
    ): List<Expense> = withContext(Dispatchers.IO) {

        val userId =
            auth.currentUser?.uid ?: return@withContext emptyList()

        try {
            // Build the query progressively
            var query: Query =
                database.child("users").child(userId).child("movements").child("expenses")
                    .orderByChild("date") // Assuming 'date' is stored appropriately (e.g., timestamp)

            // Apply date filters if they exist
            if (startDate != null) {
                query =
                    query.startAt(startDate.toDouble()) // Use Double for Firebase Realtime DB if date is Number
            }
            if (endDate != null) {
                query = query.endAt(endDate.toDouble())
            }

            val snapshot = query.get().await()
            Log.d(
                "ExpenseRepository",
                "getExpensesFiltered: Fetched ${snapshot.childrenCount} expenses after date query."
            )

            val expenses = snapshot.children.mapNotNull { dataSnapshot ->
                dataSnapshot.getValue(Expense::class.java)?.copy(id = dataSnapshot.key ?: "")
            }

            // Apply category filter in memory if it exists
            val finalExpenses = if (category != null) {
                expenses.filter { it.category == category }
            } else {
                expenses
            }

            Log.d(
                "ExpenseRepository",
                "getExpensesFiltered: Returning ${finalExpenses.size} expenses after category filter."
            )
            finalExpenses

        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error filtering expenses", e)
            emptyList() // Return empty list on error
        }
    }
}