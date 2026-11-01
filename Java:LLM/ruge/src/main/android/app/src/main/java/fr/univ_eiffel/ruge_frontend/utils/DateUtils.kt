package fr.univ_eiffel.ruge_frontend.utils

import java.text.SimpleDateFormat
import java.util.Locale

fun String.toReadableDate(): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMMM yyyy 'à' HH:mm", Locale.FRANCE)
        val date = inputFormat.parse(this)
        if (date != null) {
            outputFormat.format(date)
        } else {
            this
        }
    } catch (e: Exception) {
        this
    }
}