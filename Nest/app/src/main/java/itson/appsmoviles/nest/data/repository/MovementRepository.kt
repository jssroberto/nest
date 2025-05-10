package itson.appsmoviles.nest.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.model.Income
import itson.appsmoviles.nest.data.model.Movement
import itson.appsmoviles.nest.ui.home.state.HomeOverviewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Calendar

class MovementRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val budgetRepository = BudgetRepository()

    suspend fun getOverviewData(): HomeOverviewState? = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: run {
            return@withContext null
        }

        return@withContext try {
            val userName = auth.currentUser?.displayName ?: "User"

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis

            val totalIncome = fetchTotalForNode(userId, "incomes", startOfMonth, endOfMonth)
            val totalExpenses = fetchTotalForNode(userId, "expenses", startOfMonth, endOfMonth)
            val budget = budgetRepository.getBudgetDataSuspend()?.totalBudget ?: 0f

            HomeOverviewState(
                userName = userName,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                budget = budget.toDouble()
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchTotalForNode(
        userId: String,
        nodeName: String,
        startTime: Long,
        endTime: Long
    ): Double {
        return try {
            val path = database.child("users").child(userId).child("movements").child(nodeName)
            val snapshot = path.get().await()
            if (!snapshot.exists()) return 0.0

            snapshot.children.sumOf { itemSnapshot ->
                val itemDate = itemSnapshot.child("date").getValue(Long::class.java) ?: 0L
                if (itemDate in startTime..endTime) {
                    getAmountFromSnapshot(itemSnapshot) ?: 0.0
                } else {
                    0.0
                }
            }
        } catch (e: Exception) {
            throw IOException("Error fetching total for $nodeName", e)
        }
    }

    private fun getAmountFromSnapshot(snapshot: DataSnapshot): Double? {
        return snapshot.child("amount").getValue(Double::class.java)
            ?: snapshot.child("amount").getValue(Long::class.java)?.toDouble()
    }


    private suspend fun fetchExpenses(queryModifier: (Query) -> Query = { it }): List<Expense> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val expensesRef = database.child("users").child(userId).child("movements").child("expenses")
        val finalQuery = queryModifier(expensesRef)

        return try {
            val snapshot = finalQuery.get().await()
            Log.d(
                "MovementRepository",
                "fetchExpenses: Fetched ${snapshot.childrenCount} raw expenses."
            )
            snapshot.children.mapNotNull { dataSnapshot ->
                dataSnapshot.getValue(Expense::class.java)?.copy(id = dataSnapshot.key ?: "")
            }
        } catch (e: Exception) {
            Log.e("MovementRepository", "Error fetching expenses", e)
            emptyList()
        }
    }

    private suspend fun fetchIncomes(queryModifier: (Query) -> Query = { it }): List<Income> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val incomesRef = database.child("users").child(userId).child("movements").child("incomes")
        val finalQuery = queryModifier(incomesRef)

        return try {
            val snapshot = finalQuery.get().await()
            Log.d(
                "MovementRepository",
                "fetchIncomes: Fetched ${snapshot.childrenCount} raw incomes."
            )
            snapshot.children.mapNotNull { dataSnapshot ->
                dataSnapshot.getValue(Income::class.java)?.copy(id = dataSnapshot.key ?: "")
            }
        } catch (e: Exception) {
            Log.e("MovementRepository", "Error fetching incomes", e)
            emptyList()
        }
    }

    suspend fun getAllMovements(): List<Movement> = withContext(Dispatchers.IO) {
        auth.currentUser?.uid ?: return@withContext emptyList()

        coroutineScope {
            val expensesDeferred = async { fetchExpenses() }
            val incomesDeferred = async { fetchIncomes() }

            val expenses = expensesDeferred.await()
            val incomes = incomesDeferred.await()

            (expenses + incomes)
        }
    }

    // TODO: Maybe we are not using this function
    suspend fun getMovementsFiltered(
        startDate: Long? = null,
        endDate: Long? = null,
        category: CategoryType? = null // Filter specific to expenses
    ): List<Movement> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext emptyList()

        val expenseQueryModifier: (Query) -> Query = { baseQuery ->
            var query = baseQuery.orderByChild("date")
            if (startDate != null) query = query.startAt(startDate.toDouble())
            if (endDate != null) query = query.endAt(endDate.toDouble())
            query
        }
        val incomeQueryModifier: (Query) -> Query = { baseQuery ->
            var query = baseQuery.orderByChild("date")
            if (startDate != null) query = query.startAt(startDate.toDouble())
            if (endDate != null) query = query.endAt(endDate.toDouble())
            query
        }

        coroutineScope {
            val expensesDeferred = async { fetchExpenses(expenseQueryModifier) }
            val incomesDeferred = async { fetchIncomes(incomeQueryModifier) }

            val fetchedExpenses = expensesDeferred.await()
            val fetchedIncomes = incomesDeferred.await()

            val filteredExpenses = if (category != null) {
                fetchedExpenses.filter { it.category == category }
            } else {
                fetchedExpenses
            }
            Log.d(
                "MovementRepository",
                "getMovementsFiltered: Returning ${filteredExpenses.size} expenses after category filter."
            )

            (filteredExpenses + fetchedIncomes)
        }
    }

}