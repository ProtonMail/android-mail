/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */


package ch.protonmail.android.mailcommon.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import ch.protonmail.android.mailcommon.domain.network.NetworkStatus
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.Q)
internal class NetworkManagerImplTest {

    private val context = mockk<Context>()
    private val network: Network = mockk<Network>()
    private val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
    private val networkCapabilities = mockk<NetworkCapabilities>(relaxed = true)
    private val networkCallback = slot<ConnectivityManager.NetworkCallback>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `networkStatus returns Disconnected when activeNetwork is null`() {
        // Given
        setupDisconnectedNetwork()

        // When
        val result = NetworkManagerImpl(context).networkStatus

        // Then
        assertEquals(NetworkStatus.Disconnected, result)
    }

    @Test
    fun `networkStatus returns Disconnected when networkCapabilities is null`() {
        // Given
        setupNetworkWithNullCapabilities()

        // When
        val result = NetworkManagerImpl(context).networkStatus

        // Then
        assertEquals(NetworkStatus.Disconnected, result)
    }

    @Test
    fun `networkStatus returns Disconnected when no internet capability`() {
        // Given
        setupConnectedNetwork(hasInternet = false)

        // When
        val result = NetworkManagerImpl(context).networkStatus

        // Then
        assertEquals(NetworkStatus.Disconnected, result)
    }

    @Test
    fun `networkStatus returns Unmetered when connected and not metered`() {
        // Given
        setupConnectedNetwork(isMetered = false)

        // When
        val result = NetworkManagerImpl(context).networkStatus

        // Then
        assertEquals(NetworkStatus.Unmetered, result)
    }

    @Test
    fun `networkStatus returns Metered when connected and metered`() {
        // Given
        setupConnectedNetwork(isMetered = true)

        // When
        val result = NetworkManagerImpl(context).networkStatus

        // Then
        assertEquals(NetworkStatus.Metered, result)
    }

    @Test
    fun `register calls registerDefaultNetworkCallback`() {
        // Given
        setupConnectedNetwork()
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs

        // When
        NetworkManagerImpl(context).register()

        // Then
        verify { connectivityManager.registerDefaultNetworkCallback(any()) }
    }

    @Test
    fun `register only registers once when called multiple times`() {
        // Given
        setupConnectedNetwork()
        every { connectivityManager.registerDefaultNetworkCallback(any()) } just Runs

        // When
        val networkManager = NetworkManagerImpl(context)
        networkManager.register()
        networkManager.register()

        // Then
        verify(exactly = 1) { connectivityManager.registerDefaultNetworkCallback(any()) }
    }

    @Test
    fun `unregister calls unregisterNetworkCallback when registered`() {
        // Given
        setupConnectedNetwork()
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs

        // When
        val networkManager = NetworkManagerImpl(context)
        networkManager.register()
        networkManager.unregister()

        // Then
        verify { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) }
    }

    @Test
    fun `unregister does nothing when not registered`() {
        // Given
        setupConnectedNetwork()

        // When
        val networkManager = NetworkManagerImpl(context)
        networkManager.unregister()

        // Then
        verify(exactly = 0) {
            connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>())
        }
    }

    @Test
    fun `unregister only unregisters once when called multiple times`() {
        // Given
        setupConnectedNetwork()
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs

        // When
        val networkManager = NetworkManagerImpl(context)
        networkManager.register()
        networkManager.unregister()
        networkManager.unregister()

        // Then
        verify(exactly = 1) {
            connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>())
        }
    }

    @Test
    fun `isMetered is true when there is no active network`() {
        // Given
        setupDisconnectedNetwork()

        // When
        val result = NetworkManagerImpl(context).isMetered

        // Then
        assertTrue(result)
    }

    @Test
    fun `isMetered is true when the capabilities cannot be read`() {
        // Given
        setupNetworkWithNullCapabilities()

        // When
        val result = NetworkManagerImpl(context).isMetered

        // Then
        assertTrue(result)
    }

    @Test
    fun `isMetered is true when the transport is metered`() {
        // Given
        setupConnectedNetwork(isMetered = true)

        // When
        val result = NetworkManagerImpl(context).isMetered

        // Then
        assertTrue(result)
    }

    @Test
    fun `isMetered is false on an unmetered transport`() {
        // Given
        setupConnectedNetwork(isMetered = false)

        // When
        val result = NetworkManagerImpl(context).isMetered

        // Then
        assertFalse(result)
    }

    @Test
    fun `isMetered is true when the legacy connectivity answer disagrees with the capability`() {
        // Given - the capability says unmetered, the legacy read says otherwise: fail safe. This is
        // the VPN case too: the tunnel advertises NOT_METERED while what it runs over costs money.
        setupConnectedNetwork()
        every { connectivityManager.isActiveNetworkMetered } returns true

        // When
        val result = NetworkManagerImpl(context).isMetered

        // Then
        assertTrue(result)
    }

    private fun setupConnectedNetwork(hasInternet: Boolean = true, isMetered: Boolean = false) {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns hasInternet
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns !isMetered
        every { connectivityManager.isActiveNetworkMetered } returns isMetered
    }

    private fun setupDisconnectedNetwork() {
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(null) } returns null
    }

    private fun setupNetworkWithNullCapabilities() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null
    }
}
