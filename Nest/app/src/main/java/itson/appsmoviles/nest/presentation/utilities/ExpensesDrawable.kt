package itson.appsmoviles.nest.presentation.utilities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import kotlin.math.abs

class ExpensesDrawable(
    private val total: Float,
    private val current: Float,
    private val progressColor: Int,
    private val backgroundColor: Int
) : Drawable() {

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        val maxValue = maxOf(total, current)
        val totalWidth = (total / maxValue) * width
        val currentWidth = (current / maxValue) * width

        val backgroundPaint = Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }

        val progressPaint = Paint().apply {
            color = progressColor
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }


        if (current > total) {
            canvas.drawRect(0f, 0f, currentWidth, height, backgroundPaint)
            canvas.drawRect(0f, 0f, totalWidth, height, progressPaint)
        } else {
            canvas.drawRect(0f, 0f, totalWidth, height, backgroundPaint)
            canvas.drawRect(0f, 0f, currentWidth, height, progressPaint)
        }


        val totalText = "$${"%.2f".format(total)}"
        val currentText = "$${"%.2f".format(current)}"
        val totalTextWidth = textPaint.measureText(totalText)
        val currentTextWidth = textPaint.measureText(currentText)

        val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        val margin = 16f


        if (total >= current) {

            canvas.drawText(currentText, margin, textY, textPaint)
            canvas.drawText(totalText, width - totalTextWidth - margin, textY, textPaint)
        } else {

            canvas.drawText(totalText, margin, textY, textPaint)
            canvas.drawText(currentText, width - currentTextWidth - margin, textY, textPaint)
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
