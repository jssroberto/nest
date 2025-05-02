package itson.appsmoviles.nest.ui.home.adapter

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enums.CategoryType
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.ui.home.ExpenseDetailDialogFragment
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MovementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val icon: ImageView = view.findViewById(R.id.movement_icon)
    val description: TextView = view.findViewById(R.id.movement_description)
    val amount: TextView = view.findViewById(R.id.movement_amount)
    val date: TextView = view.findViewById(R.id.movement_date)
}