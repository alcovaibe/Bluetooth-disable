package com.pulse.bluetoothdisable.cover.calculator

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalculatorAccessCodeManagerInstrumentedTest {
    private lateinit var manager: CalculatorAccessCodeManager

    @Before
    fun setUp() {
        manager = CalculatorAccessCodeManager(ApplicationProvider.getApplicationContext())
        manager.clearCode()
    }

    @After
    fun tearDown() {
        manager.clearCode()
    }

    @Test
    fun verifiesOnlyConfiguredCode() {
        manager.setCode("58317")
        assertTrue(manager.verify("58317"))
        assertFalse(manager.verify("58318"))
        assertFalse(manager.verify("00000"))
        assertFalse(manager.verify("12345"))
    }

    @Test
    fun leadingZeroCodeVerifiesWithoutNormalization() {
        manager.setCode("01234")
        assertTrue(manager.verify("01234"))
        assertFalse(manager.verify("1234"))
    }
}
