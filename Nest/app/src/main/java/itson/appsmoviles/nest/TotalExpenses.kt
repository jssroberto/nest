package itson.appsmoviles.nest

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import itson.appsmoviles.nest.utilities.Categoria
import itson.appsmoviles.nest.utilities.PieChartDrawable

class TotalExpenses : AppCompatActivity() {

    // Lista de categorías de ejemplo
     val categorias = arrayListOf(
        Categoria("Health", 25.0f, R.color.lightest_blue, 156.0f),
        Categoria("Home", 15.0f, R.color.lighter_blue, 78.0f),
        Categoria("Food", 10.0f, R.color.light_blue, 62.4f),
        Categoria("Recreation", 20.0f, R.color.dark_blue, 104.0f),
        Categoria("Transport", 15.0f, R.color.darker_blue, 104.0f),
        Categoria("Others", 15.0f, R.color.blue, 104.0f)
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.total_expenses)

        configurarGrafico()

        configurarSpinner()
    }


    private fun configurarGrafico() {
        val graph = findViewById<View>(R.id.graph)


        val pieChartDrawable = PieChartDrawable(this, categorias)


        graph.background = pieChartDrawable
    }


    private fun configurarSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerCategories)


        val categories = listOf("Select a category", "Food", "Transport", "Entertainment", "Home", "Health", "Other")


        val customFont = ResourcesCompat.getFont(this, R.font.lexend_regular)


        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {


            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }


            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.typeface = customFont
                textView.textSize = 16f
                return view
            }


            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.off_white))
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.typeface = customFont
                textView.textSize = 16f
                textView.setPadding(20, 20, 20, 20) // Añadir padding para mejor diseño
                return view
            }
        }


        spinner.adapter = adapter


        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
}
