package ch.protonmail.android.mailupselling.domain.repository

import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEvent
import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEventDimensions
import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEventType
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryPriority
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test

class PostSubscriptionTelemetryRepositoryImplTest {

    private val getPrimaryUser = mockk<GetPrimaryUser>()
    private val telemetryManager = mockk<TelemetryManager>()
    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val scopeProvider = TestCoroutineScopeProvider(dispatcherProvider)

    private val postSubscriptionTelemetryRepository = PostSubscriptionTelemetryRepositoryImpl(
        getPrimaryUser,
        telemetryManager,
        scopeProvider
    )

    @Test
    fun `should not track events when primary user is not found`() {
        // Given
        coEvery { getPrimaryUser() } returns null

        // When
        postSubscriptionTelemetryRepository.trackEvent(PostSubscriptionTelemetryEventType.LastStepDisplayed)

        // Then
        verify(exactly = 0) { telemetryManager.enqueue(any(), any(), any()) }
    }

    @Test
    fun `should track last step displayed event`() {
        // Given
        val user = UserTestData.Primary
        coEvery { getPrimaryUser() } returns user

        // When
        postSubscriptionTelemetryRepository.trackEvent(PostSubscriptionTelemetryEventType.LastStepDisplayed)

        // Then
        val event = PostSubscriptionTelemetryEvent.LastStepDisplayed(
            PostSubscriptionTelemetryEventDimensions()
        ).toTelemetryEvent()
        verify { telemetryManager.enqueue(user.userId, event, TelemetryPriority.Immediate) }
    }

    @Test
    fun `should track download app event`() {
        // Given
        val user = UserTestData.Primary
        coEvery { getPrimaryUser() } returns user

        // When
        postSubscriptionTelemetryRepository.trackEvent(
            PostSubscriptionTelemetryEventType.DownloadApp("me.proton.android.calendar")
        )

        // Then
        val dimensions = PostSubscriptionTelemetryEventDimensions()
        dimensions.addSelectedApp("android_calendar")
        val event = PostSubscriptionTelemetryEvent.DownloadApp(dimensions).toTelemetryEvent()
        verify { telemetryManager.enqueue(user.userId, event, TelemetryPriority.Immediate) }
    }
}
