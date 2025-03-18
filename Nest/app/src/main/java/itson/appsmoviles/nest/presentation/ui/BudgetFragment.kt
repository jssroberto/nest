package itson.appsmoviles.nest.presentation.ui

import android.content.Context
import android.os.Bundle
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import itson.appsmoviles.nest.R
class BudgetFragment : Fragment() {

    var categoryExpenses = ArrayList<CategoryExpense>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {



        val view = inflater.inflate(R.layout.fragment_budget, container, false)

        // Llamar a la función que agrega las categorías
        addProducts()

        // Configura el ListView
        val listViewCategoryExpenses: ListView = view.findViewById(R.id.listViewCategoryExpenses)
        val adapter = CategoryExpenseAdapter(requireContext(), categoryExpenses)

        // Establecer el adaptador para el ListView
        listViewCategoryExpenses.adapter = adapter

        // Retorna la vista inflada
        return view
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editTextAmount: EditText = view.findViewById(R.id.et_food)
    }

    // Función para agregar las categorías de gastos
    fun addProducts() {
        categoryExpenses.add(CategoryExpense("Food"))
        categoryExpenses.add(CategoryExpense("Home"))
        categoryExpenses.add(CategoryExpense("Health"))
        categoryExpenses.add(CategoryExpense("Recreation"))
        categoryExpenses.add(CategoryExpense("Transport"))
        categoryExpenses.add(CategoryExpense("Others"))
    }

    // Adaptador para la lista de categorías de gastos
    private class CategoryExpenseAdapter(
        private val context: Context,
        private val categories: ArrayList<CategoryExpense>
    ) : BaseAdapter() {  // Aquí extendemos BaseAdapter

        override fun getCount(): Int {
            return categories.size
        }

        override fun getItem(position: Int): Any {
            return categories[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            // Usa un patrón de vista reciclada para optimizar el rendimiento
            val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.category_expense_view, parent, false)

            val textViewCategoryName: TextView = view.findViewById(R.id.textViewCategoryName)
            textViewCategoryName.text = categories[position].name

            return view
        }


    }


}
