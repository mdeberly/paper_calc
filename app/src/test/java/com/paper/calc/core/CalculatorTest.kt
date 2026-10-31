package com.paper.calc.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculatorTest {

    private val en = NumberFormat('.', ',')
    private val fr = NumberFormat(',', NumberFormat.SPACE)

    private fun calc(keys: String): Calculator {
        val c = Calculator()
        for (k in keys) when (k) {
            in '0'..'9' -> c.digit(k)
            '.' -> c.dot()
            '+', '-', '*', '/' -> c.operator(k)
            'p' -> c.parenthesis()
            's' -> c.toggleSign()
            '<' -> c.backspace()
            'C' -> c.clear()
            '=' -> c.equals()
        }
        return c
    }

    private fun preview(keys: String): String {
        val r = calc(keys).preview()
        return (r as Evaluator.Result.Value).let { en.formatResult(it.value) }
    }

    @Test fun screenshotExample() {
        val c = calc("23.32+44556.6")
        assertEquals("23.32+44,556.6", en.formatExpression(c.expression))
        assertEquals("44,579.92", preview("23.32+44556.6"))
    }

    @Test fun precedence() {
        assertEquals("14", preview("2+3*4"))
        assertEquals("20", preview("p2+3p*4"))
        assertEquals("7", preview("10-6/2"))
        assertEquals("1", preview("8/4/2"))
        assertEquals("1", preview("8-4-3"))
    }

    @Test fun liveResultIgnoresTrailingOperatorAndOpenParens() {
        assertEquals("5", preview("2+3*"))
        assertEquals("8", preview("2*p3+1"))
    }

    @Test fun smartParenthesis() {
        assertEquals("(", calc("p").expression)
        assertEquals("(2+3)", calc("p2+3p").expression)
        assertEquals("2*(", calc("2p").expression)
        assertEquals("((2", calc("pp2").expression)
        assertEquals("((2)", calc("pp2p").expression)
        assertEquals("(2)*3", calc("p2p3").expression)
    }

    @Test fun signToggle() {
        assertEquals("~5", calc("5s").expression)
        assertEquals("5", calc("5ss").expression)
        assertEquals("2*~3", calc("2*3s").expression)
        assertEquals("~(2+3)", calc("p2+3ps").expression)
        assertEquals("−6", preview("2*3s"))
        assertEquals("2*~", calc("2*-").expression)
        assertEquals("2+", calc("2*-+").expression)
    }

    @Test fun operatorReplacement() {
        assertEquals("2+", calc("2*+").expression)
        assertEquals("", calc("+").expression)
        assertEquals("~", calc("-").expression)
    }

    @Test fun decimalInput() {
        assertEquals("0.5", calc(".5").expression)
        assertEquals("1.25", calc("1.2.5").expression)
        assertEquals("5", calc("05").expression)
    }

    @Test fun divisionByZero() {
        assertTrue(calc("5/0").preview() is Evaluator.Result.DivisionByZero)
    }

    @Test fun repeatingDecimals() {
        assertEquals("0.333333333333333", preview("1/3"))
        assertEquals("1", preview("1/3*3"))
        assertEquals("0.3", preview("0.1+0.2"))
    }

    @Test fun equalsSavesAndContinues() {
        val c = calc("2+3")
        val e = c.equals()!!
        assertEquals("2+3", e.expression)
        assertEquals("5", e.result)
        assertTrue(c.saved)
        c.operator('*'); c.digit('2')
        assertEquals("5*2", c.expression)
        val d = calc("2+3=7")
        assertEquals("7", d.expression)
    }

    @Test fun equalsNeedsAnOperation() {
        assertNull(calc("5").equals())
        assertNull(calc("").equals())
        assertNull(calc("5/0").equals())
    }

    @Test fun equalsClosesParentheses() {
        val c = calc("p2+3*")
        assertEquals("(2+3)", c.equals()!!.expression)
    }

    @Test fun formatting() {
        assertEquals("1 234 567,89", fr.formatResult(BigDecimal("1234567.89")))
        assertEquals("1.234.567,89", NumberFormat(',', '.').formatResult(BigDecimal("1234567.89")))
        assertEquals("12,5×3", fr.formatExpression("12.5*3"))
        assertEquals("1,000.", en.formatExpression("1000."))
        assertEquals("−1,000", en.formatResult(BigDecimal("-1000")))
        assertEquals("1.5E20", en.formatResult(BigDecimal("1.5E20")))
        assertEquals("0", en.formatResult(BigDecimal("0.000")))
    }

    @Test fun maxDigits() {
        val c = calc("1234567890123456789")
        assertEquals(15, c.expression.length)
    }
}
