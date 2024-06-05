package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import android.app.Activity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test

class ShowRatingBoosterTest {

    private val reviewManager = mockk<ReviewManager>()

    private val showRatingBooster = ShowRatingBooster(reviewManager)

    @Test
    fun `should show the rating booster if the request was successful`() {
        // Given
        val context = mockk<Activity>()
        val reviewInfo = mockk<ReviewInfo>()
        val reviewInfoTask = mockk<Task<ReviewInfo>> {
            every { isSuccessful } returns true
            every { result } returns reviewInfo
        }
        val onCompleteListenerSlot = slot<OnCompleteListener<ReviewInfo>>()
        every { reviewManager.requestReviewFlow() } returns mockk {
            every {
                addOnCompleteListener(capture(onCompleteListenerSlot))
            } answers {
                onCompleteListenerSlot.captured.onComplete(reviewInfoTask)
                reviewInfoTask
            }
        }
        every { reviewManager.launchReviewFlow(any(), reviewInfo) } returns mockk()

        // When
        showRatingBooster(context)

        // Then
        verify { reviewManager.launchReviewFlow(any(), reviewInfo) }
    }

    @Test
    fun `should not show the rating booster if the request was not successful`() {
        // Given
        val context = mockk<Activity>()
        val reviewInfoTask = mockk<Task<ReviewInfo>> {
            every { isSuccessful } returns false
            every { exception } returns Exception()
        }
        val onCompleteListenerSlot = slot<OnCompleteListener<ReviewInfo>>()
        every { reviewManager.requestReviewFlow() } returns mockk {
            every {
                addOnCompleteListener(capture(onCompleteListenerSlot))
            } answers {
                onCompleteListenerSlot.captured.onComplete(reviewInfoTask)
                reviewInfoTask
            }
        }

        // When
        showRatingBooster(context)

        // Then
        verify(exactly = 0) { reviewManager.launchReviewFlow(any(), any()) }
    }
}
