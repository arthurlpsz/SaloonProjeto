package br.edu.fatecpg.saloonprojeto.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.ref.WeakReference

class TelefoneFormatador(editText: EditText) : TextWatcher {

    private val editTextRef: WeakReference<EditText> = WeakReference(editText)
    private var isFormatting = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable) {
        if (isFormatting) return

        isFormatting = true

        val editText = editTextRef.get() ?: return
        val originalCursorPos = editText.selectionStart
        val originalText = s.toString()
        val digits = originalText.replace("[^0-9]".toRegex(), "")

        val formattedText = format(digits)
        
        s.replace(0, s.length, formattedText)

        val newCursorPos = calculateCursorPosition(originalCursorPos, originalText, formattedText)
        editText.setSelection(newCursorPos.coerceAtMost(formattedText.length))

        isFormatting = false
    }

    private fun format(digits: String): String {
        if (digits.isEmpty()) return ""
        
        val mask = if (digits.length > 10) "## #####-####" else "## ####-####"
        val formatted = StringBuilder()
        var digitIndex = 0
        mask.forEach { maskChar ->
            if (digitIndex >= digits.length) return formatted.toString()
            if (maskChar == '#') {
                formatted.append(digits[digitIndex])
                digitIndex++
            } else {
                if (digitIndex > 1) { // Apply space and hyphen only after the area code
                    formatted.append(maskChar)
                }
            }
        }
        return formatted.toString()
    }

    private fun calculateCursorPosition(originalPos: Int, originalText: String, formattedText: String): Int {
        var digitsBeforeCursor = 0
        for (i in 0 until originalPos) {
            if (originalText.getOrNull(i)?.isDigit() == true) {
                digitsBeforeCursor++
            }
        }

        var newPos = 0
        var digitsCounted = 0
        while (digitsCounted < digitsBeforeCursor && newPos < formattedText.length) {
            if (formattedText[newPos].isDigit()) {
                digitsCounted++
            }
            newPos++
        }
        
        while (newPos < formattedText.length && !formattedText[newPos].isDigit()) {
            newPos++
        }

        return newPos
    }
}
