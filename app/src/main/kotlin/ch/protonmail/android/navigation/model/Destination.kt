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

import ch.protonmail.android.feature.account.RemoveAccountDialog.USER_ID_KEY
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen.DraftMessageIdKey
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen.SerializedDraftActionKey
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.ConversationIdKey
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen.MESSAGE_ID_KEY
import ch.protonmail.android.maillabel.presentation.labelform.LabelFormScreen.LabelIdKey
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceScreen.SWIPE_DIRECTION_KEY
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.serialize

sealed class Destination(val route: String) {

    object Screen {
        object Mailbox : Destination("mailbox")

        object Conversation : Destination("mailbox/conversation/${ConversationIdKey.wrap()}") {
            operator fun invoke(conversationId: ConversationId) =
                route.replace(ConversationIdKey.wrap(), conversationId.id)
        }

        object Message : Destination("mailbox/message/${MESSAGE_ID_KEY.wrap()}") {
            operator fun invoke(messageId: MessageId) = route.replace(MESSAGE_ID_KEY.wrap(), messageId.id)
        }

        object Composer : Destination("composer")

        object EditDraftComposer : Destination("composer/${DraftMessageIdKey.wrap()}") {
            operator fun invoke(messageId: MessageId) = route.replace(DraftMessageIdKey.wrap(), messageId.id)
        }

        object MessageActionComposer : Destination("composer/action/${SerializedDraftActionKey.wrap()}") {
            operator fun invoke(action: DraftAction) =
                route.replace(SerializedDraftActionKey.wrap(), action.serialize())
        }

        object Settings : Destination("settings")
        object AccountSettings : Destination("settings/account")
        object AlternativeRoutingSettings : Destination("settings/alternativeRouting")
        object CombinedContactsSettings : Destination("settings/combinedContacts")
        object ConversationModeSettings : Destination("settings/account/conversationMode")
        object DefaultEmailSettings : Destination("settings/account/defaultEmail")
        object PrivacySettings : Destination("settings/account/privacy")
        object LanguageSettings : Destination("settings/appLanguage")
        object SwipeActionsSettings : Destination("settings/swipeActions")
        object EditSwipeActionSettings : Destination("settings/swipeActions/edit/${SWIPE_DIRECTION_KEY.wrap()}") {
            operator fun invoke(direction: SwipeActionDirection) =
                route.replace(SWIPE_DIRECTION_KEY.wrap(), direction.name)
        }
        object ThemeSettings : Destination("settings/theme")
        object Notifications : Destination("settings/notifications")
        object DeepLinksHandler : Destination("deepLinksHandler")
        object LabelList : Destination("labelList")
        object CreateLabel : Destination("labelForm")
        object EditLabel : Destination("labelForm/${LabelIdKey.wrap()}") {
            operator fun invoke(labelId: LabelId) = route.replace(LabelIdKey.wrap(), labelId.id)
        }
    }

    object Dialog {
        object RemoveAccount : Destination("remove/${USER_ID_KEY.wrap()}") {

            operator fun invoke(userId: UserId?) = route.replace(USER_ID_KEY.wrap(), userId?.id ?: " ")
        }
    }
}

/**
 * Wrap a key in the format required by the Navigation framework: `{key_name}`
 */
private fun String.wrap() = "{$this}"
