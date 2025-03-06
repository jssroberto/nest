package itson.appsmoviles.nest.presentation.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import itson.appsmoviles.nest.R

class MovementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val movementIcon: ImageView = view.findViewById(R.id.movement_icon)
    val description: TextView = view.findViewById(R.id.movement_description)
    val amount: TextView = view.findViewById(R.id.movement_amount)
    val date: TextView = view.findViewById(R.id.movement_date)
}