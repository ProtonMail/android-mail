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
import ch.protonmail.android.mailmessage.data.local.dao.MessageAttachmentDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageAttachmentMetadataDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageBodyDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageLabelDao
import ch.protonmail.android.mailmessage.data.local.dao.OutboxDao
import ch.protonmail.android.mailmessage.data.local.dao.UnreadMessagesCountDao
import ch.protonmail.android.mailpagination.data.local.PageIntervalDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.addTableColumn
import me.proton.core.data.room.db.extension.dropTable
import me.proton.core.data.room.db.migration.DatabaseMigration

@Suppress("MaxLineLength")
interface MessageDatabase : Database, PageIntervalDatabase {

    fun messageDao(): MessageDao
    fun messageLabelDao(): MessageLabelDao
    fun messageBodyDao(): MessageBodyDao
    fun messageAttachmentDao(): MessageAttachmentDao
    fun messageAttachmentMetadataDao(): MessageAttachmentMetadataDao
    fun outboxDao(): OutboxDao
    fun unreadMessagesCountDao(): UnreadMessagesCountDao

    companion object {

        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Added MessageAttachmentEntity.
                database.execSQL("CREATE TABLE IF NOT EXISTS `MessageAttachmentEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `attachmentId` TEXT NOT NULL, `name` TEXT NOT NULL, `size` INTEGER NOT NULL, `mimeType` TEXT NOT NULL, `disposition` TEXT, `keyPackets` TEXT, `signature` TEXT, `encSignature` TEXT, `headers` TEXT NOT NULL, PRIMARY KEY(`userId`,`messageId`,`attachmentId`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageBodyEntity`(`userId`,`messageId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentEntity_userId` ON `MessageAttachmentEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentEntity_messageId` ON `MessageAttachmentEntity` (`messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentEntity_attachmentId` ON `MessageAttachmentEntity` (`attachmentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentEntity_userId_messageId` ON `MessageAttachmentEntity` (`userId`, `messageId`)")
            }
        }

        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Added StoredMessageAttachmentMetadataEntity.
                database.execSQL("CREATE TABLE IF NOT EXISTS `MessageAttachmentMetadataEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `attachmentId` TEXT NOT NULL, `hash` TEXT, `path` TEXT, `status` TEXT NOT NULL, PRIMARY KEY(`userId`,`messageId`,`attachmentId`), FOREIGN KEY(`userId`, `messageId`, `attachmentId`) REFERENCES `MessageAttachmentEntity`(`userId`,`messageId`,`attachmentId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentMetadataEntity_userId` ON `MessageAttachmentMetadataEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentMetadataEntity_messageId` ON `MessageAttachmentMetadataEntity` (`messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentMetadataEntity_attachmentId` ON `MessageAttachmentMetadataEntity` (`attachmentId`)")
            }
        }

        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addTableColumn(
                    table = "MessageEntity",
                    column = "sender_isProton",
                    type = "INTEGER NOT NULL",
                    defaultValue = "0"
                )
            }
        }

        val MIGRATION_3 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.dropTable("MessageAttachmentMetadataEntity")
                database.execSQL("CREATE TABLE IF NOT EXISTS `MessageAttachmentMetadataEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `attachmentId` TEXT NOT NULL, `uri` TEXT, `status` TEXT NOT NULL, PRIMARY KEY(`userId`,`messageId`,`attachmentId`), FOREIGN KEY(`userId`, `messageId`, `attachmentId`) REFERENCES `MessageAttachmentEntity`(`userId`,`messageId`,`attachmentId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentMetadataEntity_userId` ON `MessageAttachmentMetadataEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentMetadataEntity_messageId` ON `MessageAttachmentMetadataEntity` (`messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentMetadataEntity_attachmentId` ON `MessageAttachmentMetadataEntity` (`attachmentId`)")
            }
        }

        val MIGRATION_4 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                recreateMessageLabelWithUpdateCascade(database)
                recreateMessageBodyWithUpdateCascade(database)
                recreateMsgAttachmentWithUpdateCascade(database)
            }

            private fun recreateMsgAttachmentWithUpdateCascade(database: SupportSQLiteDatabase) {
                // Create a MessageAttachmentEntity table with the new schema
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `MessageAttachmentEntity_copy` (
                        `userId` TEXT NOT NULL,
                        `messageId` TEXT NOT NULL,
                        `attachmentId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `size` INTEGER NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `disposition` TEXT,
                        `keyPackets` TEXT,
                        `signature` TEXT,
                        `encSignature` TEXT,
                        `headers` TEXT NOT NULL,
                         PRIMARY KEY(`userId`, `messageId`, `attachmentId`),
                         FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                         FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageBodyEntity`(`userId`, `messageId`)
                         ON UPDATE CASCADE ON DELETE CASCADE )
                    """.trimIndent()
                )

                // Copy data from old table to new one
                database.execSQL(
                    """
                        INSERT INTO `MessageAttachmentEntity_copy` (
                        `userId`, `messageId`, `attachmentId`, `name`, `size`,
                        `mimeType`, `disposition`, `keyPackets`, `signature`, `encSignature`, `headers` )
                        SELECT `userId`, `messageId`, `attachmentId`, `name`, `size`,
                        `mimeType`, `disposition`, `keyPackets`, `signature`, `encSignature`, `headers` FROM `MessageAttachmentEntity`
                    """.trimIndent()
                )

                // Delete the old table
                database.execSQL("DROP TABLE `MessageAttachmentEntity`")
                // Rename new table
                database.execSQL("ALTER TABLE `MessageAttachmentEntity_copy` RENAME TO `MessageAttachmentEntity` ")

                // Recreate indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentEntity_userId` ON `MessageAttachmentEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentEntity_messageId` ON `MessageAttachmentEntity` (`messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentEntity_attachmentId` ON `MessageAttachmentEntity` (`attachmentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageAttachmentEntity_userId_messageId` ON `MessageAttachmentEntity` (`userId`, `messageId`)")
            }

            private fun recreateMessageBodyWithUpdateCascade(database: SupportSQLiteDatabase) {
                // Create a MessageBody table with the new schema
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `MessageBodyEntity_copy` (
                        `userId` TEXT NOT NULL,
                        `messageId` TEXT NOT NULL,
                        `body` TEXT,
                        `header` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `spamScore` TEXT NOT NULL,
                        `replyTo` TEXT NOT NULL,
                        `replyTos` TEXT NOT NULL,
                        `unsubscribeMethodsEntity` TEXT,
                         PRIMARY KEY(`userId`, `messageId`),
                         FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                         FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageEntity`(`userId`, `messageId`)
                         ON UPDATE CASCADE ON DELETE NO ACTION )
                    """.trimIndent()
                )

                // Copy data from old table to new one
                database.execSQL(
                    """
                        INSERT INTO `MessageBodyEntity_copy` (
                        `userId`, `messageId`, `body`, `header`, `mimeType`,
                        `spamScore`, `replyTo`, `replyTos`, `unsubscribeMethodsEntity` )
                        SELECT `userId`, `messageId`, `body`, `header`, `mimeType`,
                        `spamScore`, `replyTo`, `replyTos`, `unsubscribeMethodsEntity` FROM `MessageBodyEntity`
                    """.trimIndent()
                )

                // Delete the old table
                database.execSQL("DROP TABLE `MessageBodyEntity`")
                // Rename new table
                database.execSQL("ALTER TABLE `MessageBodyEntity_copy` RENAME TO `MessageBodyEntity` ")

                // Recreate indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageBodyEntity_userId` ON `MessageBodyEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageBodyEntity_messageId` ON `MessageBodyEntity` (`messageId`)")
            }

            private fun recreateMessageLabelWithUpdateCascade(database: SupportSQLiteDatabase) {
                // Create a MessageLabelEntity_copy table with the new schema
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `MessageLabelEntity_copy` (
                        `userId` TEXT NOT NULL,
                        `labelId` TEXT NOT NULL,
                        `messageId` TEXT NOT NULL,
                        PRIMARY KEY(`userId`,`messageId`,`labelId`),
                        FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageEntity`(`userId`,`messageId`)
                        ON UPDATE CASCADE ON DELETE CASCADE)
                    """.trimIndent()
                )

                // Copy data from old table to new one
                database.execSQL(
                    """
                        INSERT INTO `MessageLabelEntity_copy` ( `userId`, `labelId`, `messageId` )
                        SELECT `userId`, `labelId`, `messageId` FROM `MessageLabelEntity`
                    """.trimIndent()
                )

                // Delete the old table
                database.execSQL("DROP TABLE `MessageLabelEntity`")
                // Rename new table
                database.execSQL("ALTER TABLE `MessageLabelEntity_copy` RENAME TO `MessageLabelEntity` ")

                // Recreate indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageLabelEntity_userId` ON `MessageLabelEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageLabelEntity_messageId` ON `MessageLabelEntity` (`messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageLabelEntity_labelId` ON `MessageLabelEntity` (`labelId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageLabelEntity_userId_messageId` ON `MessageLabelEntity` (`userId`, `messageId`)")

            }
        }

        val MIGRATION_5 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.dropTable("MessageBodyEntity")
                // Create a MessageBody table with the new foreign key constraint
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `MessageBodyEntity` (
                        `userId` TEXT NOT NULL,
                        `messageId` TEXT NOT NULL,
                        `body` TEXT,
                        `header` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `spamScore` TEXT NOT NULL,
                        `replyTo` TEXT NOT NULL,
                        `replyTos` TEXT NOT NULL,
                        `unsubscribeMethodsEntity` TEXT,
                         PRIMARY KEY(`userId`, `messageId`),
                         FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                         FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageEntity`(`userId`, `messageId`) ON UPDATE CASCADE ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageBodyEntity_userId` ON `MessageBodyEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageBodyEntity_messageId` ON `MessageBodyEntity` (`messageId`)")
            }
        }

        val MIGRATION_6 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Added UnreadMessagesCountEntity.
                database.execSQL("CREATE TABLE IF NOT EXISTS `UnreadMessagesCountEntity` (`userId` TEXT NOT NULL, `labelId` TEXT NOT NULL, `totalCount` INTEGER NOT NULL, `unreadCount` INTEGER NOT NULL, PRIMARY KEY(`userId`,`labelId`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_UnreadMessagesCountEntity_userId` ON `UnreadMessagesCountEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_UnreadMessagesCountEntity_labelId` ON `UnreadMessagesCountEntity` (`labelId`)")
            }
        }

        val MIGRATION_7 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a MessageEntity_copy table with the new schema
                database.execSQL("CREATE TABLE IF NOT EXISTS `MessageEntity_copy` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `order` INTEGER NOT NULL, `subject` TEXT NOT NULL, `unread` INTEGER NOT NULL, `toList` TEXT NOT NULL, `ccList` TEXT NOT NULL, `bccList` TEXT NOT NULL, `time` INTEGER NOT NULL, `size` INTEGER NOT NULL, `expirationTime` INTEGER NOT NULL, `isReplied` INTEGER NOT NULL, `isRepliedAll` INTEGER NOT NULL, `isForwarded` INTEGER NOT NULL, `addressId` TEXT NOT NULL, `externalId` TEXT, `numAttachments` INTEGER NOT NULL, `flags` INTEGER NOT NULL, `attachmentCount` TEXT NOT NULL, `sender_address` TEXT NOT NULL, `sender_name` TEXT NOT NULL, `sender_isProton` INTEGER NOT NULL, `sender_group` TEXT, PRIMARY KEY(`userId`, `messageId`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                // Copy data from old table to new one
                database.execSQL(
                    """
                        INSERT INTO `MessageEntity_copy` (`userId`, `messageId`, `conversationId`, `order`, `subject`, `unread`, `toList`, `ccList`, `bccList`, `time`, `size`, `expirationTime`, `isReplied`, `isRepliedAll`, `isForwarded`, `addressId`, `externalId`, `numAttachments`, `flags`, `attachmentCount`, `sender_address`, `sender_name`, `sender_isProton`, `sender_group` )
                        SELECT `userId`, `messageId`, `conversationId`, `order`, `subject`, `unread`, `toList`, `ccList`, `bccList`, `time`, `size`, `expirationTime`, `isReplied`, `isRepliedAll`, `isForwarded`, `addressId`, `externalId`, `numAttachments`, `flags`, `attachmentCount`, `sender_address`, `sender_name`, `sender_isProton`, `sender_group` FROM `MessageEntity`
                    """.trimIndent()
                )
                // Delete the old table
                database.execSQL("DROP TABLE `MessageEntity`")
                // Rename new table
                database.execSQL("ALTER TABLE `MessageEntity_copy` RENAME TO `MessageEntity` ")
                // Create indices
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageEntity_userId` ON `MessageEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageEntity_messageId` ON `MessageEntity` (`messageId`)")
            }
        }
    }
}
