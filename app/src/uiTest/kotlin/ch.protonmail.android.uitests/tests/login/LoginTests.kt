package ch.protonmail.android.uitests.tests.login

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.protonmail.android.uitests.BaseTest
import ch.protonmail.android.uitests.annotations.SmokeTest
import ch.protonmail.android.uitests.login.InboxRobot
import me.proton.core.test.android.robots.auth.AddAccountRobot
import me.proton.core.test.android.robots.auth.login.LoginRobot
import me.proton.core.test.android.robots.auth.login.MailboxPasswordRobot
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginTests : BaseTest() {

    private val addAccountRobot = AddAccountRobot()
    private val loginRobot = LoginRobot()

    @Before
    fun signIn() {
        addAccountRobot
            .signIn()
            .verify { loginElementsDisplayed() }
    }

    @Test
    @Category(SmokeTest::class)
    fun loginUserHappyPath() {
        val user = users.getUser { it.name == "pro" }
        loginRobot
            .loginUser<InboxRobot>(user)
            .verify { mailboxScreenDisplayed() }
    }

    @Test
    @Category(SmokeTest::class)
    fun loginUserWithSecondaryPasswordHappyPath() {
        val user = users.getUser(usernameAndOnePass = false) { it.name == "twopasswords" }
        loginRobot
            .loginUser<MailboxPasswordRobot>(user)
            .unlockMailbox<InboxRobot>(user)
            .verify { mailboxScreenDisplayed() }
    }
}
