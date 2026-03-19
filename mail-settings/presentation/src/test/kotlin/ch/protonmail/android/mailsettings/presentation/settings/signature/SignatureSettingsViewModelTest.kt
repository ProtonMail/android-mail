/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.settings.signature

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.MobileSignaturePreference
import ch.protonmail.android.mailsettings.domain.model.MobileSignatureStatus
import ch.protonmail.android.mailsettings.domain.repository.MobileSignatureRepository
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.signature.model.MobileSignatureMenuState
import ch.protonmail.android.mailsettings.presentation.settings.signature.model.MobileSignatureUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SignatureSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = UserId("user-123")
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val upsellingFlow = MutableSharedFlow<UpsellingVisibility>()
    private val observeUpsellingVisibility = mockk<ObserveUpsellingVisibility> {
        every {
            this@mockk.invoke(entryPoint = UpsellingEntryPoint.Feature.MobileSignature)
        } returns upsellingFlow
    }

    private val signatureFlow = MutableSharedFlow<MobileSignaturePreference>()


    private val mobileSignatureRepository = mockk<MobileSignatureRepository>(relaxed = true).apply {
        every { observeMobileSignature(userId) } returns signatureFlow
    }
    private lateinit var viewModel: SignatureSettingsMenuViewModel

    private val signatureSettings =
        MobileSignaturePreference(value = "signature value", status = MobileSignatureStatus.Enabled)

    private val expectedSignatureSettings =
        MobileSignatureMenuState.Data(
            MobileSignatureUiModel(
                signatureStatus = MobileSignatureStatus.Enabled,
                signatureValue = "signature value",
                statusText = TextUiModel.TextRes(
                    R.string.mail_settings_app_customization_mobile_signature_on
                )
            ),
            upsellingVisibility = UpsellingVisibility.Normal.MailPlus
        )

    @Before
    fun setUp() {

        viewModel = SignatureSettingsMenuViewModel(
            observePrimaryUserId,
            mobileSignatureRepository,
            observeUpsellingVisibility
        )
    }

    @Test
    fun `emits loading state when initialised`() = runTest {
        viewModel.state.test {
            Assert.assertEquals(MobileSignatureMenuState.Loading, awaitItem())
        }
    }

    @Test
    fun `state has signature preference disabled when get app settings use case returns valid data`() = runTest {
        viewModel.state.test {
            // Given
            awaitItem()

            // When
            signatureFlow.emit(signatureSettings)
            upsellingFlow.emit(UpsellingVisibility.Normal.MailPlus)

            // Then
            val actual = awaitItem() as MobileSignatureMenuState.Data
            val expected = expectedSignatureSettings
            Assert.assertEquals(expected, actual)
        }
    }

    @Test
    fun `state has signature preference when get app settings use case returns valid data`() = runTest {
        viewModel.state.test {
            // Given
            awaitItem()

            // When
            signatureFlow.emit(
                signatureSettings.copy(
                    status = MobileSignatureStatus.Disabled
                )
            )
            upsellingFlow.emit(UpsellingVisibility.Normal.MailPlus)

            // Then
            val actual = awaitItem() as MobileSignatureMenuState.Data
            val expected = expectedSignatureSettings.copy(
                settings = expectedSignatureSettings.run {
                    settings.copy(
                        signatureStatus = MobileSignatureStatus.Disabled,
                        statusText = TextUiModel.TextRes(
                            R.string.mail_settings_app_customization_mobile_signature_off
                        )
                    )
                }
            )
            Assert.assertEquals(expected, actual)
        }
    }

    @Test
    fun `state has signature preference AND upselling hidden when get app settings use case returns valid data`() =
        runTest {
            viewModel.state.test {
                // Given
                awaitItem()

                // When
                signatureFlow.emit(signatureSettings)
                upsellingFlow.emit(UpsellingVisibility.Hidden)

                // Then
                val actual = awaitItem() as MobileSignatureMenuState.Data
                val expected = expectedSignatureSettings.copy(
                    settings = expectedSignatureSettings.settings,
                    upsellingVisibility = UpsellingVisibility.Hidden
                )
                Assert.assertEquals(expected, actual)
            }
        }
}
