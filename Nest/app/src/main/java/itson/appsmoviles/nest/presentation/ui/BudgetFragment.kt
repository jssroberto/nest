package itson.appsmoviles.nest.presentation.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import itson.appsmoviles.nest.R
class BudgetFragment : Fragment() {

    private var isEditing = false // Evitar el bucle infinito

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_budget, container, false)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editTextAmountFood: EditText = view.findViewById(R.id.et_food)
        val editTextAmountHome: EditText = view.findViewById(R.id.et_home)
        val editTextAmountRecreation: EditText = view.findViewById(R.id.et_recreation)
        val editTextAmountTransport: EditText = view.findViewById(R.id.et_transport)
        val editTextAmountOthers: EditText = view.findViewById(R.id.et_others)
        val editTextAmountHealth: EditText = view.findViewById(R.id.et_health)
        val editTextBudget: EditText = view.findViewById(R.id.monthly_budget)

        setUpCurrencyEditText(editTextAmountFood)
        setUpCurrencyEditText(editTextBudget)
        setUpCurrencyEditText(editTextAmountHome)
        setUpCurrencyEditText(editTextAmountRecreation)
        setUpCurrencyEditText(editTextAmountTransport)
        setUpCurrencyEditText(editTextAmountOthers)
        setUpCurrencyEditText(editTextAmountHealth)
    }

    private fun setUpCurrencyEditText(editText: EditText) {
        editText.setText("$0.00")

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // No es necesario realizar ninguna acción aquí
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                if (isEditing) return // Evita cambios cuando estamos actualizando el texto

                var input = charSequence.toString().replace("$", "") // Eliminar el símbolo $

                // Evitar que se borre todo y deje el campo vacío
                if (input.isEmpty()) {
                    return
                }

                // Permitir solo números y un solo punto decimal
                if (!input.matches("^[0-9]*\\.?[0-9]{0,2}$".toRegex())) {
                    editText.setText("")
                    Toast.makeText(requireContext(), "Solo números y un punto decimal permitidos", Toast.LENGTH_SHORT).show()
                    return
                }

                try {
                    isEditing = true // Marcar que estamos modificando el texto

                    // Si el usuario solo ingresó un punto, no formateamos aún
                    if (input == ".") {
                        editText.setText("$0.")
                        editText.setSelection(editText.text.length)
                        return
                    }

                    var amount = input.toDoubleOrNull()

                    // Si es null, dejamos la entrada como está (permite escribir el punto correctamente)
                    if (amount == null) {
                        isEditing = false
                        return
                    }

                    // Validación de límites
                    if (amount < 0.00) {
                        amount = 0.00
                    } else if (amount > 999999.00) {
                        Toast.makeText(requireContext(), "El valor no puede ser mayor a $999,999.00", Toast.LENGTH_SHORT).show()
                        isEditing = false
                        return
                    }

                    // Formatear el número sin afectar la entrada del usuario
                    val formattedAmount = if (input.contains(".")) {
                        "$$input" // Mantener los decimales como los ingresa el usuario
                    } else {
                        "$${amount.toInt()}" // Si no hay punto decimal, mostramos sin decimales
                    }

                    editText.setText(formattedAmount)
                    editText.setSelection(editText.text.length) // Mantener el cursor al final

                } catch (e: NumberFormatException) {
                    // Ignorar error si ocurre
                } finally {
                    isEditing = false // Marcar que ya terminamos de modificar el texto
                }
            }

            override fun afterTextChanged(editable: Editable?) {
                // No se necesita realizar ninguna acción aquí
            }
        })
    }

}
