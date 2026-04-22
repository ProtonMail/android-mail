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

package ch.protonmail.android.mailcomposer.presentation.ui.form

import android.content.Context
import android.net.Uri
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.compose.FocusableForm
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcomposer.domain.model.DraftMimeType
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.DraftDisplayBodyUiModel
import ch.protonmail.android.mailcomposer.presentation.model.FocusedFieldType
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.model.editor.EditorViewDrawingState
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.mailcomposer.presentation.ui.EditableMessageBodyPlainText
import ch.protonmail.android.mailcomposer.presentation.ui.EditableMessageBodyWebView
import ch.protonmail.android.mailcomposer.presentation.ui.SenderEmailWithSelector
import ch.protonmail.android.mailcomposer.presentation.ui.SubjectTextField
import ch.protonmail.android.mailcomposer.presentation.ui.util.ComposerFocusUtils
import ch.protonmail.android.mailcomposer.presentation.viewmodel.RecipientsViewModel
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.presentation.model.attachment.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.ui.AttachmentList
import ch.protonmail.android.uicomponents.keyboardVisibilityAsState
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

@Composable
internal fun ComposerForm(
    composerInstanceId: String,
    changeFocusToField: Effect<FocusedFieldType>,
    senderEmail: String,
    draftType: DraftMimeType,
    recipientsStateManager: RecipientsStateManager,
    subjectTextField: TextFieldState,
    bodyInitialValue: DraftDisplayBodyUiModel,
    bodyTextFieldState: TextFieldState,
    attachments: AttachmentGroupUiModel,
    focusTextBody: Effect<Unit>,
    initialFocusField: FocusedFieldType = FocusedFieldType.TO,
    actions: ComposerForm.Actions,
    formHeightPx: Float,
    injectInlineAttachments: Effect<List<String>>,
    stripInlineAttachment: Effect<String>,
    modifier: Modifier = Modifier,
    refreshBody: Effect<DraftDisplayBodyUiModel>,
    viewportCoordinateAlignmentEnabled: Boolean
) {

    val recipientsViewModel = hiltViewModel<RecipientsViewModel, RecipientsViewModel.Factory>(
        key = "recipientsViewModel_$composerInstanceId"
    ) { factory ->
        factory.create(recipientsStateManager)
    }

    val isKeyboardVisible by keyboardVisibilityAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val maxWidthModifier = Modifier.fillMaxWidth()

    var showSubjectAndBody by remember { mutableStateOf(true) }
    var isSubjectFocused by remember { mutableStateOf(false) }

    FocusableForm(
        fieldList = listOf(
            FocusedFieldType.TO,
            FocusedFieldType.CC,
            FocusedFieldType.BCC,
            FocusedFieldType.SUBJECT,
            FocusedFieldType.BODY
        ).toImmutableList(),
        initialFocus = initialFocusField,
        onFocusedField = {
            Timber.d("Focus changed: onFocusedField: $it")

            isSubjectFocused = it == FocusedFieldType.SUBJECT
        }
    ) { fieldFocusRequesters ->

        ConsumableLaunchedEffect(effect = changeFocusToField) {
            fieldFocusRequesters[it]?.requestFocus()
            if (!isKeyboardVisible) {
                keyboardController?.show()
            }
        }

        val webViewCache = remember { mutableStateOf<WebView?>(null) }
        val context = LocalContext.current

        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val headerBounds = coordinates.boundsInWindow()
                        val headerHeight = coordinates.boundsInParent().height
                        actions.onHeaderPositioned(headerBounds, headerHeight)
                    }
            ) {
                RecipientFields(
                    fieldFocusRequesters = fieldFocusRequesters,
                    onToggleSuggestions = { isShown -> showSubjectAndBody = isShown },
                    viewModel = recipientsViewModel,
                    formHeightPx = formHeightPx
                )

                if (showSubjectAndBody) {
                    MailDivider()

                    SenderEmailWithSelector(
                        modifier = maxWidthModifier.testTag(ComposerTestTags.FromSender),
                        selectedEmail = senderEmail,
                        onChangeSender = actions.onChangeSender
                    )
                    MailDivider()

                    SubjectTextField(
                        textFieldState = subjectTextField,
                        isFocused = isSubjectFocused,
                        modifier = maxWidthModifier
                            .testTag(ComposerTestTags.Subject)
                            .retainFieldFocusOnConfigurationChange(FocusedFieldType.SUBJECT),
                        focusRequester = fieldFocusRequesters.getValue(FocusedFieldType.SUBJECT),
                        nextFocusRequester = fieldFocusRequesters.getValue(FocusedFieldType.BODY),
                        onNextToBody = {

                            if (draftType == DraftMimeType.Html) {
                                webViewCache.value?.let { webView ->
                                    ComposerFocusUtils.focusEditorAndShowKeyboard(webView, context)
                                }
                            } else {
                                fieldFocusRequesters.getValue(FocusedFieldType.BODY).requestFocus()
                            }
                        }
                    )
                    MailDivider()

                    if (attachments.attachments.isNotEmpty()) {
                        AttachmentList(
                            messageAttachmentsUiModel = attachments,
                            actions = AttachmentList.Actions(
                                onShowAllAttachments = { Timber.d("On show all attachments clicked") },
                                onAttachmentClicked = { _, id -> Timber.d("On attachment clicked: $id") },
                                onAttachmentDeleteClicked = {
                                    actions.onAttachmentRemoveRequested(it)
                                }
                            )
                        )
                    }
                }
            }

            if (showSubjectAndBody) {

                when (draftType) {
                    DraftMimeType.PlainText -> {
                        EditableMessageBodyPlainText(
                            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Large),
                            bodyTextFieldState = bodyTextFieldState,
                            shouldRequestFocus = focusTextBody,
                            focusRequester = fieldFocusRequesters.getValue(FocusedFieldType.BODY),
                            actions = EditableMessageBodyPlainText.Actions(
                                onMessageBodyChanged = actions.onBodyChanged,
                                onEditorViewDrawingStateChanged = actions.onEditorViewMeasuresChanged,
                                onEditorViewPositioned = actions.onEditorViewPositioned
                            )
                        )
                    }

                    DraftMimeType.Html -> {
                        EditableMessageBodyWebView(
                            messageBodyUiModel = bodyInitialValue,
                            shouldRequestFocus = focusTextBody,
                            focusRequester = fieldFocusRequesters.getValue(FocusedFieldType.BODY),
                            injectInlineAttachments = injectInlineAttachments,
                            stripInlineAttachment = stripInlineAttachment,
                            refreshBody = refreshBody,
                            webViewActions = EditableMessageBodyWebView.Actions(
                                sanitizePastedText = actions.sanitizePastedText,
                                loadImage = actions.loadImage,
                                onMessageBodyChanged = actions.onBodyChanged,
                                onWebViewParamsChanged = actions.onEditorViewMeasuresChanged,
                                onBuildWebView = onBuildWebView(webViewCache, actions.onInlineImageAdded),
                                onInlineImageRemoved = actions.onInlineImageRemoved,
                                onInlineImageClicked = actions.onInlineImageClicked,
                                onInlineImagePasted = actions.onInlineImagePasted
                            ),
                            modifier = maxWidthModifier
                                .testTag(ComposerTestTags.MessageBody)
                                .retainFieldFocusOnConfigurationChange(FocusedFieldType.BODY)
                                .onGloballyPositioned { coordinates ->
                                    val webViewBounds = coordinates.boundsInWindow()
                                    actions.onEditorViewPositioned(webViewBounds)
                                },
                            viewportCoordinateAlignmentEnabled = viewportCoordinateAlignmentEnabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun onBuildWebView(editorWebView: MutableState<WebView?>, onMediaAdded: (Uri) -> Unit) = { context: Context ->
    if (editorWebView.value == null) {
        Timber.d("editor-webview: factory creating new editor webview")

        val webView = object : WebView(context) {

            override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? {
                val inputConnection = super.onCreateInputConnection(outAttrs) ?: return null

                EditorInfoCompat.setContentMimeTypes(outAttrs!!, EditableMessageBodyWebView.contentMimeTypes)
                return InputConnectionCompat.createWrapper(this, inputConnection, outAttrs)
            }

            override fun startActionMode(callback: ActionMode.Callback, type: Int): ActionMode? =
                if (type == ActionMode.TYPE_FLOATING) {
                    super.startActionMode(ClearFormattingActionModeCallback(this, callback), type)
                } else {
                    super.startActionMode(callback, type)
                }
        }

        ViewCompat.setOnReceiveContentListener(
            webView,
            EditableMessageBodyWebView.contentMimeTypes,
            OnReceiveContentListener { _, content ->
                val split = content.partition { item -> item.uri != null }
                val contentWithUri = split.first
                val otherContent = split.second

                runCatching { contentWithUri.clip.getItemAt(0).uri }
                    .getOrNull()
                    ?.let(onMediaAdded)

                return@OnReceiveContentListener otherContent
            }
        )
        editorWebView.value = webView
    }

    Timber.d("editor-webview: factory returning editor webview")
    editorWebView.value ?: throw IllegalStateException("Editor WebView wasn't initialized.")
}

private class ClearFormattingActionModeCallback(
    private val webView: WebView,
    private val wrapped: ActionMode.Callback
) : ActionMode.Callback2() {

    private var clearFormattingRequested = false

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val created = wrapped.onCreateActionMode(mode, menu)
        addClearFormattingItem(menu)
        // Android WebView doesn't reliably emit selectionchange during touch
        // drag selection, so we explicitly snapshot the live range now — the
        // floating toolbar is only shown when a non-collapsed selection exists.
        webView.evaluateJavascript("__protonCaptureSelection();", null)
        return created
    }

    // Chromium's WebView callback clears the menu in onPrepareActionMode and
    // re-populates it from scratch on every selection change, so we must
    // re-inject our item here, or it disappears.
    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        wrapped.onPrepareActionMode(mode, menu)
        addClearFormattingItem(menu)
        // Keep the cache in sync if Chromium refreshes the toolbar because
        // the user adjusted the selection handles.
        webView.evaluateJavascript("__protonCaptureSelection();", null)
        return true
    }

    private fun addClearFormattingItem(menu: Menu) {
        if (menu.findItem(MENU_ITEM_ID) != null) return
        menu.add(
            Menu.NONE,
            MENU_ITEM_ID,
            Menu.NONE,
            webView.context.getString(R.string.composer_action_clear_formatting)
        )
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == MENU_ITEM_ID) {
            clearFormattingRequested = true
            webView.evaluateJavascript("clearSelectionFormatting();", null)
            mode.finish()
            return true
        }
        return wrapped.onActionItemClicked(mode, item)
    }

    // Invalidate the JS-side selection cache when the action mode dismisses
    // for any reason other than our own item firing — otherwise a future
    // ActionMode could resurrect a stale range and clear it.
    override fun onDestroyActionMode(mode: ActionMode) {
        wrapped.onDestroyActionMode(mode)
        if (!clearFormattingRequested) {
            webView.evaluateJavascript("__protonInvalidateSelectionCache();", null)
        }
    }

    override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: android.graphics.Rect?) {
        if (wrapped is ActionMode.Callback2) {
            wrapped.onGetContentRect(mode, view, outRect)
        } else {
            super.onGetContentRect(mode, view, outRect)
        }
    }

    private companion object {

        private const val MENU_ITEM_ID = 0x0FFF0001
    }
}

internal object ComposerForm {
    data class Actions(
        val onChangeSender: () -> Unit,
        val onBodyChanged: (String) -> Unit,
        val onEditorViewMeasuresChanged: (EditorViewDrawingState) -> Unit,
        val onHeaderPositioned: (boundsInWindow: Rect, height: Float) -> Unit,
        val onEditorViewPositioned: (boundsInWindow: Rect) -> Unit,
        val loadImage: (String) -> MessageBodyImage?,
        val sanitizePastedText: (String?, String) -> String,
        val onAttachmentRemoveRequested: (AttachmentId) -> Unit,
        val onInlineImageRemoved: (String) -> Unit,
        val onInlineImageClicked: (String) -> Unit,
        val onInlineImageAdded: (Uri) -> Unit,
        val onInlineImagePasted: (String) -> Unit
    )
}
