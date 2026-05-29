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

package ch.protonmail.android.testdata.maillabel

import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelIdWithCategory
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId

object MailLabelTestData {

    val inboxSystemLabel = buildDynamicLabelId(SystemLabelId.Inbox)
    val archiveSystemLabel = buildDynamicLabelId(SystemLabelId.Archive)
    val spamSystemLabel = buildDynamicLabelId(SystemLabelId.Spam)
    val draftsSystemLabel = buildDynamicLabelId(SystemLabelId.Drafts)
    val trashSystemLabel = buildDynamicLabelId(SystemLabelId.Trash)
    val allMailSystemLabel = buildDynamicLabelId(SystemLabelId.AllMail)
    val almostAllMailSystemLabel = buildDynamicLabelId(SystemLabelId.AlmostAllMail)
    val sentSystemLabel = buildDynamicLabelId(SystemLabelId.Sent)
    val starredSystemLabel = buildDynamicLabelId(SystemLabelId.Starred)
    val allDraftsSystemLabel = buildDynamicLabelId(SystemLabelId.AllDrafts)
    val allSentSystemLabel = buildDynamicLabelId(SystemLabelId.AllSent)
    val outboxSystemLabel = buildDynamicLabelId(SystemLabelId.Outbox)

    val inboxSystemLabelWithCategory = MailLabelIdWithCategory(inboxSystemLabel.id)
    val archiveSystemLabelWithCategory = MailLabelIdWithCategory(archiveSystemLabel.id)
    val spamSystemLabelWithCategory = MailLabelIdWithCategory(spamSystemLabel.id)
    val draftsSystemLabelWithCategory = MailLabelIdWithCategory(draftsSystemLabel.id)
    val trashSystemLabelWithCategory = MailLabelIdWithCategory(trashSystemLabel.id)
    val allMailSystemLabelWithCategory = MailLabelIdWithCategory(allMailSystemLabel.id)
    val almostAllMailSystemLabelWithCategory = MailLabelIdWithCategory(almostAllMailSystemLabel.id)
    val sentSystemLabelWithCategory = MailLabelIdWithCategory(sentSystemLabel.id)
    val starredSystemLabelWithCategory = MailLabelIdWithCategory(starredSystemLabel.id)
    val allDraftsSystemLabelWithCategory = MailLabelIdWithCategory(allDraftsSystemLabel.id)
    val allSentSystemLabelWithCategory = MailLabelIdWithCategory(allSentSystemLabel.id)
    val outboxSystemLabelWithCategory = MailLabelIdWithCategory(outboxSystemLabel.id)

    val primaryCategoryLabelId = CategoryLabelId(CategorySystemLabelId.Primary.labelId.id)
    val socialCategoryLabelId = CategoryLabelId(CategorySystemLabelId.Social.labelId.id)
    val promotionsCategoryLabelId = CategoryLabelId(CategorySystemLabelId.Promotions.labelId.id)
    val updatesCategoryLabelId = CategoryLabelId(CategorySystemLabelId.Updates.labelId.id)
    val forumsCategoryLabelId = CategoryLabelId(CategorySystemLabelId.Forums.labelId.id)
    val newsletterCategoryLabelId = CategoryLabelId(CategorySystemLabelId.Newsletter.labelId.id)
    val transactionsCategoryLabelId = CategoryLabelId(CategorySystemLabelId.Transactions.labelId.id)

    val inboxPrimarySystemLabelWithCategory = MailLabelIdWithCategory(inboxSystemLabel.id, primaryCategoryLabelId)
    val inboxSocialSystemLabelWithCategory = MailLabelIdWithCategory(inboxSystemLabel.id, socialCategoryLabelId)
    val inboxPromotionsSystemLabelWithCategory =
        MailLabelIdWithCategory(inboxSystemLabel.id, promotionsCategoryLabelId)

    val dynamicSystemLabels = listOf(
        inboxSystemLabel,
        draftsSystemLabel,
        allMailSystemLabel,
        archiveSystemLabel,
        trashSystemLabel,
        starredSystemLabel,
        sentSystemLabel,
        spamSystemLabel,
        allDraftsSystemLabel,
        allSentSystemLabel,
        outboxSystemLabel
    )

    val customLabelOne = buildCustomLabel("customLabel1")
    val customLabelTwo = buildCustomLabel("customLabel2")

    val document = buildCustomLabel("document")
    val label2021 = buildCustomLabel("Label2021")
    val label2022 = buildCustomLabel("Label2022")

    val listOfCustomLabels = listOf(
        // See LabelIdSample
        document,
        label2021,
        label2022
    )

    fun buildCustomLabel(
        id: String,
        name: String = id,
        color: Int = 0,
        parent: MailLabel.Custom? = null,
        isExpanded: Boolean = true,
        level: Int = 0,
        order: Int = 0,
        children: List<String> = emptyList()
    ) = buildCustomLabel(
        id = MailLabelId.Custom.Label(LabelId(id)),
        name = name,
        color = color,
        parent = parent,
        isExpanded = isExpanded,
        level = level,
        order = order,
        children = children.map { MailLabelId.Custom.Label(LabelId(it)) }
    )

    fun buildCustomFolder(
        id: String,
        name: String = id,
        color: Int? = 0,
        parent: MailLabel.Custom? = null,
        isExpanded: Boolean = true,
        level: Int = 0,
        order: Int = 0,
        children: List<String> = emptyList()
    ) = buildCustomLabel(
        id = MailLabelId.Custom.Folder(LabelId(id)),
        name = name,
        color = color,
        parent = parent,
        isExpanded = isExpanded,
        level = level,
        order = order,
        children = children.map { MailLabelId.Custom.Folder(LabelId(it)) }
    )

    private fun buildCustomLabel(
        id: MailLabelId.Custom,
        name: String = id.labelId.id,
        color: Int? = 0,
        parent: MailLabel.Custom? = null,
        isExpanded: Boolean = true,
        level: Int = 0,
        order: Int = 0,
        children: List<MailLabelId.Custom> = emptyList()
    ) = MailLabel.Custom(
        id = id,
        text = name,
        color = color,
        parent = parent,
        isExpanded = isExpanded,
        level = level,
        order = order,
        children = children
    )

    /**
     * Utility method to create a dynamic label id.
     * The point of "dynamic" is that the local id is defined by rust lib (and is arbitrary).
     * We pass along the remote systemLabelId for system locations to be able to know
     * which location is which statically (as needed by some logic).
     *
     * **NOTE that in here, the two ids have been kept the same for the sake of allowing
     * existing unit tests that are based on mocked, static data to keep working**
     */
    private fun buildDynamicLabelId(systemLabelId: SystemLabelId) = MailLabel.System(
        MailLabelId.System(systemLabelId.labelId),
        systemLabelId,
        0
    )

    fun withCategory(mailLabelId: MailLabelId) = MailLabelIdWithCategory(mailLabelId)

    fun withCategory(mailLabel: MailLabel) = MailLabelIdWithCategory(mailLabel.id)

    fun withCategory(mailLabelId: MailLabelId, categorySystemLabelId: CategorySystemLabelId) =
        MailLabelIdWithCategory(mailLabelId, CategoryLabelId(categorySystemLabelId.labelId.id))

    fun withCategory(mailLabel: MailLabel, categorySystemLabelId: CategorySystemLabelId) =
        MailLabelIdWithCategory(mailLabel.id, CategoryLabelId(categorySystemLabelId.labelId.id))

}
