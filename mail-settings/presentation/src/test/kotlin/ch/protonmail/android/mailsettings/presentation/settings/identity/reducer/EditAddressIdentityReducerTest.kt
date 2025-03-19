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

package ch.protonmail.android.mailsettings.presentation.settings.identity.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.mapper.EditAddressIdentityMapper
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.AddressSignatureUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.DisplayNameUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityEvent
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityOperation
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityState
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityViewAction
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.MobileFooterUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.reducer.EditAddressIdentityReducer
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class EditAddressIdentityReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val editAddressIdentityMapper = EditAddressIdentityMapper()
    private val editAddressIdentityReducer = EditAddressIdentityReducer(editAddressIdentityMapper)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = editAddressIdentityReducer.newStateFrom(currentState, event)
        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val baseDisplayName = DisplayName("display-name")
        private val baseSignature = Signature(enabled = true, SignatureValue("signature"))
        private val baseFooter = MobileFooter.FreeUserMobileFooter("mobile-footer")

        private val baseDisplayNameState = EditAddressIdentityState.DisplayNameState(
            DisplayNameUiModel("display-name")
        )
        private val baseSignatureState = EditAddressIdentityState.SignatureState(
            AddressSignatureUiModel("signature", enabled = true)
        )
        private val baseMobileFooterState = EditAddressIdentityState.MobileFooterState(
            MobileFooterUiModel(
                "mobile-footer",
                enabled = true,
                isFieldEnabled = false,
                isToggleEnabled = false,
                isUpsellingVisible = false
            )
        )
        private val baseError = Effect.empty<Unit>()
        private val baseClose = Effect.empty<Unit>()
        private val BaseUpsellingVisibility = Effect.empty<BottomSheetVisibilityEffect>()
        private val BaseUpsellingInProgress = Effect.empty<TextUiModel>()
        private val baseLoadedState = EditAddressIdentityState.DataLoaded(
            baseDisplayNameState,
            baseSignatureState,
            baseMobileFooterState,
            baseError,
            baseClose,
            BaseUpsellingVisibility,
            BaseUpsellingInProgress
        )

        private val transitionFromLoadingState = listOf(
            TestInput(
                currentState = EditAddressIdentityState.Loading,
                event = EditAddressIdentityEvent.Data.ContentLoaded(baseDisplayName, baseSignature, baseFooter),
                expectedState = baseLoadedState
            ),
            TestInput(
                currentState = EditAddressIdentityState.Loading,
                event = EditAddressIdentityEvent.Error.LoadingError,
                expectedState = EditAddressIdentityState.LoadingError
            )
        )

        private val transitionsFromLoadedState = listOf(
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityEvent.Error.UpdateError,
                expectedState = baseLoadedState.copy(
                    updateError = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityViewAction.DisplayName.UpdateValue("display-name-2"),
                expectedState = baseLoadedState.copy(
                    baseDisplayNameState.copy(DisplayNameUiModel("display-name-2"))
                )
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityViewAction.Signature.UpdateValue("signature-2"),
                expectedState = baseLoadedState.copy(
                    signatureState = baseSignatureState.copy(AddressSignatureUiModel("signature-2", enabled = true))
                )
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityViewAction.Signature.ToggleState(false),
                expectedState = baseLoadedState.copy(
                    signatureState = baseSignatureState.copy(AddressSignatureUiModel("signature", enabled = false))
                )
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityViewAction.MobileFooter.UpdateValue("mobile-footer-2"),
                expectedState = baseLoadedState.copy(
                    mobileFooterState = baseMobileFooterState.copy(
                        MobileFooterUiModel(
                            "mobile-footer-2",
                            enabled = true,
                            isFieldEnabled = false,
                            isToggleEnabled = false,
                            isUpsellingVisible = false
                        )
                    )
                )
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityViewAction.MobileFooter.ToggleState(false),
                expectedState = baseLoadedState.copy(
                    mobileFooterState = baseMobileFooterState.copy(
                        MobileFooterUiModel(
                            "mobile-footer",
                            enabled = false,
                            isFieldEnabled = false,
                            isToggleEnabled = false,
                            isUpsellingVisible = false
                        )
                    )
                )
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityEvent.UpgradeStateChanged(
                    mobileFooter = baseFooter,
                    shouldShowUpselling = true,
                    userUpgradeCheckState = UserUpgradeState.UserUpgradeCheckState.Initial
                ),
                expectedState = baseLoadedState
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityEvent.UpgradeStateChanged(
                    mobileFooter = baseFooter,
                    shouldShowUpselling = false,
                    userUpgradeCheckState = UserUpgradeState.UserUpgradeCheckState.Completed
                ),
                expectedState = baseLoadedState
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityEvent.UpgradeStateChanged(
                    mobileFooter = baseFooter,
                    shouldShowUpselling = false,
                    userUpgradeCheckState = UserUpgradeState.UserUpgradeCheckState.Pending
                ),
                expectedState = baseLoadedState.copy(
                    mobileFooterState = baseMobileFooterState.copy(
                        mobileFooterUiModel = baseMobileFooterState.mobileFooterUiModel.copy(
                            isFieldEnabled = false
                        )
                    )
                )
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityViewAction.Save,
                expectedState = baseLoadedState
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityEvent.Navigation.Close,
                expectedState = baseLoadedState.copy(
                    close = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = baseLoadedState,
                event = EditAddressIdentityEvent.ShowUpselling,
                expectedState = baseLoadedState.copy(
                    upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Show)
                )
            ),
            TestInput(
                currentState = baseLoadedState.copy(
                    upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Show)
                ),
                event = EditAddressIdentityEvent.HideUpselling,
                expectedState = baseLoadedState.copy(
                    upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = baseLoadedState.copy(
                    upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Show)
                ),
                event = EditAddressIdentityViewAction.HideUpselling,
                expectedState = baseLoadedState.copy(
                    upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = (transitionFromLoadingState + transitionsFromLoadedState)
            .map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Event: ${testInput.event}
                    Next state: ${testInput.expectedState}        
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }

    data class TestInput(
        val currentState: EditAddressIdentityState,
        val event: EditAddressIdentityOperation,
        val expectedState: EditAddressIdentityState
    )
}
