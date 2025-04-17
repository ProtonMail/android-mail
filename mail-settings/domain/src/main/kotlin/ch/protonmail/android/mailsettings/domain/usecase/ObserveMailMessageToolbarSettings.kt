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

package ch.protonmail.android.mailsettings.domain.usecase

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ToolbarAction
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class ObserveMailMessageToolbarSettings @Inject constructor(
    private val mailSettingsRepository: MailSettingsRepository
) {

    operator fun invoke(userId: UserId, isMailBox: Boolean): Flow<List<Action>?> =
        mailSettingsRepository.getMailSettingsFlow(userId).mapSuccessValueOrNull().map { settings ->
            val isConvMode = settings?.viewMode?.enum == ViewMode.ConversationGrouping
            val toolbar = when {
                isMailBox -> settings?.mobileSettings?.listToolbar?.actions
                isConvMode ->
                    settings?.mobileSettings?.conversationToolbar?.actions
                        ?.filterNot { it.enum in ToolbarActionsPreference.Defaults.IgnoredConversationActions }
                else -> settings?.mobileSettings?.messageToolbar?.actions
            }
            val toolbarActions = toolbar?.mapNotNull { it.enum }?.takeIfNotEmpty()
            toolbarActions?.map { toolbarAction ->
                when (toolbarAction) {
                    ToolbarAction.ReplyOrReplyAll -> Action.Reply
                    ToolbarAction.Forward -> Action.Forward
                    ToolbarAction.MarkAsReadOrUnread -> Action.MarkUnread
                    ToolbarAction.StarOrUnstar -> Action.Star
                    ToolbarAction.LabelAs -> Action.Label
                    ToolbarAction.MoveTo -> Action.Move
                    ToolbarAction.MoveToTrash -> Action.Trash
                    ToolbarAction.MoveToArchive -> Action.Archive
                    ToolbarAction.MoveToSpam -> Action.Spam
                    ToolbarAction.ViewMessageInLightMode -> Action.ViewInLightMode
                    ToolbarAction.Print -> Action.Print
                    ToolbarAction.ReportPhishing -> Action.ReportPhishing
                }
            }
        }
}
