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
    var selectedCategoria: Categoria? = null
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


                val color = if (selectedCategoria == null || e == selectedCategoria) {
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

    // Método que se llama cuando el gráfico es tocado
    fun onTouch(x: Float, y: Float) {
        val centroX = bounds.width() / 2f
        val centroY = bounds.height() / 2f
        val dx = x - centroX
        val dy = y - centroY
        val distancia = sqrt(dx * dx + dy * dy)

        val radio = bounds.width() / 2.5f
        if (distancia > radio) {
            // Si el toque está fuera del gráfico, deseleccionar la categoría
            selectedCategoria = null // Restaurar el estado original
            invalidateSelf() // Redibujar
            return
        }

        val anguloToque = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val anguloNormalizado = if (anguloToque < 0) anguloToque + 360 else anguloToque

        var anguloActual = 0f
        for (categoria in categorias) {
            val anguloBarrido = (categoria.porcentaje * 360) / 100
            if (anguloNormalizado in anguloActual..(anguloActual + anguloBarrido)) {
                selectedCategoria = categoria // Seleccionar la categoría
                touchX = x
                touchY = y
                break
            }
            anguloActual += anguloBarrido
        }
        invalidateSelf() // Redibujar el gráfico después de seleccionar
    }
}
