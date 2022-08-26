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
import ch.protonmail.android.mailmessage.data.local.entity.AttachmentCountEntity
import ch.protonmail.android.mailpagination.data.local.PageIntervalDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.addTableColumn
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.util.kotlin.serialize

@Suppress("MaxLineLength")
interface ConversationDatabase : Database, PageIntervalDatabase {
    fun conversationDao(): ConversationDao
    fun conversationLabelDao(): ConversationLabelDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Added ConversationEntity.
                database.execSQL("CREATE TABLE IF NOT EXISTS `ConversationEntity` (`userId` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `order` INTEGER NOT NULL, `subject` TEXT NOT NULL, `senders` TEXT NOT NULL, `recipients` TEXT NOT NULL, `expirationTime` INTEGER NOT NULL, `numMessages` INTEGER NOT NULL, `numUnread` INTEGER NOT NULL, `numAttachments` INTEGER NOT NULL, PRIMARY KEY(`userId`, `conversationId`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_ConversationEntity_userId` ON `ConversationEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_ConversationEntity_conversationId` ON `ConversationEntity` (`conversationId`)")

                // Added ConversationLabelEntity.
                database.execSQL("CREATE TABLE IF NOT EXISTS `ConversationLabelEntity` (`userId` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `labelId` TEXT NOT NULL, `contextTime` INTEGER NOT NULL, `contextSize` INTEGER NOT NULL, `contextNumMessages` INTEGER NOT NULL, `contextNumUnread` INTEGER NOT NULL, `contextNumAttachments` INTEGER NOT NULL, PRIMARY KEY(`userId`, `conversationId`, `labelId`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`userId`, `conversationId`) REFERENCES `ConversationEntity`(`userId`, `conversationId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_ConversationLabelEntity_userId` ON `ConversationLabelEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_ConversationLabelEntity_labelId` ON `ConversationLabelEntity` (`labelId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_ConversationLabelEntity_conversationId` ON `ConversationLabelEntity` (`conversationId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_ConversationLabelEntity_userId_conversationId` ON `ConversationLabelEntity` (`userId`, `conversationId`)")
            }
        }

        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addTableColumn(
                    table = "ConversationEntity",
                    column = "attachmentCount",
                    type = "TEXT NOT NULL",
                    defaultValue = AttachmentCountEntity(0).serialize()
                )
            }
        }
    }
}
