package itson.appsmoviles.nest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Budget : AppCompatActivity() {

    var categoryExpenses = ArrayList<CategoryExpense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        addProducts()

        var listViewCategoryExpenses: ListView = findViewById(R.id.listViewCategoryExpenses)
        var adapter: CategoryExpenseAdapter = CategoryExpenseAdapter(this, categoryExpenses)

        listViewCategoryExpenses.adapter = adapter
    }

    fun addProducts(){
        categoryExpenses.add(CategoryExpense("Food"))
        categoryExpenses.add(CategoryExpense("Home"))
        categoryExpenses.add(CategoryExpense("Health"))
        categoryExpenses.add(CategoryExpense("Recreation"))
        categoryExpenses.add(CategoryExpense("Transport"))
        categoryExpenses.add(CategoryExpense("Others"))
    }


    private class CategoryExpenseAdapter: BaseAdapter {
        var categories = ArrayList<CategoryExpense>()
        var context: Context? = null

        constructor(context: Context, categories: ArrayList<CategoryExpense>){
            this.categories = categories
            this.context = context
        }

        override fun getCount(): Int {
            return categories.size
        }

        override fun getItem(p0: Int): Any {
            return categories[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            var category = categories[p0]
            var inflater = LayoutInflater.from(context)
            var view = inflater.inflate(R.layout.category_expense_view, null)

            val textViewCategoryName = view.findViewById<TextView>(R.id.textViewCategoryName)

            textViewCategoryName.text = category.name

            return view
        }
    }
}