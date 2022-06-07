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

package ch.protonmail.android.mailpagination.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import ch.protonmail.android.mailpagination.data.local.dao.PageIntervalDao
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration

interface PageIntervalDatabase : Database {
    fun pageIntervalDao(): PageIntervalDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Added PageIntervalEntity.
                database.execSQL("CREATE TABLE IF NOT EXISTS `PageIntervalEntity` (`userId` TEXT NOT NULL, `type` TEXT NOT NULL, `orderBy` TEXT NOT NULL, `labelId` TEXT NOT NULL, `keyword` TEXT NOT NULL, `read` TEXT NOT NULL, `minValue` INTEGER NOT NULL, `maxValue` INTEGER NOT NULL, `minOrder` INTEGER NOT NULL, `maxOrder` INTEGER NOT NULL, `minId` TEXT, `maxId` TEXT, PRIMARY KEY(`userId`, `type`, `orderBy`, `labelId`, `keyword`, `read`, `minValue`, `maxValue`), FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_PageIntervalEntity_userId` ON `PageIntervalEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_PageIntervalEntity_type` ON `PageIntervalEntity` (`type`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_PageIntervalEntity_minValue` ON `PageIntervalEntity` (`minValue`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_PageIntervalEntity_maxValue` ON `PageIntervalEntity` (`maxValue`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_PageIntervalEntity_minOrder` ON `PageIntervalEntity` (`minOrder`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_PageIntervalEntity_maxOrder` ON `PageIntervalEntity` (`maxOrder`)")
            }
        }
    }
}
