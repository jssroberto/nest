package itson.appsmoviles.nest.ui.util

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import itson.appsmoviles.nest.R

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@SuppressLint("ClickableViewAccessibility")
fun EditText.setupPasswordVisibilityToggle() {
    this.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    this.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eye, 0)
    var isPasswordVisible = false

    this.setOnTouchListener { v, event ->
        if (event.action != MotionEvent.ACTION_UP) {
            return@setOnTouchListener false
        }

        val drawable = this.compoundDrawablesRelative[2] ?: this.compoundDrawables[2]
        ?: return@setOnTouchListener false

        val drawableWidth = drawable.bounds.width()
        val isDrawableClicked: Boolean

        if (this.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            isDrawableClicked = event.x <= (drawableWidth + this.paddingLeft)
        } else {
            isDrawableClicked = event.x >= (this.width - this.paddingRight - drawableWidth)
        }

        if (!isDrawableClicked) {
            return@setOnTouchListener false
        }

        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            this.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            this.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eye_off, 0)
        } else {
            this.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            this.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eye, 0)
        }
        this.setSelection(this.text.length)
        v.performClick()
        return@setOnTouchListener true
    }
}

fun String.toTitleCase(): String {
    if (this.isEmpty()) {
        return this
    }
    return this.lowercase().replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase() else char.toString()
    }
}