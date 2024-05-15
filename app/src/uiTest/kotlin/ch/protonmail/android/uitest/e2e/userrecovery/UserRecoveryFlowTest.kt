package ch.protonmail.android.uitest.e2e.userrecovery

import ch.protonmail.android.test.annotations.suite.CoreLibraryTest
import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.di.LocalhostApi
import ch.protonmail.android.uitest.di.LocalhostApiModule
import ch.protonmail.android.uitest.robot.account.section.buttonsSection
import ch.protonmail.android.uitest.robot.account.signOutAccountDialogRobot
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.verify
import ch.protonmail.android.uitest.robot.menu.menuRobot
import ch.protonmail.android.uitest.util.awaitProgressIsHidden
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import me.proton.core.auth.test.usecase.WaitForPrimaryAccount
import me.proton.core.domain.entity.UserId
import me.proton.core.test.quark.Quark
import me.proton.core.userrecovery.dagger.CoreDeviceRecoveryFeaturesModule
import me.proton.core.userrecovery.domain.IsDeviceRecoveryEnabled
import me.proton.core.userrecovery.domain.repository.DeviceRecoveryRepository
import me.proton.core.userrecovery.presentation.compose.DeviceRecoveryHandler
import me.proton.core.userrecovery.presentation.compose.DeviceRecoveryNotificationSetup
import me.proton.core.userrecovery.test.MinimalUserRecoveryTest
import javax.inject.Inject
import kotlin.test.BeforeTest

@CoreLibraryTest
@HiltAndroidTest
@UninstallModules(
    LocalhostApiModule::class,
    CoreDeviceRecoveryFeaturesModule::class
)
internal class UserRecoveryFlowTest : BaseTest(), MinimalUserRecoveryTest {
    override val quark: Quark = BaseTest.quark

    @JvmField
    @BindValue
    @LocalhostApi
    val localhostApi = false

    @Inject
    override lateinit var deviceRecoveryHandler: DeviceRecoveryHandler

    @Inject
    override lateinit var deviceRecoveryNotificationSetup: DeviceRecoveryNotificationSetup

    @Inject
    override lateinit var deviceRecoveryRepository: DeviceRecoveryRepository

    @Inject
    override lateinit var waitForPrimaryAccount: WaitForPrimaryAccount

    @BindValue
    internal val isDeviceRecoveryEnabled = object : IsDeviceRecoveryEnabled {
        override fun invoke(userId: UserId?): Boolean = true
        override fun isLocalEnabled(): Boolean = true
        override fun isRemoteEnabled(userId: UserId?): Boolean = true
    }

    @BeforeTest
    override fun prepare() {
        super.prepare()
        initFusion(composeTestRule)
    }

    override fun signOut() {
        menuRobot { openSidebarMenu() }
        menuRobot { tapSignOut() }
        signOutAccountDialogRobot {
            buttonsSection { tapSignOut() }
        }
    }
}
