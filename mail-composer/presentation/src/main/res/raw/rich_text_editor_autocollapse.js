
/*******************************************************************************
 * Event listeners
 ******************************************************************************/
/* Listen for changes to the body and dispatches them to KT */
document.getElementById('$EDITOR_ID').addEventListener('input', function () {
    var body = document.getElementById('$EDITOR_ID').innerHTML
    $JAVASCRIPT_CALLBACK_INTERFACE_NAME.onBodyUpdated(body)

    requestAnimationFrame(() => {
        CaretPositionUpdater.scheduleUpdate();
    });
});

/* Listen for PASTE events to perform sanitization */
document.getElementById('$EDITOR_ID').addEventListener('paste', function (event) {
    debugLog("Paste event detected.");

    // Intercept and handle paste ourselves
    event.preventDefault();
    event.stopPropagation();

    const cd = event.clipboardData || event.originalEvent?.clipboardData || window.clipboardData;
    if (!cd) {
        debugLog("No clipboard data available on paste event.");
        return;
    }

    const items = Array.from(cd.items || []);

    // Handle image files
    items.forEach(item => {
        if (item.kind === 'file') {
            handleFilePaste(item);
        }
    });

    // Prefer HTML text over plain text, post only once
    const htmlItem = items.find(item => item.kind === 'string' && item.type === 'text/html');
    const plainItem = items.find(item => item.kind === 'string' && item.type === 'text/plain');
    const chosenTextItem = htmlItem || plainItem;

    if (chosenTextItem) {
        handleTextPaste(chosenTextItem);
    }

    // Update caret position after paste
    setTimeout(() => {
        requestAnimationFrame(() => {
            CaretPositionUpdater.scheduleUpdate();
        });
    }, 50);

});

/* Listen for changes to the body where images are removed and dispatches them to KT */
// Tracks CIDs with a pending deletion check to avoid scheduling multiple rAF callbacks
// for the same image (e.g. when hold-backspace fires several mutation batches per frame).
const _pendingCidDeletionChecks = new Set();

const removeInlineImageObserver = new MutationObserver(mutations => {
    mutations.forEach(mutation => {
        if (mutation.type === 'childList') {
            mutation.removedNodes.forEach(node => {
                if (node.nodeName === 'IMG') {
                    const src = node.getAttribute('src');
                    if (src && src.startsWith('cid:')) {
                        const cid = src.substring(4);
                        if (!_pendingCidDeletionChecks.has(cid)) {
                            _pendingCidDeletionChecks.add(cid);
                            // Defer the check to the next animation frame so that all
                            // synchronous DOM restructuring from the current task (block
                            // splits/merges caused by Enter or backspace near an inline
                            // image) has fully settled before deciding if the image is
                            // truly gone. Rapid hold-backspace can produce mutations
                            // across separate batches within the same frame, so
                            // document.contains() checked inside the callback is not
                            // reliable — querying the editor after the frame is.
                            requestAnimationFrame(() => {
                                _pendingCidDeletionChecks.delete(cid);
                                const editor = document.getElementById('$EDITOR_ID');
                                const imgs = editor ? Array.from(editor.getElementsByTagName('img')) : [];
                                const stillPresent = imgs.some(img => img.getAttribute('src') === 'cid:' + cid);
                                if (!stillPresent) {
                                    $JAVASCRIPT_CALLBACK_INTERFACE_NAME.onInlineImageDeleted(cid);
                                }
                            });
                        }
                    }
                }
            });
        }
    });
});
removeInlineImageObserver.observe(document.getElementById('$EDITOR_ID'), {childList: true, subtree: true});

/* Listen for taps on images in the body that contains a "cid" (inline images) and dispatches the event to KT */
document.getElementById('$EDITOR_ID').addEventListener('click', function (event) {
    if (event.target.nodeName === 'IMG') {
        const src = event.target.getAttribute('src');
        if (src && src.startsWith('cid:')) {
            const cid = src.substring(4);
            $JAVASCRIPT_CALLBACK_INTERFACE_NAME.onInlineImageTapped(cid)
        }
    }
});


/* Observes the cursor position and notifies kotlin through js interface. Invoked at script init (bottom of this file).*/
function trackCursorPosition() {
    var editor = document.getElementById('$EDITOR_ID');
    editor.addEventListener('keyup', (e) => {
        // Let the 'input' + requestAnimationFrame path handle Enter,
        // to prevent double caret updates that cause a visible jitter
        // when inserting a new line (layout still settling).
        if (e.key === 'Enter') {
            return;
        }
        CaretPositionUpdater.scheduleUpdate();
    });

    editor.addEventListener('click', (e) => {
        // Ignore clicks on images to avoid caret jumps when tapping inline images
        const isImageClick = e.target && e.target.closest('img');
        if (isImageClick) {
            debugLog("Ignoring click on image");
            return;
        }
        CaretPositionUpdater.scheduleUpdate();
    });

    let touchStartTime = 0;

    editor.addEventListener('touchstart', (e) => {
        touchStartTime = Date.now();
    });

    editor.addEventListener('touchend', (e) => {
        // This bit is required to allow the "native" long press to be triggered (for context menu in Android)
        if (Date.now() - touchStartTime < 500) {
            CaretPositionUpdater.scheduleUpdate();
        }
    });
}

trackCursorPosition();

/*******************************************************************************
 * Debounces calls to updateCaretPosition() to avoid redundant layout reads
 * and reduce visible jitter when multiple events (input, keyup, touchend)
 * fire close together.
 ******************************************************************************/
const CaretPositionUpdater = {
    debounceTimer: null,
    DEBOUNCE_DELAY_MS: 50,

    scheduleUpdate() {
        if (this.debounceTimer) {
            clearTimeout(this.debounceTimer);
        }
        this.debounceTimer = setTimeout(() => {
            updateCaretPosition();
        }, this.DEBOUNCE_DELAY_MS);
    }
};

function updateCaretPosition() {
    var editor = document.getElementById('$EDITOR_ID');
    var selection = window.getSelection();
    if (selection.rangeCount > 0) {
        var range = selection.getRangeAt(0);

        // Update the caret position only if the range is collapsed to prevent selection deletion.
        if (!range.collapsed) {
            // If the text is selected, we can't modify the DOM.
            return;
        }

        // Create a temporary span element to measure the caret position
        const span = document.createElement('span');
        span.textContent = '\u200B'; // Zero-width space character

        range.insertNode(span);

        // Get the bounding client rect of the span
        const rect = span.getBoundingClientRect();

        // Get the line height of the span
        const lineHeight = window.getComputedStyle(span).lineHeight;
        let parsedLineHeight = 16; // Default fallback
        let parsedLineHeightFactor = 1.2

        // Check if lineHeight is not 'normal' before parsing
        if (lineHeight && lineHeight !== 'normal') {
            const lineHeightValue = lineHeight.replace(/[^\d.]/g, '');
            // Add another check to ensure parsing is possible
            if (lineHeightValue) {
                parsedLineHeight = parseFloat(lineHeightValue) * parsedLineHeightFactor;
            }
        } else {
            // Handle 'normal' line height - still using 1.2 * font-size.
            const fontSize = window.getComputedStyle(span).fontSize;
            const fontSizeValue = fontSize.replace(/[^\d.]/g, '');
            if (fontSizeValue) {
                parsedLineHeight = parseFloat(fontSizeValue) * parsedLineHeightFactor;
            }
        }

        // Remove the temporary span element using its parent node
        if (span.parentNode) {
            span.parentNode.removeChild(span);
        }

        // Restore the original selection (caret position)
        selection.removeAllRanges();
        selection.addRange(range); // Add the original range back

        const density = window.devicePixelRatio || 1.0;

        const coordinateAlignment =
            window.EditorViewportState
                ? window.EditorViewportState.getCoordinateAlignmentAmount()
                : 0;

        const unalignedCaretPositionPx = (rect.top - editor.getBoundingClientRect().top) * density;
        const alignedCaretPositionPx = unalignedCaretPositionPx - (coordinateAlignment * density);
        debugLog(
            "Reporting caret position change, " +
            "unalignedCaretPositionPx = " + Math.round(unalignedCaretPositionPx) + ", " +
            "alignedCaretPositionPx = " + Math.round(alignedCaretPositionPx)
        );
        // Calculate the height of the caret position relative to the inputDiv
        $JAVASCRIPT_CALLBACK_INTERFACE_NAME.onCaretPositionChanged(
            alignedCaretPositionPx,
            parsedLineHeight * density);
    }
}

/*******************************************************************************
 * Enhanced ProtonMail Quote Toggle Handler - START
 *******************************************************************************/
document.addEventListener('click', function (e) {
    const quote = e.target.closest('.protonmail_quote');
    if (quote) {
        const parentQuote = quote.parentElement?.closest('.protonmail_quote');
        if (!parentQuote && !quote.hasAttribute('data-expanded')) {
            quote.setAttribute('data-expanded', '');
            e.preventDefault();
            e.stopPropagation();
        }
    }
});

// Handle Enter key to keep quotes at the bottom
document.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') {
        const selection = window.getSelection();
        if (selection.rangeCount > 0) {
            const range = selection.getRangeAt(0);
            const container = range.startContainer;
            let currentElement = container.nodeType === Node.TEXT_NODE ? container.parentElement : container;

            const allQuotes = document.querySelectorAll('.protonmail_quote');
            let quoteToMove = null;

            allQuotes.forEach(quote => {
                const isTopLevel = !quote.parentElement?.closest('.protonmail_quote');
                if (isTopLevel) {
                    const position = currentElement.compareDocumentPosition(quote);
                    if (position & Node.DOCUMENT_POSITION_FOLLOWING) {
                        if (!quoteToMove) {
                            quoteToMove = quote;
                        }
                    }
                }
            });

            if (quoteToMove) {
                setTimeout(() => {
                    const editor = document.getElementById('$EDITOR_ID') || document.body;
                    const quote = quoteToMove;
                    quote.parentNode.removeChild(quote);
                    editor.appendChild(quote);

                    const prevSibling = quote.previousElementSibling;
                    if (prevSibling && prevSibling.tagName !== 'BR') {
                        const br = document.createElement('br');
                        editor.insertBefore(br, quote);
                    }
                }, 10);
            }
        }
    }
});

// Handle selection changes (prevents from auto-expanding and glitching)
document.addEventListener('selectionchange', function () {
    const selection = window.getSelection();
    if (selection.rangeCount > 0) {
        const range = selection.getRangeAt(0);
        const container = range.commonAncestorContainer;
        let element = container.nodeType === Node.TEXT_NODE ? container.parentElement : container;

        // Allow normal editing when the quote is expanded
        const expandedQuote = element?.closest('.protonmail_quote[data-expanded]');
        if (expandedQuote) {
            return;
        }

        const collapsedQuote = element?.closest('.protonmail_quote:not([data-expanded])');

        if (collapsedQuote) {
            selection.removeAllRanges();
            const newRange = document.createRange();
            newRange.setStartAfter(collapsedQuote);
            newRange.collapse(true);
            selection.addRange(newRange);
        }
    }
});

/*******************************************************************************
 * Enhanced ProtonMail Quote Toggle Handler - END
 *******************************************************************************/

/*******************************************************************************
 * Public functions invoked by kotlin through webview evaluate javascript method
 ******************************************************************************/

function focusEditor() {
    var editor = document.getElementById('$EDITOR_ID');
    editor.focus();
}

function injectInlineImage(contentId) {
    var editor = document.getElementById('$EDITOR_ID');

    editor.focus();

    var selection = window.getSelection();
    if (selection.rangeCount > 0) {
        var range = selection.getRangeAt(0);

        const img = document.createElement('img');
        img.src = "cid:" + contentId;
        img.style = "max-width: 100%;";
        range.insertNode(img);
        range.setStartAfter(img);
        range.collapse(true);

        // Insert a blank line after the image
        const br = document.createElement('br');
        const br1 = document.createElement('br');
        range.insertNode(br1);
        range.insertNode(br);

        // Move the cursor after the <br>
        range.setStartAfter(br1);
        range.collapse(true);

        selection.removeAllRanges();
        selection.addRange(range);
    }
    // Dispatch an input updated event to ensure body is saved
    editor.dispatchEvent(new Event('input'));
}

function stripInlineImage(contentId) {
    // Disable remove image observer as we don't want this strip to trigger a delete
    removeInlineImageObserver.disconnect();

    var editor = document.getElementById('$EDITOR_ID');
    const exactCidPattern = 'cid:' + contentId + '(?![0-9a-zA-Z])';
    const cidMatcher = new RegExp(exactCidPattern);
    const images = editor.getElementsByTagName('img');

    for (const img of images) {
        console.log("Checking image..." + img.src)
        // Check src attribute for a match
        if (cidMatcher.test(img.src)) {
            console.log("Image was actually matched and removed")
            img.remove();
            break;
        }
    }
    // Dispatch an input updated event to ensure body is saved
    editor.dispatchEvent(new Event('input'));
    // Re-enable remove image observer to react to DOM events again
    removeInlineImageObserver.observe(document.getElementById('$EDITOR_ID'), {childList: true, subtree: true});
}

/*******************************************************************************
 * Global editor viewport state
 * (toggled by Kotlin to enable/disable visual viewport coordinate alignment)
 ******************************************************************************/
window.EditorViewportState = {
    viewportCoordinateAlignmentEnabled: true,

    // The last padding actually applied to align the viewport coordinates
    lastAppliedPadding: 0,

    // This function is called by Kotlin to enable/disable viewport coordinate alignment
    setViewportCoordinateAlignmentEnabled(flag) {
        this.viewportCoordinateAlignmentEnabled = !!flag;
        debugLog("Viewport coordinate alignment enabled = " + this.viewportCoordinateAlignmentEnabled);
    },

    isCoordinateAlignmentEnabled() {
        return this.viewportCoordinateAlignmentEnabled;
    },

    setAppliedPadding(paddingPx) {
        this.lastAppliedPadding = paddingPx || 0;
    },

    /**
     * Returns required coordinate alignment amount so that Kotlin vs JS caret positions match each other
     * This actually defines how much the viewport has moved relative to the last padding we actually
     * applied to the document.
     *
     * When alignment is disabled (in order to prevent scroll jumps), this value can be > 0 and we can use it
     * to adjust caret coordinates without touching layout.
     */
    getCoordinateAlignmentAmount() {
        const topCssPadding = Math.round(window.visualViewport.offsetTop || 0);

        return topCssPadding - this.lastAppliedPadding;
    }
};

/*******************************************************************************
 * This function compensates for the visual viewport’s vertical offset
 * by applying CSS padding.
 *
 * Problem: unreachable top content due to the visual viewport having top offset. This happens
 * when we copy-paste some text into the editor and then scroll up and down. It's observed randomly.
 * There is no clear pattern to it.
 *
 * How it works:
 *
 * Read `window.visualViewport.offsetTop` and apply it as a
 * `padding-top` via a CSS custom property (`--vv-top-inset`).
 * This visually shifts the page content down so that the real top text becomes
 * visible again inside the viewport. Please ensure the CSS custom property is defined.
 *
 ******************************************************************************/
function compensateVisualViewportOffset() {
    // Visual viewport is not supported on this browser; nothing to fix.
    if (!window.visualViewport) return;

    const MIN_OFFSET_CHANGE_PX = 8;

    function applyOffsetCompensation() {
        const topCssPadding = Math.round(window.visualViewport.offsetTop || 0);

        if (!EditorViewportState.viewportCoordinateAlignmentEnabled) {
            return;
        }

        const lastPadding = window.EditorViewportState.lastAppliedPadding;

        // Update only if the value changed meaningfully
        if (Math.abs(topCssPadding - lastPadding) > MIN_OFFSET_CHANGE_PX) {
            document.documentElement.style.setProperty('--vv-top-inset', topCssPadding + 'px');
            document.body.style.paddingTop = 'var(--vv-top-inset)';

            window.EditorViewportState.setAppliedPadding(topCssPadding);

            debugLog("Applied visual viewport top offset compensation: " + topCssPadding + "Css px");
        }
    }

    // Keep padding in sync with viewport movements/resizes
    window.visualViewport.addEventListener('scroll', applyOffsetCompensation, {passive: true});
    window.visualViewport.addEventListener('resize', applyOffsetCompensation, {passive: true});

    // Initial compensation
    applyOffsetCompensation();
}

compensateVisualViewportOffset();

/*******************************************************************************
 * Utility function to forward log messages to Kotlin
 ******************************************************************************/
function debugLog(message) {
    $JAVASCRIPT_CALLBACK_INTERFACE_NAME.onDebugLog(message);
}

/*******************************************************************************
 * Functions to handle content pasting into the editor
 ******************************************************************************/
function handleFilePaste(item) {
    const file = item.getAsFile();
    if (!file || !file.type || !file.type.startsWith("image/")) {
        debugLog("Pasted file is not an image: " + (file ? file.type : "no file"));
        return;
    }

    debugLog("Handling pasted image file of type: " + file.type);
    const reader = new FileReader();
    reader.onload = function (e) {
        const result = e.target && e.target.result;
        if (!result || typeof result !== 'string') {
            debugLog("Image FileReader produced no data.");
            return;
        }

        const base64data = result.split(',')[1];
        debugLog("Image pasted, size (base64) = " + (base64data ? base64data.length : 0));

        // Send image data to Android
        $JAVASCRIPT_CALLBACK_INTERFACE_NAME.onImagePasted(base64data);
    };
    reader.readAsDataURL(file);
}

function handleTextPaste(item) {
    const mimeType = item.type || "text/plain";

    item.getAsString(function (text) {
        const rawText = text || "";
        const sanitizedText =  $JAVASCRIPT_CALLBACK_INTERFACE_NAME.onSanitizePastedText(mimeType, rawText) || rawText;

        insertHtmlAtCurrentPosition(sanitizedText);
    });
}

function insertHtmlAtCurrentPosition(html) {
    const editor = document.getElementById('$EDITOR_ID');
    const selection = window.getSelection();

    if (!editor) {
        debugLog("insertHtmlAtCurrentPosition: Editor not found.");
        return;
    }

    // Ensure cursor is inside the editor
    const editorHasCursor =
        document.activeElement === editor &&
        selection &&
        selection.rangeCount > 0;

    if (!editorHasCursor && selection) {
        const range = document.createRange();
        range.setStart(editor, 0);
        range.collapse(true);
        selection.removeAllRanges();
        selection.addRange(range);
    }

    debugLog("Inserting sanitized HTML at current position. Length = " + html.length);

    // Use the browser's built-in editing command to insert the provided HTML
    document.execCommand('insertHTML', false, html);

    // Dispatch an input event so that body update callback is invoked
    editor.dispatchEvent(new Event('input'));

    const allImages = editor.getElementsByTagName('img');
    waitForImagesLoaded(allImages).then(() => {
        requestAnimationFrame(() => {
            debugLog("All pasted images loaded, updating caret position.");
            CaretPositionUpdater.scheduleUpdate();
        });
    });
}

function waitForImagesLoaded(images) {
    return Promise.all(Array.from(images).map(img => {
        if (img.complete) {
            return Promise.resolve();
        }
        return new Promise(resolve => {
            img.onload = resolve;
            img.onerror = resolve;
        });
    }));
}
