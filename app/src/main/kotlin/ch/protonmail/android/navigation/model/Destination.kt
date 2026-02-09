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

package ch.protonmail.android.navigation.model

import ch.protonmail.android.feature.account.SignOutAccountDialog.USER_ID_KEY
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsViewItemMode
import ch.protonmail.android.mailbugreport.presentation.ui.ApplicationLogsPeekView.ApplicationLogsViewMode
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen.DraftMessageIdKey
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen.SerializedDraftActionKey
import ch.protonmail.android.mailcontact.domain.model.ContactGroupId
import ch.protonmail.android.mailcontact.domain.model.ContactId
import ch.protonmail.android.mailcontact.presentation.contactdetails.ui.ContactDetailsScreen.CONTACT_DETAILS_ID_KEY
import ch.protonmail.android.mailcontact.presentation.contactgroupdetails.ContactGroupDetailsScreen.CONTACT_GROUP_DETAILS_ID_KEY
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.maildetail.presentation.model.RawMessageDataType
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.ConversationDetailEntryPointNameKey
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.ConversationIdKey
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.IsSingleMessageMode
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.OpenedFromLocationKey
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.ScrollToMessageIdKey
import ch.protonmail.android.maildetail.presentation.ui.EntireMessageBodyScreen
import ch.protonmail.android.maildetail.presentation.ui.EntireMessageBodyScreen.INPUT_PARAMS_KEY
import ch.protonmail.android.maildetail.presentation.ui.EntireMessageBodyScreen.MESSAGE_ID_KEY
import ch.protonmail.android.maildetail.presentation.ui.PagedConversationDetailScreen.LocationViewModeIsConversation
import ch.protonmail.android.maildetail.presentation.ui.RawMessageDataScreen
import ch.protonmail.android.maildetail.presentation.ui.RawMessageDataScreen.RAW_DATA_TYPE_KEY
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailpinlock.presentation.autolock.model.AutoLockInsertionMode
import ch.protonmail.android.mailpinlock.presentation.autolock.model.DialogType
import ch.protonmail.android.mailpinlock.presentation.pin.ui.AutoLockPinScreen.AutoLockPinModeKey
import ch.protonmail.android.mailpinlock.presentation.pin.ui.dialog.AutoLockPinScreenDialogKeys.AutoLockPinDialogModeKey
import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.domain.model.ToolbarType
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceScreen.SWIPE_DIRECTION_KEY
import ch.protonmail.android.mailsettings.presentation.settings.toolbar.ui.CustomizeToolbarEditScreen
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen.UpsellingEntryPointKey
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen.UpsellingTypeKey
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.serialize

sealed class Destination(val route: String) {

    object Screen {
        object Mailbox : Destination("mailbox")

        object ConversationRouter : Destination(
            "mailbox/conversation/${ConversationIdKey.wrap()}/" +
                "${ScrollToMessageIdKey.wrap()}/${OpenedFromLocationKey.wrap()}/" +
                ConversationDetailEntryPointNameKey.wrap() + "/${LocationViewModeIsConversation.wrap()}"
        ) {

            operator fun invoke(
                conversationId: ConversationId,
                scrollToMessageId: MessageId? = null,
                openedFromLocation: LabelId,
                entryPoint: ConversationDetailEntryPoint,
                locationViewModeIsConversation: Boolean
            ) = route.replace(ConversationIdKey.wrap(), conversationId.id)
                .replace(ScrollToMessageIdKey.wrap(), scrollToMessageId?.id ?: "null")
                .replace(OpenedFromLocationKey.wrap(), openedFromLocation.id)
                .replace(ConversationDetailEntryPointNameKey.wrap(), entryPoint.name)
                .replace(LocationViewModeIsConversation.wrap(), locationViewModeIsConversation.toString())
        }


        object Conversation : Destination(
            "mailbox/conversation/${ConversationIdKey.wrap()}/" +
                "${ScrollToMessageIdKey.wrap()}/${OpenedFromLocationKey.wrap()}/${IsSingleMessageMode.wrap()}" +
                "/${ConversationDetailEntryPointNameKey.wrap()}/${LocationViewModeIsConversation.wrap()}"
        ) {

            operator fun invoke(
                conversationId: ConversationId,
                scrollToMessageId: MessageId? = null,
                openedFromLocation: LabelId,
                isSingleMessageMode: Boolean,
                entryPoint: ConversationDetailEntryPoint,
                locationViewModeIsConversation: Boolean
            ) = route.replace(ConversationIdKey.wrap(), conversationId.id)
                .replace(ScrollToMessageIdKey.wrap(), scrollToMessageId?.id ?: "null")
                .replace(OpenedFromLocationKey.wrap(), openedFromLocation.id)
                .replace(IsSingleMessageMode.wrap(), isSingleMessageMode.toString())
                .replace(ConversationDetailEntryPointNameKey.wrap(), entryPoint.name)
                .replace(LocationViewModeIsConversation.wrap(), locationViewModeIsConversation.toString())
        }

        data object EntireMessageBody : Destination(
            "mailbox/message/${MESSAGE_ID_KEY.wrap()}/body/${INPUT_PARAMS_KEY.wrap()}"
        ) {

            operator fun invoke(
                messageId: MessageId,
                shouldShowEmbeddedImages: Boolean,
                shouldShowRemoteContent: Boolean,
                viewModePreference: ViewModePreference
            ) = route.replace(MESSAGE_ID_KEY.wrap(), messageId.id)
                .replace(
                    INPUT_PARAMS_KEY.wrap(),
                    EntireMessageBodyScreen.InputParams(
                        shouldShowEmbeddedImages,
                        shouldShowRemoteContent,
                        viewModePreference
                    ).serialize()
                )
        }

        data object RawMessageData : Destination(
            "mailbox/message/${RawMessageDataScreen.MESSAGE_ID_KEY.wrap()}/rawData/${RAW_DATA_TYPE_KEY.wrap()}"
        ) {

            operator fun invoke(messageId: MessageId, rawMessageDataType: RawMessageDataType) =
                route.replace(RawMessageDataScreen.MESSAGE_ID_KEY.wrap(), messageId.id)
                    .replace(RAW_DATA_TYPE_KEY.wrap(), rawMessageDataType.serialize())
        }

        object Composer : Destination("composer")
        object SetMessagePassword : Destination("composer/setMessagePassword")

        object EditDraftComposer : Destination("composer/${DraftMessageIdKey.wrap()}") {

            operator fun invoke(messageId: MessageId) = route.replace(DraftMessageIdKey.wrap(), messageId.id)
        }

        object ShareFileComposer :
            Destination("composer/share/${SerializedDraftActionKey.wrap()}?isExternal={isExternal}") {

            operator fun invoke(draftAction: DraftAction, isExternal: Boolean) = route
                .replace(
                    SerializedDraftActionKey.wrap(),
                    draftAction.serialize()
                )
                .replace("{isExternal}", isExternal.toString())
        }

        object MessageActionComposer : Destination("composer/action/${SerializedDraftActionKey.wrap()}") {

            operator fun invoke(action: DraftAction) =
                route.replace(SerializedDraftActionKey.wrap(), action.serialize())
        }

        object Settings : Destination("settings")
        object AccountsManager : Destination("settings/accountsManager")
        object AccountSettings : Destination("settings/account")
        object AppSettings : Destination("settings/app")
        object EmailSettings : Destination("settings/email")
        object FolderAndLabelSettings : Destination("settings/folderAndLabel")
        object PrivacyAndSecuritySettings : Destination("settings/privacyAndSecurity")
        object SecurityKeysSettings : Destination("settings/securityKeys")
        object SpamFilterSettings : Destination("settings/spamFilter")
        object MobileSignatureSettings : Destination("settings/mobileSignature")
        object EmailSignatureSettings : Destination("settings/emailSignature")
        object SignatureSettingsMenu : Destination("settings/signatureMenu")
        object AutoLockSettings : Destination("settings/autolock")
        object AutoLockInterval : Destination("settings/autolock/interval")
        object AutoLockPinScreen : Destination("settings/autolock/pin/${AutoLockPinModeKey.wrap()}") {

            operator fun invoke(mode: AutoLockInsertionMode) =
                route.replace(AutoLockPinModeKey.wrap(), mode.serialize())
        }

        object AutoLockOverlay : Destination("settings/autolock/landing/overlay")

        object AutoLockPinConfirmDialog :
            Destination("settings/autolock/pindialog/${AutoLockPinDialogModeKey.wrap()}") {

            operator fun invoke(mode: DialogType) = route.replace(AutoLockPinDialogModeKey.wrap(), mode.serialize())
        }

        object CombinedContactsSettings : Destination("settings/combinedContacts")
        object PrivacySettings : Destination("settings/account/privacy")
        object LanguageSettings : Destination("settings/appLanguage")
        object AppIconSettings : Destination("settings/appIcon")
        object SwipeActionsSettings : Destination("settings/swipeActions")
        object EditSwipeActionSettings : Destination("settings/swipeActions/edit/${SWIPE_DIRECTION_KEY.wrap()}") {

            operator fun invoke(direction: SwipeActionDirection) =
                route.replace(SWIPE_DIRECTION_KEY.wrap(), direction.name)
        }

        object ThemeSettings : Destination("settings/theme")
        object Notifications : Destination("settings/notifications")

        object CustomizeToolbar : Destination("settings/customizetoolbar")
        object EditToolbarScreen :
            Destination("settings/customizetoolbar/${CustomizeToolbarEditScreen.OpenMode.wrap()}") {

            operator fun invoke(item: ToolbarType) =
                route.replace(CustomizeToolbarEditScreen.OpenMode.wrap(), item.serialize())
        }

        object ApplicationDebug : Destination("settings/appDebug")
        object ApplicationDebugDangerZone : Destination("settings/appDebug/dangerZone")
        object ApplicationLogs : Destination("settings/applicationLogs")
        object ApplicationLogsView : Destination("settings/applicationLogs/view/${ApplicationLogsViewMode.wrap()}") {

            operator fun invoke(item: ApplicationLogsViewItemMode) =
                route.replace(ApplicationLogsViewMode.wrap(), item.serialize())
        }

        object FeatureFlagsOverrides : Destination("settings/debug/featureFlags")
        object DeepLinksHandler : Destination("deepLinksHandler")

        object Contacts : Destination("contacts")
        object ContactDetails : Destination("contacts/contact/${CONTACT_DETAILS_ID_KEY.wrap()}") {

            operator fun invoke(contactId: ContactId) = route.replace(CONTACT_DETAILS_ID_KEY.wrap(), contactId.id)
        }

        object ContactGroupDetails : Destination("contacts/group/${CONTACT_GROUP_DETAILS_ID_KEY.wrap()}") {

            operator fun invoke(contactGroupId: ContactGroupId) =
                route.replace(CONTACT_GROUP_DETAILS_ID_KEY.wrap(), contactGroupId.id)
        }

        object CreateContact : Destination("contacts/contact/form")

        object ManageMembers : Destination("contacts/group/manageMembers")

        object ContactSearch : Destination("contacts/search")

        object Onboarding {
            data object MainScreen : Destination("onboarding/main")
            data object Upselling : Destination("onboarding/upselling")
        }

        object BugReporting : Destination("support/bugreporting")

        object FeatureUpselling :
            Destination("upselling/entrypoint/feature/${UpsellingEntryPointKey.wrap()}/${UpsellingTypeKey.wrap()}") {

            operator fun invoke(entryPoint: UpsellingEntryPoint.Feature, type: UpsellingVisibility) = route
                .replace(UpsellingEntryPointKey.wrap(), entryPoint.serialize())
                .replace(UpsellingTypeKey.wrap(), type.serialize())
        }

        object FeatureSpotlight : Destination("featureSpotlight")
    }

    object Dialog {
        object SignOut : Destination("signout/${USER_ID_KEY.wrap()}") {

            operator fun invoke(userId: UserId?) = route.replace(USER_ID_KEY.wrap(), userId?.id ?: " ")
        }

        object RemoveAccount : Destination("remove/${USER_ID_KEY.wrap()}") {

            operator fun invoke(userId: UserId?) = route.replace(USER_ID_KEY.wrap(), userId?.id ?: " ")
        }
    }
}

/**
 * Wrap a key in the format required by the Navigation framework: `{key_name}`
 */
private fun String.wrap() = "{$this}"

