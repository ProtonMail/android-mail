package ch.protonmail.android.mailmailbox.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailmailbox.domain.repository.InMemoryMailboxRepository
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.featureflag.domain.entity.Scope
import kotlin.test.Test
import kotlin.test.assertEquals

class ShouldShowRatingBoosterTest {

    private val userId = UserIdTestData.userId

    private val inMemoryMailboxRepository = mockk<InMemoryMailboxRepository>()
    private val observeMailFeature = mockk<ObserveMailFeature>()

    private val shouldShowRatingBooster = ShouldShowRatingBooster(inMemoryMailboxRepository, observeMailFeature)

    @Test
    fun `should emit true when the feature flag is true and mailbox has been viewed at least 2 times`() = runTest {
        // Given
        mockObserveScreenViewCount(viewCount = 2)
        mockObserveRatingBoosterMailFeature(value = true)

        // When
        shouldShowRatingBooster(userId).test {
            // Then
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should emit false when the feature flag is false and mailbox has been viewed at least 2 times`() = runTest {
        // Given
        mockObserveScreenViewCount(viewCount = 2)
        mockObserveRatingBoosterMailFeature(value = false)

        // When
        shouldShowRatingBooster(userId).test {
            // Then
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should emit false when the feature flag is true and mailbox has been viewed only once`() = runTest {
        // Given
        mockObserveScreenViewCount(viewCount = 1)
        mockObserveRatingBoosterMailFeature(value = true)

        // When
        shouldShowRatingBooster(userId).test {
            // Then
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should emit false when the feature flag is false and mailbox has been viewed only once`() = runTest {
        // Given
        mockObserveScreenViewCount(viewCount = 1)
        mockObserveRatingBoosterMailFeature(value = false)

        // When
        shouldShowRatingBooster(userId).test {
            // Then
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    private fun mockObserveScreenViewCount(viewCount: Int) {
        every { inMemoryMailboxRepository.observeScreenViewCount() } returns flowOf(viewCount)
    }

    private fun mockObserveRatingBoosterMailFeature(value: Boolean) {
        every {
            observeMailFeature(userId, MailFeatureId.RatingBooster)
        } returns flowOf(
            FeatureFlag(userId, MailFeatureId.RatingBooster.id, Scope.Unknown, defaultValue = false, value = value)
        )
    }
}
