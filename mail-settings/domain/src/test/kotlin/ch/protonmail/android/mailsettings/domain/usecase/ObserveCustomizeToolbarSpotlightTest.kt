package ch.protonmail.android.mailsettings.domain.usecase

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.SpotlightLastSeenPreference
import ch.protonmail.android.mailsettings.domain.repository.LocalSpotlightEventsRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import org.junit.After
import kotlin.test.BeforeTest
import kotlin.test.Test

class ObserveCustomizeToolbarSpotlightTest {

    private val repo = mockk<LocalSpotlightEventsRepository>()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserId("first"))
    }
    private val useCase by lazy {
        ObserveCustomizeToolbarSpotlight(repo, observePrimaryUserId)
    }

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `emits an item if preference fails`() = runTest {
        every { repo.observeCustomizeToolbar() } returns flowOf(PreferencesError.left())

        useCase.invoke().test {
            awaitItem()
            awaitComplete()
        }
    }

    @Test
    fun `emits an item if preference seen zero times`() = runTest {
        every { repo.observeCustomizeToolbar() } returns
            flowOf(SpotlightLastSeenPreference(seen = false).right())

        useCase.invoke().test {
            awaitItem()
            awaitComplete()
        }
    }

    @Test
    fun `emits an item for each user emission if preference seen zero times `() = runTest {
        every { observePrimaryUserId.invoke() } returns flow {
            emit(UserId("first"))
            delay(4000)
            emit(UserId("second"))
        }
        every { repo.observeCustomizeToolbar() } returns
            flowOf(SpotlightLastSeenPreference(seen = false).right())

        useCase.invoke().test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }
    }

    @Test
    fun `does not emit if preference returns non-zero times seen`() = runTest {
        every { repo.observeCustomizeToolbar() } returns
            flowOf(SpotlightLastSeenPreference(seen = true).right())

        useCase.invoke().test {
            awaitComplete()
        }
    }
}
