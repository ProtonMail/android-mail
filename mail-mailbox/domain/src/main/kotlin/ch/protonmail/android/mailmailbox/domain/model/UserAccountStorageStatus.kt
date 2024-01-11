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

package ch.protonmail.android.mailmailbox.domain.model

import ch.protonmail.android.mailmailbox.domain.model.UserAccountStorageStatus.Companion.FIRST_LIMIT_VALUE
import ch.protonmail.android.mailmailbox.domain.model.UserAccountStorageStatus.Companion.SECOND_LIMIT_VALUE

data class UserAccountStorageStatus(
    /** Used space size in Bytes. */
    val usedSpace: Long,
    /** Max space size in Bytes. */
    val maxSpace: Long
) {

    companion object {

        const val FIRST_LIMIT_VALUE = 0.8f
        const val SECOND_LIMIT_VALUE = 0.9f
    }
}

fun UserAccountStorageStatus.isOverQuota() = usedSpace >= maxSpace
fun UserAccountStorageStatus.isOverFirstLimit() = usedSpace >= maxSpace * FIRST_LIMIT_VALUE
fun UserAccountStorageStatus.isOverSecondLimit() = usedSpace >= maxSpace * SECOND_LIMIT_VALUE
fun UserAccountStorageStatus.isBelowFirstLimit() = usedSpace < maxSpace * FIRST_LIMIT_VALUE
fun UserAccountStorageStatus.isBelowSecondLimit() = usedSpace < maxSpace * SECOND_LIMIT_VALUE
