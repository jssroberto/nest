package itson.appsmoviles.nest.ui.add.expense

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.enum.PaymentMethod
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.ui.budget.BudgetViewModel
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.main.MainActivity
import itson.appsmoviles.nest.ui.util.addDollarSign
import itson.appsmoviles.nest.ui.util.formatDateLongForm
import itson.appsmoviles.nest.ui.util.setUpSpinner
import itson.appsmoviles.nest.ui.util.showDatePicker
import itson.appsmoviles.nest.ui.util.showToast

@RequiresApi(Build.VERSION_CODES.O)
class AddExpenseFragment : Fragment() {

    private val sharedMovementsViewModel: SharedMovementsViewModel by activityViewModels()

    private lateinit var edtAmount: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnDate: Button
    private lateinit var btnAddExpense: Button
    private lateinit var spinner: Spinner
    private lateinit var radioCash: RadioButton
    private lateinit var radioCard: RadioButton
    private var selectedTimestamp: Long? = null

    private lateinit var viewModel: AddExpenseViewModel
    private lateinit var budgetViewModel: BudgetViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[AddExpenseViewModel::class.java]
        budgetViewModel = ViewModelProvider(requireActivity())[BudgetViewModel::class.java]

        edtAmount = view.findViewById(R.id.edt_amount_expense)
        edtDescription = view.findViewById(R.id.edt_description_expense)
        btnDate = view.findViewById(R.id.btn_date_expense)
        spinner = view.findViewById(R.id.spinner_categories_expense)
        radioCash = view.findViewById(R.id.radio_cash)
        radioCard = view.findViewById(R.id.radio_card)
        btnAddExpense = view.findViewById(R.id.btn_add_expense)

        // Configuración inicial
        setUpSpinner(requireContext(), spinner)
        addDollarSign(edtAmount)
        setUpClickListeners()
        setRadioColors()

        // Observar gastos (debug)
        viewModel.fetchExpenses()
        viewModel.expenses.observe(viewLifecycleOwner) { expenses ->
            Log.d("AddExpenseFragment", "Expenses: $expenses")
        }
    }

    private fun setUpClickListeners() {
        btnDate.setOnClickListener {
            showDatePicker(
                context = requireContext(),
                onDateSelected = { timestampMillis ->
                    selectedTimestamp = timestampMillis
                    btnDate.text = formatDateLongForm(timestampMillis)
                }
            )
        }

        btnAddExpense.setOnClickListener {
            addExpense()
        }
    }

    private fun addExpense() {
        if (!areFieldsValid()) return

        val expense = Expense(
            id = "",
            description = edtDescription.text.toString().trim(),
            amount = getAmount(),
            date = selectedTimestamp ?: 0L,
            category = CategoryType.fromDisplayName(spinner.selectedItem.toString()),
            paymentMethod = if (radioCash.isChecked) PaymentMethod.CASH else PaymentMethod.CARD
        )

        val category = expense.category
        val newExpense = expense.amount

        // Verificar si el umbral de la categoría se ha excedido
        val thresholdExceeded = budgetViewModel.checkAndNotifyIfThresholdExceeded(category, newExpense.toFloat())

        if (thresholdExceeded) {
            // Si el umbral ha sido excedido, ya se envió la notificación en el ViewModel
            // Aquí puedes manejar otros casos si es necesario
        }

        // Añadir el gasto al ViewModel
        viewModel.addExpense(
            expense = expense,
            context = requireContext().applicationContext,
            alarmThreshold = (budgetViewModel.alarmThresholdMap[category] ?: 0f).toDouble(),  // Conversión a Double
            onSuccess = {
                sharedMovementsViewModel.notifyMovementDataChanged()
                requireActivity().supportFragmentManager.popBackStack()
            },
            onFailure = { e ->
                // manejar el error
            }
        )

    }


    private fun setRadioColors() {
        radioCash.buttonTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_cash)
        radioCard.buttonTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_card)
    }

    private fun getAmount(): Double {
        return edtAmount.text.toString().replace("$", "").replace(",", ".").toDouble()
    }

    private fun areFieldsValid(): Boolean {
        if (edtAmount.text.isNullOrEmpty()) {
            Toast.makeText(context, "Please enter an amount.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spinner.selectedItemPosition == 0) {
            Toast.makeText(context, "Please select a category.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (edtDescription.text.isNullOrEmpty()) {
            Toast.makeText(context, "Please enter a description.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedTimestamp == null) {
            Toast.makeText(context, "Please select a date.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!radioCash.isChecked && !radioCard.isChecked) {
            Toast.makeText(context, "Please select a payment method.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun sendNotification(expenseAmount: Double, alarmThreshold: Double) {
        val context = requireContext()

        // Crear el canal de notificación (para API 26 y superior)
        val channelId = "expense_alarm_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Expense Alarm"
            val importance = NotificationManager.IMPORTANCE_HIGH  // Importancia alta para mostrar notificación
            val channel = NotificationChannel(channelId, name, importance)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        val notificationTitle = "Alerta de Gasto"
        val notificationText = "El gasto de $expenseAmount ha excedido el umbral de $alarmThreshold."

        // Crear la notificación
        val builder = Notification.Builder(context, channelId)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.alert_circle)
            .setPriority(Notification.PRIORITY_HIGH) // Usamos la nueva constante

        // Mostrar la notificación
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

}
