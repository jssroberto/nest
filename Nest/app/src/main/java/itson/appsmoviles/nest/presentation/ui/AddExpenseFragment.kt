package itson.appsmoviles.nest.presentation.ui

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class AddExpenseFragment : Fragment() {
    private lateinit var edtAmount: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnDate: Button
    private lateinit var addExpense: Button
    private lateinit var spinner: Spinner
    private lateinit var radioCash: RadioButton
    private lateinit var radioCard: RadioButton
    private var selectedDate: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edtAmount = view.findViewById(R.id.edt_amount_expense)
        edtDescription = view.findViewById(R.id.edt_description_expense)
        btnDate = view.findViewById(R.id.btn_date_expense)
        spinner = view.findViewById(R.id.spinner_categories_expense)
        radioCash = view.findViewById(R.id.radio_cash)
        radioCard = view.findViewById(R.id.radio_card)
        addExpense = view.findViewById(R.id.add_expense)

        setUpSpinner()
        addDollarSign(edtAmount)

        btnDate.setOnClickListener {
            showDatePicker()
        }

        addExpense.setOnClickListener {
            if (validarCampos()) {
                val monto = obtenerMonto()
                val descripcion = edtDescription.text.toString().trim()
                val categoria = spinner.selectedItem.toString()
                val metodoPago = if (radioCash.isChecked) "cash" else "card"

                agregarGasto(monto, descripcion, categoria, metodoPago, selectedDate!!)
            }
        }

        setRadioColors()
    }

    private fun setUpSpinner() {
        val categories = listOf("Select a category", "Food", "Transport", "Entertainment", "Home", "Health", "Other")
        val adapter = object : ArrayAdapter<String>(requireContext(), R.layout.spinner_item, categories) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }
        }
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter
        spinner.setSelection(0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.Nest_DatePicker,
            { _, year, month, day ->
                selectedDate = formatDate(day, month, year)
                btnDate.text = selectedDate
            },
            currentYear, currentMonth, currentDay
        )

        datePickerDialog.show()

        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_color))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDate(day: Int, month: Int, year: Int): String {
        val selectedDate = LocalDate.of(year, month + 1, day)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
        return selectedDate.format(formatter)
    }

    private fun addDollarSign(edtAmount: EditText) {
        edtAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                edtAmount.removeTextChangedListener(this)

                val input = editable.toString()
                val formattedInput = if (!input.startsWith("$")) "$$input" else input

                edtAmount.setText(formattedInput)
                edtAmount.setSelection(formattedInput.length)
                edtAmount.addTextChangedListener(this)
            }
        })
    }

    private fun setRadioColors() {
        radioCash.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_cash)
        radioCard.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.txt_color_radio_card)
    }

    private fun agregarGasto(monto: Double, descripcion: String, categoria: String, metodoPago: String, fecha: String) {
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid

        if (userId != null) {
            val nuevoGastoRef = database.child("usuarios").child(userId).child("gastos").push()

            val gasto = mapOf(
                "monto" to monto,
                "descripcion" to descripcion,
                "fecha" to fecha,
                "categoria" to categoria,
                "metodoPago" to metodoPago
            )

            nuevoGastoRef.setValue(gasto)
                .addOnSuccessListener {
                    limpiarCampos()
                    Toast.makeText(requireContext(), "Gasto agregado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun limpiarCampos() {
        edtAmount.text.clear()
        spinner.setSelection(0)
        edtDescription.text.clear()
        btnDate.text = getString(R.string.select_date)
        selectedDate = null
        radioCash.isChecked = false
        radioCard.isChecked = false
    }

    private fun obtenerMonto(): Double {
        return edtAmount.text.toString()
            .replace("$", "")
            .replace(",", ".")
            .toDouble()
    }

    private fun validarCampos(): Boolean {
        var valido = true

        val amountText = edtAmount.text.toString().trim()
        if (amountText.isEmpty() || amountText == "$") {
            edtAmount.error = "Ingresa un monto válido"
            valido = false
        } else {
            try {
                val monto = obtenerMonto()
                if (monto <= 0) {
                    edtAmount.error = "El monto debe ser mayor a cero"
                    valido = false
                }
            } catch (e: NumberFormatException) {
                edtAmount.error = "Formato de monto inválido"
                valido = false
            }
        }

        if (spinner.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show()
            valido = false
        }

        if (edtDescription.text.toString().trim().isEmpty()) {
            edtDescription.error = "La descripción es obligatoria"
            valido = false
        }

        if (selectedDate == null) {
            Toast.makeText(requireContext(), "Selecciona una fecha", Toast.LENGTH_SHORT).show()
            valido = false
        }

        if (!radioCash.isChecked && !radioCard.isChecked) {
            Toast.makeText(requireContext(), "Selecciona un método de pago", Toast.LENGTH_SHORT).show()
            valido = false
        }

        return valido
    }
}