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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CrawlerStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRose
import com.example.ui.theme.FbBlue
import com.example.ui.theme.FbBlueDark
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val crawlerState by viewModel.crawlerState.collectAsState()
    val isOverlayGranted by viewModel.isOverlayGranted.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(FbBlueDark, FbBlue, AccentCyan)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Facebook Comment Loader",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "استخراج وتجميع التعليقات والردود تلقائياً عبر النافذة العائمة",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f)
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.checkPermissions(context) },
                        modifier = Modifier
                            .testTag("refresh_permissions_button")
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث الصلاحيات",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Permissions Section
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("permissions_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "صلاحيات التشغيل المطلوبة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "يعمل التطبيق بدون أي حساب أو كلمة مرور، فقط يحتاج هاتين الصلاحيتين:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Accessibility Permission Card
                PermissionItem(
                    icon = Icons.Default.AccessibilityNew,
                    title = "خدمة إمكانية الوصول (Accessibility)",
                    description = "لقراءة التعليقات والردود على الشاشة والتمرير التلقائي",
                    isGranted = isAccessibilityEnabled,
                    actionText = if (isAccessibilityEnabled) "مفعّلة بنجاح ✓" else "تفعيل الخدمة الآن",
                    onActionClick = { viewModel.openAccessibilitySettings(context) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Overlay Permission Card
                PermissionItem(
                    icon = Icons.Default.Layers,
                    title = "الظهور فوق التطبيقات (Floating Overlay)",
                    description = "لعرض أزرار التشغيل والإيقاف وعداد التعليقات فوق فيسبوك",
                    isGranted = isOverlayGranted,
                    actionText = if (isOverlayGranted) "ممنوحة بنجاح ✓" else "منح الإذن الآن",
                    onActionClick = { viewModel.openOverlaySettings(context) }
                )
            }
        }

        // Overlay Controls Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("overlay_controls_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "النافذة العائمة فوق فيسبوك",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "شغّل النافذة العائمة ثم انتقل لأي منشور فيسبوك لبدء جمع كل تعليقاته وردوده",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.startOverlayService(context) },
                        enabled = isOverlayGranted,
                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_overlay_button")
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تشغيل النافذة", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.stopOverlayService(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stop_overlay_button")
                    ) {
                        Text("إخفاء النافذة", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.launchFacebookApp(context) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_facebook_button")
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فتح فيسبوك الآن", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Live Crawler State Box
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("crawler_status_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حالة الجلسة الحالية",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    val statusColor = when (crawlerState.status) {
                        CrawlerStatus.RUNNING -> AccentGreen
                        CrawlerStatus.PAUSED -> AccentRose
                        CrawlerStatus.FINISHED -> AccentCyan
                        CrawlerStatus.IDLE -> Color.Gray
                    }
                    val statusText = when (crawlerState.status) {
                        CrawlerStatus.RUNNING -> "جاري العمل..."
                        CrawlerStatus.PAUSED -> "متوقف مؤقتاً"
                        CrawlerStatus.FINISHED -> "مكتمل"
                        CrawlerStatus.IDLE -> "جاهز"
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Counters Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CounterBox(
                        title = "التعليقات المسجلة",
                        count = crawlerState.commentsCount,
                        color = AccentCyan,
                        modifier = Modifier.weight(1f)
                    )
                    CounterBox(
                        title = "الردود المسجلة",
                        count = crawlerState.repliesCount,
                        color = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Last Comment Preview
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "آخر تعليق تم تسجيله لحظياً:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (crawlerState.lastDetectedAuthor.isNotBlank()) {
                            Text(
                                text = crawlerState.lastDetectedAuthor,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "\"${crawlerState.lastDetectedText}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = "لا يوجد تعليقات بعد. اضغط تشغيل عند فتح منشور فيسبوك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // In-App Start / Stop Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.startExtraction() },
                        enabled = crawlerState.status != CrawlerStatus.RUNNING,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("in_app_start_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بدء الاستخراج")
                    }

                    Button(
                        onClick = { viewModel.stopExtraction() },
                        enabled = crawlerState.status == CrawlerStatus.RUNNING,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("in_app_stop_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إيقاف")
                    }
                }
            }
        }

        // Settings / Crawl Tuning Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إعدادات الاستخراج والتمرير",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scroll Delay Slider
                Text(
                    text = "فترة الانتظار بعد كل تمريرة / ضغطة: ${(crawlerState.scrollDelayMs / 1000f)} ثانية",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "يساعد زيادة الانتظار في منح فيسبوك الوقت الكافي لتحميل جميع التعليقات والردود خاصة مع الاتصال الضعيف.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = crawlerState.scrollDelayMs.toFloat(),
                    onValueChange = { viewModel.updateScrollDelay(it.toLong()) },
                    valueRange = 800f..3500f,
                    steps = 26,
                    colors = SliderDefaults.colors(thumbColor = FbBlue, activeTrackColor = FbBlue),
                    modifier = Modifier.testTag("scroll_delay_slider")
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Auto Expand Replies Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "البحث عن أزرار الردود وفتحها تلقائياً",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "الضغط على 'عرض الردود' و'عرض المزيد من الردود' قبل التمرير للأسفل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = crawlerState.isAutoExpandReplies,
                        onCheckedChange = { viewModel.toggleAutoExpandReplies(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = FbBlue, checkedTrackColor = FbBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("auto_expand_replies_switch")
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionText: String,
    onActionClick: () -> Unit
) {
    Surface(
        color = if (isGranted) AccentGreen.copy(alpha = 0.08f) else Color.Red.copy(alpha = 0.06f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) AccentGreen.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) AccentGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) AccentGreen else AccentRose,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) AccentGreen.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(actionText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CounterBox(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}
