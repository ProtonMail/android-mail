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

package ch.protonmail.android.maillabel.domain.repository

import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelIdWithCategory
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface SelectedMailLabelIdRepository {


    /**
     * Updates the selected category label
     */
    fun selectCategory(categoryLabelId: CategoryLabelId)

    /**
     * Resets selected category to default
     */
    fun resetSelectedCategory()

    /**
     * Updates the selected mail label
     */
    fun selectLocation(mailLabelId: MailLabelId)

    /**
     * Updates the selected mail label + category status as Loaded
     */
    fun setLocationAsLoaded(mailLabelIdWithCategory: MailLabelIdWithCategory)

    /**
     * Returns the currently selected MailLabelId synchronously.
     */
    suspend fun getSelectedMailLabelId(): MailLabelId

    /**
     * Emits new MailLabelId values only when the corresponding location has been loaded.
     */
    fun observeLoadedMailLabelId(): Flow<MailLabelId>

    /**
     * Emits new MailLabelId + Category values only when the corresponding
     * location & category has been loaded.
     */
    fun observeLoadedLabelWithCategory(): Flow<MailLabelIdWithCategory>

    /**
     * Emits new MailLabelId values when a location change is requested.
     */
    fun observeSelectedMailLabelId(): Flow<MailLabelId>

    /**
     * Emits new MailLabelId + Category values when a location & category change is requested.
     */
    fun observeSelectedLabelWithCategory(): Flow<MailLabelIdWithCategory>

    /**
     * Updates the selected mail label to Inbox
     */
    suspend fun selectInitialLocationIfNeeded(userId: UserId, mailLabelIds: Set<MailLabelId>)
}
