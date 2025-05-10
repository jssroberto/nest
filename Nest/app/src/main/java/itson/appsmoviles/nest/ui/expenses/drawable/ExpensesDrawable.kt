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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import itson.appsmoviles.nest.R

class ExpensesDrawable(
    private val total: Float,
    private val current: Float,
    private val totalColor: Int,
    private val isFiltered: Boolean = false,
    private val textColor: Int,
    private val context: Context
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
        val overBudget = current > total

        val baseColor = if (shouldGrayOut) {
            ContextCompat.getColor(context, R.color.edt_text)
        } else {
            totalColor
        }

        val currentColor = if (overBudget) {
            ContextCompat.getColor(context, R.color.dark_orange)
        } else {
            ContextCompat.getColor(context, R.color.category_living)
        }

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = baseColor
        }

        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = currentColor
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 40f
            typeface = ResourcesCompat.getFont(context, R.font.lexend_regular)
        }

        val margin = 16f
        val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f

        val totalText = "$${"%.2f".format(total)}"
        val currentText = "$${"%.2f".format(current)}"
        val totalTextWidth = textPaint.measureText(totalText)


        canvas.drawRect(0f, 0f, width, height, backgroundPaint)


        val ratio = if (total == 0f) 1f else (current / total).coerceAtMost(1f)
        val progressWidth = animatedProgress * ratio * width
        canvas.drawRect(0f, 0f, progressWidth, height, progressPaint)


        canvas.drawText(currentText, margin, textY, textPaint)
        canvas.drawText(totalText, width - totalTextWidth - margin, textY, textPaint)
    }


    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
