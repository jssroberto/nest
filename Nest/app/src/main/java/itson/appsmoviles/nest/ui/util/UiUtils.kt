package itson.appsmoviles.nest.ui.util

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

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

@RequiresApi(Build.VERSION_CODES.O)
fun showDatePicker(
    context: Context,
    initialTimestamp: Long = System.currentTimeMillis(),
    maxTimestamp: Long = System.currentTimeMillis(),
    onDateSelected: (timestampMillis: Long) -> Unit
) {
    val initialCalendar = Calendar.getInstance().apply {
        timeInMillis = initialTimestamp
    }
    val initialYear = initialCalendar.get(Calendar.YEAR)
    val initialMonth = initialCalendar.get(Calendar.MONTH)
    val initialDay = initialCalendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        R.style.Nest_DatePicker,
        { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate =
                LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            val millis = selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            onDateSelected(millis)
        },
        initialYear, initialMonth, initialDay
    )


    datePickerDialog.datePicker.maxDate = maxTimestamp

    datePickerDialog.setOnShowListener {
        val txtColor = ContextCompat.getColor(
            context,
            R.color.txt_color
        )
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(txtColor)
        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(txtColor)
    }

    datePickerDialog.show()
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDateLongForm(timestampMillis: Long): String? {
    return Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()))
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDateShortForm(timestampMillis: Long): String? {
    return Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.getDefault()))
}

fun addDollarSign(editText: EditText) {
    editText.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable?) {
            editText.removeTextChangedListener(this)

            val input = editable.toString()

            editText.setText(if (!input.startsWith("$")) "$$input" else input)
            editText.setSelection(editText.text.length)
            editText.addTextChangedListener(this)
        }
    })
}

fun getCategoryColors(context: Context): Map<CategoryType, String> {
    fun colorToHex(colorResId: Int): String {
        val colorInt = ContextCompat.getColor(context, colorResId)
        return String.format("#%06X", 0xFFFFFF and colorInt)
    }

    return mapOf(
        CategoryType.LIVING to colorToHex(R.color.category_living),
        CategoryType.RECREATION to colorToHex(R.color.category_recreation),
        CategoryType.TRANSPORT to colorToHex(R.color.category_transport),
        CategoryType.FOOD to colorToHex(R.color.category_food),
        CategoryType.HEALTH to colorToHex(R.color.category_health),
        CategoryType.OTHER to colorToHex(R.color.category_other)
    )
}

fun String.unaccent(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    return regex.replace(normalized, "")
}