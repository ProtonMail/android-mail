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

package ch.protonmail.android.mailcontact.domain.model

import java.time.LocalDate

sealed interface ContactProperty {

    data class StructuredName(
        val family: String,
        val given: String
    ) : ContactProperty

    data class FormattedName(
        val value: String
    ) : ContactProperty

    data class Email(
        val type: Type,
        val value: String
    ) : ContactProperty {

        enum class Type(val value: String) {
            Email(""),
            Home("home"),
            Work("work"),
            Other("other");

            companion object {

                fun from(value: String?) = values().find { it.value == value } ?: Email
            }
        }

    }

    data class Photo(
        val data: ByteArray,
        val contentType: String?,
        val mediaType: String?,
        val extension: String?
    ) : ContactProperty {

        @Suppress("ReturnCount")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Photo

            if (!data.contentEquals(other.data)) return false
            if (contentType != other.contentType) return false
            if (mediaType != other.mediaType) return false
            return extension == other.extension
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + (contentType?.hashCode() ?: 0)
            result = 31 * result + (mediaType?.hashCode() ?: 0)
            result = 31 * result + (extension?.hashCode() ?: 0)
            return result
        }


    }

    data class Birthday(
        val date: LocalDate
    ) : ContactProperty

    data class Note(
        val value: String
    ) : ContactProperty

    data class Organization(
        val value: String
    ) : ContactProperty

    data class Title(
        val value: String
    ) : ContactProperty

    data class Role(
        val value: String
    ) : ContactProperty

    data class Timezone(
        val text: String
    ) : ContactProperty

    data class Logo(
        val data: ByteArray,
        val contentType: String?,
        val mediaType: String?,
        val extension: String?
    ) : ContactProperty {

        @Suppress("ReturnCount")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Logo

            if (!data.contentEquals(other.data)) return false
            if (contentType != other.contentType) return false
            if (mediaType != other.mediaType) return false
            return extension == other.extension
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + (contentType?.hashCode() ?: 0)
            result = 31 * result + (mediaType?.hashCode() ?: 0)
            result = 31 * result + (extension?.hashCode() ?: 0)
            return result
        }

    }

    data class Member(
        val value: String
    ) : ContactProperty

    data class Language(
        val value: String
    ) : ContactProperty

    data class Gender(
        val gender: String
    ) : ContactProperty

    data class Anniversary(
        val date: LocalDate
    ) : ContactProperty

    data class Url(
        val value: String
    ) : ContactProperty

    data class Telephone(
        val type: Type,
        val text: String
    ) : ContactProperty {

        enum class Type(val value: String) {
            Telephone(""),
            Home("home"),
            Work("work"),
            Other("other"),
            Mobile("mobile"),
            Main("main"),
            Fax("fax"),
            Pager("pager");

            companion object {

                fun from(value: String?) = values().find { it.value == value } ?: Telephone
            }
        }
    }

    data class Address(
        val type: Type,
        val streetAddress: String,
        val locality: String,
        val region: String,
        val postalCode: String,
        val country: String
    ) : ContactProperty {

        enum class Type(val value: String) {
            Address(""),
            Home("home"),
            Work("work"),
            Other("other");

            companion object {

                fun from(value: String?) = values().find { it.value == value } ?: Address
            }
        }

    }

}
