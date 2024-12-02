package ch.protonmail.android.mailupselling.presentation.viewmodel.postsubscription

import android.content.Context
import android.content.pm.PackageManager
import app.cash.turbine.test
import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEventType
import ch.protonmail.android.mailupselling.domain.repository.PostSubscriptionTelemetryRepository
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.postsubscription.AppUiModel
import ch.protonmail.android.mailupselling.presentation.model.postsubscription.PostSubscriptionOperation
import ch.protonmail.android.mailupselling.presentation.model.postsubscription.PostSubscriptionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PostSubscriptionViewModelTest {

    private val mockPackageManager = mockk<PackageManager>()
    private val mockContext = mockk<Context> {
        every { packageManager } returns mockPackageManager
    }
    private val postSubscriptionTelemetryRepository = mockk<PostSubscriptionTelemetryRepository>(relaxUnitFun = true)

    private val postSubscriptionViewModel by lazy {
        PostSubscriptionViewModel(mockContext, postSubscriptionTelemetryRepository)
    }

    @Test
    fun `emit state with a sorted list of apps`() = runTest {
        // Given
        every {
            mockPackageManager.getPackageInfo(PACKAGE_NAME_CALENDAR, PackageManager.GET_ACTIVITIES)
        } returns mockk()
        every {
            mockPackageManager.getPackageInfo(PACKAGE_NAME_DRIVE, PackageManager.GET_ACTIVITIES)
        } throws PackageManager.NameNotFoundException()
        every {
            mockPackageManager.getPackageInfo(PACKAGE_NAME_VPN, PackageManager.GET_ACTIVITIES)
        } returns mockk()
        every {
            mockPackageManager.getPackageInfo(PACKAGE_NAME_PASS, PackageManager.GET_ACTIVITIES)
        } throws PackageManager.NameNotFoundException()

        val expected = PostSubscriptionState.Data(
            apps = listOf(
                AppUiModel(
                    packageName = PACKAGE_NAME_DRIVE,
                    logo = R.drawable.ic_logo_drive,
                    name = R.string.post_subscription_proton_drive,
                    message = R.string.post_subscription_proton_drive_message,
                    isInstalled = false
                ),
                AppUiModel(
                    packageName = PACKAGE_NAME_PASS,
                    logo = R.drawable.ic_logo_pass,
                    name = R.string.post_subscription_proton_pass,
                    message = R.string.post_subscription_proton_pass_message,
                    isInstalled = false
                ),
                AppUiModel(
                    packageName = PACKAGE_NAME_CALENDAR,
                    logo = R.drawable.ic_logo_calendar,
                    name = R.string.post_subscription_proton_calendar,
                    message = R.string.post_subscription_proton_calendar_message,
                    isInstalled = true
                ),
                AppUiModel(
                    packageName = PACKAGE_NAME_VPN,
                    logo = R.drawable.ic_logo_vpn,
                    name = R.string.post_subscription_proton_vpn,
                    message = R.string.post_subscription_proton_vpn_message,
                    isInstalled = true
                )
            ).toImmutableList()
        )

        // When
        postSubscriptionViewModel.state.test {

            // Then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `should handle track telemetry event when action is submitted`() {
        // Given
        every {
            mockPackageManager.getPackageInfo(PACKAGE_NAME_CALENDAR, PackageManager.GET_ACTIVITIES)
        } returns mockk()
        every {
            mockPackageManager.getPackageInfo(PACKAGE_NAME_DRIVE, PackageManager.GET_ACTIVITIES)
        } throws mockk()
        every {
            mockPackageManager.getPackageInfo(PACKAGE_NAME_VPN, PackageManager.GET_ACTIVITIES)
        } returns mockk()
        every {
            mockPackageManager.getPackageInfo(PACKAGE_NAME_PASS, PackageManager.GET_ACTIVITIES)
        } throws mockk()

        // When
        postSubscriptionViewModel.submit(
            PostSubscriptionOperation.TrackTelemetryEvent(
                PostSubscriptionTelemetryEventType.LastStepDisplayed
            )
        )

        // Then
        verify {
            postSubscriptionTelemetryRepository.trackEvent(
                PostSubscriptionTelemetryEventType.LastStepDisplayed
            )
        }
    }
}
