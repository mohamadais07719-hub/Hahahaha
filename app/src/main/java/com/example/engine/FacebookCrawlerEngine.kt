package com.example.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.CommentItem
import java.util.Locale
import java.util.regex.Pattern

class FacebookCrawlerEngine {

    companion object {
        private const val TAG = "FBCrawlerEngine"

        // Arabic Eastern digits (٠-٩) and Western digits (0-9)
        private const val DIGITS = "[0-9\\u0660-\\u0669]+"

        // Regex patterns for "Switch to All Comments"
        private val ALL_COMMENTS_PATTERNS = listOf(
            Pattern.compile("عرض كل التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("جميع التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("كل التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("الأكثر ملاءمة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("أحدث التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("All comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View all comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Most relevant", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Newest comments", Pattern.CASE_INSENSITIVE)
        )

        // Regex patterns for "Load More Comments" / "Previous Comments"
        private val MORE_COMMENTS_PATTERNS = listOf(
            Pattern.compile("عرض المزيد من التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض مزيد من التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض التعليقات السابقة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض تعليقات سابقة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض تعليقات إضافية", Pattern.CASE_INSENSITIVE),
            Pattern.compile("المزيد من التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("تعليقات سابقة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("التعليقات السابقة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض\\s+$DIGITS\\s+(?:من\\s+)?التعليقات", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View more comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View previous comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("See previous comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("See more comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("More comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Previous comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Load more comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Load previous comments", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View\\s+$DIGITS\\s+(?:more\\s+)?comments", Pattern.CASE_INSENSITIVE)
        )

        // Regex patterns for "View Replies" / "More Replies"
        private val MORE_REPLIES_PATTERNS = listOf(
            Pattern.compile("عرض الردود", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض المزيد من الردود", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض مزيد من الردود", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض الردود السابقة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض ردود سابقة", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض ردود إضافية", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض ردود أخرى", Pattern.CASE_INSENSITIVE),
            Pattern.compile("عرض\\s+$DIGITS\\s+(?:من\\s+)?(?:الردود|رد(?:ود)?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("رد واحد", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ردان", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ردان اثنان", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^$DIGITS\\s+رد(?:ود)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("رد من\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View replies", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View more replies", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View previous replies", Pattern.CASE_INSENSITIVE),
            Pattern.compile("View\\s+$DIGITS\\s+(?:more\\s+)?repl(?:y|ies)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^$DIGITS\\s+repl(?:y|ies)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("1 reply", Pattern.CASE_INSENSITIVE),
            Pattern.compile("2 replies", Pattern.CASE_INSENSITIVE)
        )

        // Badges and labels commonly found next to names in Facebook that shouldn't be treated as comment text
        private val FB_BADGES = setOf(
            "متابع مميز", "متابع نشط", "مؤلف", "المؤلف", "مسؤول", "المسؤول", "مشرف",
            "Top Fan", "Author", "Admin", "Moderator", "Follow", "متابعة", "محرر"
        )

        // Ignored UI actions / buttons
        private val IGNORED_TEXTS = setOf(
            "إعجاب", "أعجبني", "رد", "مشاركة", "نسخ", "إبلاغ", "حذف", "تعديل",
            "Like", "Reply", "Share", "Copy", "Report", "Delete", "Edit",
            "Send", "إرسال", "Write a comment...", "اكتب تعليقاً...",
            "اكتب رداً...", "Write a reply...", "أهم التعليقات",
            "Top comments", "Public", "عام", "Follow", "متابعة",
            "التعليقات", "Comments", "أعجبني هذا التعليق", "تفاعل", "التفاعلات"
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
        service: AccessibilityService,
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
            val text = (allCommentsBtn.text ?: allCommentsBtn.contentDescription)?.toString() ?: "كل التعليقات"
            val clicked = clickNodeRobust(service, allCommentsBtn)
            if (clicked) {
                recordClick(allCommentsBtn)
                Log.d(TAG, "Clicked All-Comments switcher: $text")
                return ScanResult(emptyList(), "تبديل إلى ($text)", false)
            }
        }

        // 2. Look for reply expand buttons first so replies are visible before scrolling past them!
        if (autoExpandReplies) {
            val replyBtn = findButtonMatching(allNodes, MORE_REPLIES_PATTERNS)
            if (replyBtn != null && shouldClick(replyBtn)) {
                val text = (replyBtn.text ?: replyBtn.contentDescription)?.toString() ?: "عرض الردود"
                val clicked = clickNodeRobust(service, replyBtn)
                if (clicked) {
                    recordClick(replyBtn)
                    Log.d(TAG, "Clicked Reply expander: $text")
                    return ScanResult(emptyList(), "عرض الردود ($text)", false)
                }
            }
        }

        // 3. Look for "View more comments" / "Previous comments" button
        val moreCommentsBtn = findButtonMatching(allNodes, MORE_COMMENTS_PATTERNS)
        if (moreCommentsBtn != null && shouldClick(moreCommentsBtn)) {
            val text = (moreCommentsBtn.text ?: moreCommentsBtn.contentDescription)?.toString() ?: "عرض المزيد"
            val clicked = clickNodeRobust(service, moreCommentsBtn)
            if (clicked) {
                recordClick(moreCommentsBtn)
                Log.d(TAG, "Clicked More-Comments expander: $text")
                return ScanResult(emptyList(), "توسيع ($text)", false)
            }
        }

        // 4. Extract visible comments and replies using multi-strategy parser
        val extractedComments = extractCommentsFromNodes(allNodes, sessionId, seenSignatures)

        return ScanResult(extractedComments, null, false)
    }

    /**
     * Traverses the node list and parses comment blocks using multi-strategy extraction:
     * Strategy 1: Facebook Composite contentDescription ("X commented: Y...")
     * Strategy 2: Single node multiline blocks (Author\n[Badge]\nContent...)
     * Strategy 3: Nearby sibling nodes (Author node followed by Content node)
     */
    private fun extractCommentsFromNodes(
        nodes: List<AccessibilityNodeInfo>,
        sessionId: Long,
        seenSignatures: Set<String>
    ): List<CommentItem> {
        val results = mutableListOf<CommentItem>()

        data class RawItem(
            val node: AccessibilityNodeInfo,
            val text: String,
            val bounds: Rect
        )

        val rawItems = mutableListOf<RawItem>()

        // 1. Collect all non-empty text/contentDescription nodes
        for (node in nodes) {
            val text = (node.text ?: node.contentDescription)?.toString()?.trim()
            if (!text.isNullOrBlank() && text.length > 1) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (rect.width() > 0 && rect.height() > 0) {
                    rawItems.add(RawItem(node, text, rect))
                }
            }
        }

        if (rawItems.isEmpty()) return emptyList()

        // Sort items by vertical position on screen
        rawItems.sortBy { it.bounds.top }

        // Find baseline left margin to identify indented replies
        var minLeftIndent = Int.MAX_VALUE
        for (item in rawItems) {
            if (item.bounds.left in 1 until minLeftIndent && item.bounds.width() > 100) {
                minLeftIndent = item.bounds.left
            }
        }
        if (minLeftIndent == Int.MAX_VALUE) minLeftIndent = 0

        var lastParentAuthor = ""
        var lastParentSignature = ""

        // =========================================================================
        // STRATEGY 1 & 2: Single-node composite extraction (FB Accessibility strings)
        // =========================================================================
        val handledItemIndices = mutableSetOf<Int>()

        for ((index, item) in rawItems.withIndex()) {
            val text = item.text

            // Pattern A: "فلان علّق: محتوى التعليق" or "X commented: Content"
            val commentMatchAr = Regex("^(.*?)\\s+(?:علّق|علقت|كتب|كتبت)[:\\s]+(.*)", RegexOption.DOT_MATCHES_ALL).find(text)
            val commentMatchEn = Regex("^(.*?)\\s+(?:commented|replied)[:\\s]+(.*)", RegexOption.DOT_MATCHES_ALL).find(text)

            val matched = commentMatchAr ?: commentMatchEn
            if (matched != null) {
                val author = cleanAuthorName(matched.groupValues[1])
                var content = matched.groupValues[2].trim()

                // Clean tail metadata (e.g., ", منذ ساعتين، 4 تفاعلات")
                content = cleanCommentTail(content)

                if (isValidAuthor(author) && content.isNotBlank()) {
                    val isReply = (item.bounds.left - minLeftIndent) > 35 || text.contains("replied") || text.contains("رد على")
                    val signature = generateSignature(author, content, isReply, lastParentSignature)

                    if (!seenSignatures.contains(signature) && results.none { it.signature == signature }) {
                        val comment = CommentItem(
                            sessionId = sessionId,
                            author = author,
                            content = content,
                            isReply = isReply,
                            parentAuthor = if (isReply) lastParentAuthor else "",
                            parentCommentSignature = if (isReply) lastParentSignature else "",
                            signature = signature,
                            timestamp = System.currentTimeMillis()
                        )
                        results.add(comment)
                        if (!isReply) {
                            lastParentAuthor = author
                            lastParentSignature = signature
                        }
                    }
                    handledItemIndices.add(index)
                    continue
                }
            }

            // Pattern B: Multiline node where first line is Author and subsequent is Content
            if (text.contains("\n")) {
                val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                if (lines.size >= 2) {
                    val firstLine = lines[0]
                    if (isValidAuthor(firstLine) && !isIgnoredOrBadge(firstLine)) {
                        // Find the first line that represents the actual comment text (skip badges & time)
                        val contentLines = lines.drop(1).filter { line ->
                            !isIgnoredOrBadge(line) && !isTimestampText(line) && !isActionText(line)
                        }

                        if (contentLines.isNotEmpty()) {
                            val author = cleanAuthorName(firstLine)
                            val content = contentLines.joinToString(" ")

                            if (isValidAuthor(author) && content.isNotBlank()) {
                                val isReply = (item.bounds.left - minLeftIndent) > 35
                                val signature = generateSignature(author, content, isReply, lastParentSignature)

                                if (!seenSignatures.contains(signature) && results.none { it.signature == signature }) {
                                    val comment = CommentItem(
                                        sessionId = sessionId,
                                        author = author,
                                        content = content,
                                        isReply = isReply,
                                        parentAuthor = if (isReply) lastParentAuthor else "",
                                        parentCommentSignature = if (isReply) lastParentSignature else "",
                                        signature = signature,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    results.add(comment)
                                    if (!isReply) {
                                        lastParentAuthor = author
                                        lastParentSignature = signature
                                    }
                                }
                                handledItemIndices.add(index)
                                continue
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // STRATEGY 3: Proximity Sibling nodes (Separate Author and Content nodes)
        // =========================================================================
        var i = 0
        while (i < rawItems.size) {
            if (handledItemIndices.contains(i)) {
                i++
                continue
            }

            val current = rawItems[i]
            val currentText = current.text

            // Check if current item is a valid candidate for Author name
            if (isValidAuthor(currentText) && !isIgnoredOrBadge(currentText) && !isActionText(currentText)) {
                // Look ahead up to 4 nodes to find the matching content (skipping badges, times, etc.)
                var foundContentIndex = -1
                for (k in (i + 1)..minOf(i + 4, rawItems.size - 1)) {
                    if (handledItemIndices.contains(k)) continue
                    val candidate = rawItems[k]
                    val candidateText = candidate.text

                    // If we encounter another author name or action button, break
                    if (isActionText(candidateText) || isIgnoredOrBadge(candidateText) || isTimestampText(candidateText)) {
                        continue
                    }

                    // Vertical distance check: content should be close to author (< 220px)
                    val verticalDistance = candidate.bounds.top - current.bounds.bottom
                    if (verticalDistance in -20..220 && candidateText.length > 1) {
                        foundContentIndex = k
                        break
                    }
                }

                if (foundContentIndex != -1) {
                    val contentItem = rawItems[foundContentIndex]
                    val author = cleanAuthorName(currentText)
                    val content = contentItem.text.trim()

                    val isReply = (current.bounds.left - minLeftIndent) > 35
                    val signature = generateSignature(author, content, isReply, lastParentSignature)

                    if (!seenSignatures.contains(signature) && results.none { it.signature == signature }) {
                        val comment = CommentItem(
                            sessionId = sessionId,
                            author = author,
                            content = content,
                            isReply = isReply,
                            parentAuthor = if (isReply) lastParentAuthor else "",
                            parentCommentSignature = if (isReply) lastParentSignature else "",
                            signature = signature,
                            timestamp = System.currentTimeMillis()
                        )
                        results.add(comment)
                        if (!isReply) {
                            lastParentAuthor = author
                            lastParentSignature = signature
                        }
                    }

                    handledItemIndices.add(i)
                    handledItemIndices.add(foundContentIndex)
                    i = foundContentIndex + 1
                    continue
                }
            }

            i++
        }

        return results
    }

    private fun generateSignature(
        author: String,
        content: String,
        isReply: Boolean,
        parentSignature: String
    ): String {
        val cleanAuth = author.trim()
        val cleanCont = content.trim()
        return if (isReply && parentSignature.isNotBlank()) {
            "REPLY|||$cleanAuth|||$cleanCont|||$parentSignature"
        } else {
            "$cleanAuth|||$cleanCont"
        }
    }

    private fun cleanAuthorName(raw: String): String {
        return raw.trim()
            .replace(Regex("^(?:رد من|من|By)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[:·•].*"), "")
            .trim()
    }

    private fun cleanCommentTail(content: String): String {
        // Strip out trailing things like "· منذ ساعتين · أعجبني · رد"
        return content
            .replace(Regex("·\\s*\\d+\\s*(?:س|د|ي|أ|h|m|d|w).*$"), "")
            .replace(Regex(",\\s*منذ.*$"), "")
            .replace(Regex(",\\s*\\d+\\s*hours? ago.*$", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun isValidAuthor(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length !in 2..45) return false
        if (isIgnoredOrBadge(trimmed)) return false
        if (isActionText(trimmed)) return false
        if (isTimestampText(trimmed)) return false
        // Authors rarely contain punctuation like full stops or question marks
        if (trimmed.contains("?") || trimmed.contains("؟") || trimmed.endsWith(".")) return false
        // Authors don't have too many words
        val words = trimmed.split("\\s+".toRegex())
        return words.size in 1..6
    }

    private fun isIgnoredOrBadge(text: String): Boolean {
        val trimmed = text.trim()
        if (IGNORED_TEXTS.contains(trimmed)) return true
        if (FB_BADGES.contains(trimmed)) return true
        return false
    }

    private fun isTimestampText(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.startsWith("منذ") || trimmed.endsWith("ago")) return true
        if (trimmed.matches(Regex("^$DIGITS\\s*(?:د|س|ي|أ|h|m|d|w|min|hr|sec|دقيقة|ساعة|يوم|أسبوع)$", RegexOption.IGNORE_CASE))) return true
        return false
    }

    private fun isActionText(text: String): Boolean {
        val trimmed = text.trim()
        if (IGNORED_TEXTS.contains(trimmed)) return true
        return MORE_COMMENTS_PATTERNS.any { it.matcher(trimmed).find() } ||
                MORE_REPLIES_PATTERNS.any { it.matcher(trimmed).find() } ||
                ALL_COMMENTS_PATTERNS.any { it.matcher(trimmed).find() }
    }

    private fun findButtonMatching(
        nodes: List<AccessibilityNodeInfo>,
        patterns: List<Pattern>
    ): AccessibilityNodeInfo? {
        for (node in nodes) {
            val text = (node.text ?: node.contentDescription)?.toString()?.trim() ?: continue
            for (p in patterns) {
                if (p.matcher(text).find()) {
                    return node
                }
            }
        }
        return null
    }

    private fun shouldClick(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val key = "${node.text ?: node.contentDescription}_${rect.left}_${rect.top}"
        val lastClicked = clickedButtonHistory[key] ?: 0L
        val now = System.currentTimeMillis()
        // Wait at least 2500ms before clicking the same button at the same position again
        return (now - lastClicked) > 2500L
    }

    private fun recordClick(node: AccessibilityNodeInfo) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val key = "${node.text ?: node.contentDescription}_${rect.left}_${rect.top}"
        clickedButtonHistory[key] = System.currentTimeMillis()
    }

    /**
     * Robust clicking on Facebook Litho/React-Native components:
     * 1. Tries standard Accessibility ACTION_CLICK on the node or its parents
     * 2. Also dispatches a real Touch Tap Gesture at the center of the node's screen bounds!
     */
    fun clickNodeRobust(
        service: AccessibilityService,
        node: AccessibilityNodeInfo
    ): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        var standardClicked = false
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                standardClicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (standardClicked) break
            }
            current = current.parent
        }

        // Always also perform a direct Tap gesture at the exact center of the button!
        // This guarantees that Facebook's Litho touch-listeners receive the press event!
        var gestureClicked = false
        if (rect.width() > 0 && rect.height() > 0) {
            val clickX = rect.centerX().toFloat()
            val clickY = rect.centerY().toFloat()

            val clickPath = Path().apply {
                moveTo(clickX, clickY)
            }
            val tapGesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(clickPath, 0, 60))
                .build()

            gestureClicked = service.dispatchGesture(tapGesture, null, null)
        }

        return standardClicked || gestureClicked
    }

    /**
     * Performs a scroll down gesture or action.
     */
    fun performScroll(service: AccessibilityService, rootNode: AccessibilityNodeInfo?): Boolean {
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
            .addStroke(GestureDescription.StrokeDescription(path, 0, 420))
            .build()

        return service.dispatchGesture(gesture, null, null)
    }

    private fun flattenTree(node: AccessibilityNodeInfo, outList: MutableList<AccessibilityNodeInfo>) {
        outList.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            flattenTree(child, outList)
        }
    }
}
