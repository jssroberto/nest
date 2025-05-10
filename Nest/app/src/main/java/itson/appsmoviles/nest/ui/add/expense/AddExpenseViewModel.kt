package itson.appsmoviles.nest.ui.add.expense

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.util.Locale

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
        currentSpentInCategory: Double,
        alarmThreshold: Double,
        isAlarmEnabled: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.addExpense(expense, {
                    checkAndNotifyIfOverThreshold(
                        context = context,
                        amountAddedToCategory = expense.amount,
                        currentSpentInCategory = currentSpentInCategory,
                        alarmThresholdForCategory = alarmThreshold,
                        isAlarmEnabledForCategory = isAlarmEnabled,
                        categoryName = expense.category.displayName
                    )
                    onSuccess()
                    fetchExpenses()
                }, onFailure)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    private fun checkAndNotifyIfOverThreshold(
        context: Context,
        amountAddedToCategory: Double,
        currentSpentInCategory: Double,
        alarmThresholdForCategory: Double,
        isAlarmEnabledForCategory: Boolean,
        categoryName: String
    ) {
        if (!isAlarmEnabledForCategory || alarmThresholdForCategory <= 0) {
            return
        }

        val newTotalSpentInCategory = currentSpentInCategory + amountAddedToCategory

        if (newTotalSpentInCategory >= alarmThresholdForCategory) {
            showNotification(
                context,
                newTotalSpentInCategory,
                alarmThresholdForCategory,
                categoryName
            )
        }
    }

    private fun showNotification(
        context: Context,
        totalSpentInCategory: Double,
        thresholdForCategory: Double,
        categoryName: String
    ) {


        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentText = String.format(
            Locale.US,
            "En %s: Llevas $%,.2f de tu límite de $%,.2f.",
            categoryName,
            totalSpentInCategory,
            thresholdForCategory
        )

        val notification = NotificationCompat.Builder(context, "budget_channel") // Usa tu ID de canal
            .setSmallIcon(R.drawable.alert_circle) // Asegúrate de tener este ícono
            .setContentTitle("¡Alerta de presupuesto de categoría!")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
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