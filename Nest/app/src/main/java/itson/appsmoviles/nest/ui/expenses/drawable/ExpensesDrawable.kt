package itson.appsmoviles.nest.ui.expenses.drawable

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import itson.appsmoviles.nest.R

class ExpensesDrawable(
    private val total: Float,
    private val current: Float,
    private val currentColor: Int,
    private val totalColor: Int,
    private val typeface: Typeface?,
    private val isFiltered: Boolean = false
) : Drawable() {

    private var animatedProgress: Float = 0f

    init {
        val target = if (total == 0f) 0f else (current / total).coerceAtMost(1f)
        ValueAnimator.ofFloat(0f, target).apply {
            duration = 600L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                animatedProgress = it.animatedValue as Float
                invalidateSelf()
            }
            start()
        }
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        if (width == 0f || height == 0f) return


        val isZeroExpense = current == 0f
        val shouldGrayOut = isFiltered || isZeroExpense

        val currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (shouldGrayOut) Color.GRAY else currentColor
        }

        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (shouldGrayOut) Color.GRAY else totalColor
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            this.typeface = typeface
        }

        val margin = 16f
        val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f

        val totalText = "$${"%.2f".format(total)}"
        val currentText = "$${"%.2f".format(current)}"
        val totalTextWidth = textPaint.measureText(totalText)
        val currentTextWidth = textPaint.measureText(currentText)

        if (current > total) {
            val totalRatio = total / current
            val totalWidth = animatedProgress * totalRatio * width
            canvas.drawRect(0f, 0f, width, height, currentPaint)
            canvas.drawRect(0f, 0f, totalWidth, height, totalPaint)

            canvas.drawText(currentText, width - currentTextWidth - margin, textY, textPaint)
            canvas.drawText(totalText, margin, textY, textPaint)
        } else {
            val currentWidth = animatedProgress * (current / total.coerceAtLeast(1f)) * width
            canvas.drawRect(0f, 0f, width, height, totalPaint)
            canvas.drawRect(0f, 0f, currentWidth, height, currentPaint)

            canvas.drawText(currentText, margin, textY, textPaint)
            canvas.drawText(totalText, width - totalTextWidth - margin, textY, textPaint)
        }
    }


    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
