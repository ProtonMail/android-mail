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
import ch.protonmail.android.mailcommon.domain.model.BasicContactInfo
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.encode
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen.DraftActionForShareKey
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen.DraftMessageIdKey
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen.SerializedDraftActionKey
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen
import ch.protonmail.android.mailcontact.presentation.contactdetails.ContactDetailsScreen.ContactDetailsContactIdKey
import ch.protonmail.android.mailcontact.presentation.contactform.ContactFormScreen.ContactFormBasicContactInfoKey
import ch.protonmail.android.mailcontact.presentation.contactform.ContactFormScreen.ContactFormContactIdKey
import ch.protonmail.android.mailcontact.presentation.contactgroupdetails.ContactGroupDetailsScreen.ContactGroupDetailsLabelIdKey
import ch.protonmail.android.mailcontact.presentation.contactgroupform.ContactGroupFormScreen.ContactGroupFormLabelIdKey
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.ConversationIdKey
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.FilterByLocationKey
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.ScrollToMessageIdKey
import ch.protonmail.android.maildetail.presentation.ui.EntireMessageBodyScreen
import ch.protonmail.android.maildetail.presentation.ui.EntireMessageBodyScreen.INPUT_PARAMS_KEY
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen.MESSAGE_ID_KEY
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.presentation.folderform.FolderFormScreen.FolderFormLabelIdKey
import ch.protonmail.android.maillabel.presentation.folderparentlist.ParentFolderListScreen.ParentFolderListLabelIdKey
import ch.protonmail.android.maillabel.presentation.folderparentlist.ParentFolderListScreen.ParentFolderListParentLabelIdKey
import ch.protonmail.android.maillabel.presentation.labelform.LabelFormScreen.LabelFormLabelIdKey
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInsertionMode
import ch.protonmail.android.mailsettings.presentation.settings.autolock.ui.pin.AutoLockPinScreen.AutoLockPinModeKey
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceScreen.SWIPE_DIRECTION_KEY
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.serialize

sealed class Destination(val route: String) {

    object Screen {
        object Mailbox : Destination("mailbox")

        object Conversation : Destination(
            "mailbox/conversation/${ConversationIdKey.wrap()}/" +
                "${ScrollToMessageIdKey.wrap()}/${FilterByLocationKey.wrap()}"
        ) {
            operator fun invoke(
                conversationId: ConversationId,
                scrollToMessageId: MessageId? = null,
                filterByLocation: MailLabel? = null
            ) = route.replace(ConversationIdKey.wrap(), conversationId.id)
                .replace(ScrollToMessageIdKey.wrap(), scrollToMessageId?.id ?: "null")
                .replace(FilterByLocationKey.wrap(), filterByLocation?.id?.labelId?.id ?: "null")
        }

        object Message : Destination("mailbox/message/${MESSAGE_ID_KEY.wrap()}") {

            operator fun invoke(messageId: MessageId) = route.replace(MESSAGE_ID_KEY.wrap(), messageId.id)
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

        object Composer : Destination("composer")
        object SetMessagePassword : Destination(
            "composer/setMessagePassword/${SetMessagePasswordScreen.InputParamsKey.wrap()}"
        ) {
            operator fun invoke(messageId: MessageId, senderEmail: SenderEmail) = route.replace(
                SetMessagePasswordScreen.InputParamsKey.wrap(),
                SetMessagePasswordScreen.InputParams(messageId, senderEmail).serialize()
            )
        }

        object EditDraftComposer : Destination("composer/${DraftMessageIdKey.wrap()}") {

            operator fun invoke(messageId: MessageId) = route.replace(DraftMessageIdKey.wrap(), messageId.id)
        }

        object ShareFileComposer : Destination("composer/share/${DraftActionForShareKey.wrap()}") {

            operator fun invoke(draftAction: DraftAction) = route.replace(
                DraftActionForShareKey.wrap(),
                draftAction.serialize()
            )
        }

        object MessageActionComposer : Destination("composer/action/${SerializedDraftActionKey.wrap()}") {

            operator fun invoke(action: DraftAction) =
                route.replace(SerializedDraftActionKey.wrap(), action.serialize())
        }

        object Settings : Destination("settings")
        object AccountSettings : Destination("settings/account")
        object AlternativeRoutingSettings : Destination("settings/alternativeRouting")
        object AutoLockSettings : Destination("settings/autolock")
        object AutoLockPinScreen : Destination("settings/autolock/pin/${AutoLockPinModeKey.wrap()}") {

            operator fun invoke(mode: AutoLockInsertionMode) =
                route.replace(AutoLockPinModeKey.wrap(), mode.serialize())
        }

        object CombinedContactsSettings : Destination("settings/combinedContacts")
        object ConversationModeSettings : Destination("settings/account/conversationMode")
        object AutoDeleteSettings : Destination("settings/account/autoDelete")
        object DefaultEmailSettings : Destination("settings/account/defaultEmail")
        object DisplayNameSettings : Destination("settings/account/displayName")
        object PrivacySettings : Destination("settings/account/privacy")
        object LanguageSettings : Destination("settings/appLanguage")
        object CustomizeToolbar : Destination("settings/customizeToolbar")
        object SwipeActionsSettings : Destination("settings/swipeActions")
        object EditSwipeActionSettings : Destination("settings/swipeActions/edit/${SWIPE_DIRECTION_KEY.wrap()}") {

            operator fun invoke(direction: SwipeActionDirection) =
                route.replace(SWIPE_DIRECTION_KEY.wrap(), direction.name)
        }

        object ThemeSettings : Destination("settings/theme")
        object Notifications : Destination("settings/notifications")
        object ApplicationLogs : Destination("settings/applicationLogs")
        object ApplicationLogsView : Destination("settings/applicationLogs/view/${ApplicationLogsViewMode.wrap()}") {
            operator fun invoke(item: ApplicationLogsViewItemMode) =
                route.replace(ApplicationLogsViewMode.wrap(), item.serialize())
        }
        object DeepLinksHandler : Destination("deepLinksHandler")
        object LabelList : Destination("labelList")
        object CreateLabel : Destination("labelForm")
        object EditLabel : Destination("labelForm/${LabelFormLabelIdKey.wrap()}") {

            operator fun invoke(labelId: LabelId) = route.replace(LabelFormLabelIdKey.wrap(), labelId.id)
        }

        object FolderList : Destination("folderList")
        object CreateFolder : Destination("folderForm")
        object EditFolder : Destination("folderForm/${FolderFormLabelIdKey.wrap()}") {

            operator fun invoke(labelId: LabelId) = route.replace(FolderFormLabelIdKey.wrap(), labelId.id)
        }

        object ParentFolderList : Destination(
            "parentFolderList/${ParentFolderListLabelIdKey.wrap()}/${ParentFolderListParentLabelIdKey.wrap()}"
        ) {

            operator fun invoke(labelId: LabelId?, parentLabelId: LabelId?) = run {
                route.replace(
                    ParentFolderListLabelIdKey.wrap(), labelId?.id ?: "null"
                ).replace(
                    ParentFolderListParentLabelIdKey.wrap(), parentLabelId?.id ?: "null"
                )
            }
        }

        object Contacts : Destination("contacts")
        object ContactDetails : Destination("contacts/contact/${ContactDetailsContactIdKey.wrap()}") {

            operator fun invoke(contactId: ContactId) = route.replace(ContactDetailsContactIdKey.wrap(), contactId.id)
        }

        object CreateContact : Destination("contacts/contact/form")
        object AddContact : Destination(
            "contacts/addContact/${ContactFormBasicContactInfoKey.wrap()}/form"
        ) {
            operator fun invoke(contactInfo: BasicContactInfo): String {
                return route.replace(
                    ContactFormBasicContactInfoKey.wrap(),
                    contactInfo.encode().serialize()
                )
            }
        }

        object EditContact : Destination("contacts/contact/${ContactFormContactIdKey.wrap()}/form") {

            operator fun invoke(contactId: ContactId) = route.replace(ContactFormContactIdKey.wrap(), contactId.id)
        }

        object ContactGroupDetails : Destination("contacts/group/${ContactGroupDetailsLabelIdKey.wrap()}") {

            operator fun invoke(labelId: LabelId) = route.replace(ContactGroupDetailsLabelIdKey.wrap(), labelId.id)
        }

        object CreateContactGroup : Destination("contacts/group/form")
        object EditContactGroup : Destination("contacts/group/${ContactGroupFormLabelIdKey.wrap()}/form") {

            operator fun invoke(labelId: LabelId) = route.replace(ContactGroupFormLabelIdKey.wrap(), labelId.id)
        }

        object ManageMembers : Destination("contacts/group/manageMembers")

        object ContactSearch : Destination("contacts/search")

        object Onboarding {
            data object MainScreen : Destination("onboarding/main")
            data object Upselling : Destination("onboarding/upselling")
        }

        object PostSubscription : Destination("postSubscription")

        object Upselling {
            data object StandaloneMailbox : Destination("upselling/standalone/mailbox")
            data object StandaloneMailboxPromo : Destination("upselling/standalone/mailboxPromo")
            data object StandaloneNavbar : Destination("upselling/standalone/navbar")
        }

        object DriveSpotlight : Destination("spotlight/drive")

        object NPSFeedback : Destination("nps/feedback")
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

