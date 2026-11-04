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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailattachments.presentation.model.AttachmentMetadataUiModel
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.Attachment.TotalIconAndPaddingDp
import ch.protonmail.android.uicomponents.text.containsHighlight
import ch.protonmail.android.uicomponents.text.rememberSearchHighlightedText
import kotlin.math.max
import kotlin.math.min

@Composable
fun AttachmentList(
    modifier: Modifier = Modifier,
    attachments: List<AttachmentMetadataUiModel>,
    onAttachmentClicked: (AttachmentIdUiModel) -> Unit,
    textColor: Color,
    downloadingAttachmentId: AttachmentIdUiModel? = null,
    highlightText: String = ""
) {
    // In search results, the attachments that matched come first, so they are not hidden behind "+N"
    val orderedAttachments = remember(attachments, highlightText) {
        val (matching, others) = attachments.partition { chipHighlights(it.name, highlightText) }
        matching + others
    }

    SubcomposeLayout(
        modifier = modifier
            .wrapContentSize()
    ) { constraints ->

        val (placeableWithInfoList, height) = measureAttachments(
            attachments = orderedAttachments,
            constraints = constraints,
            textColor = textColor,
            onAttachmentClicked = onAttachmentClicked,
            downloadingAttachmentId = downloadingAttachmentId,
            highlightText = highlightText
        )

        layout(
            width = constraints.maxWidth,
            height = height
        ) {
            placeableWithInfoList.forEach { placeableWithInfo ->
                val placeable = placeableWithInfo.placeable
                val placeableInfo = placeableWithInfo.placeableInfo
                placeable.place(x = placeableInfo.x, y = placeableInfo.y)
            }
        }
    }
}

private fun SubcomposeMeasureScope.measureAttachments(
    attachments: List<AttachmentMetadataUiModel>,
    constraints: Constraints,
    textColor: Color,
    onAttachmentClicked: (AttachmentIdUiModel) -> Unit,
    downloadingAttachmentId: AttachmentIdUiModel? = null,
    highlightText: String
): MeasureResult {

    val filteredAttachments = attachments.filter { it.includeInPreview }

    val (plusOneDigitWidth, plusTwoDigitWidth, plusThreeDigitWidth) = measurePlusWidths(constraints)

    val iconAndPaddingWidth = TotalIconAndPaddingDp.roundToPx()
    val externalPadding = ProtonDimens.Spacing.Small.roundToPx()

    val minTruncatedAttachmentWidth = measureMinTruncatedWidth(constraints)
    val attachmentsFullWidth =
        measureAttachmentsFullWidth(
            filteredAttachments,
            textColor,
            constraints,
            onAttachmentClicked,
            downloadingAttachmentId,
            highlightText
        )

    var attachmentsWidth = 0
    var notPlacedCount = attachments.size

    val attachmentPlaceables = mutableListOf<Placeable>()

    attachmentsFullWidth.forEachIndexed { index, (attachment, placeable, extensionWidth) ->
        val plusWidth = calculatePlusWidth(notPlacedCount, plusOneDigitWidth, plusTwoDigitWidth, plusThreeDigitWidth)
        val availableWidth = constraints.maxWidth - attachmentsWidth - plusWidth

        if (availableWidth < minTruncatedAttachmentWidth) {
            if (notPlacedCount > 0) {
                val plusPlaceable = subcompose(generateSlotId("Plus", attachment.id.value, notPlacedCount)) {
                    PlusText(count = notPlacedCount)
                }.single().measure(constraints)
                attachmentPlaceables.add(plusPlaceable)
            }
            return calculateCoordinates(attachmentPlaceables)
        }

        // For the first attachment, if there's only one attachment total, use full width
        // Otherwise, use at most half of the available width
        val maxAttachmentWidth = if (index == 0 && filteredAttachments.size > 1) {
            constraints.maxWidth / 2
        } else {
            availableWidth
        }

        // If the item can fit into maxAttachmentWidth, add it to the placeable list.
        if (placeable.width <= maxAttachmentWidth) {
            attachmentPlaceables.add(placeable)
            attachmentsWidth += placeable.width
            attachmentsWidth += externalPadding
            notPlacedCount--

        } else {
            // If the item can't fit into maxAttachmentWidth, add a truncated version of the item to the placeable list.
            val truncatedPlaceable = createTruncatedPlaceable(
                attachment = attachment,
                textColor = textColor,
                minTruncatedAttachmentWidth = minTruncatedAttachmentWidth,
                maxAttachmentWidth = maxAttachmentWidth,
                baseNameWidth = maxAttachmentWidth - extensionWidth - iconAndPaddingWidth - externalPadding,
                onAttachmentClicked = onAttachmentClicked,
                isDownloading = downloadingAttachmentId == attachment.id,
                highlightText = highlightText
            )
            attachmentPlaceables.add(truncatedPlaceable)
            attachmentsWidth += truncatedPlaceable.width
            attachmentsWidth += externalPadding
            notPlacedCount--

        }
    }

    return calculateCoordinates(attachmentPlaceables)
}

@Suppress("LongParameterList")
private fun SubcomposeMeasureScope.createTruncatedPlaceable(
    attachment: AttachmentMetadataUiModel,
    textColor: Color,
    minTruncatedAttachmentWidth: Int,
    maxAttachmentWidth: Int,
    baseNameWidth: Int,
    onAttachmentClicked: (AttachmentIdUiModel) -> Unit,
    isDownloading: Boolean = false,
    highlightText: String
): Placeable {
    // Ensure minWidth doesn't exceed maxWidth to prevent constraint errors
    val safeMinWidth = min(minTruncatedAttachmentWidth, maxAttachmentWidth)
    val safeBaseNameWidth = max(0, baseNameWidth)

    return subcompose(generateSlotId("place-attachment", attachment.id.value)) {
        Attachment(
            attachment = attachment,
            textColor = textColor,
            minWidth = safeMinWidth,
            maxWidth = maxAttachmentWidth,
            baseNameWidth = safeBaseNameWidth,
            onAttachmentClicked = onAttachmentClicked,
            isDownloading = isDownloading,
            highlightText = highlightText
        )
    }.single().measure(
        Constraints(
            minWidth = safeMinWidth,
            maxWidth = maxAttachmentWidth
        )
    )
}

private fun calculatePlusWidth(
    notPlacedCount: Int,
    plusOneDigitWidth: Int,
    plusTwoDigitWidth: Int,
    plusThreeDigitWidth: Int
): Int {
    val notPlacedCountExcludingCurrent = notPlacedCount - 1

    return when {
        notPlacedCountExcludingCurrent <= 0 -> 0
        notPlacedCount <= Attachment.Plus1CharLimit -> plusOneDigitWidth
        notPlacedCount <= Attachment.Plus2CharsLimit -> plusTwoDigitWidth
        else -> plusThreeDigitWidth
    }
}

private fun SubcomposeMeasureScope.measureAttachmentsFullWidth(
    attachments: List<AttachmentMetadataUiModel>,
    textColor: Color,
    constraints: Constraints,
    onAttachmentClicked: (AttachmentIdUiModel) -> Unit,
    downloadingAttachmentId: AttachmentIdUiModel? = null,
    highlightText: String
): List<FullWidthAttachmentInfo> {
    val result = mutableListOf<FullWidthAttachmentInfo>()

    attachments.forEach { attachment ->

        val extensionWidth = measureExtensionWidth(attachment.name, attachmentId = attachment.id.value)

        val placeable = subcompose(generateSlotId("measure-attachment", attachment.id.value)) {

            Attachment(
                attachment = attachment,
                textColor = textColor,
                minWidth = 0,
                maxWidth = constraints.maxWidth,
                onAttachmentClicked = onAttachmentClicked,
                isDownloading = downloadingAttachmentId == attachment.id,
                highlightText = highlightText
            )
        }.single().measure(constraints)

        result += FullWidthAttachmentInfo(attachment, placeable, extensionWidth)
    }

    return result
}

private fun SubcomposeMeasureScope.measureMinTruncatedWidth(constraints: Constraints): Int {
    return subcompose("Min-Truncated-Attachment") {
        MinTruncatedAttachment(maxWidth = constraints.maxWidth)
    }.single().measure(constraints).width
}

private fun SubcomposeMeasureScope.measurePlusWidths(constraints: Constraints): Triple<Int, Int, Int> {
    return Triple(
        PlusTextWidthCache.measurePlusTextWidth(this, constraints, Attachment.Plus1CharLimit),
        PlusTextWidthCache.measurePlusTextWidth(this, constraints, Attachment.Plus2CharsLimit),
        PlusTextWidthCache.measurePlusTextWidth(this, constraints, Attachment.Plus3CharsLimit)
    )
}

@Composable
private fun Attachment(
    attachment: AttachmentMetadataUiModel,
    textColor: Color,
    minWidth: Int,
    maxWidth: Int,
    baseNameWidth: Int? = null,
    onAttachmentClicked: (AttachmentIdUiModel) -> Unit,
    isDownloading: Boolean = false,
    highlightText: String = ""
) {
    Box(
        modifier = Modifier
            .background(ProtonTheme.colors.backgroundNorm, shape = ProtonTheme.shapes.huge)
            .clip(ProtonTheme.shapes.huge)
            .clickable(enabled = !isDownloading) { onAttachmentClicked(attachment.id) }
            .border(
                width = ProtonDimens.OutlinedBorderSize,
                color = ProtonTheme.colors.borderStrong,
                shape = ProtonTheme.shapes.huge
            )
            .padding(horizontal = ProtonDimens.Spacing.Standard, vertical = ProtonDimens.Spacing.Compact)
            .widthIn(
                min = with(LocalDensity.current) { minWidth.toDp() },
                max = with(LocalDensity.current) { maxWidth.toDp() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconModifier = Modifier.size(ProtonDimens.IconSize.Medium)
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = iconModifier,
                    strokeWidth = ProtonDimens.BorderSize.Medium,
                    color = ProtonTheme.colors.brandNorm
                )
            } else {
                Icon(
                    modifier = iconModifier,
                    painter = painterResource(id = attachment.icon),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
            }
            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Compact))
            val highlightedName = rememberSearchHighlightedText(attachment.name, highlightText)
            val nameParts = remember(highlightedName) { splitChipName(highlightedName) }
            val baseNameWidthDp = with(LocalDensity.current) { baseNameWidth?.toDp() }

            val baseNameModifier = baseNameWidthDp?.let { Modifier.widthIn(max = it) } ?: Modifier

            FileBaseNameText(
                modifier = Modifier.then(baseNameModifier),
                baseName = nameParts.baseName,
                textColor = textColor,
                trailingFill = nameParts.joinBackground
            )
            nameParts.extension?.let { extension ->
                ExtensionText(extension = extension, textColor = textColor)
            }
        }
    }
}

@Composable
private fun MinTruncatedAttachment(maxWidth: Int) {

    Attachment(
        attachment = AttachmentMetadataUiModel(
            AttachmentIdUiModel("0"),
            "AAA... .pdf",
            R.drawable.ic_file_type_default,
            R.string.attachment_type_unknown,
            size = 0L,
            includeInPreview = true
        ),
        textColor = ProtonTheme.colors.textWeak,
        minWidth = 0,
        maxWidth = maxWidth,
        baseNameWidth = maxWidth,
        onAttachmentClicked = { }
    )
}

@Composable
private fun FileBaseNameText(
    modifier: Modifier,
    baseName: AnnotatedString,
    textColor: Color,
    trailingFill: Color?
) {
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        modifier = modifier.drawBehind {
            val fill = trailingFill ?: return@drawBehind
            val textLayout = layout.value ?: return@drawBehind
            fillAfterText(fill, textLayout)
        },
        text = baseName,
        style = ProtonTheme.typography.bodySmall.copy(
            color = textColor
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout.value = it }
    )
}

/**
 * Paints [color] between the end of the text and the edge of its box, on the extension's side.
 *
 * The box is wider than the text: it rounds up to whole pixels, and a cut-short name leaves room after
 * the ellipsis. Without this, a highlight that runs into the extension shows a thin gap there.
 */
private fun DrawScope.fillAfterText(color: Color, textLayout: TextLayoutResult) {
    val (start, end) = when (layoutDirection) {
        LayoutDirection.Ltr -> textLayout.getLineRight(0) to size.width
        LayoutDirection.Rtl -> 0f to textLayout.getLineLeft(0)
    }
    if (end > start) drawRect(color, topLeft = Offset(start, 0f), size = Size(end - start, size.height))
}

@Composable
private fun ExtensionText(extension: AnnotatedString, textColor: Color) {
    Text(
        text = extension,
        style = ProtonTheme.typography.bodySmall.copy(
            color = textColor
        ),
        maxLines = 1
    )
}

/** Whether a chip shows a highlight. The chip highlights its full name, so this checks the full name too. */
private fun chipHighlights(fileName: String, highlightText: String): Boolean =
    containsHighlight(fileName, highlightText)

/**
 * A chip's name, split so the base name can be cut short while the extension stays visible.
 *
 * [joinBackground] is the highlight background when a highlight runs across the dot, and null otherwise.
 */
internal data class ChipNameParts(
    val baseName: AnnotatedString,
    val extension: AnnotatedString?,
    val joinBackground: Color? = null
)

/**
 * Splits [highlightedName] at its last dot, keeping the highlight on both parts.
 *
 * The name is highlighted before the split. Search can match a whole name, such as "IMG_1234.jpg" for
 * "IMG", and highlighting each part on its own would miss that match.
 *
 * The extension keeps its leading dot. A trailing dot gives no extension and is dropped.
 */
internal fun splitChipName(highlightedName: AnnotatedString): ChipNameParts {
    val dotIndex = highlightedName.text.lastIndexOf('.')
    if (dotIndex < 0) return ChipNameParts(baseName = highlightedName, extension = null)
    val extension = highlightedName.subSequence(dotIndex, highlightedName.length).takeIf { it.length > 1 }
    val joinBackground = highlightedName.backgroundAt(dotIndex - 1)
        ?.takeIf { extension != null && highlightedName.backgroundAt(dotIndex) != null }
    return ChipNameParts(
        baseName = highlightedName.subSequence(0, dotIndex),
        extension = extension,
        joinBackground = joinBackground
    )
}

private fun AnnotatedString.backgroundAt(index: Int): Color? = spanStyles
    .firstOrNull { index >= it.start && index < it.end && it.item.background.isSpecified }
    ?.item
    ?.background

private fun SubcomposeMeasureScope.measureExtensionWidth(fileName: String, attachmentId: String): Int {
    val extension = fileName.substringAfterLast('.', "")
    return if (extension.isNotEmpty()) {
        subcompose(generateSlotId("extension", attachmentId)) {
            ExtensionText(extension = AnnotatedString(".$extension"), textColor = Color.Unspecified)
        }
            .single()
            .measure(Constraints())
            .width
    } else 0
}

private fun SubcomposeMeasureScope.calculateCoordinates(placeables: List<Placeable>): MeasureResult {
    val rowHeight = placeables.firstOrNull()?.height ?: 0

    var x = 0

    val placeableWithInfoList = placeables.map { placeable ->
        val y = if (placeable.height < rowHeight) {
            (rowHeight - placeable.height) / 2
        } else {
            0
        }

        val placeableInfo = PlaceableInfo(x, y, width = placeable.width).also {
            x += placeable.width + ProtonDimens.Spacing.Small.roundToPx()
        }

        PlaceableWithInfo(placeable, placeableInfo)
    }

    return MeasureResult(
        placeableWithInfoList = placeableWithInfoList,
        height = rowHeight
    )
}

private fun generateSlotId(
    type: String,
    key1: Any,
    key2: Any? = null
): String = if (key2 != null) "$type-$key1-$key2" else "$type-$key1"

@Composable
private fun PlusText(count: Int) {
    Box(
        modifier = Modifier
            .height(MailDimens.MailboxItemLabelHeight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier,
            text = "+$count",
            style = ProtonTheme.typography.labelMedium.copy(
                color = ProtonTheme.colors.textWeak
            ),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentPreview() {
    ProtonTheme {
        AttachmentList(
            attachments = listOf(
                AttachmentMetadataUiModel(
                    AttachmentIdUiModel("0"),
                    "Attachment name.pdf",
                    R.drawable.ic_file_type_unknown,
                    R.string.attachment_type_unknown,
                    size = 1024L,
                    includeInPreview = true
                )
            ),
            textColor = ProtonTheme.colors.textWeak,
            onAttachmentClicked = {}
        )
    }
}

private data class MeasureResult(
    val placeableWithInfoList: List<PlaceableWithInfo>,
    val height: Int
)

private data class PlaceableInfo(
    val x: Int,
    val y: Int,
    val width: Int
)

private data class PlaceableWithInfo(
    val placeable: Placeable,
    val placeableInfo: PlaceableInfo
)

private data class FullWidthAttachmentInfo(
    val attachment: AttachmentMetadataUiModel,
    val placeable: Placeable,
    val measuredExtensionWidth: Int
)

object PlusTextWidthCache {

    // To avoid recalculations
    private val cache = mutableMapOf<Int, Int>()

    fun measurePlusTextWidth(
        scope: SubcomposeMeasureScope,
        constraints: Constraints,
        charsLimit: Int
    ): Int {
        return cache.getOrPut(charsLimit) {
            scope.subcompose(generateSlotId("measure-plus", charsLimit)) { PlusText(count = charsLimit) }
                .maxOf { it.measure(constraints).width }
        }
    }
}

object Attachment {

    private const val IconWidthDp = 16
    private const val IconPaddingDp = 6
    private const val TotalStartEndPaddingDp = 20
    internal val TotalIconAndPaddingDp = (IconWidthDp + IconPaddingDp + TotalStartEndPaddingDp).dp
    internal const val Plus1CharLimit = 9
    internal const val Plus2CharsLimit = 99
    internal const val Plus3CharsLimit = 999
}
