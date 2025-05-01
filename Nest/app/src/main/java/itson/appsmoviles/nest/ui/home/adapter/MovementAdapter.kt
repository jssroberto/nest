package itson.appsmoviles.nest.ui.home.adapter

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.enums.CategoryType
import itson.appsmoviles.nest.ui.home.ExpenseDetailDialogFragment
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

        val iconResId = when (item.categoryType) {
            CategoryType.LIVING -> R.drawable.icon_category_living
            CategoryType.RECREATION -> R.drawable.icon_category_recreation
            CategoryType.TRANSPORT -> R.drawable.icon_category_transport
            CategoryType.FOOD -> R.drawable.icon_category_food
            CategoryType.HEALTH -> R.drawable.icon_category_health
            CategoryType.OTHER -> R.drawable.icon_category_other
            else -> R.drawable.alert_circle
        }

        holder.icon.setImageResource(iconResId)


        holder.itemView.setOnClickListener {

            val bundle = Bundle().apply {
                putString("id", item.id)
                putString("description", item.description)
                putFloat("amount", item.amount)
                putString("date", item.date.toString())
                putString("category", item.categoryType.name)
                putString("paymentMethod", item.paymentMethod.name)
            }


            val dialog = ExpenseDetailDialogFragment()
            dialog.arguments = bundle
            dialog.show((holder.itemView.context as AppCompatActivity).supportFragmentManager, "ExpenseDetailDialog")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
