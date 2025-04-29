package itson.appsmoviles.nest.presentation.utilities

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R
import kotlin.math.atan2
import kotlin.math.sqrt

class PieChartDrawable(context: Context, var categorias: ArrayList<Categoria>) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var coordenadas: RectF? = null
    private var anguloInicio: Float = 0.0f
    private val padding = 25.0f
    private val context: Context = context
    private var selectedCategoria: Categoria? = null
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

        val total = categorias.sumOf { it.total.toDouble() }.toFloat()
        if (total == 0f) return

        for (e in categorias) {
            e.porcentaje = (e.total / total) * 100
        }

        if (categorias.isNotEmpty()) {
            for (e in categorias) {
                val sweep = (e.porcentaje * 360) / 100
                val anguloBarrido = if (sweep >= 360f) 359.9f else sweep
                val color = ContextCompat.getColor(context, e.color)
                paint.color = color

                coordenadas?.let {
                    canvas.drawArc(it, anguloInicio, anguloBarrido, true, paint)
                }

                anguloInicio += anguloBarrido
            }
        }

        selectedCategoria?.let { categoria ->
            val texto = "${categoria.nombre}: ${"%.1f".format(categoria.porcentaje)}%"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 40f
                R.font.lexend_bold
                setShadowLayer(8f, 4f, 4f, Color.LTGRAY) // Sombra bonita
            }

            val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(10f, 0f, 0f, Color.GRAY) // Sombra al fondo
            }

            val textPadding = 30f  // Padding interno
            val textWidth = textPaint.measureText(texto)
            val textHeight = textPaint.descent() - textPaint.ascent()

            // Definir posiciones basadas en el toque
            var left = touchX
            var top = touchY - textHeight - textPadding
            var right = left + textWidth + 2 * textPadding
            var bottom = touchY

            // Correcciones para que no se corte
            if (right > bounds.right) {
                left = bounds.right - textWidth - 2 * textPadding
                right = bounds.right.toFloat()
            }
            if (left < bounds.left) {
                left = bounds.left.toFloat()
                right = left + textWidth + 2 * textPadding
            }
            if (top < bounds.top) {
                top = bounds.top.toFloat()
                bottom = top + textHeight + textPadding
            }
            if (bottom > bounds.bottom) {
                bottom = bounds.bottom.toFloat()
                top = bottom - textHeight - textPadding
            }

            val rectF = RectF(left, top, right, bottom)

            // Dibuja fondo con esquinas redondeadas
            canvas.drawRoundRect(rectF, 20f, 20f, backgroundPaint)

            // Dibuja texto centrado
            val textX = left + textPadding
            val textY = top + textPadding - textPaint.ascent() // Centrar bien el texto
            canvas.drawText(texto, textX, textY, textPaint)
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

    fun onTouch(x: Float, y: Float) {
        val centroX = bounds.width() / 2f
        val centroY = bounds.height() / 2f
        val dx = x - centroX
        val dy = y - centroY
        val distancia = sqrt(dx * dx + dy * dy)

        val radio = bounds.width() / 2.5f
        if (distancia > radio) {
            selectedCategoria = null
            invalidateSelf()
            return
        }

        val anguloToque = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val anguloNormalizado = if (anguloToque < 0) anguloToque + 360 else anguloToque

        var anguloActual = 0f
        for (categoria in categorias) {
            val anguloBarrido = (categoria.porcentaje * 360) / 100
            if (anguloNormalizado in anguloActual..(anguloActual + anguloBarrido)) {
                selectedCategoria = categoria
                touchX = x
                touchY = y
                break
            }
            anguloActual += anguloBarrido
        }
        invalidateSelf()
    }
}
