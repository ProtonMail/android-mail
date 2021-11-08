package ch.protonmail.android.uitests.login

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText

/**
 * [LoginRobot] class contains actions and verifications for login functionality.
 */
class LoginRobot {

    fun launchApp() = this

    /**
     * Contains all the validations that can be performed by [LoginRobot].
     */
    class Verify {

        fun appIsLaunchedCorrectly(): LoginRobot {
            onView(withText("Hello World!")).check(matches(isDisplayed()))
            return LoginRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
