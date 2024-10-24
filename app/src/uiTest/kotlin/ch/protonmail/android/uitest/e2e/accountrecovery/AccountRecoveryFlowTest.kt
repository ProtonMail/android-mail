package ch.protonmail.android.uitest.e2e.accountrecovery

import ch.protonmail.android.test.annotations.suite.CoreLibraryTest
import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.di.LocalhostApi
import ch.protonmail.android.uitest.di.LocalhostApiModule
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import me.proton.core.accountmanager.data.AccountStateHandler
import me.proton.core.accountrecovery.dagger.CoreAccountRecoveryFeaturesModule
import me.proton.core.accountrecovery.domain.IsAccountRecoveryEnabled
import me.proton.core.accountrecovery.domain.IsAccountRecoveryResetEnabled
import me.proton.core.accountrecovery.test.MinimalAccountRecoveryNotificationTest
import me.proton.core.auth.test.flow.SignInFlow
import me.proton.core.auth.test.robot.AddAccountRobot
import me.proton.core.auth.test.usecase.WaitForPrimaryAccount
import me.proton.core.domain.entity.UserId
import me.proton.core.eventmanager.domain.EventManagerProvider
import me.proton.core.eventmanager.domain.repository.EventMetadataRepository
import me.proton.core.network.data.ApiProvider
import me.proton.core.notification.dagger.CoreNotificationFeaturesModule
import me.proton.core.notification.domain.repository.NotificationRepository
import me.proton.core.notification.domain.usecase.IsNotificationsEnabled
import me.proton.core.test.android.instrumented.FusionConfig
import javax.inject.Inject

@CoreLibraryTest
@HiltAndroidTest
@UninstallModules(
    LocalhostApiModule::class,
    CoreAccountRecoveryFeaturesModule::class,
    CoreNotificationFeaturesModule::class
)
internal class AccountRecoveryFlowTest : BaseTest(), MinimalAccountRecoveryNotificationTest {
    @JvmField
    @BindValue
    @LocalhostApi
    val localhostApi = false

    @Inject
    override lateinit var accountStateHandler: AccountStateHandler

    @Inject
    override lateinit var apiProvider: ApiProvider

    @Inject
    override lateinit var eventManagerProvider: EventManagerProvider

    @Inject
    override lateinit var eventMetadataRepository: EventMetadataRepository

    @Inject
    override lateinit var notificationRepository: NotificationRepository

    @Inject
    override lateinit var waitForPrimaryAccount: WaitForPrimaryAccount

    @BindValue
    internal val isAccountRecoveryEnabled = object : IsAccountRecoveryEnabled {
        override fun invoke(userId: UserId?): Boolean = true
        override fun isLocalEnabled(): Boolean = true
        override fun isRemoteEnabled(userId: UserId?): Boolean = true
    }

    @BindValue
    internal val isAccountRecoveryResetEnabled = object : IsAccountRecoveryResetEnabled {
        override fun invoke(userId: UserId?): Boolean = true
        override fun isLocalEnabled(): Boolean = true
        override fun isRemoteEnabled(userId: UserId?): Boolean = true
    }

    @BindValue
    internal val isNotificationsEnabled = IsNotificationsEnabled { true }

    init {
        FusionConfig.Compose.testRule = composeTestRule
    }

    override fun setup() {
        super.setup()
        val user = users.getUser { it.name == "pro" }

        AddAccountRobot.clickSignIn()
        SignInFlow.signInInternal(user.name, user.password)
    }

    override fun verifyAfterLogin() {
        mailboxRobot { verify { isShown() } }
    }
}
