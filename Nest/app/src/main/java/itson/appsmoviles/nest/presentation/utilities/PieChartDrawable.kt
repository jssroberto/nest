package itson.appsmoviles.nest.presentation.utilities

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
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

        if (categorias.isNotEmpty()) {
            for (e in categorias) {
                val anguloBarrido: Float = (e.porcentaje * 360) / 100
                val color = ContextCompat.getColor(context, e.color)
                paint.color = color

                coordenadas?.let {
                    canvas.drawArc(it, anguloInicio, anguloBarrido, true, paint)
                }

                anguloInicio += anguloBarrido
            }
        }

        // Dibujar tooltip si hay una categoría seleccionada
        selectedCategoria?.let { categoria ->
            val texto = "${categoria.nombre}: ${categoria.porcentaje}%"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 40f
            }
            val textWidth = textPaint.measureText(texto)

            // Dibuja un rectángulo de fondo
            paint.color = Color.WHITE
            canvas.drawRect(
                touchX - 20, touchY - 80, touchX + textWidth + 20, touchY - 20, paint
            )

            // Dibuja el texto
            paint.color = Color.BLACK
            canvas.drawText(texto, touchX, touchY - 40, textPaint)
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

    // Detecta el toque en el gráfico y actualiza la categoría seleccionada
    fun onTouch(x: Float, y: Float) {
        val centroX = bounds.width() / 2f
        val centroY = bounds.height() / 2f
        val dx = x - centroX
        val dy = y - centroY
        val distancia = sqrt(dx * dx + dy * dy)

        // Si el toque está fuera del círculo, ignorarlo
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
