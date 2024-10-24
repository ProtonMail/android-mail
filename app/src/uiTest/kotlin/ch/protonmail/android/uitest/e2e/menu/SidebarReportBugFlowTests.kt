package ch.protonmail.android.uitest.e2e.menu

import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.MainActivity
import ch.protonmail.android.initializer.MainInitializer
import ch.protonmail.android.test.annotations.suite.CoreLibraryTest
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uitest.di.LocalhostApi
import ch.protonmail.android.uitest.di.LocalhostApiModule
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.verify
import ch.protonmail.android.uitest.robot.menu.menuRobot
import ch.protonmail.android.uitest.rule.GrantNotificationsPermissionRule
import ch.protonmail.android.uitest.rule.MockOnboardingRuntimeRule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.report.test.MinimalReportInternalTests
import me.proton.core.test.rule.extension.protonAndroidComposeRule
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

@CoreLibraryTest
@HiltAndroidTest
@UninstallModules(LocalhostApiModule::class)
internal class SidebarReportBugFlowTests : MinimalReportInternalTests {

    @JvmField
    @BindValue
    @LocalhostApi
    val localhostApi = false

    @Inject
    lateinit var mockOnboardingRuntimeRule: MockOnboardingRuntimeRule

    @get:Rule
    val protonTestRule = protonAndroidComposeRule<MainActivity>(
        composeTestRule = ComposeTestRuleHolder.createAndGetComposeRule(),
        logoutBefore = false,
        fusionEnabled = false,
        additionalRules = linkedSetOf(GrantNotificationsPermissionRule()),
        afterHilt = { mainInitializer() }
    )

    @Before
    fun setup() {
        mockOnboardingRuntimeRule(false)
    }

    override fun startReport() {
        navigator { navigateTo(Destination.Inbox, performLoginViaUI = false) }

        menuRobot {
            openSidebarMenu()
            openReportBugs()
        }
    }

    override fun verifyAfter() {
        mailboxRobot { verify { isShown() } }
    }

    private fun mainInitializer() = runBlocking {
        withContext(Dispatchers.Main) { MainInitializer.init(ApplicationProvider.getApplicationContext()) }
    }
}
