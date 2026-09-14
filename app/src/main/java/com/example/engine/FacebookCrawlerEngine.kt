package com.example.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.CommentItem
import java.util.Locale
import java.util.regex.Pattern

class FacebookCrawlerEngine {

    companion object {
        // Regex patterns for "Switch to All Comments"
        private val ALL_COMMENTS_PATTERNS = listOf(
            Pattern.compile("عرض كل التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("جميع التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("كل التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("الأكثر ملاءمة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("All comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View all comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Most relevant", Pattern.CASE_INSENSITIVE)
        )

        // Regex patterns for "Load More Comments" / "Previous Comments"
        private val MORE_COMMENTS_PATTERNS = listOf(
            Pattern.compile("عرض المزيد من التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض التعليقات السابقة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض تعليقات إضافية", Pattern.CASE_INSENSITIVE),
            Pattern.compile("المزيد من التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("التعليقات السابقة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض\\s+\\d+\\s+من\\s+التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View more comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View previous comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("More comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Previous comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Load more comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Load previous comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View \\d+ more comments", Pattern.CASE_INSENSITIVE)
        )

        // Regex patterns for "View Replies" / "More Replies"
        private val MORE_REPLIES_PATTERNS = listOf(
            Pattern.compile("عرض الردود", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض المزيد من الردود", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض الردود السابقة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض ردود إضافية", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض\\s+\\d+\\s+رد(?:ود)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("رد واحد", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ردان", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\d+\\s+رد(?:ود)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View replies", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View more replies", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View previous replies", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View \\d+ repl(?:y|ies)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\d+\\s+repl(?:y|ies)", Pattern.CASE_INSENSITIVE)
        )

        // Ignored UI actions / buttons
        private val IGNORED_TEXTS = setOf(
            "إعجاب", "أعجبني", "رد", "مشاركة", "نسخ", "إبلاغ", "حذف", "تعديل",
            "Like", "Reply", "Share", "Copy", "Report", "Delete", "Edit",
            "Send", "إرسال", "Write a comment...", "اكتب تعليقاً...",
            "اكتب رداً...", "Write a reply...", "أهم التعليقات",
            "Top comments", "Public", "عام", "Follow", "متابعة"
        )
    }

    data class ScanResult(
        val newComments: List<CommentItem>,
        val clickedButtonType: String?,
        val scrolled: Boolean
    )

    // Keep track of recently clicked buttons to avoid repeated rapid clicking on same node
    private val clickedButtonHistory = mutableMapOf<String, Long>()

    fun reset() {
        clickedButtonHistory.clear()
    }

    /**
     * Crawls the current window root node.
     * 1. Looks for "All Comments" switcher
     * 2. Looks for "View Replies" buttons to expand nested replies
     * 3. Looks for "View More Comments" buttons to expand main comments
     * 4. Extracts all visible comment & reply nodes
     */
    fun analyzeAndAct(
        rootNode: AccessibilityNodeInfo?,
        sessionId: Long,
        seenSignatures: Set<String>,
        autoExpandReplies: Boolean
    ): ScanResult {
        if (rootNode == null) {
            return ScanResult(emptyList(), null, false)
        }

        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        flattenTree(rootNode, allNodes)

        // 1. Try to switch to "All comments" if dropdown / button found
        val allCommentsBtn = findButtonMatching(allNodes, ALL_COMMENTS_PATTERNS)
        if (allCommentsBtn != null && shouldClick(allCommentsBtn)) {
            val clicked = clickNode(allCommentsBtn)
            if (clicked) {
                recordClick(allCommentsBtn)
                return ScanResult(emptyList(), "عرض كل التعليقات", false)
            }
        }

        // 2. Look for reply expand buttons first so replies are visible before scrolling past them!
        if (autoExpandReplies) {
            val replyBtn = findButtonMatching(allNodes, MORE_REPLIES_PATTERNS)
            if (replyBtn != null && shouldClick(replyBtn)) {
                val clicked = clickNode(replyBtn)
                if (clicked) {
                    recordClick(replyBtn)
                    return ScanResult(emptyList(), "عرض الردود", false)
                }
            }
        }

        // 3. Look for "View more comments" / "Previous comments" button
        val moreCommentsBtn = findButtonMatching(allNodes, MORE_COMMENTS_PATTERNS)
        if (moreCommentsBtn != null && shouldClick(moreCommentsBtn)) {
            val clicked = clickNode(moreCommentsBtn)
            if (clicked) {
                recordClick(moreCommentsBtn)
                return ScanResult(emptyList(), "عرض المزيد من التعليقات", false)
            }
        }

        // 4. Extract visible comments and replies
        val extractedComments = extractCommentsFromNodes(allNodes, sessionId, seenSignatures)

        return ScanResult(extractedComments, null, false)
    }

    /**
     * Traverses the node list and parses comment blocks.
     */
    private fun extractCommentsFromNodes(
        nodes: List<AccessibilityNodeInfo>,
        sessionId: Long,
        seenSignatures: Set<String>
    ): List<CommentItem> {
        val results = mutableListOf<CommentItem>()

        // Group candidate text nodes with their bounds and properties
        data class TextCandidate(
            val text: String,
            val bounds: Rect,
            val isClickable: Boolean,
            val className: String
        )

        val textCandidates = mutableListOf<TextCandidate>()

        for (node in nodes) {
            val text = (node.text ?: node.contentDescription)?.toString()?.trim()
            if (!text.isNullOrBlank() && text.length > 1 && !isIgnoredText(text)) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (rect.width() > 0 && rect.height() > 0) {
                    textCandidates.add(
                        TextCandidate(
                            text = text,
                            bounds = rect,
                            isClickable = node.isClickable,
                            className = node.className?.toString() ?: ""
                        )
                    )
                }
            }
        }

        // Sort by vertical position (top to bottom)
        textCandidates.sortBy { it.bounds.top }

        // Find comment pairs (Author -> Content)
        var lastParentAuthor = ""
        var lastParentSignature = ""
        var minLeftIndent = Int.MAX_VALUE

        // Calculate baseline left indent to identify nested replies
        for (c in textCandidates) {
            if (c.bounds.left > 0 && c.bounds.left < minLeftIndent) {
                minLeftIndent = c.bounds.left
            }
        }

        var i = 0
        while (i < textCandidates.size) {
            val current = textCandidates[i]

            // Check if current candidate looks like an author (usually shorter, distinct line)
            // and the next candidate is the comment content
            if (i + 1 < textCandidates.size) {
                val next = textCandidates[i + 1]

                // Determine if this is a comment block
                val isNearbyVertically = (next.bounds.top - current.bounds.bottom) in -15..80
                val currentText = current.text
                val nextText = next.text

                val isAuthorCandidate = currentText.length in 2..50 &&
                        !isLikelyBodyText(currentText) &&
                        !isActionText(currentText)

                if (isNearbyVertically && isAuthorCandidate && !isActionText(nextText)) {
                    val isReply = (current.bounds.left - minLeftIndent) > 50 ||
                            (current.bounds.width() < next.bounds.width() * 0.85 && current.bounds.left > minLeftIndent)

                    val signature = if (isReply && lastParentSignature.isNotEmpty()) {
                        "REPLY|||${currentText.trim()}|||${nextText.trim()}|||PARENT|||$lastParentSignature"
                    } else {
                        "${currentText.trim()}|||${nextText.trim()}"
                    }

                    if (!seenSignatures.contains(signature) && results.none { it.signature == signature }) {
                        val commentItem = CommentItem(
                            sessionId = sessionId,
                            author = currentText.trim(),
                            content = nextText.trim(),
                            isReply = isReply,
                            parentAuthor = if (isReply) lastParentAuthor else "",
                            parentCommentSignature = if (isReply) lastParentSignature else "",
                            signature = signature,
                            timestamp = System.currentTimeMillis()
                        )
                        results.add(commentItem)

                        if (!isReply) {
                            lastParentAuthor = currentText.trim()
                            lastParentSignature = signature
                        }
                    }
                    i += 2
                    continue
                }
            }

            // Single block comment (where author and text are merged, e.g. "أحمد: تعليق جميل")
            if (current.text.contains(":") || current.text.contains("\n")) {
                val parts = if (current.text.contains("\n")) {
                    current.text.split("\n", limit = 2)
                } else {
                    current.text.split(":", limit = 2)
                }

                if (parts.size == 2 && parts[0].trim().length in 2..45 && parts[1].trim().length > 1) {
                    val author = parts[0].trim()
                    val content = parts[1].trim()

                    val isReply = (current.bounds.left - minLeftIndent) > 50
                    val signature = if (isReply && lastParentSignature.isNotEmpty()) {
                        "REPLY|||$author|||$content|||PARENT|||$lastParentSignature"
                    } else {
                        "$author|||$content"
                    }

                    if (!seenSignatures.contains(signature) && results.none { it.signature == signature }) {
                        val commentItem = CommentItem(
                            sessionId = sessionId,
                            author = author,
                            content = content,
                            isReply = isReply,
                            parentAuthor = if (isReply) lastParentAuthor else "",
                            parentCommentSignature = if (isReply) lastParentSignature else "",
                            signature = signature,
                            timestamp = System.currentTimeMillis()
                        )
                        results.add(commentItem)

                        if (!isReply) {
                            lastParentAuthor = author
                            lastParentSignature = signature
                        }
                    }
                }
            }

            i++
        }

        return results
    }

    private fun isIgnoredText(text: String): Boolean {
        if (IGNORED_TEXTS.contains(text.trim())) return true
        // Match timestamps like "10 د", "2 س", "3d", "5h", "1w"
        if (text.matches(Regex("^\\d+\\s*(?:د|س|ي|أ|h|m|d|w|min|hr|sec)$", RegexOption.IGNORE_CASE))) return true
        return false
    }

    private fun isActionText(text: String): Boolean {
        return isIgnoredText(text) ||
                MORE_COMMENTS_PATTERNS.any { it.matcher(text).find() } ||
                MORE_REPLIES_PATTERNS.any { it.matcher(text).find() } ||
                ALL_COMMENTS_PATTERNS.any { it.matcher(text).find() }
    }

    private fun isLikelyBodyText(text: String): Boolean {
        // More than 10 words or ends with full stop/question mark usually indicates comment body, not author name
        val words = text.split("\\s+".toRegex())
        return words.size > 8 || text.endsWith(".") || text.endsWith("؟") || text.endsWith("!")
    }

    private fun findButtonMatching(
        nodes: List<AccessibilityNodeInfo>,
        patterns: List<Pattern>
    ): AccessibilityNodeInfo? {
        for (node in nodes) {
            val text = (node.text ?: node.contentDescription)?.toString() ?: continue
            for (p in patterns) {
                if (p.matcher(text).find()) {
                    // Find the clickable parent or the node itself
                    return findClickableNode(node) ?: node
                }
            }
        }
        return null
    }

    private fun findClickableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun shouldClick(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val key = "${node.text}_${rect.left}_${rect.top}"
        val lastClicked = clickedButtonHistory[key] ?: 0L
        val now = System.currentTimeMillis()
        // Wait at least 2500ms before clicking the same button at the same position again
        return (now - lastClicked) > 2500L
    }

    private fun recordClick(node: AccessibilityNodeInfo) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val key = "${node.text}_${rect.left}_${rect.top}"
        clickedButtonHistory[key] = System.currentTimeMillis()
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        val target = findClickableNode(node) ?: node
        return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Performs a scroll down gesture or action.
     */
    fun performScroll(service: AccessibilityService, rootNode: AccessibilityNodeInfo?): Boolean {
        // First try standard action on scrollable container
        if (rootNode != null) {
            val scrollableNodes = mutableListOf<AccessibilityNodeInfo>()
            findScrollableNodes(rootNode, scrollableNodes)
            for (scrollable in scrollableNodes) {
                val scrolled = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                if (scrolled) return true
            }
        }

        // Fallback to gesture swipe
        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        val startX = screenWidth / 2f
        val startY = screenHeight * 0.72f
        val endX = screenWidth / 2f
        val endY = screenHeight * 0.32f

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 450))
            .build()

        return service.dispatchGesture(gesture, null, null)
    }

    private fun findScrollableNodes(node: AccessibilityNodeInfo, outList: MutableList<AccessibilityNodeInfo>) {
        if (node.isScrollable) {
            outList.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findScrollableNodes(child, outList)
        }
    }

    private fun flattenTree(node: AccessibilityNodeInfo, outList: MutableList<AccessibilityNodeInfo>) {
        outList.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            flattenTree(child, outList)
        }
    }
}
