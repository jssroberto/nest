package itson.appsmoviles.nest.presentation.adapter

import android.R.attr.text
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.domain.model.Movement

class MovementAdapter(private val items: List<Movement>) : RecyclerView.Adapter<MovementViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.movement_layout, parent, false)
        return MovementViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        val item = items[position]
        holder.description.text = item.description
        holder.amount.text = item.amount.toString()
        holder.date.text = item.date.toString()
    }


    override fun getItemCount(): Int {
        return items.size
    }

}