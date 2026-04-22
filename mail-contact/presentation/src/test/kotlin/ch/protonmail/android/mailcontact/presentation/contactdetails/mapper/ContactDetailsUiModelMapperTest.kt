package ch.protonmail.android.mailcontact.presentation.contactdetails.mapper

import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.graphics.Color
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.AvatarInformation
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.ContactDate
import ch.protonmail.android.mailcontact.domain.model.ContactDetailAddress
import ch.protonmail.android.mailcontact.domain.model.ContactDetailCard
import ch.protonmail.android.mailcontact.domain.model.ContactDetailEmail
import ch.protonmail.android.mailcontact.domain.model.ContactField
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.ExtendedName
import ch.protonmail.android.mailcontact.domain.model.GenderKind
import ch.protonmail.android.mailcontact.domain.model.PartialDate
import ch.protonmail.android.mailcontact.domain.model.VCardPropType
import ch.protonmail.android.mailcontact.domain.model.VCardUrl
import ch.protonmail.android.mailcontact.domain.model.VCardUrlValue
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.AvatarUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemBadgeUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemGroupUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemType
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.HeaderUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.QuickActionType
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.QuickActionUiModel
import ch.protonmail.android.testdata.contact.ContactIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContactDetailsUiModelMapperTest {

    private val colorMapper = mockk<ColorMapper> {
        every { toColor(any()) } returns Color.Blue.right()
    }

    private val mapper = ContactDetailsUiModelMapper(colorMapper)

    @Test
    fun `should map contact detail card to ui model`() {
        // Given
        val contactDetailCard = ContactDetailCard(
            id = ContactIdTestData.contactId1,
            remoteId = "id",
            avatarInformation = AvatarInformation(
                initials = "P",
                color = "color"
            ),
            extendedName = ExtendedName(
                last = "Mail",
                first = "Proton"
            ),
            fields = listOf(
                ContactField.Emails(
                    list = listOf(
                        ContactDetailEmail(
                            email = "pm@pm.me",
                            emailType = listOf(VCardPropType.Home),
                            groups = listOf(ContactGroup(name = "group", color = "color"))
                        ),
                        ContactDetailEmail(
                            email = "proton@pm.me",
                            emailType = listOf(VCardPropType.Work),
                            groups = emptyList()
                        )
                    )
                ),
                ContactField.Addresses(
                    list = listOf(
                        ContactDetailAddress(
                            street = "123 Main St",
                            postalCode = "12345",
                            city = "Springfield",
                            region = "IL",
                            country = "USA",
                            addressTypes = listOf(VCardPropType.Home)
                        )
                    )
                ),
                ContactField.Birthday(
                    date = ContactDate.Text("05-07-1980")
                ),
                ContactField.Notes(
                    list = listOf(
                        "This is a note.",
                        "This is another note."
                    )
                ),
                ContactField.Anniversary(
                    date = ContactDate.Date(
                        partialDate = PartialDate(
                            day = null,
                            month = 12,
                            year = 1999
                        )
                    )
                ),
                ContactField.Gender(
                    type = GenderKind.Female
                ),
                ContactField.Organizations(
                    list = listOf(
                        "organization"
                    )
                ),
                ContactField.Urls(
                    list = listOf(
                        VCardUrl(
                            url = VCardUrlValue.Http("url"),
                            urlTypes = emptyList()
                        )
                    )
                )
            )
        )

        // When
        val result = mapper.toUiModel(contactDetailCard)

        // Then
        val expected = ContactDetailsUiModel(
            remoteId = "id",
            avatarUiModel = AvatarUiModel.Initials(
                value = "P",
                color = Color.Blue
            ),
            headerUiModel = HeaderUiModel(
                displayName = "Proton Mail",
                displayEmailAddress = "pm@pm.me"
            ),
            quickActionUiModels = listOf(
                QuickActionUiModel(
                    quickActionType = QuickActionType.Message,
                    icon = R.drawable.ic_proton_pen_square,
                    label = R.string.contact_details_quick_action_message,
                    isEnabled = true
                ),
                QuickActionUiModel(
                    quickActionType = QuickActionType.Call,
                    icon = R.drawable.ic_proton_phone,
                    label = R.string.contact_details_quick_action_call,
                    isEnabled = false
                ),
                QuickActionUiModel(
                    quickActionType = QuickActionType.Share,
                    icon = R.drawable.ic_proton_arrow_up_from_square,
                    label = R.string.contact_details_quick_action_share,
                    isEnabled = true
                )
            ),
            contactDetailsItemGroupUiModels = listOf(
                ContactDetailsItemGroupUiModel(
                    contactDetailsItemUiModels = listOf(
                        ContactDetailsItemUiModel(
                            contactDetailsItemType = ContactDetailsItemType.Email,
                            label = TextUiModel.TextRes(R.string.contact_type_home),
                            value = TextUiModel.Text("pm@pm.me"),
                            badges = listOf(ContactDetailsItemBadgeUiModel(name = "group", color = Color.Blue))
                        ),
                        ContactDetailsItemUiModel(
                            contactDetailsItemType = ContactDetailsItemType.Email,
                            label = TextUiModel.TextRes(R.string.contact_type_work),
                            value = TextUiModel.Text("proton@pm.me")
                        )
                    )
                ),
                ContactDetailsItemGroupUiModel(
                    contactDetailsItemUiModels = listOf(
                        ContactDetailsItemUiModel(
                            contactDetailsItemType = ContactDetailsItemType.Other,
                            label = TextUiModel.TextRes(R.string.contact_type_home),
                            value = TextUiModel.Text("123 Main St, 12345, Springfield, IL, USA")
                        )
                    )
                ),
                ContactDetailsItemGroupUiModel(
                    contactDetailsItemUiModels = listOf(
                        ContactDetailsItemUiModel(
                            contactDetailsItemType = ContactDetailsItemType.Other,
                            label = TextUiModel.TextRes(R.string.contact_property_birthday),
                            value = TextUiModel.Text("05-07-1980")
                        )
                    )
                ),
                ContactDetailsItemGroupUiModel(
                    contactDetailsItemUiModels = listOf(
                        ContactDetailsItemUiModel(
                            contactDetailsItemType = ContactDetailsItemType.Other,
                            label = TextUiModel.TextRes(R.string.contact_property_note),
                            value = TextUiModel.Text("This is a note.")
                        ),
                        ContactDetailsItemUiModel(
                            contactDetailsItemType = ContactDetailsItemType.Other,
                            label = TextUiModel.TextRes(R.string.contact_property_note),
                            value = TextUiModel.Text("This is another note.")
                        )
                    )
                ),
                ContactDetailsItemGroupUiModel(
                    contactDetailsItemUiModels = listOf(
                        ContactDetailsItemUiModel(
                            contactDetailsItemType = ContactDetailsItemType.Other,
                            label = TextUiModel.TextRes(R.string.contact_property_anniversary),
                            value = TextUiModel.Text("${getMonthName(12)} 1999")
                        )
                    )
                ),
                ContactDetailsItemGroupUiModel(
                    contactDetailsItemUiModels = listOf(
                        ContactDetailsItemUiModel(
                            contactDetailsItemType = ContactDetailsItemType.Other,
                            label = TextUiModel.TextRes(R.string.contact_property_gender),
                            value = TextUiModel.TextRes(R.string.contact_gender_female)
                        )
                    )
                ),
                ContactDetailsItemGroupUiModel(
                    contactDetailsItemUiModels = listOf(
                        ContactDetailsItemUiModel(
                            contactDetailsItemType = ContactDetailsItemType.Other,
                            label = TextUiModel.TextRes(R.string.contact_property_organization),
                            value = TextUiModel.Text("organization")
                        )
                    )
                ),
                ContactDetailsItemGroupUiModel(
                    contactDetailsItemUiModels = listOf(
                        ContactDetailsItemUiModel(
                            contactDetailsItemType = ContactDetailsItemType.Url,
                            label = TextUiModel.TextRes(R.string.contact_property_url),
                            value = TextUiModel.Text("url")
                        )
                    )
                )
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun `should deduplicate repeated contact groups when mapping email groups (as badges)`() {
        // Given
        val duplicatedGroup = ContactGroup(name = "group", color = "color")
        val expectedBadges = listOf(ContactDetailsItemBadgeUiModel(name = "group", color = Color.Blue))
        val contactDetailCard = ContactDetailCard(
            id = ContactIdTestData.contactId1,
            remoteId = "id",
            avatarInformation = AvatarInformation(initials = "P", color = "color"),
            extendedName = ExtendedName(last = "Mail", first = "Proton"),
            fields = listOf(
                ContactField.Emails(
                    list = listOf(
                        ContactDetailEmail(
                            email = "pm@pm.me",
                            emailType = listOf(VCardPropType.Home),
                            groups = listOf(duplicatedGroup, duplicatedGroup, duplicatedGroup)
                        )
                    )
                )
            )
        )

        // When
        val result = mapper.toUiModel(contactDetailCard)

        // Then
        val emailItem = result.contactDetailsItemGroupUiModels.first().contactDetailsItemUiModels.first()
        assertEquals(expectedBadges, emailItem.badges)
    }

    private fun getMonthName(month: Int): String = Month.of(month).getDisplayName(TextStyle.FULL, Locale.US)
}
