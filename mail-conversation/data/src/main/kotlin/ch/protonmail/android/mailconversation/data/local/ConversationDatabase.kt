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

package ch.protonmail.android.mailconversation.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import ch.protonmail.android.mailconversation.data.local.dao.ConversationDao
import ch.protonmail.android.mailconversation.data.local.dao.ConversationLabelDao
import ch.protonmail.android.mailconversation.data.local.dao.UnreadConversationsCountDao
import ch.protonmail.android.mailpagination.data.local.PageIntervalDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration

@Suppress("MaxLineLength")
interface ConversationDatabase : Database, PageIntervalDatabase {
    fun conversationDao(): ConversationDao
    fun conversationLabelDao(): ConversationLabelDao
    fun unreadConversationsCountDao(): UnreadConversationsCountDao

    companion object {

        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Added UnreadConversationsCountEntity.
                database.execSQL("CREATE TABLE IF NOT EXISTS `UnreadConversationsCountEntity` (`userId` TEXT NOT NULL, `labelId` TEXT NOT NULL, `totalCount` INTEGER NOT NULL, `unreadCount` INTEGER NOT NULL, PRIMARY KEY(`userId`,`labelId`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_UnreadConversationsCountEntity_userId` ON `UnreadConversationsCountEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_UnreadConversationsCountEntity_labelId` ON `UnreadConversationsCountEntity` (`labelId`)")
            }
        }

    }
}
