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

package ch.protonmail.android.uitest

import android.Manifest
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.rule.GrantPermissionRule
import ch.protonmail.android.test.idlingresources.ComposeIdlingResource
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uitest.helpers.core.TestIdWatcher
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.login.LoginType
import ch.protonmail.android.uitest.helpers.network.authenticationDispatcher
import ch.protonmail.android.uitest.rule.GrantNotificationsPermissionRule
import ch.protonmail.android.uitest.rule.MainInitializerRule
import ch.protonmail.android.uitest.rule.MockIntentsRule
import ch.protonmail.android.uitest.rule.MockOnboardingRuntimeRule
import ch.protonmail.android.uitest.rule.MockTimeRule
import ch.protonmail.android.uitest.rule.SpotlightSeenRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.unmockkObject
import me.proton.core.network.domain.NetworkManager
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import javax.inject.Inject

/**
 * A base test class used in UI tests that require complete network isolation.
 *
 * @param captureIntents whether intents shall be captured (and mocked) for further verification.
 * @param loginType the login type to use for a given test suite.
 */
@HiltAndroidTest
internal open class MockedNetworkTest(
    captureIntents: Boolean = true,
    private val showOnboarding: Boolean = false,
    private val loginType: LoginType = LoginTestUserTypes.Deprecated.GrumpyCat
) {

    private val hiltAndroidRule = HiltAndroidRule(this)
    private val composeTestRule: ComposeTestRule = ComposeTestRuleHolder.createAndGetComposeRule()
    private val writeExtStoragePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Inject
    lateinit var mockWebServer: MockWebServer

    @Inject
    lateinit var idlingResources: Set<@JvmSuppressWildcards ComposeIdlingResource>

    @Inject
    lateinit var networkManager: NetworkManager

    @Inject
    lateinit var mockOnboardingRuntimeRule: MockOnboardingRuntimeRule

    @Inject
    lateinit var spotlightSeenRule: SpotlightSeenRule

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(
        hiltAndroidRule
    ).around(
        composeTestRule
    ).around(
        writeExtStoragePermissionRule
    ).around(
        GrantNotificationsPermissionRule()
    ).around(
        MockIntentsRule(captureIntents)
    ).around(
        MainInitializerRule()
    ).around(
        MockTimeRule()
    ).around(
        TestIdWatcher()
    )

    @Before
    fun setup() {
        hiltAndroidRule.inject()

        idlingResources.forEach { idlingResource ->
            idlingResource.clear()
            composeTestRule.registerIdlingResource(idlingResource)
        }

        mockWebServer.dispatcher = authenticationDispatcher(loginType)
        mockOnboardingRuntimeRule(showOnboarding)
        spotlightSeenRule.invoke(seen = true)
    }

    @After
    fun tearDown() {
        idlingResources.forEach { composeTestRule.unregisterIdlingResource(it) }
        unmockkObject(networkManager)
    }
}
