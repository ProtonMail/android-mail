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

package ch.protonmail.android.mailcommon.data.file

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShareUriValidatorTest {

    private val packageManager = mockk<PackageManager> {
        every { getPackageInfo(TestData.PackageName, any<Int>()) } returns packageInfoWithProviders(
            "${TestData.PackageName}.provider",
            "${TestData.PackageName}.androidx-startup",
            "${TestData.PackageName}.first;${TestData.PackageName}.second"
        )
    }

    private val context = mockk<Context> {
        every { packageName } returns TestData.PackageName
        every { this@mockk.packageManager } returns this@ShareUriValidatorTest.packageManager
    }

    private val validator = ShareUriValidator(context)

    @Test
    fun `should allow a content uri served by a foreign authority`() {
        // Given
        val uri = uri(scheme = "content", authority = "media")

        // When + Then
        assertTrue(validator.isAllowed(uri))
    }

    @Test
    fun `should reject a content uri served by our own file provider`() {
        // Given
        val uri = uri(scheme = "content", authority = "${TestData.PackageName}.provider")

        // When + Then
        assertFalse(validator.isAllowed(uri))
    }

    @Test
    fun `should reject a content uri served by any other authority we declare`() {
        // Given
        val startup = uri(scheme = "content", authority = "${TestData.PackageName}.androidx-startup")

        // When + Then
        assertFalse(validator.isAllowed(startup))
    }

    @Test
    fun `should reject each authority of a provider declaring several of them`() {
        // Given
        val first = uri(scheme = "content", authority = "${TestData.PackageName}.first")
        val second = uri(scheme = "content", authority = "${TestData.PackageName}.second")

        // When + Then
        assertFalse(validator.isAllowed(first))
        assertFalse(validator.isAllowed(second))
    }

    @Test
    fun `should reject our own authority regardless of casing`() {
        // Given
        val uri = uri(scheme = "CONTENT", authority = "${TestData.PackageName.uppercase()}.PROVIDER")

        // When + Then
        assertFalse(validator.isAllowed(uri))
    }

    @Test
    fun `should reject a file uri`() {
        // Given
        val uri = uri(scheme = "file", authority = null)

        // When + Then
        assertFalse(validator.isAllowed(uri))
    }

    @Test
    fun `should reject an android resource uri`() {
        // Given
        val uri = uri(scheme = "android.resource", authority = TestData.PackageName)

        // When + Then
        assertFalse(validator.isAllowed(uri))
    }

    @Test
    fun `should reject a content uri without an authority`() {
        // Given
        val uri = uri(scheme = "content", authority = null)

        // When + Then
        assertFalse(validator.isAllowed(uri))
    }

    @Test
    fun `should still reject our own file provider when the providers cannot be enumerated`() {
        // Given
        every { packageManager.getPackageInfo(TestData.PackageName, any<Int>()) } throws
            PackageManager.NameNotFoundException()
        val ownUri = uri(scheme = "content", authority = "${TestData.PackageName}.provider")
        val foreignUri = uri(scheme = "content", authority = "media")

        // When + Then
        assertFalse(validator.isAllowed(ownUri))
        assertTrue(validator.isAllowed(foreignUri))
    }

    @Test
    fun `should keep only the allowed uris when filtering`() {
        // Given
        val allowed = uri(scheme = "content", authority = "media")
        val own = uri(scheme = "content", authority = "${TestData.PackageName}.provider")
        val file = uri(scheme = "file", authority = null)

        // When
        val actual = validator.filterAllowed(listOf(allowed, own, file))

        // Then
        assertEquals(listOf(allowed), actual)
    }

    private fun uri(scheme: String?, authority: String?) = mockk<Uri> {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.authority } returns authority
    }

    // `PackageInfo.providers` and `ProviderInfo.authority` are public fields, so they are written directly on the
    // mock instances rather than stubbed: the framework classes cannot be constructed against the android.jar stubs.
    private fun packageInfoWithProviders(vararg authorities: String) = mockk<PackageInfo>().apply {
        providers = authorities.map { authorityValue ->
            mockk<ProviderInfo>().apply { authority = authorityValue }
        }.toTypedArray()
    }

    private object TestData {

        const val PackageName = "ch.protonmail.android"
    }
}
