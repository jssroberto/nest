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
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.ui.home.detail.ExpenseDetailDialogFragment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MovementAdapter(
    // Initialize with an empty list, it will be updated
    private var items: MutableList<Expense> = mutableListOf()
) : RecyclerView.Adapter<MovementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_movement, parent, false)
        return MovementViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O) // Keep if API level requires
    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        val item = items[position]
        // Keep your existing binding logic...
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) // Corrected pattern: yyyy

        holder.description.text = item.description
        holder.amount.text = String.format(Locale.getDefault(), "$%.2f", item.amount) // Format amount nicely
        val dateTime = Instant.ofEpochMilli(item.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        holder.date.text = dateTime.format(formatter)

        val iconResId = when (item.category) {
            CategoryType.LIVING -> R.drawable.icon_category_living
            CategoryType.RECREATION -> R.drawable.icon_category_recreation
            CategoryType.TRANSPORT -> R.drawable.icon_category_transport
            CategoryType.FOOD -> R.drawable.icon_category_food
            CategoryType.HEALTH -> R.drawable.icon_category_health
            CategoryType.OTHER -> R.drawable.icon_category_other
            // Consider removing 'else' if CategoryType is a sealed class or enum covering all cases
            // else -> R.drawable.alert_circle
        }
        holder.icon.setImageResource(iconResId)

        holder.itemView.setOnClickListener {
            val bundle = Bundle().apply {
                putString("id", item.id)
                putString("description", item.description)
                putDouble("amount", item.amount)
                putLong("date", item.date)
                putString("category", item.category.name)
                putString("paymentMethod", item.paymentMethod.name)
            }

            val dialog = ExpenseDetailDialogFragment()
            dialog.arguments = bundle
            // Ensure context is an Activity context for FragmentManager
            (holder.itemView.context as? AppCompatActivity)?.supportFragmentManager?.let { fm ->
                dialog.show(fm, "ExpenseDetailDialog")
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Updates the data in the adapter and notifies the RecyclerView.
     * @param newItems The new list of expenses to display.
     */
    fun updateData(newItems: List<Expense>) {
        items.clear()
        items.addAll(newItems)
        // This is crucial to tell the RecyclerView to redraw
        notifyDataSetChanged()
    }
}