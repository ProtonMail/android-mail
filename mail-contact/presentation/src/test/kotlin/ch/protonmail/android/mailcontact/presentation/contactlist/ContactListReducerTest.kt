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

package ch.protonmail.android.mailcontact.presentation.contactlist

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModel
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class ContactListReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = ContactListReducer()

    @Test
    fun `should produce the expected state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, event)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val loadedContactListItemUiModels = listOf(
            ContactListItemUiModel.Header("F"),
            ContactListItemUiModel.Contact(
                id = ContactId("1"),
                name = "first contact",
                emailSubtext = TextUiModel("firstcontact+alias@protonmail.com"),
                avatar = AvatarUiModel.ParticipantInitial("FC")
            ),
            ContactListItemUiModel.Contact(
                id = ContactId("1.1"),
                name = "first contact bis",
                emailSubtext = TextUiModel("firstcontactbis@protonmail.com"),
                avatar = AvatarUiModel.ParticipantInitial("FB")
            )
        )
        private val loadedContactGroupItemUiModels = listOf(
            ContactGroupItemUiModel(
                labelId = LabelId("Id1"),
                name = "Name 1",
                memberCount = 2,
                color = Color.Blue
            ),
            ContactGroupItemUiModel(
                labelId = LabelId("Id2"),
                name = "Name 2",
                memberCount = 3,
                color = Color.Red
            )
        )

        private val emptyLoadingState = ContactListState.Loading()
        private val errorLoadingState = ContactListState.Loading(
            errorLoading = Effect.of(TextUiModel(R.string.contact_list_loading_error))
        )
        private val emptyLoadedState = ContactListState.Loaded.Empty()
        private val dataLoadedState = ContactListState.Loaded.Data(
            contacts = loadedContactListItemUiModels,
            contactGroups = loadedContactGroupItemUiModels,
            isContactGroupsUpsellingVisible = true
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.ContactListLoaded(
                    loadedContactListItemUiModels,
                    loadedContactGroupItemUiModels,
                    isContactGroupsUpsellingVisible = true
                ),
                expectedState = dataLoadedState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.ContactListLoaded(
                    emptyList(),
                    emptyList(),
                    isContactGroupsUpsellingVisible = false
                ),
                expectedState = ContactListState.Loaded.Empty()
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.ErrorLoadingContactList,
                expectedState = errorLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.OpenContactForm,
                expectedState = emptyLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.OpenContactGroupForm,
                expectedState = emptyLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.OpenImportContact,
                expectedState = emptyLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.OpenBottomSheet,
                expectedState = emptyLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.OpenContactSearch,
                expectedState = emptyLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.DismissBottomSheet,
                expectedState = emptyLoadingState
            )
        )

        private val transitionsFromEmptyLoadedState = listOf(
            TestInput(
                currentState = emptyLoadedState,
                event = ContactListEvent.ContactListLoaded(
                    loadedContactListItemUiModels,
                    loadedContactGroupItemUiModels,
                    isContactGroupsUpsellingVisible = true
                ),
                expectedState = dataLoadedState
            ),
            TestInput(
                currentState = emptyLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                ),
                event = ContactListEvent.ContactListLoaded(
                    emptyList(),
                    emptyList(),
                    isContactGroupsUpsellingVisible = false
                ),
                expectedState = ContactListState.Loaded.Empty().copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = emptyLoadedState,
                event = ContactListEvent.ErrorLoadingContactList,
                expectedState = errorLoadingState
            ),
            TestInput(
                currentState = emptyLoadedState,
                event = ContactListEvent.OpenContactForm,
                expectedState = emptyLoadedState.copy(
                    openContactForm = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = emptyLoadedState,
                event = ContactListEvent.OpenContactGroupForm,
                expectedState = emptyLoadedState.copy(
                    openContactGroupForm = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = emptyLoadedState,
                event = ContactListEvent.OpenImportContact,
                expectedState = emptyLoadedState.copy(
                    openImportContact = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = emptyLoadedState,
                event = ContactListEvent.OpenBottomSheet,
                expectedState = emptyLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
                )
            ),
            TestInput(
                currentState = emptyLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.empty(),
                    bottomSheetType = ContactListState.BottomSheetType.Menu
                ),
                event = ContactListEvent.OpenUpsellingBottomSheet,
                expectedState = emptyLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show),
                    bottomSheetType = ContactListState.BottomSheetType.Upselling
                )
            ),
            TestInput(
                currentState = emptyLoadedState,
                event = ContactListEvent.OpenContactSearch,
                expectedState = emptyLoadedState.copy(
                    openContactSearch = Effect.of(true)
                )
            ),
            TestInput(
                currentState = emptyLoadedState,
                event = ContactListEvent.DismissBottomSheet,
                expectedState = emptyLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = emptyLoadedState,
                event = ContactListEvent.SubscriptionUpgradeRequiredError,
                expectedState = emptyLoadedState.copy(
                    subscriptionError = Effect.of(TextUiModel.TextRes(R.string.contact_group_form_subscription_error)),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = emptyLoadedState,
                event = ContactListEvent.UpsellingInProgress,
                expectedState = emptyLoadedState.copy(
                    upsellingInProgress = Effect.of(
                        TextUiModel.TextRes(R.string.upselling_snackbar_upgrade_in_progress)
                    ),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            )
        )

        private val transitionsFromDataLoadedState = listOf(
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.ContactListLoaded(
                    loadedContactListItemUiModels,
                    loadedContactGroupItemUiModels,
                    isContactGroupsUpsellingVisible = true
                ),
                expectedState = dataLoadedState
            ),
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.ContactListLoaded(
                    emptyList(),
                    emptyList(),
                    isContactGroupsUpsellingVisible = false
                ),
                expectedState = ContactListState.Loaded.Empty()
            ),
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.ErrorLoadingContactList,
                expectedState = errorLoadingState
            ),
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.OpenContactForm,
                expectedState = dataLoadedState.copy(
                    openContactForm = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.OpenContactGroupForm,
                expectedState = dataLoadedState.copy(
                    openContactGroupForm = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.OpenImportContact,
                expectedState = dataLoadedState.copy(
                    openImportContact = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.OpenBottomSheet,
                expectedState = dataLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show),
                    bottomSheetType = ContactListState.BottomSheetType.Menu
                )
            ),
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.OpenContactSearch,
                expectedState = dataLoadedState.copy(
                    openContactSearch = Effect.of(true)
                )
            ),
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.DismissBottomSheet,
                expectedState = dataLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.SubscriptionUpgradeRequiredError,
                expectedState = dataLoadedState.copy(
                    subscriptionError = Effect.of(TextUiModel.TextRes(R.string.contact_group_form_subscription_error)),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = dataLoadedState,
                event = ContactListEvent.UpsellingInProgress,
                expectedState = dataLoadedState.copy(
                    upsellingInProgress = Effect.of(
                        TextUiModel.TextRes(R.string.upselling_snackbar_upgrade_in_progress)
                    ),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = dataLoadedState.copy(bottomSheetType = ContactListState.BottomSheetType.Menu),
                event = ContactListEvent.ContactListLoaded(
                    loadedContactListItemUiModels,
                    loadedContactGroupItemUiModels,
                    isContactGroupsUpsellingVisible = true
                ),
                expectedState = dataLoadedState.copy(bottomSheetType = ContactListState.BottomSheetType.Menu)
            ),
            TestInput(
                currentState = dataLoadedState.copy(bottomSheetType = ContactListState.BottomSheetType.Upselling),
                event = ContactListEvent.ContactListLoaded(
                    loadedContactListItemUiModels,
                    loadedContactGroupItemUiModels,
                    isContactGroupsUpsellingVisible = true
                ),
                expectedState = dataLoadedState.copy(bottomSheetType = ContactListState.BottomSheetType.Upselling)
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (
            transitionsFromLoadingState +
                transitionsFromEmptyLoadedState +
                transitionsFromDataLoadedState
            )
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
        val currentState: ContactListState,
        val event: ContactListEvent,
        val expectedState: ContactListState
    )
}
