package ch.protonmail.android.mailupselling.domain.repository

import java.time.Instant
import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEvent
import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEventDimensions
import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEventType
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsPostSubscriptionTelemetryEnabled
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryPriority
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostSubscriptionTelemetryRepositoryImplTest {

    private val getPrimaryUser = mockk<GetPrimaryUser>()
    private val isPostSubscriptionTelemetryEnabled = mockk<IsPostSubscriptionTelemetryEnabled>()
    private val telemetryManager = mockk<TelemetryManager>()
    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val scopeProvider = TestCoroutineScopeProvider(dispatcherProvider)

    private val postSubscriptionTelemetryRepository = PostSubscriptionTelemetryRepositoryImpl(
        getPrimaryUser,
        isPostSubscriptionTelemetryEnabled,
        telemetryManager,
        scopeProvider
    )

    @BeforeTest
    fun setup() {
        mockInstant()
    }

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
    fun `should not track event when FF is turned off`() {
        // Given
        val user = UserTestData.Primary
        coEvery { getPrimaryUser() } returns user
        every { isPostSubscriptionTelemetryEnabled(user.userId) } returns false

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
        every { isPostSubscriptionTelemetryEnabled(user.userId) } returns true

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
        every { isPostSubscriptionTelemetryEnabled(user.userId) } returns true

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

    private fun mockInstant() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns mockk { every { epochSecond } returns 0 }
    }
}
