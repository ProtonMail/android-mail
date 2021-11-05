package ch.protonmail.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ch.protonmail.android.foo.ClassNotTested
import ch.protonmail.android.foo.ClassUnderTest

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.ProtonTheme_Mail)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val classUnderTest = ClassUnderTest()
        classUnderTest.doingAddition(1, 2)

        val classNotTested = ClassNotTested()
        classNotTested.untestedAddition(1, 2)
    }
}
