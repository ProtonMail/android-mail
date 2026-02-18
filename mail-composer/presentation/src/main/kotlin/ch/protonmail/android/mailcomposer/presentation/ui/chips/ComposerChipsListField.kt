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

package ch.protonmail.android.mailcomposer.presentation.ui.chips

import android.content.ClipData
import android.os.Parcelable
import android.view.Gravity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetLayout
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcomposer.presentation.ui.RecipientChipActionsBottomSheetContent
import ch.protonmail.android.mailcomposer.presentation.ui.chips.item.ChipItem
import ch.protonmail.android.mailcomposer.presentation.ui.suggestions.ContactSuggestionState
import ch.protonmail.android.mailcomposer.presentation.ui.suggestions.ContactSuggestionsList
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerChipsListViewModel
import ch.protonmail.android.mailpadlocks.presentation.EncryptionInfoBottomSheetContent
import ch.protonmail.android.mailpadlocks.presentation.EncryptionInfoSheetState
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoUiModel
import ch.protonmail.android.uicomponents.thenIf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposerChipsListField(
    label: String,
    chipsList: ImmutableList<ChipItem>,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    focusOnClick: Boolean = true,
    actions: ComposerChipsListField.Actions,
    contactSuggestionState: ContactSuggestionState,
    chevronIconContent: @Composable () -> Unit = {}
) {
    // Every chip field needs its specific VM instance, as it can't be shared.
    // We use the `label` String as key, as it's stable and won't change throughout the whole lifecycle.
    val composerChipsListViewModel = hiltViewModel<ComposerChipsListViewModel>(key = label)
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current

    val state by composerChipsListViewModel.state.collectAsStateWithLifecycle()
    val listState = state.listState
    val textFieldState = composerChipsListViewModel.textFieldState

    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var bottomSheetContent by rememberSaveable { mutableStateOf<ChipBottomSheetContent?>(null) }

    bottomSheetContent?.let {
        LaunchedEffect(bottomSheetContent) {
            showBottomSheet = true
            bottomSheetState.show()
        }
    }

    fun resetBottomSheetState() {
        coroutineScope.launch {
            showBottomSheet = false
            bottomSheetState.hide()
            bottomSheetContent = null
        }
    }

    LaunchedEffect(chipsList) {
        composerChipsListViewModel.updateItems(chipsList)
    }

    ProtonModalBottomSheetLayout(
        showBottomSheet = showBottomSheet,
        onDismissed = { resetBottomSheetState() },
        dismissOnBack = true,
        sheetState = bottomSheetState,
        sheetContent = {
            when (val content = bottomSheetContent) {
                is ChipBottomSheetContent.ChipActions -> {
                    RecipientChipActionsBottomSheetContent(
                        chipItem = content.chipItem,
                        onCopy = { chipItem ->
                            coroutineScope.launch {
                                clipboardManager.setClipEntry(
                                    ClipData.newPlainText(chipItem.value, chipItem.value).toClipEntry()
                                )
                            }
                            resetBottomSheetState()
                        },
                        onRemove = { chipItem ->
                            listState.onDelete(chipItem)
                            resetBottomSheetState()
                        },
                        onEncryptionInfoClicked = { encryptionInfo ->
                            bottomSheetContent = ChipBottomSheetContent.EncryptionInfo(encryptionInfo)
                        }
                    )
                }

                is ChipBottomSheetContent.EncryptionInfo -> {
                    EncryptionInfoBottomSheetContent(
                        state = EncryptionInfoSheetState.Requested(content.encryptionInfo),
                        onDismissed = { resetBottomSheetState() }
                    )
                }

                null -> Unit
            }
        }
    ) {
        ChipsListContent(
            label = label,
            modifier = modifier,
            focusRequester = focusRequester,
            nextFocusRequester = nextFocusRequester,
            focusOnClick = focusOnClick,
            actions = actions,
            onChipItemClicked = { chipItem ->
                bottomSheetContent = ChipBottomSheetContent.ChipActions(chipItem)
            },
            contactSuggestionState = contactSuggestionState,
            textFieldState = textFieldState,
            listState = listState,
            chevronIconContent = chevronIconContent
        )
    }

    BackHandler(contactSuggestionState.areSuggestionsExpanded) {
        actions.onSuggestionsDismissed()
    }

    ConsumableLaunchedEffect(state.listChanged) {
        actions.onListChanged(it)
    }

    ConsumableLaunchedEffect(state.suggestionsTermTyped) {
        actions.onSuggestionTermTyped(it)
    }

    ConsumableTextEffect(state.duplicateRemovalWarning) {
        Toast.makeText(context, it, Toast.LENGTH_LONG).apply {
            setGravity(Gravity.BOTTOM, 0, 0)
        }.show()
    }

    state.invalidRecipientsWarning?.let { invalidRecipients ->
        val text = invalidRecipients.errorMessage.string()
        LaunchedEffect(state.invalidRecipientsWarning) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).apply {
                setGravity(Gravity.BOTTOM, 0, 0)
            }.show()
        }
    }
}

@Composable
private fun ChipsListContent(
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    focusOnClick: Boolean = true,
    actions: ComposerChipsListField.Actions,
    onChipItemClicked: (ChipItem) -> Unit,
    contactSuggestionState: ContactSuggestionState,
    textFieldState: TextFieldState,
    listState: ChipsListState,
    chevronIconContent: @Composable () -> Unit = {}
) {

    val interactionSource = remember { MutableInteractionSource() }
    val chipsListActions = remember {
        ChipsListTextField.Actions(
            onFocusChanged = { focusChange ->
                listState.setFocusState(focusChange.isFocused)
                if (!focusChange.hasFocus) {
                    listState.createChip()
                    textFieldState.edit { delete(0, length) }
                    actions.onSuggestionsDismissed()
                }
            },
            onItemClicked = { index ->
                val chipItem = listState.getChipItemAt(index)
                onChipItemClicked(chipItem)
            },
            onTriggerChipCreation = {
                listState.createChip()
                textFieldState.edit { delete(0, length) }
                actions.onSuggestionsDismissed()
            },
            onDeleteLastItem = { listState.onDelete() }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .testTag(ChipsTestTags.FieldPrefix)
                    .align(Alignment.Top)
                    .padding(vertical = ProtonDimens.Spacing.Large)
                    .padding(start = ProtonDimens.Spacing.Large),
                color = ProtonTheme.colors.textWeak,
                style = ProtonTheme.typography.bodyMedium
            )

            val contentDesc = stringResource(R.string.composer_enter_recipient_content_description)
            ChipsListTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .thenIf(focusOnClick) {
                        clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { focusRequester.requestFocus() }
                        )
                    },
                textFieldState = textFieldState,
                state = listState,
                focusRequester = focusRequester,
                nextFocusRequester = nextFocusRequester,
                actions = chipsListActions,
                enterTextForChipContentDescription = contentDesc
            )

            chevronIconContent()
        }

        if (contactSuggestionState.areSuggestionsExpanded &&
            contactSuggestionState.contactSuggestionItems.isNotEmpty()
        ) {
            HorizontalDivider(color = ProtonTheme.colors.backgroundInvertedBorder)

            ContactSuggestionsList(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contactSuggestionState.suggestionListHeightDp),
                currentText = textFieldState.text.toString(),
                contactSuggestionItems = contactSuggestionState.contactSuggestionItems,
                actions = ContactSuggestionsList.Actions(
                    onContactSuggestionsDismissed = actions.onSuggestionsDismissed,
                    onContactSuggestionSelected = { item ->
                        actions.onSuggestionsDismissed()
                        when (item) {
                            is ContactSuggestionUiModel.Data.ContactGroup -> {
                                if (item.emails.isNotEmpty()) {
                                    listState.addGroupChip(
                                        name = item.name,
                                        color = item.color,
                                        members = item.emails
                                    )
                                }
                            }

                            is ContactSuggestionUiModel.Data.Contact -> {
                                listState.typeWord(item.email)
                            }
                        }
                        textFieldState.edit { delete(0, length) }
                    },
                    onRequestContactsPermission = actions.onPermissionRequest,
                    onDeniedContactsPermission = actions.onPermissionInteraction
                )
            )
        }
    }
}

internal object ComposerChipsListField {
    data class Actions(
        val onSuggestionTermTyped: (String) -> Unit,
        val onSuggestionsDismissed: () -> Unit,
        val onListChanged: (List<ChipItem>) -> Unit,
        val onPermissionRequest: () -> Unit,
        val onPermissionInteraction: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onSuggestionTermTyped = {},
                onSuggestionsDismissed = {},
                onListChanged = { _ -> },
                onPermissionRequest = {},
                onPermissionInteraction = {}
            )
        }
    }
}

@Parcelize
private sealed class ChipBottomSheetContent : Parcelable {

    data class ChipActions(val chipItem: ChipItem) : ChipBottomSheetContent()
    data class EncryptionInfo(val encryptionInfo: EncryptionInfoUiModel.WithLock) : ChipBottomSheetContent()
}
