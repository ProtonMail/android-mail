package ch.protonmail.android.initializer.featureflag

import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.sample.AccountSample
import ch.protonmail.android.testdata.AccountTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test

class RefreshNPSFeedbackFeatureFlagsTest {

    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val scopeProvider = TestCoroutineScopeProvider(dispatcherProvider)

    private val accountManager = mockk<AccountManager>()
    private val featureFlagManager = mockk<FeatureFlagManager>()

    private val sut = RefreshNPSFeedbackFeatureFlags(
        accountManager,
        scopeProvider,
        featureFlagManager
    )

    @Test
    fun `should refresh rating booster feature flags for all users when the use case is called`() {
        // Given
        coEvery {
            accountManager.getAccounts()
        } returns flowOf(listOf(AccountTestData.readyAccount, AccountSample.Primary))
        coEvery {
            featureFlagManager.getOrDefault(
                userId = any(),
                featureId = MailFeatureId.NPSFeedback.id,
                default = FeatureFlag.default(MailFeatureId.NPSFeedback.id.id, false),
                refresh = true
            )
        } returns FeatureFlag.default(MailFeatureId.NPSFeedback.id.id, false)

        // When
        sut()

        // Then
        verifyFFRefreshedForUser(AccountTestData.readyAccount.userId)
        verifyFFRefreshedForUser(AccountSample.Primary.userId)
    }

    private fun verifyFFRefreshedForUser(userId: UserId) {
        coVerify {
            featureFlagManager.getOrDefault(
                userId = userId,
                featureId = MailFeatureId.NPSFeedback.id,
                default = FeatureFlag.default(MailFeatureId.NPSFeedback.id.id, false),
                refresh = true
            )
        }
    }
}
