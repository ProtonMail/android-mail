package ch.protonmail.android.uitests.tests.login

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.protonmail.android.MainActivity
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginTests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val loginRobot = LoginRobot()

    @Category(SmokeTest::class)
    @Test
    fun openEmptyApp() {
        loginRobot
            .launchApp()
            .verify { protonMailTitleDisplayed() }
    }

}
