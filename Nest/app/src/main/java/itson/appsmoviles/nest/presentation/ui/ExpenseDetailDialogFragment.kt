package itson.appsmoviles.nest.presentation.ui

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.domain.model.entity.Expense
import itson.appsmoviles.nest.domain.model.enums.Category
import java.time.format.DateTimeFormatter


class ExpenseDetailDialogFragment : DialogFragment() {
    private lateinit var descriptionTextView: TextView
    private lateinit var amountTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var categoryTextView: TextView
    private lateinit var categoryImageView: ImageView

    companion object {
        fun newInstance(expense: Expense): ExpenseDetailDialogFragment {
            val fragment = ExpenseDetailDialogFragment()
            val bundle = Bundle().apply {
                putString("description", expense.description)
                putFloat("amount", expense.amount)
                putString("date", expense.date.toString())
                putString("category", expense.category.name)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT  // Esto asegurará que ocupe todo el ancho disponible
            val height = ViewGroup.LayoutParams.WRAP_CONTENT  // Deja que la altura se ajuste al contenido
            dialog.window?.setLayout(width, height) // Establecer el tamaño del diálogo
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar la vista del fragmento
        return inflater.inflate(R.layout.fragment_expense_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencias a las vistas del fragmento
        descriptionTextView = view.findViewById(R.id.tv_description)
        amountTextView = view.findViewById(R.id.tv_amount)
        dateTextView = view.findViewById(R.id.tv_date)
        categoryImageView = view.findViewById(R.id.iv_icon)  // Aquí se carga el icono

        // Obtener los datos del Bundle y mostrarlos
        val description = arguments?.getString("description")
        val amount = arguments?.getFloat("amount")
        val date = arguments?.getString("date")
        val category = arguments?.getString("category")

        descriptionTextView.text = description
        amountTextView.text = "$$amount"
        dateTextView.text = date

        // Cargar el icono correspondiente según la categoría
        val iconResId = when (category) {
            "LIVING" -> R.drawable.icon_category_living
            "RECREATION" -> R.drawable.icon_category_recreation
            "TRANSPORT" -> R.drawable.icon_category_transport
            "FOOD" -> R.drawable.icon_category_food
            "HEALTH" -> R.drawable.icon_category_health
            "OTHER" -> R.drawable.icon_category_other
            else -> R.drawable.alert_circle  // Default icon if category is unknown
        }

        categoryImageView.setImageResource(iconResId)  // Establecer el icono
    }
}