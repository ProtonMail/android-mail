package ch.protonmail.android.mailsidebar.presentation.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveFreeUserClickUpsellingVisibility
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.presentation.compose.usecase.ShouldUpgradeStorage
import org.junit.After
import javax.inject.Provider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveSidebarUpsellingVisibilityTest {

    private val shouldUpgradeStorage = mockk<ShouldUpgradeStorage>()
    private val upsellingVisibility = mockk<ObserveFreeUserClickUpsellingVisibility>()

    private val provideIsSidebarUpsellingEnabled = mockk<Provider<Boolean>>()

    private val usecase by lazy {
        ObserveSidebarUpsellingVisibility(
            shouldUpgradeStorage,
            upsellingVisibility,
            provideIsSidebarUpsellingEnabled.get()
        )
    }

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        featureFlagEnabled(true)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }


    @Test
    fun `does not emit if upgrade storage emits storage warning`() = runTest {
        // Given
        every { shouldUpgradeStorage.invoke() } returns
            flowOf(ShouldUpgradeStorage.Result.MailStorageUpgrade(85, UserId("id")))

        // When
        usecase.invoke().test {
            // Then
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `does not emit if upselling emits false`() = runTest {
        // Given
        every { shouldUpgradeStorage.invoke() } returns
            flowOf(ShouldUpgradeStorage.Result.NoUpgrade)
        every { upsellingVisibility.invoke() } returns flowOf(false)

        // When
        usecase.invoke().test {
            // Then
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `does not emit if FF disabled`() = runTest {
        // Given
        featureFlagEnabled(false)

        // When
        usecase.invoke().test {
            // Then
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits if upselling emits true`() = runTest {
        // Given
        every { shouldUpgradeStorage.invoke() } returns
            flowOf(ShouldUpgradeStorage.Result.NoUpgrade)
        every { upsellingVisibility.invoke() } returns flowOf(true)

        // When
        usecase.invoke().test {
            // Then
            assertFalse(awaitItem())
            assertTrue(awaitItem())
            awaitComplete()
        }
    }

    private fun featureFlagEnabled(value: Boolean) {
        every {
            provideIsSidebarUpsellingEnabled.get()
        } returns value
    }
}
