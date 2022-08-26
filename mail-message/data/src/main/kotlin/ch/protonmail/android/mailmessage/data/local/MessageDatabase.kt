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

package ch.protonmail.android.mailmessage.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import ch.protonmail.android.mailmessage.data.local.dao.MessageDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageLabelDao
import ch.protonmail.android.mailmessage.data.local.entity.AttachmentCountEntity
import ch.protonmail.android.mailpagination.data.local.PageIntervalDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.addTableColumn
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.util.kotlin.serialize

@Suppress("MaxLineLength")
interface MessageDatabase : Database, PageIntervalDatabase {
    fun messageDao(): MessageDao
    fun messageLabelDao(): MessageLabelDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Added MessageEntity.
                database.execSQL("CREATE TABLE IF NOT EXISTS `MessageEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `order` INTEGER NOT NULL, `subject` TEXT NOT NULL, `unread` INTEGER NOT NULL, `toList` TEXT NOT NULL, `ccList` TEXT NOT NULL, `bccList` TEXT NOT NULL, `time` INTEGER NOT NULL, `size` INTEGER NOT NULL, `expirationTime` INTEGER NOT NULL, `isReplied` INTEGER NOT NULL, `isRepliedAll` INTEGER NOT NULL, `isForwarded` INTEGER NOT NULL, `addressId` TEXT NOT NULL, `externalId` TEXT, `numAttachments` INTEGER NOT NULL, `flags` INTEGER NOT NULL, `sender_address` TEXT NOT NULL, `sender_name` TEXT NOT NULL, PRIMARY KEY(`userId`, `messageId`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`addressId`) REFERENCES `AddressEntity`(`addressId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageEntity_userId` ON `MessageEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageEntity_messageId` ON `MessageEntity` (`messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageEntity_addressId` ON `MessageEntity` (`addressId`)")

                // Added MessageLabelEntity.
                database.execSQL("CREATE TABLE IF NOT EXISTS `MessageLabelEntity` (`userId` TEXT NOT NULL, `labelId` TEXT NOT NULL, `messageId` TEXT NOT NULL, PRIMARY KEY(`userId`, `messageId`, `labelId`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageEntity`(`userId`, `messageId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageLabelEntity_userId` ON `MessageLabelEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageLabelEntity_messageId` ON `MessageLabelEntity` (`messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageLabelEntity_labelId` ON `MessageLabelEntity` (`labelId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageLabelEntity_userId_messageId` ON `MessageLabelEntity` (`userId`, `messageId`)")
            }
        }

        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Added MessageEntity.sender_group.
                database.addTableColumn(
                    table = "MessageEntity",
                    column = "sender_group",
                    type = "TEXT",
                    defaultValue = null,
                )
            }
        }

        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Added MessageEntity.attachmentCount.
                database.addTableColumn(
                    table = "MessageEntity",
                    column = "attachmentCount",
                    type = "TEXT NOT NULL",
                    defaultValue = AttachmentCountEntity(0).serialize()
                )
            }
        }
    }
}
