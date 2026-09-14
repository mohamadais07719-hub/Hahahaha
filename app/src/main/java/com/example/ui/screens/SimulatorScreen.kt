package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.FbBlue

@Composable
fun SimulatorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var showRepliesForFirstComment by remember { mutableStateOf(false) }
    var showExtraComments by remember { mutableStateOf(false) }

    var customAuthor by remember { mutableStateOf("") }
    var customContent by remember { mutableStateOf("") }
    var isCustomReply by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Card
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = FbBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "مختبر التجربة السريعة (Simulator)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "يمكنك هنا اختبار النافذة العائمة، أو حقن تعليقات تجريبية لمعاينة آليات منع التكرار، العدادات، والتصدير.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Mock Facebook Post Card
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mock_facebook_post")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Post Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(FbBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("صفحة التكنولوجيا والمعرفة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("منذ 3 ساعات · 🌐 عام", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Post Content
                Text(
                    text = "ما رأيكم في تطبيقات الذكاء الاصطناعي والأتمتة في تحسين الإنتاجية وتسهيل الأعمال اليومية؟ شاركونا تجاربكم وآراءكم في التعليقات! 👇",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Post Reactions summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, tint = FbBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("342 إعجاب", fontSize = 12.sp, color = Color.Gray)
                    }
                    Text("48 تعليق", fontSize = 12.sp, color = Color.Gray)
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                // "View All Comments" Button
                OutlinedButton(
                    onClick = {
                        showExtraComments = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_view_all_comments")
                ) {
                    Text("عرض كل التعليقات (All comments)")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mock Comment 1
                MockCommentView(
                    author = "محمد عبد الله",
                    content = "تطبيقات الأتمتة وفرت عليّ أكثر من 4 ساعات عمل أسبوعياً، خاصة في معالجة البيانات والتقارير.",
                    time = "منذ ساعة"
                )

                // "View Replies" button
                if (!showRepliesForFirstComment) {
                    Button(
                        onClick = {
                            showRepliesForFirstComment = true
                            viewModel.simulateMockComment("محمد عبد الله", "تطبيقات الأتمتة وفرت عليّ أكثر من 4 ساعات عمل أسبوعياً", false)
                            viewModel.simulateMockComment("سارة خليل", "أتفق معك تماماً، هل تستخدم بايثون أم أدوات بدون كود؟", true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(start = 24.dp, top = 4.dp)
                            .testTag("btn_view_replies")
                    ) {
                        Text("عرض الردود (View replies - 2)", color = FbBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Nested mock reply
                    MockReplyView(
                        author = "سارة خليل",
                        content = "أتفق معك تماماً، هل تستخدم بايثون أم أدوات بدون كود؟",
                        time = "منذ 45 دقيقة"
                    )
                    MockReplyView(
                        author = "محمد عبد الله",
                        content = "أستخدم مزيجاً من أدوات Google Workspace وسكربتات بايثون خفيفة.",
                        time = "منذ 30 دقيقة"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mock Comment 2
                MockCommentView(
                    author = "خالد عمر",
                    content = "الأهم هو الاستخدام المسؤول والتحقق الدائم من صحة النتائج والمخرجات.",
                    time = "منذ ساعتين"
                )

                // Extra comments when expanded
                if (showExtraComments) {
                    Spacer(modifier = Modifier.height(10.dp))
                    MockCommentView(
                        author = "ريم أحمد",
                        content = "تجربة ممتازة جداً وأتمنى توفير دورات تدريبية أكثر باللغة العربية.",
                        time = "منذ ساعتين"
                    )
                } else {
                    OutlinedButton(
                        onClick = {
                            showExtraComments = true
                            viewModel.simulateMockComment("ريم أحمد", "تجربة ممتازة جداً وأتمنى توفير دورات تدريبية أكثر باللغة العربية.", false)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("btn_view_more_comments")
                    ) {
                        Text("عرض المزيد من التعليقات (View more comments)")
                    }
                }
            }
        }

        // Manual Injection Card for fast testing
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "حقن تعليق / رد يدوي للتجربة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "أضف تعليقاً مباشرة لاختبار دمج التعليقات في الذاكرة والحفظ وتحديث النافذة العائمة لحظياً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                OutlinedTextField(
                    value = customAuthor,
                    onValueChange = { customAuthor = it },
                    label = { Text("اسم صاحب التعليق") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customContent,
                    onValueChange = { customContent = it },
                    label = { Text("نص التعليق أو الرد") },
                    singleLine = false,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = isCustomReply,
                        onCheckedChange = { isCustomReply = it }
                    )
                    Text(text = "هذا العنصر هو رد (Reply) وليس تعليقاً رئيسياً", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val author = customAuthor.ifBlank { "مستخدم تجريبي" }
                        val content = customContent.ifBlank { "تعليق تجريبي لاختبار التصدير والعد." }
                        viewModel.simulateMockComment(author, content, isCustomReply)
                        customContent = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة التعليق إلى الجلسة الحالية")
                }
            }
        }
    }
}

@Composable
private fun MockCommentView(author: String, content: String, time: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(author, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("· $time", fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(content, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MockReplyView(author: String, content: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SubdirectoryArrowRight,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier
                .padding(top = 8.dp, end = 4.dp)
                .size(16.dp)
        )
        Surface(
            color = AccentGreen.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(author, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("· $time", fontSize = 10.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(content, fontSize = 11.sp)
            }
        }
    }
}
