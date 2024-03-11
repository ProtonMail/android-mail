package ch.protonmail.android.mailmailbox.presentation.upselling

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanState
import me.proton.core.plan.domain.entity.DynamicPlans
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpsellingButtonViewModelTest {

    private val userId = UserIdSample.Primary

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val getDynamicPlansAdjustedPrices = mockk<GetDynamicPlansAdjustedPrices>()

    private val upsellingButtonViewModel by lazy {
        UpsellingButtonViewModel(
            observePrimaryUserId,
            getDynamicPlansAdjustedPrices
        )
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when plans are available then show upselling button`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(UserIdSample.Primary)
        coEvery { getDynamicPlansAdjustedPrices(userId) } returns DynamicPlans(
            defaultCycle = 1,
            plans = listOf(
                DynamicPlan(
                    name = "plus",
                    order = 1,
                    state = DynamicPlanState.Available,
                    title = "Plus",
                    type = null
                )
            )
        )

        // When
        upsellingButtonViewModel.state.test {
            // Then
            assertTrue { awaitItem().isShown }
        }
    }

    @Test
    fun `when plans are not available then hide upselling button`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(UserIdSample.Primary)
        coEvery { getDynamicPlansAdjustedPrices(userId) } returns DynamicPlans(
            defaultCycle = 1,
            plans = emptyList()
        )

        // When
        upsellingButtonViewModel.state.test {
            // Then
            assertFalse { awaitItem().isShown }
        }
    }

}
