package itson.appsmoviles.nest.ui.add.expense

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.repository.ExpenseRepository
import itson.appsmoviles.nest.ui.main.MainActivity
import kotlinx.coroutines.launch

class AddExpenseViewModel : ViewModel() {
    private val repository = ExpenseRepository()

    private val _expenses = MutableLiveData<List<Expense>>()
    val expenses: LiveData<List<Expense>> get() = _expenses

    fun fetchExpenses() {
        viewModelScope.launch {
            _expenses.value = repository.getAllExpenses()
        }
    }
    fun addExpense(
        expense: Expense,
        context: Context,
        alarmThreshold: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.addExpense(expense, {
                    checkAndNotifyIfOverThreshold(context, expense.amount, alarmThreshold)
                    onSuccess()
                }, onFailure)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    private fun checkAndNotifyIfOverThreshold(context: Context, amountAdded: Double, alarmThreshold: Double) {
        val totalSpent = _expenses.value?.sumOf { it.amount } ?: 0.0
        val newTotal = totalSpent + amountAdded

        if (newTotal >= alarmThreshold) {
            showNotification(context, newTotal, alarmThreshold)
        }
    }

    private fun showNotification(context: Context, totalSpent: Double, threshold: Double) {
        createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "budget_channel")
            .setSmallIcon(R.drawable.alert_circle) // asegúrate de tener un ícono válido
            .setContentTitle("¡Alerta de presupuesto!")
            .setContentText("Has gastado $totalSpent de tu límite de $threshold.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
    }



    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Notifications"
            val descriptionText = "Notifications for budget thresholds"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("budget_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}