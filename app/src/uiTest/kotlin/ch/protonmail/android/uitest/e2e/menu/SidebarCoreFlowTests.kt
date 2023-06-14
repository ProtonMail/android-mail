package ch.protonmail.android.uitest.e2e.menu

import ch.protonmail.android.test.annotations.suite.CoreLibraryTest
import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.di.LocalhostApi
import ch.protonmail.android.uitest.di.LocalhostApiModule
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.verify
import ch.protonmail.android.uitest.robot.menu.menuRobot
import ch.protonmail.android.uitest.util.extensions.waitUntilSignInScreenIsGone
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import me.proton.core.report.test.MinimalReportInternalTests
import me.proton.core.test.android.robots.auth.login.LoginRobot
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.User

@CoreLibraryTest
@HiltAndroidTest
@UninstallModules(LocalhostApiModule::class)
class SidebarCoreFlowTests : BaseTest(), MinimalReportInternalTests {

    private val loginRobot = LoginRobot()

    @JvmField
    @BindValue
    @LocalhostApi
    val localhostApi = false

    override val quark: Quark = BaseTest.quark
    override val users: User.Users = BaseTest.users

    override fun verifyBefore() {
        loginRobot.waitUntilSignInScreenIsGone()
        mailboxRobot { verify { isShown() } }
    }

    override fun startReport() {
        menuRobot {
            swipeOpenSidebarMenu()
            openReportBugs()
        }
    }

    override fun verifyAfter() {
        mailboxRobot { verify { isShown() } }
    }
}
