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
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.network.NetworkStatus
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
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
    private val connectivityManager = mockk<ConnectivityManager>(relaxUnitFun = true)
    private val networkCapabilities = mockk<NetworkCapabilities>()
    private val networkCallback = slot<ConnectivityManager.NetworkCallback>()

    @Before
    fun setUp() {
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

    @Test
    fun `networkStatus reads the system while nothing is registered`() {
        // Given - constructed while the app could not see a network
        setupDisconnectedNetwork()
        val networkManager = NetworkManagerImpl(context)
        assertEquals(NetworkStatus.Disconnected, networkManager.networkStatus)

        // When - the network becomes visible again without any callback firing
        setupConnectedNetwork(isMetered = true)

        // Then
        assertEquals(NetworkStatus.Metered, networkManager.networkStatus)
    }

    @Test
    fun `networkStatus answers from the tracked state once registered`() {
        // Given
        setupConnectedNetwork(isMetered = false)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)
        networkManager.register()
        assertEquals(NetworkStatus.Unmetered, networkManager.networkStatus)

        // When - the app is blocked, which only the callback can report
        blockNetworkAccess()
        networkCallback.captured.onBlockedStatusChanged(network, true)

        // Then - and the answer costs no binder call beyond the one register made
        assertEquals(NetworkStatus.Disconnected, networkManager.networkStatus)
        verify(exactly = 1) { connectivityManager.activeNetwork }
    }

    @Test
    fun `observe emits Disconnected when the app is blocked from the network`() = runTest {
        // Given
        setupConnectedNetwork(isMetered = true)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)

        networkManager.observe().test {
            assertEquals(NetworkStatus.Metered, awaitItem())
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)

            // When - Doze blocks access, so the platform stops reporting an active network
            blockNetworkAccess()
            networkCallback.captured.onBlockedStatusChanged(network, true)

            // Then
            assertEquals(NetworkStatus.Disconnected, awaitItem())
        }
    }

    @Test
    fun `observe emits when the app is unblocked and the system answers with null`() = runTest {
        // Given - blocked, which is where a long Doze window leaves the app
        setupConnectedNetwork(isMetered = true)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)

        networkManager.observe().test {
            assertEquals(NetworkStatus.Metered, awaitItem())
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)
            blockNetworkAccess()
            networkCallback.captured.onBlockedStatusChanged(network, true)
            assertEquals(NetworkStatus.Disconnected, awaitItem())

            // When - the block lifts. This is the only callback the system fires on that edge, and
            // the platform is entitled to answer a synchronous read made from inside it with null.
            networkCallback.captured.onBlockedStatusChanged(network, false)

            // Then - the network the system last described, not what the null read would say
            assertEquals(NetworkStatus.Metered, awaitItem())
        }
    }

    @Test
    fun `observe reports the capabilities seen during the block, not the ones from before it`() = runTest {
        // Given - an unmetered network the app is then blocked from
        setupConnectedNetwork(isMetered = false)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)

        networkManager.observe().test {
            assertEquals(NetworkStatus.Unmetered, awaitItem())
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)
            blockNetworkAccess()
            networkCallback.captured.onBlockedStatusChanged(network, true)
            assertEquals(NetworkStatus.Disconnected, awaitItem())

            // When - the default network turns metered while the app is still blocked
            every {
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            } returns false
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)
            networkCallback.captured.onBlockedStatusChanged(network, false)

            // Then
            assertEquals(NetworkStatus.Metered, awaitItem())
        }
    }

    @Test
    fun `observe reports an unmetered network as unmetered when its capabilities arrive while blocked`() = runTest {
        // Given - an unmetered network the app is then blocked from
        setupConnectedNetwork(isMetered = false)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)

        networkManager.observe().test {
            assertEquals(NetworkStatus.Unmetered, awaitItem())
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)
            blockNetworkAccess()
            networkCallback.captured.onBlockedStatusChanged(network, true)
            assertEquals(NetworkStatus.Disconnected, awaitItem())

            // When - the same unmetered network is described again while the block is on. There is
            // no active network for isActiveNetworkMetered to describe, so it answers `true`, and
            // corroborating the capabilities with it would turn the wifi network into a metered one.
            every { connectivityManager.isActiveNetworkMetered } returns true
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)
            networkCallback.captured.onBlockedStatusChanged(network, false)

            // Then
            assertEquals(NetworkStatus.Unmetered, awaitItem())
        }
    }

    @Test
    fun `observe reports Disconnected when the held network is lost and a read still answers`() = runTest {
        // Given - an unmetered default network
        setupConnectedNetwork(isMetered = false)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)

        networkManager.observe().test {
            assertEquals(NetworkStatus.Unmetered, awaitItem())
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)

            // When - airplane mode. A read made from inside the callback is entitled to answer with
            // the network being torn down, while isActiveNetworkMetered already has no active
            // network to describe and answers `true`. Together they read as a live metered network.
            every { connectivityManager.isActiveNetworkMetered } returns true
            networkCallback.captured.onLost(network)

            // Then - the network we held is gone, whatever that read produces
            assertEquals(NetworkStatus.Disconnected, awaitItem())
        }
    }

    @Test
    fun `observe keeps the new default network when the old one is reported lost after a handover`() = runTest {
        // Given - an unmetered default network
        setupConnectedNetwork(isMetered = false)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)
        val cellular = mockk<Network>()

        networkManager.observe().test {
            assertEquals(NetworkStatus.Unmetered, awaitItem())
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)

            // The handover: the new default is described first, the old one reported lost after
            every {
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            } returns false
            every { connectivityManager.activeNetwork } returns cellular
            every { connectivityManager.getNetworkCapabilities(cellular) } returns networkCapabilities
            networkCallback.captured.onCapabilitiesChanged(cellular, networkCapabilities)
            assertEquals(NetworkStatus.Metered, awaitItem())
            networkCallback.captured.onLost(network)

            // When - the app is blocked and then unblocked
            blockNetworkAccess()
            networkCallback.captured.onBlockedStatusChanged(cellular, true)
            assertEquals(NetworkStatus.Disconnected, awaitItem())
            networkCallback.captured.onBlockedStatusChanged(cellular, false)

            // Then - the network that is actually there, not the one the stale loss would report
            assertEquals(NetworkStatus.Metered, awaitItem())
        }
    }

    @Test
    fun `observe publishes the new default network when the app was blocked on the old one`() = runTest {
        // Given - blocked on the wifi network that is about to be switched off
        setupConnectedNetwork(isMetered = false)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)
        val cellular = mockk<Network>()

        networkManager.observe().test {
            assertEquals(NetworkStatus.Unmetered, awaitItem())
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)
            networkCallback.captured.onBlockedStatusChanged(network, true)
            assertEquals(NetworkStatus.Disconnected, awaitItem())

            // When - cellular takes over. The app is not blocked on it, and the system does not
            // have to repeat the blocked status to say so. A read from inside the callback can
            // still answer with nothing, because it answers for the network the app may use.
            blockNetworkAccess()
            every {
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            } returns false
            networkCallback.captured.onAvailable(cellular)
            networkCallback.captured.onCapabilitiesChanged(cellular, networkCapabilities)

            // Then - the block belonged to the network that went away
            assertEquals(NetworkStatus.Metered, awaitItem())
        }
    }

    @Test
    fun `observe does not emit when the block is lifted and nothing changed`() = runTest {
        // Given
        setupConnectedNetwork(isMetered = false)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)

        networkManager.observe().test {
            assertEquals(NetworkStatus.Unmetered, awaitItem())
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)

            // When
            networkCallback.captured.onBlockedStatusChanged(network, false)

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `refresh emits the current status even when it did not change`() = runTest {
        // Given
        setupConnectedNetwork(isMetered = false)
        every { connectivityManager.registerDefaultNetworkCallback(any()) } just Runs
        val networkManager = NetworkManagerImpl(context)

        networkManager.observe().test {
            assertEquals(NetworkStatus.Unmetered, awaitItem())

            // When
            networkManager.refresh()

            // Then
            assertEquals(NetworkStatus.Unmetered, awaitItem())
        }
    }

    @Test
    fun `refresh emits the status the callbacks missed`() = runTest {
        // Given
        setupDisconnectedNetwork()
        every { connectivityManager.registerDefaultNetworkCallback(any()) } just Runs
        val networkManager = NetworkManagerImpl(context)

        networkManager.observe().test {
            assertEquals(NetworkStatus.Disconnected, awaitItem())

            // When - the network came back without a callback reaching the app
            setupConnectedNetwork(isMetered = true)
            networkManager.refresh()

            // Then
            assertEquals(NetworkStatus.Metered, awaitItem())
        }
    }

    @Test
    fun `refresh clears a block the app was never told had lifted`() = runTest {
        // Given - blocked, and the unblock callback never arrives
        setupConnectedNetwork(isMetered = true)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)

        networkManager.observe().test {
            assertEquals(NetworkStatus.Metered, awaitItem())
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)
            blockNetworkAccess()
            networkCallback.captured.onBlockedStatusChanged(network, true)
            assertEquals(NetworkStatus.Disconnected, awaitItem())

            // When - the app is opened and the read sees a network it may use again
            setupConnectedNetwork(isMetered = true)
            networkManager.refresh()

            // Then
            assertEquals(NetworkStatus.Metered, awaitItem())
        }
    }

    @Test
    fun `refresh keeps the blocked network for the unblock edge`() = runTest {
        // Given - blocked on a metered network
        setupConnectedNetwork(isMetered = true)
        every { connectivityManager.registerDefaultNetworkCallback(capture(networkCallback)) } just Runs
        val networkManager = NetworkManagerImpl(context)

        networkManager.observe().test {
            assertEquals(NetworkStatus.Metered, awaitItem())
            networkCallback.captured.onCapabilitiesChanged(network, networkCapabilities)
            blockNetworkAccess()
            networkCallback.captured.onBlockedStatusChanged(network, true)
            assertEquals(NetworkStatus.Disconnected, awaitItem())

            // When - a refresh runs while the block is still on. The read cannot tell that from a
            // device with no network, so it may only decide what is published now.
            networkManager.refresh()
            assertEquals(NetworkStatus.Disconnected, awaitItem())
            networkCallback.captured.onBlockedStatusChanged(network, false)

            // Then - the network the app was blocked from, not the nothing the read saw
            assertEquals(NetworkStatus.Metered, awaitItem())
        }
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

    // While the app's network access is blocked, the platform reports no active network for it.
    private fun blockNetworkAccess() {
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(null) } returns null
    }

    private fun setupNetworkWithNullCapabilities() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null
    }
}
