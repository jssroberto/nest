package itson.appsmoviles.nest.presentation.decorator

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class BottomMarginItemDecoration(private val bottomMargin: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position < (parent.adapter?.itemCount?.minus(1) ?: 0)) {
            outRect.bottom = bottomMargin // Add space below every item except the last one
        }
    }
}