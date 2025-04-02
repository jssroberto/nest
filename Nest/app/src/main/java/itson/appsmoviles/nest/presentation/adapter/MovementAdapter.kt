package itson.appsmoviles.nest.presentation.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.domain.model.entity.Expense
import itson.appsmoviles.nest.domain.model.enums.Category
import java.time.format.DateTimeFormatter
import java.util.Locale

class MovementAdapter(private val items: List<Expense>) : RecyclerView.Adapter<MovementViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_movement, parent, false)
        return MovementViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        val item = items[position]
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm", Locale.getDefault())
        holder.description.text = item.description
        holder.amount.text = "$${item.amount}"
        holder.date.text = item.date.format(formatter)

        // Asignar un valor predeterminado para el icono si no hay coincidencia
        val iconResId = when (item.category) {
            Category.LIVING -> R.drawable.icon_category_living
            Category.RECREATION -> R.drawable.icon_category_recreation
            Category.TRANSPORT -> R.drawable.icon_category_transport
            Category.FOOD -> R.drawable.icon_category_food
            Category.HEALTH-> R.drawable.icon_category_health
            Category.OTHER -> R.drawable.icon_category_other
            else -> R.drawable.alert_circle  // Valor predeterminado en caso de categoría desconocida
        }

        holder.icon.setImageResource(iconResId)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
