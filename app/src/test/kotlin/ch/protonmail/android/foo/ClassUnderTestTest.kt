package ch.protonmail.android.foo

import org.junit.Assert.assertEquals
import org.junit.Test

class ClassUnderTestTest {

    val subject = ClassUnderTest()

    @Test
    fun testingAddition() {
        val actual = subject.doingAddition(2, 4)

        assertEquals(6, actual)
    }
}