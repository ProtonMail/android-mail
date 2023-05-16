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
import ch.protonmail.android.mailmessage.data.local.dao.MessageAttachmentMetaDataDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageBodyDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageLabelDao
import ch.protonmail.android.mailpagination.data.local.PageIntervalDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration

@Suppress("MaxLineLength")
interface MessageDatabase : Database, PageIntervalDatabase {

    fun messageDao(): MessageDao
    fun messageLabelDao(): MessageLabelDao
    fun messageBodyDao(): MessageBodyDao
    fun messageAttachmentDao(): MessageAttachmentDao
    fun storedMessageAttachmentMetaDataDao(): MessageAttachmentMetaDataDao

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
    }
}
