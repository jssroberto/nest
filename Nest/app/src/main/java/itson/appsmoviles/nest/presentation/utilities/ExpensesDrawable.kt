package itson.appsmoviles.nest.presentation.utilities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class ExpensesDrawable(
    private val total: Float,
    private val current: Float,
    private val progressColor: Int, // Color para la barra de progreso
    private val backgroundColor: Int // Color para el fondo de la barra
) : Drawable() {

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        // Calculamos el ancho del progreso con base en el total y el actual
        val progressWidth = (current / total) * width

        // Pintamos el fondo de la barra de progreso
        val backgroundPaint = Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width, height, backgroundPaint)

        // Pintamos la barra de progreso
        val progressPaint = Paint().apply {
            color = progressColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, progressWidth, height, progressPaint)
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }
}
