package com.metacommander.app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────── Business Suite data (demo preview) ─────────────────────────

data class BizPost(val text: String, val network: String, val reach: String, val engagement: String)
data class BizScheduled(val text: String, val network: String, val time: String)
data class BizMessage(val name: String, val network: String, val text: String, val time: String, val unread: Boolean)

internal object BizData {
    val posts = mutableStateListOf(
        BizPost("Eid collection is live 🎉", "Facebook", "12.4K", "820"),
        BizPost("New arrivals reel", "Instagram", "8.9K", "640")
    )
    val scheduled = mutableStateListOf(
        BizScheduled("Weekend flash sale", "Facebook", "Sat 10:00 AM"),
        BizScheduled("Customer story", "Instagram", "Mon 6:30 PM")
    )
    val inbox = mutableStateListOf(
        BizMessage("Sabbir Ahmed", "Messenger", "Price koto? Delivery ache?", "5m", true),
        BizMessage("Tania Islam", "Instagram", "Size M ache naki?", "22m", true),
        BizMessage("Mahmud Hasan", "WhatsApp", "Order confirm korte chai", "1h", false)
    )
    var composing by mutableStateOf(false)
}

private val BsBlue = Color(0xFF0866FF)

private fun networkColor(n: String): Color = when (n) {
    "Facebook" -> Color(0xFF1877F2)
    "Instagram" -> Color(0xFFE1306C)
    "WhatsApp" -> Color(0xFF25D366)
    else -> Color(0xFF0084FF)
}

// ───────────────────────── Screen ─────────────────────────

@Composable
internal fun BusinessSuiteScreen(vm: AdsViewModel, act: Actions) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Home", "Inbox", "Planner", "Insights", "Ads")
    val account = vm.accounts.firstOrNull()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = BsBlue, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("∞", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Meta Business Suite", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("${account?.name ?: "No account"} ▾", color = BsBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { vm.notice = "Search coming soon" }) { Icon(Icons.Default.Search, "Search") }
            IconButton(onClick = { vm.notice = "${BizData.inbox.count { it.unread }} new notifications" }) {
                Icon(Icons.Default.Notifications, "Notifications")
            }
            IconButton(onClick = act.business) { Icon(Icons.Default.OpenInNew, "Open full Business Suite in browser") }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { i, t ->
                FilterChip(
                    selected = tab == i, onClick = { tab = i }, label = { Text(t) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BsBlue.copy(alpha = 0.14f), selectedLabelColor = BsBlue
                    )
                )
            }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))

        when (tab) {
            0 -> BsHome(vm, onTab = { tab = it })
            1 -> BsInbox(vm)
            2 -> BsPlanner()
            3 -> BsInsights()
            else -> BsAds(vm)
        }
    }

    if (BizData.composing) ComposeDialog(vm)
}

// ───────────────────────── Home ─────────────────────────

@Composable
private fun BsHome(vm: AdsViewModel, onTab: (Int) -> Unit) {
    val name = vm.accounts.firstOrNull()?.name ?: "your business"
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column {
                Text("Good morning", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Last 7 days for $name", fontSize = 13.sp, color = MutedBs)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BsQuick("✍️", "Create post", Modifier.weight(1f)) { BizData.composing = true }
                BsQuick("📣", "Create ad", Modifier.weight(1f)) { vm.nav = 0; vm.selectedAccount = -1 }
                BsQuick("💬", "Reply inbox", Modifier.weight(1f)) { onTab(1) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                BsKpi("Reach", "48.2K", "+12%", Modifier.weight(1f))
                BsKpi("Engagement", "3.4K", "+8%", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                BsKpi("New followers", "612", "+5%", Modifier.weight(1f))
                BsKpi("Link clicks", "1.9K", "+3%", Modifier.weight(1f))
            }
        }
        item { Text("Recent posts", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) }
        items(BizData.posts) { p -> BsPostRow(p.text, p.network, "Reach ${p.reach} · Engagement ${p.engagement}") }
        item { Text("Scheduled", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) }
        items(BizData.scheduled) { s -> BsPostRow(s.text, s.network, s.time) }
    }
}

@Composable
private fun BsQuick(emoji: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        onClick = onClick, shape = shape, color = MaterialTheme.colorScheme.surface,
        modifier = modifier.soft(shape, 4.dp)
    ) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BsKpi(title: String, value: String, delta: String, modifier: Modifier) {
    CardTile(modifier = modifier) {
        Text(title, fontSize = 12.sp, color = MutedBs)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(delta, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Good)
    }
}

@Composable
private fun BsPostRow(title: String, network: String, detail: String) {
    CardTile {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, fontSize = 12.sp, color = MutedBs)
            }
            NetPill(network)
        }
    }
}

@Composable
private fun NetPill(network: String) {
    Surface(shape = RoundedCornerShape(50), color = networkColor(network)) {
        Text(
            network, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ───────────────────────── Inbox ─────────────────────────

@Composable
private fun BsInbox(vm: AdsViewModel) {
    var replyIndex by remember { mutableStateOf(-1) }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(BizData.inbox) { i, m ->
            CardTile(onClick = { BizData.inbox[i] = m.copy(unread = false); replyIndex = i }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = BsBlue, modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(m.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                m.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(m.time, fontSize = 12.sp, color = MutedBs)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NetPill(m.network)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                m.text, fontSize = 13.sp, color = MutedBs, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (m.unread) Box(Modifier.size(9.dp).background(BsBlue, CircleShape))
                }
            }
        }
    }

    if (replyIndex >= 0 && replyIndex < BizData.inbox.size) {
        val m = BizData.inbox[replyIndex]
        var reply by remember(replyIndex) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { replyIndex = -1 },
            title = { Text(m.name) },
            text = {
                Column {
                    Text("${m.network}: ${m.text}", color = MutedBs)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(reply, { reply = it }, label = { Text("Reply") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (reply.isNotBlank()) {
                        vm.notice = "Reply sent to ${m.name} (demo preview)."
                        replyIndex = -1
                    }
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { replyIndex = -1 }) { Text("Close") } }
        )
    }
}

// ───────────────────────── Planner ─────────────────────────

@Composable
private fun BsPlanner() {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            CardTile {
                Text("This week", fontWeight = FontWeight.ExtraBold)
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    days.forEachIndexed { i, d ->
                        val hit = i == 5 || i == 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(d, fontSize = 11.sp, color = MutedBs)
                            Surface(
                                shape = CircleShape,
                                color = if (hit) BsBlue else Color.Transparent,
                                modifier = Modifier.padding(top = 4.dp).size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${13 + i}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        color = if (hit) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        items(BizData.scheduled) { s -> BsPostRow(s.text, s.network, s.time) }
        item {
            Button(onClick = { BizData.composing = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("+ Schedule post", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ComposeDialog(vm: AdsViewModel) {
    var text by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var network by remember { mutableStateOf("Facebook") }
    AlertDialog(
        onDismissRequest = { BizData.composing = false },
        title = { Text("Schedule post") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(text, { text = it }, label = { Text("Write your post") }, maxLines = 4, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(time, { time = it }, label = { Text("When (e.g. Sun 9:00 PM)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Facebook", "Instagram").forEach { n ->
                        FilterChip(selected = network == n, onClick = { network = n }, label = { Text(n) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isBlank()) {
                    vm.notice = "Write the post first."
                } else {
                    BizData.scheduled.add(0, BizScheduled(text.trim(), network, time.ifBlank { "Scheduled" }))
                    BizData.composing = false
                    vm.notice = "Post scheduled (demo preview — not published to Meta)."
                }
            }) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = { BizData.composing = false }) { Text("Cancel") } }
    )
}

// ───────────────────────── Insights ─────────────────────────

@Composable
private fun BsInsights() {
    var metric by remember { mutableStateOf("Reach") }
    val series = mapOf(
        "Reach" to listOf(4f, 6f, 5f, 8f, 7f, 9f, 11f),
        "Engagement" to listOf(1f, 2f, 2f, 3f, 3f, 4f, 5f),
        "Followers" to listOf(2f, 3f, 2f, 4f, 3f, 5f, 6f)
    )
    val values = series[metric] ?: emptyList()
    val max = values.maxOrNull() ?: 1f
    val top = BizData.posts.firstOrNull()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                series.keys.forEach { k ->
                    FilterChip(selected = metric == k, onClick = { metric = k }, label = { Text(k) })
                }
            }
        }
        item {
            CardTile {
                Text("$metric, last 7 days", fontWeight = FontWeight.ExtraBold)
                Row(
                    Modifier.fillMaxWidth().height(130.dp).padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    values.forEach { y ->
                        Box(
                            Modifier.weight(1f).height((y / max * 110f).dp)
                                .background(BsBlue, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, fontSize = 11.sp, color = MutedBs) }
                }
            }
        }
        if (top != null) item {
            CardTile {
                Text("Top post", fontWeight = FontWeight.ExtraBold)
                Text(top.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                Text("Reach ${top.reach} · Engagement ${top.engagement}", fontSize = 12.sp, color = MutedBs)
            }
        }
    }
}

// ───────────────────────── Ads shortcut ─────────────────────────

@Composable
private fun BsAds(vm: AdsViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(vm.accounts) { i, a ->
            val spend = vm.spendOf(a.id)
            CardTile(onClick = { vm.nav = 0; vm.selectedAccount = i; vm.accountTab = 0 }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Pill(a.active)
                }
                Row(Modifier.padding(top = 10.dp)) {
                    Mini("Spent today", usd(spend), Modifier.weight(1f))
                    Mini("Limit left", usd(a.limit - spend), Modifier.weight(1f))
                }
            }
        }
        item {
            Button(onClick = { vm.nav = 0; vm.selectedAccount = -1 }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Open in Meta Ads Manager", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// The muted colour used across this file (same value as MetaBlue's neighbour in MainActivity theme).
private val MutedBs = Color(0xFF64748B)
