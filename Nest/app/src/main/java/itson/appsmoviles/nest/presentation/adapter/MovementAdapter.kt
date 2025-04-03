package itson.appsmoviles.nest.presentation.adapter

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.domain.model.entity.Expense
import itson.appsmoviles.nest.domain.model.enums.Category
import itson.appsmoviles.nest.presentation.ui.ExpenseDetailDialogFragment
import java.time.format.DateTimeFormatter
import java.util.Locale

class MovementAdapter(private val items: List<Expense>) : RecyclerView.Adapter<MovementViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_movement, parent, false)
        return MovementViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        val item = items[position]
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm", Locale.getDefault())

        holder.description.text = item.description
        holder.amount.text = "$${item.amount}"
        holder.date.text = item.date.format(formatter)

        val iconResId = when (item.category) {
            Category.LIVING -> R.drawable.icon_category_living
            Category.RECREATION -> R.drawable.icon_category_recreation
            Category.TRANSPORT -> R.drawable.icon_category_transport
            Category.FOOD -> R.drawable.icon_category_food
            Category.HEALTH -> R.drawable.icon_category_health
            Category.OTHER -> R.drawable.icon_category_other
            else -> R.drawable.alert_circle
        }

        holder.icon.setImageResource(iconResId)

        // Agregar el evento de clic para abrir el diálogo
        holder.itemView.setOnClickListener {
            // Crear un Bundle para pasar los datos al Fragment
            val bundle = Bundle().apply {
                putString("description", item.description)
                putFloat("amount", item.amount)
                putString("date", item.date.toString())
                putString("category", item.category.name)
            }

            // Crear el fragmento y pasar el Bundle con los datos
            val dialog = ExpenseDetailDialogFragment()
            dialog.arguments = bundle
            dialog.show((holder.itemView.context as AppCompatActivity).supportFragmentManager, "ExpenseDetailDialog")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
