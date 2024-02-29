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

package ch.protonmail.android.mailmailbox.presentation.helper

import androidx.paging.AsyncPagingDataDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import kotlinx.coroutines.Dispatchers

object MailboxAsyncPagingDataDiffer {

    val differ = AsyncPagingDataDiffer(
        diffCallback = MailboxDiffCallback(),
        updateCallback = NoopListCallback(),
        workerDispatcher = Dispatchers.Main
    )
}

class NoopListCallback : ListUpdateCallback {

    override fun onChanged(
        position: Int,
        count: Int,
        payload: Any?
    ) {
    }
    override fun onMoved(fromPosition: Int, toPosition: Int) {}
    override fun onInserted(position: Int, count: Int) {}
    override fun onRemoved(position: Int, count: Int) {}
}

class MailboxDiffCallback : DiffUtil.ItemCallback<MailboxItemUiModel>() {

    override fun areItemsTheSame(oldItem: MailboxItemUiModel, newItem: MailboxItemUiModel) = oldItem == newItem

    override fun areContentsTheSame(oldItem: MailboxItemUiModel, newItem: MailboxItemUiModel) = oldItem == newItem
}

