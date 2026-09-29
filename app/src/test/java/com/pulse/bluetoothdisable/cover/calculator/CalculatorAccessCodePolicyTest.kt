package com.pulse.bluetoothdisable.cover.calculator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorAccessCodePolicyTest {
    @Test fun matchingFiveDigitCodesAreAccepted() =
        assertTrue(CalculatorAccessCodePolicy.matches("12345", "12345"))

    @Test fun differentCodesAreRejected() =
        assertFalse(CalculatorAccessCodePolicy.matches("12345", "12346"))

    @Test fun tooShortCodeIsRejected() =
        assertFalse(CalculatorAccessCodePolicy.isValid("1234"))

    @Test fun tooLongCodeIsRejected() =
        assertFalse(CalculatorAccessCodePolicy.isValid("123456"))

    @Test fun nonDigitsAreRejected() =
        assertFalse(CalculatorAccessCodePolicy.isValid("abcde"))

    @Test fun leadingZeroIsPreservedAndAccepted() =
        assertTrue(CalculatorAccessCodePolicy.isValid("01234"))
}
