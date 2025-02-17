package ch.protonmail.android.mailsettings.data.repository

import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.SpotlightLastSeenPreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import kotlin.test.Test

class LocalSpotlightEventsRepositoryImplTest {

    private val preferences = mockk<Preferences>()
    private val spotlightDataStoreSpy = spyk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<MailSettingsDataStoreProvider> {
        every { this@mockk.spotlightDataStore } returns spotlightDataStoreSpy
    }
    private val repo = LocalSpotlightEventsRepositoryImpl(dataStoreProvider)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun test() {
        coEvery { preferences.get<Boolean>(any()) } returns null
        every { spotlightDataStoreSpy.data } returns flowOf(preferences)

        repo.observeCustomizeToolbar()
    }

    @Test
    fun `returns default when no preference is stored locally`() = runTest {
        // Given
        coEvery { preferences.get<Boolean>(any()) } returns null
        every { spotlightDataStoreSpy.data } returns flowOf(preferences)
        // When
        repo.observeCustomizeToolbar().test {
            // Then
            assertEquals(SpotlightLastSeenPreference(false).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns locally stored preference from data store when available`() = runTest {
        // Given
        coEvery { preferences[booleanPreferencesKey("customizeToolbarSeen")] } returns false
        every { spotlightDataStoreSpy.data } returns flowOf(preferences)
        // When
        repo.observeCustomizeToolbar().test {
            // Then
            assertEquals(SpotlightLastSeenPreference(seen = false).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return error when an exception is thrown while observing preference`() = runTest {
        // Given
        every { spotlightDataStoreSpy.data } returns flow { throw IOException() }

        // When
        repo.observeCustomizeToolbar().test {
            // Then
            assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return success when preference is saved`() = runTest {
        // Given
        coEvery { preferences[booleanPreferencesKey("customizeToolbarSeen")] } returns false
        every { spotlightDataStoreSpy.data } returns flowOf(preferences)

        // When
        val result = repo.markCustomizeToolbarSeen()

        // Then
        coVerify { spotlightDataStoreSpy.updateData(any()) }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should return failure when an exception is thrown while saving preference`() = runTest {
        // Given
        coEvery { spotlightDataStoreSpy.updateData(any()) } throws IOException()

        // When
        val result = repo.markCustomizeToolbarSeen()

        // Then
        assertEquals(PreferencesError.left(), result)
    }
}
