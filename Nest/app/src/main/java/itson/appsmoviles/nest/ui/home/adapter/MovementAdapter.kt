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
import itson.appsmoviles.nest.ui.home.detail.ExpenseDetailFragment
import itson.appsmoviles.nest.ui.home.detail.IncomeDetailFragment
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
                    val dialog = ExpenseDetailFragment.newInstance(movement)
                    (holder.itemView.context as? AppCompatActivity)?.supportFragmentManager?.let { fm ->
                        dialog.show(fm, "ExpenseDetailFragment")
                    }
                }
            }

            is Income -> {
                holder.amount.text = String.format(Locale.getDefault(), "+$%d", movement.amount.toInt()) // Using %.2f for currency
                holder.amount.setTextColor(holder.itemView.context.getColor(R.color.txt_income))
                holder.icon.setImageResource(R.drawable.icon_category_income)

                holder.itemView.setOnClickListener {
                    val dialog = IncomeDetailFragment.newInstance(movement)
                    (holder.itemView.context as? AppCompatActivity)?.supportFragmentManager?.let { fm ->
                        dialog.show(fm, "IncomeDetailFragment")
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateData(newItems: List<Movement>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}