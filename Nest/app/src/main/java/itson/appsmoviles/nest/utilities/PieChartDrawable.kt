package itson.appsmoviles.nest.utilities

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R

class PieChartDrawable(context: Context, var categorias: ArrayList<Categoria>) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var coordenadas: RectF? = null
    private var anguloInicio: Float = 0.0f
    private val padding = 25.0f
    private val context: Context = context

    init {
        paint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val ancho: Float = (bounds.width() - padding)
        val alto: Float = (bounds.height() - padding)
        coordenadas = RectF(padding, padding, ancho, alto)

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
}
