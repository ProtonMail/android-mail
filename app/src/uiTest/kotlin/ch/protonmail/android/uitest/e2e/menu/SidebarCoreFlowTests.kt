package ch.protonmail.android.uitest.e2e.menu

import ch.protonmail.android.test.annotations.suite.CoreLibraryTest
import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.di.LocalhostApi
import ch.protonmail.android.uitest.di.LocalhostApiModule
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import me.proton.core.report.test.MinimalReportInternalTests
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.User

@CoreLibraryTest
@HiltAndroidTest
@UninstallModules(LocalhostApiModule::class)
class SidebarCoreFlowTests : BaseTest(), MinimalReportInternalTests {

    @JvmField
    @BindValue
    @LocalhostApi
    val localhostApi = false

    private val inboxRobot = InboxRobot(composeTestRule)
    private val menuRobot = MenuRobot(composeTestRule)

    override val quark: Quark = BaseTest.quark
    override val users: User.Users = BaseTest.users

    override fun verifyBefore() {
        inboxRobot.verify { mailboxScreenDisplayed() }
    }

    override fun startReport() {
        menuRobot
            .swipeOpenSidebarMenu()
            .openReportBugs()
    }

    override fun verifyAfter() {
        inboxRobot.verify { mailboxScreenDisplayed() }
    }
}
