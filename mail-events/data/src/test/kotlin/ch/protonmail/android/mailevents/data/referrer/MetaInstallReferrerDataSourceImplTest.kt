/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailevents.data.referrer

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.domain.model.MetaInstallReferrer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

internal class MetaInstallReferrerDataSourceImplTest {

    private val contentResolver = mockk<ContentResolver>()
    private val context = mockk<Context> {
        every { contentResolver } returns this@MetaInstallReferrerDataSourceImplTest.contentResolver
    }
    private val facebookUri = mockk<Uri>()
    private val instagramUri = mockk<Uri>()
    private val facebookLiteUri = mockk<Uri>()

    private val testDispatcher = StandardTestDispatcher()
    private val dataSource = MetaInstallReferrerDataSourceImpl(context, testDispatcher)

    @BeforeTest
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(providerUri("com.facebook.katana.provider")) } returns facebookUri
        every { Uri.parse(providerUri("com.instagram.contentprovider")) } returns instagramUri
        every { Uri.parse(providerUri("com.facebook.lite.provider")) } returns facebookLiteUri
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `should return referrer from Facebook when it has data`() = runTest(testDispatcher) {
        // Given
        expectQuery(facebookUri, cursorWith(installReferrer = "encrypted", isCt = 1, actualTimestamp = 1_700_000_000L))

        // When
        val result = dataSource.getMetaInstallReferrer()

        // Then
        val expected = MetaInstallReferrer(
            installReferrer = "encrypted",
            isClickThrough = true,
            actualTimestamp = Instant.fromEpochSeconds(1_700_000_000L)
        )
        assertEquals(expected.right(), result)
    }

    @Test
    fun `should fall back to Instagram when Facebook is not installed`() = runTest(testDispatcher) {
        // Given
        expectQuery(instagramUri, cursorWith(installReferrer = "encrypted", isCt = 0, actualTimestamp = 1_000L))

        // When
        val result = dataSource.getMetaInstallReferrer()

        // Then
        val expected = MetaInstallReferrer(
            installReferrer = "encrypted",
            isClickThrough = false,
            actualTimestamp = Instant.fromEpochSeconds(1_000L)
        )
        assertEquals(expected.right(), result)
    }

    @Test
    fun `should fall back to the next app when a query throws`() = runTest(testDispatcher) {
        // Given
        every { contentResolver.query(facebookUri, any(), any(), any(), any()) } throws SecurityException()
        expectQuery(facebookLiteUri, cursorWith(installReferrer = "encrypted", isCt = 1, actualTimestamp = 1_000L))

        // When
        val result = dataSource.getMetaInstallReferrer()

        // Then
        val expected = MetaInstallReferrer(
            installReferrer = "encrypted",
            isClickThrough = true,
            actualTimestamp = Instant.fromEpochSeconds(1_000L)
        )
        assertEquals(expected.right(), result)
    }

    @Test
    fun `should return not found when no Meta app is installed`() = runTest(testDispatcher) {
        // When
        val result = dataSource.getMetaInstallReferrer()

        // Then
        assertEquals(DataError.Local.NotFound.left(), result)
    }

    @Test
    fun `should return not found when install referrer is missing`() = runTest(testDispatcher) {
        // Given
        expectQuery(facebookUri, cursorWith(installReferrer = null, isCt = 0, actualTimestamp = 0L))

        // When
        val result = dataSource.getMetaInstallReferrer()

        // Then
        assertEquals(DataError.Local.NotFound.left(), result)
    }

    @Test
    fun `should fall back to Instagram when Facebook has no install referrer`() = runTest(testDispatcher) {
        // Given
        expectQuery(facebookUri, cursorWith(installReferrer = null, isCt = 0, actualTimestamp = 0L))
        expectQuery(instagramUri, cursorWith(installReferrer = "encrypted", isCt = 1, actualTimestamp = 1_000L))

        // When
        val result = dataSource.getMetaInstallReferrer()

        // Then
        val expected = MetaInstallReferrer(
            installReferrer = "encrypted",
            isClickThrough = true,
            actualTimestamp = Instant.fromEpochSeconds(1_000L)
        )
        assertEquals(expected.right(), result)
    }

    @Test
    fun `should return not found when cursor is empty`() = runTest(testDispatcher) {
        // Given
        val cursor = mockk<Cursor>(relaxUnitFun = true) {
            every { moveToFirst() } returns false
        }
        expectQuery(facebookUri, cursor)

        // When
        val result = dataSource.getMetaInstallReferrer()

        // Then
        assertEquals(DataError.Local.NotFound.left(), result)
    }

    private fun providerUri(authorityPrefix: String) = "content://$authorityPrefix.InstallReferrerProvider/$APP_ID"

    private fun expectQuery(uri: Uri, cursor: Cursor) {
        every { contentResolver.query(uri, any(), any(), any(), any()) } returns cursor
    }

    private fun cursorWith(
        installReferrer: String?,
        isCt: Int,
        actualTimestamp: Long
    ) = mockk<Cursor>(relaxUnitFun = true) {
        every { moveToFirst() } returns true
        every { getColumnIndexOrThrow("install_referrer") } returns 0
        every { getColumnIndexOrThrow("is_ct") } returns 1
        every { getColumnIndexOrThrow("actual_timestamp") } returns 2
        every { getString(0) } returns installReferrer
        every { getInt(1) } returns isCt
        every { getLong(2) } returns actualTimestamp
    }

    private companion object {

        const val APP_ID = "1444920889910716"
    }
}
