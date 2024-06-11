package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import kotlin.test.Test

class RecordRatingBoosterTriggeredTest {

    private val userId = UserIdTestData.userId

    private val featureFlagManager = mockk<FeatureFlagManager>(relaxUnitFun = true)

    private val recordRatingBoosterTriggered = RecordRatingBoosterTriggered(featureFlagManager)

    @Test
    fun `should reset the feature flag value to false when the use case is called`() = runTest {
        // Given
        val featureFlagOn = FeatureFlag.default(MailFeatureId.RatingBooster.id.id, true)
        val featureFlagOff = featureFlagOn.copy(value = false)
        coEvery {
            featureFlagManager.getOrDefault(
                userId,
                MailFeatureId.RatingBooster.id,
                FeatureFlag.default(MailFeatureId.RatingBooster.id.id, false)
            )
        } returns featureFlagOn

        // When
        recordRatingBoosterTriggered(userId)

        // Then
        coVerify { featureFlagManager.update(featureFlagOff) }
    }
}
