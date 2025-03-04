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

class TotalExpenses : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.total_expenses)


        val spinner = findViewById<Spinner>(R.id.spinnerCategories)


        val categories = listOf("Select a category","Food", "Transport", "Entertainment", "Home", "Health", "Other")


        val customFont = ResourcesCompat.getFont(this, R.font.lexend_regular) // Asegúrate de tener tu fuente en res/font

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.typeface = customFont
                textView.textSize = 16f


//                if (position == 0) {
//                    textView.setTextColor(ContextCompat.getColor(context, R.color.off_white)) // Color más tenue
//                } else {
//                    textView.setTextColor(ContextCompat.getColor(context, R.color.darker_blue)) // Color normal
//                }

                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.off_white))
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.typeface = customFont
                textView.textSize = 16f
                textView.setPadding(20, 20, 20, 20) // Agregar padding para mejor diseño


//                if (position == 0) {
//                    textView.setTextColor(ContextCompat.getColor(context, R.color.off_white))
//                } else {
//                    textView.setTextColor(ContextCompat.getColor(context, R.color.darker_blue))
//                }

                return view
            }
        }


        spinner.adapter = adapter


        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                if (position == 0) {
//                    (view as TextView).setTextColor(ContextCompat.getColor(this@TotalExpenses, R.color.darker_blue))
//                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
}