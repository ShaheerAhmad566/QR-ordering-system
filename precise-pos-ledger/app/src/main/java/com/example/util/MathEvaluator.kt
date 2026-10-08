package com.example.util

object MathEvaluator {

    /**
     * Evaluates a mathematical expression like "=8+3", "9-3", "12+8*2", "100/4".
     * If the input is a plain number or invalid expression, returns null or fallback.
     */
    fun evaluate(input: String): Double? {
        val trimmed = input.trim().removePrefix("=").trim()
        if (trimmed.isEmpty()) return null

        // Try direct number first
        trimmed.toDoubleOrNull()?.let { return it }

        return try {
            parseExpression(trimmed)
        } catch (e: Exception) {
            null
        }
    }

    fun evaluateToInt(input: String, fallback: Int = 0): Int {
        val result = evaluate(input) ?: return fallback
        return result.toInt()
    }

    fun evaluateToDouble(input: String, fallback: Double = 0.0): Double {
        return evaluate(input) ?: fallback
    }

    private fun parseExpression(expression: String): Double {
        val clean = expression.replace("\\s".toRegex(), "")
        var pos = -1
        var ch = -1

        fun nextChar() {
            ch = if (++pos < clean.length) clean[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression(clean)
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = clean.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }

            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    else -> return x
                }
            }
        }

        fun parse(): Double {
            nextChar()
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        return parse()
    }
}
