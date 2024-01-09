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
import ch.protonmail.android.mailmessage.data.local.dao.SearchResultDao
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration

interface SearchResultsDatabase : Database {
    fun searchResultsDao(): SearchResultDao

    companion object {

        val MIGRATION_0: DatabaseMigration = object : DatabaseMigration {
            @Suppress("MaxLineLength")
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create SearchResult table
                database.execSQL("CREATE TABLE IF NOT EXISTS `SearchResultEntity` (`userId` TEXT NOT NULL, `keyword` TEXT NOT NULL, `messageId` TEXT NOT NULL, PRIMARY KEY(`userId`, `messageId`, `keyword`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageEntity`(`userId`, `messageId`) ON UPDATE CASCADE ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_SearchResultEntity_userId` ON `SearchResultEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_SearchResultEntity_messageId` ON `SearchResultEntity` (`messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_SearchResultEntity_keyword` ON `SearchResultEntity` (`keyword`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_SearchResultEntity_userId_messageId` ON `SearchResultEntity` (`userId`, `messageId`)")
            }
        }
    }
}
