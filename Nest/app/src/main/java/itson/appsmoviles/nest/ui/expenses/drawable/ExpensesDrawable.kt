package itson.appsmoviles.nest.ui.expenses.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable

class ExpensesDrawable(
    private val total: Float,
    private val current: Float,
    private val currentColor: Int,     // Color de current
    private val totalColor: Int         // Color de total
) : Drawable() {

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        if (width == 0f || height == 0f) return

        val currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = currentColor
        }

        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = totalColor
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
        }

        val margin = 16f
        val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f

        val totalText = "$${"%.2f".format(total)}"
        val currentText = "$${"%.2f".format(current)}"
        val totalTextWidth = textPaint.measureText(totalText)
        val currentTextWidth = textPaint.measureText(currentText)

        if (current > total) {
            // current domina (100%), total es proporcional
            val totalRatio = total / current
            val totalWidth = totalRatio * width

            // Fondo: current
            canvas.drawRect(0f, 0f, width, height, currentPaint)

            // Barra interna: total
            canvas.drawRect(0f, 0f, totalWidth, height, totalPaint)

            // Textos
            canvas.drawText(totalText, margin, textY, textPaint)
            canvas.drawText(currentText, width - currentTextWidth - margin, textY, textPaint)

        } else {
            // total domina (100%), current es proporcional
            val currentRatio = current / total
            val currentWidth = currentRatio * width

            // Fondo: total
            canvas.drawRect(0f, 0f, width, height, totalPaint)

            // Barra interna: current
            canvas.drawRect(0f, 0f, currentWidth, height, currentPaint)

            // Textos
            canvas.drawText(currentText, margin, textY, textPaint)
            canvas.drawText(totalText, width - totalTextWidth - margin, textY, textPaint)
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
