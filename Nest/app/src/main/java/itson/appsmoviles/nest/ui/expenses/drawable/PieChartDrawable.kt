package itson.appsmoviles.nest.ui.expenses.drawable

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Category
import kotlin.math.atan2

class PieChartDrawable(
    private val context: Context,
    var categories: List<Category>,
    private val categoryTextViews: List<TextView>,
    private val onCategorySelected: ((String?) -> Unit)? = null
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var coordenadas: RectF? = null
    private var anguloInicio: Float = 0.0f
    private val padding = 25.0f
    var selectedCategory: Category? = null

    init {
        paint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val ancho: Float = (bounds.width() - padding)
        val alto: Float = (bounds.height() - padding)
        coordenadas = RectF(padding, padding, ancho, alto)

        anguloInicio = 0.0f

        val total = categories.sumOf { it.total.toDouble() }.toFloat()
        if (total == 0f) return

        for (e in categories) {
            e.percentage = (e.total / total) * 100
        }

        for (e in categories) {
            val sweep = (e.percentage * 360) / 100
            val anguloBarrido = if (sweep >= 360f) 359.9f else sweep

            val color = if (selectedCategory == null || e == selectedCategory) {
                ContextCompat.getColor(context, e.color)
            } else {
                ContextCompat.getColor(context, R.color.gray)
            }

            paint.color = color

            coordenadas?.let {
                canvas.drawArc(it, anguloInicio, anguloBarrido, true, paint)
            }

            anguloInicio += anguloBarrido
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE

    fun getCategoryFromTouch(x: Float, y: Float): Category? {
        coordenadas?.let { rect ->
            val cx = rect.centerX()
            val cy = rect.centerY()
            val dx = x - cx
            val dy = y - cy
            val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

            if (distance > rect.width() / 2) return null

            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            if (angle < 0) angle += 360f

            var startAngle = 0f
            for (categoria in categories) {
                val sweepAngle = (categoria.percentage * 360) / 100
                val endAngle = startAngle + sweepAngle

                if (angle >= startAngle && angle < endAngle) {
                    return categoria
                }
                startAngle = endAngle
            }
        }
        return null
    }

    fun handleTouch(x: Float, y: Float) {
        val clickedCategory = getCategoryFromTouch(x, y)
        if (clickedCategory != null) {
            if (clickedCategory == selectedCategory) {
                selectedCategory = null
                highlightSelectedCategory(null)
                onCategorySelected?.invoke(null)
            } else {
                selectedCategory = clickedCategory
                highlightSelectedCategory(clickedCategory.type.displayName)
                onCategorySelected?.invoke(clickedCategory.type.displayName)
            }
        } else {
            selectedCategory = null
            highlightSelectedCategory(null)
            onCategorySelected?.invoke(null)
        }
        invalidateSelf()
    }

    private fun highlightSelectedCategory(categoryName: String?) {
        if (categoryName == null) {

            categoryTextViews.forEach { view ->
                view.setTextColor(ContextCompat.getColor(context, R.color.txt_color))
                view.text = view.tag?.toString() ?: ""
            }
        } else {

            categoryTextViews.forEach { view ->
                view.setTextColor(ContextCompat.getColor(context, R.color.txt_hint))
                view.text = view.tag?.toString() ?: ""
            }

            val selectedTextView = categoryTextViews.find { it.tag == categoryName } ?: return
            selectedTextView.setTextColor(ContextCompat.getColor(context, R.color.txt_color))

            val percentage = categories.find { it.type.displayName == categoryName }?.percentage ?: 0.0f

            selectedTextView.text = "$categoryName  ${"%.1f".format(percentage)}%"
        }
    }




}
