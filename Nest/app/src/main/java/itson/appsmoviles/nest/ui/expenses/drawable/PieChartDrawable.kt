package itson.appsmoviles.nest.ui.expenses.drawable

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.model.Category
import kotlin.math.atan2

class PieChartDrawable(context: Context, var categories: ArrayList<Category>) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var coordenadas: RectF? = null
    private var anguloInicio: Float = 0.0f
    private val padding = 25.0f
    private val context: Context = context
    var selectedCategory: Category? = null
    private var touchX = 0f
    private var touchY = 0f

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

        if (categories.isNotEmpty()) {
            for (e in categories) {
                val sweep = (e.percentage * 360) / 100
                val anguloBarrido = if (sweep >= 360f) 359.9f else sweep


                val color = if (selectedCategory == null || e == selectedCategory) {
                    ContextCompat.getColor(context, e.color) // Colores normales si no hay selección
                } else {
                    ContextCompat.getColor(context, R.color.gray) // Gris solo si hay algo seleccionado
                }

                paint.color = color

                coordenadas?.let {
                    canvas.drawArc(it, anguloInicio, anguloBarrido, true, paint)
                }

                anguloInicio += anguloBarrido
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }
    fun getCategoryFromTouch(x: Float, y: Float): Category? {
        coordenadas?.let { rect ->
            val cx = rect.centerX()
            val cy = rect.centerY()
            val dx = x - cx
            val dy = y - cy
            val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

            // Si tocó fuera del círculo
            val radius = rect.width() / 2
            if (distance > radius) return null

            // Ángulo tocado
            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            if (angle < 0) angle += 360f

            // Recorrer cada categoría para ver en qué ángulo está
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

}
