package itson.appsmoviles.nest.ui.home.adapter

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.model.Income
import itson.appsmoviles.nest.data.model.Movement
import itson.appsmoviles.nest.ui.home.detail.ExpenseDetailDialogFragment
import itson.appsmoviles.nest.ui.util.showToast
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MovementAdapter(
    private var items: MutableList<Movement> = mutableListOf()
) : RecyclerView.Adapter<MovementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_movement, parent, false)
        return MovementViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        val movement = items[position]

        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
        holder.description.text = movement.description
        val dateTime = Instant.ofEpochMilli(movement.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        holder.date.text = dateTime.format(formatter)

        when (movement) {
            is Expense -> {
                holder.amount.text = String.format(Locale.getDefault(), "-$%d", movement.amount.toInt())
                holder.amount.setTextColor(holder.itemView.context.getColor(R.color.txt_expenses))

                val iconResId = when (movement.category) {
                    CategoryType.LIVING -> R.drawable.icon_category_living
                    CategoryType.RECREATION -> R.drawable.icon_category_recreation
                    CategoryType.TRANSPORT -> R.drawable.icon_category_transport
                    CategoryType.FOOD -> R.drawable.icon_category_food
                    CategoryType.HEALTH -> R.drawable.icon_category_health
                    CategoryType.OTHER -> R.drawable.icon_category_other
                }
                holder.icon.setImageResource(iconResId)

                holder.itemView.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("movementType", "Expense")
                        putString("id", movement.id)
                        putString("description", movement.description)
                        putDouble("amount", movement.amount)
                        putLong("date", movement.date)
                        putString("category", movement.category.name)
                        putString("paymentMethod", movement.paymentMethod.name)
                    }
                    val dialog = ExpenseDetailDialogFragment() // Use your existing dialog
                    dialog.arguments = bundle
                    (holder.itemView.context as? AppCompatActivity)?.supportFragmentManager?.let { fm ->
                        dialog.show(fm, "ExpenseDetailDialog")
                    }
                }
            }

            is Income -> {
                holder.amount.text = String.format(Locale.getDefault(), "+$%d", movement.amount.toInt())
                holder.amount.setTextColor(holder.itemView.context.getColor(R.color.txt_income))
                holder.icon.setImageResource(R.drawable.icon_category_income) // Use a specific income icon resource

                // Handle click listener for Income
                // Option 1: Disable click or show a simple message
                holder.itemView.setOnClickListener{
                    showToast(holder.itemView.context, "Income item clicked: ${movement.description}")
                }


                // Show a different dialog for Income
                /*
                holder.itemView.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("movementType", "Income") // Add type identifier
                        putString("id", movement.id)
                        putString("description", movement.description)
                        putDouble("amount", movement.amount)
                        putLong("date", movement.date)
                    }
                    // val dialog = IncomeDetailDialogFragment() // If you create this
                    // dialog.arguments = bundle
                    // (holder.itemView.context as? AppCompatActivity)?.supportFragmentManager?.let { fm ->
                    //     dialog.show(fm, "IncomeDetailDialog")
                    // }
                }
                */
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateData(newItems: List<Movement>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged() // Consider using DiffUtil for better performance
    }
}