package com.metacommander.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.browser.customtabs.CustomTabsIntent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private val AppBg = Color(0xFFF8FAFC)
private val Ink = Color(0xFF0F172A)
private val MetaBlue = Color(0xFF0668E1)
private val Muted = Color(0xFF64748B)
private val SoftBorder = Color(0xFFE8EDF4)

data class AdAccount(
    val name: String, val id: String, val active: Boolean,
    val spend: Double, val limit: Double, val campaigns: Int
)
data class Campaign(val name: String, val status: Boolean, val spend: Double, val ctr: String, val cpc: String)

class AdsViewModel : ViewModel() {
    // Sample preview data only. Replace repository with authenticated Graph API responses.
    val accounts = listOf(
        AdAccount("Dhaka Fashion House", "act_1029384756", true, 248.62, 1500.0, 8),
        AdAccount("Bright Home BD", "act_2048571936", true, 186.40, 900.0, 5),
        AdAccount("Urban Wear Studio", "act_3019284756", false, 0.0, 500.0, 0)
    )
    val campaigns = listOf(
        Campaign("Ramadan Retargeting", true, 98.45, "2.84%", "$0.31"),
        Campaign("Website Conversions", true, 76.20, "1.92%", "$0.46"),
        Campaign("New Audience Test", false, 23.97, "1.21%", "$0.62")
    )
    var selectedAccount by mutableStateOf<AdAccount?>(null)
    var selectedTab by mutableIntStateOf(0)
    var bottomTab by mutableIntStateOf(0)
    var search by mutableStateOf("")
    var showBudgetDialog by mutableStateOf(false)
    var budgetText by mutableStateOf("50")
    var notice by mutableStateOf<String?>(null)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MetaAdsApp(
            onMetaLogin = { openMetaLogin() },
            onBusinessSuite = { openCustomTab("https://business.facebook.com/") },
            onPages = { openCustomTab("https://www.facebook.com/pages/") },
            onBiometric = { showBiometricPrompt() },
            onWhatsApp = { openWhatsApp() }
        ) }
    }
    private fun openMetaLogin() {
        // Configure a Meta app, OAuth redirect URI, scopes, and token exchange on a trusted backend.
        // Do not embed an app secret or long-lived access token in the APK.
        openCustomTab("https://www.facebook.com/adsmanager/")
    }
    private fun openCustomTab(url: String) {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, Uri.parse(url))
    }
    private fun openWhatsApp() {
        val text = Uri.encode("Campaign performance summary: please review today's spend and results.")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=$text")))
    }
    private fun showBiometricPrompt() {
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) return
        val executor = ContextCompat.getMainExecutor(this)
        BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {}).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock MetaCommander")
                .setSubtitle("Verify your identity to continue")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
        )
    }
}

@Composable
fun MetaAdsApp(
    onMetaLogin: () -> Unit, onBusinessSuite: () -> Unit, onPages: () -> Unit,
    onBiometric: () -> Unit, onWhatsApp: () -> Unit, vm: AdsViewModel = viewModel()
) {
    MaterialTheme(colorScheme = lightColorScheme(primary = MetaBlue, background = AppBg, surface = Color.White, onSurface = Ink)) {
        Surface(Modifier.fillMaxSize(), color = AppBg) {
            Column(Modifier.fillMaxSize()) {
                if (vm.selectedAccount == null) {
                    Header(vm)
                    if (vm.bottomTab == 0) Dashboard(vm, onMetaLogin, onBiometric)
                    else WebShortcut(vm.bottomTab, onBusinessSuite, onPages, onWhatsApp)
                } else AccountDetails(vm, vm.selectedAccount!!)
                Spacer(Modifier.weight(1f, fill = false))
                BottomDock(vm)
            }
        }
        vm.notice?.let { message ->
            AlertDialog(onDismissRequest = { vm.notice = null }, title = { Text("Notice") },
                text = { Text(message) }, confirmButton = { TextButton(onClick = { vm.notice = null }) { Text("OK") } })
        }
        if (vm.showBudgetDialog) {
            AlertDialog(onDismissRequest = { vm.showBudgetDialog = false },
                title = { Text("Update daily budget", fontWeight = FontWeight.Bold) },
                text = { OutlinedTextField(value = vm.budgetText, onValueChange = { vm.budgetText = it },
                    label = { Text("Budget (USD)") }, singleLine = true) },
                confirmButton = { TextButton(onClick = {
                    vm.showBudgetDialog = false
                    vm.notice = "Budget changes require an authenticated Graph API integration. No live change was made."
                }) { Text("Save") } },
                dismissButton = { TextButton(onClick = { vm.showBudgetDialog = false }) { Text("Cancel") } })
        }
    }
}

@Composable private fun Header(vm: AdsViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("MetaCommander", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Campaign performance at a glance", color = Muted, fontSize = 13.sp)
            }
            Surface(shape = CircleShape, color = Color.White, shadowElevation = 5.dp,
                modifier = Modifier.size(46.dp).clickable { vm.notice = "Settings and secure account setup are not configured yet." }) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MetaBlue) }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = vm.search, onValueChange = { vm.search = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Search ad accounts") }, leadingIcon = { Icon(Icons.Default.Search, null) })
    }
}

@Composable private fun Dashboard(vm: AdsViewModel, onMetaLogin: () -> Unit, onBiometric: () -> Unit) {
    val visible = vm.accounts.filter { it.name.contains(vm.search, ignoreCase = true) || it.id.contains(vm.search, ignoreCase = true) }
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Active accounts", "${vm.accounts.count { it.active }}", "Accounts", Modifier.weight(1f), Icons.Default.Business)
                MetricCard("Today's spend", "$435.02", "USD • sample", Modifier.weight(1f), Icons.Default.Payments)
            }
            Spacer(Modifier.height(10.dp))
            MetricCard("Active campaigns", "${vm.accounts.sumOf { it.campaigns }}", "Across sample accounts", Modifier.fillMaxWidth(), Icons.Default.Campaign)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ad accounts", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Ink, modifier = Modifier.weight(1f))
                Text("DEMO DATA", color = MetaBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        items(visible) { account ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable { vm.selectedAccount = account; vm.selectedTab = 0 },
                shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFEAF2FF), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Campaign, null, tint = MetaBlue) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(account.name, fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                            Text(account.id, color = Muted, fontSize = 12.sp)
                        }
                        StatusPill(account.active)
                    }
                    Spacer(Modifier.height(15.dp))
                    Row {
                        MiniValue("Today's spend", "$${"%.2f".format(account.spend)}", Modifier.weight(1f))
                        MiniValue("Remaining limit", "$${"%.2f".format(account.limit - account.spend)}", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onMetaLogin, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Login, null); Spacer(Modifier.width(8.dp)); Text("Login with Facebook / Meta Ads", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBiometric, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Fingerprint, null); Spacer(Modifier.width(6.dp)); Text("Unlock with biometrics") }
        }
    }
}

@Composable private fun MetricCard(title: String, value: String, caption: String, modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ElevatedCard(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp)) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, null, tint = MetaBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(caption, color = Muted, fontSize = 10.sp)
        }
    }
}
@Composable private fun MiniValue(label: String, value: String, modifier: Modifier) {
    Column(modifier) { Text(label, color = Muted, fontSize = 11.sp); Text(value, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}
@Composable private fun StatusPill(active: Boolean) {
    Surface(shape = RoundedCornerShape(50), color = if (active) Color(0xFFE7F8EF) else Color(0xFFFFF0F0)) {
        Text(if (active) "Active" else "Disabled", color = if (active) Color(0xFF16834A) else Color(0xFFB42318),
            fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable private fun AccountDetails(vm: AdsViewModel, account: AdAccount) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.selectedAccount = null }) { Icon(Icons.Default.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text(account.name, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(account.id, color = Muted, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Campaigns", "Billing", "Analytics").forEachIndexed { index, label ->
                Surface(shape = RoundedCornerShape(13.dp), color = if (vm.selectedTab == index) MetaBlue else Color.White,
                    shadowElevation = 2.dp, modifier = Modifier.weight(1f).clickable { vm.selectedTab = index }) {
                    Text(label, color = if (vm.selectedTab == index) Color.White else Ink, fontWeight = FontWeight.Bold,
                        fontSize = 12.sp, modifier = Modifier.padding(vertical = 13.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        when (vm.selectedTab) {
            0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                item { MetricCard("Today's spend", "$${"%.2f".format(account.spend)}", "Lifetime insights require API data", Modifier.fillMaxWidth(), Icons.Default.Payments) }
                items(vm.campaigns) { campaign ->
                    ElevatedCard(shape = RoundedCornerShape(18.dp), elevation = CardDefaults.elevatedCardElevation(4.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(campaign.name, fontWeight = FontWeight.Bold, color = Ink); Text("Spend $${campaign.spend}", color = Muted, fontSize = 12.sp) }
                                Switch(checked = campaign.status, onCheckedChange = { vm.notice = "Campaign status changes require a configured Meta Graph API token. No live change was made." })
                            }
                            Spacer(Modifier.height(8.dp))
                            Row { MiniValue("CTR", campaign.ctr, Modifier.weight(1f)); MiniValue("CPC", campaign.cpc, Modifier.weight(1f)) }
                        }
                    }
                }
                item { Button(onClick = { vm.showBudgetDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Edit daily budget") } }
            }
            1 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Account spending limit", "$${"%.2f".format(account.limit)}", "Sample threshold • not live billing data", Modifier.fillMaxWidth(), Icons.Default.AccountBalance)
                LinearProgressIndicator(progress = { (account.spend / account.limit).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                ElevatedCard(shape = RoundedCornerShape(18.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(18.dp)) { Text("Payment method", fontWeight = FontWeight.Bold); Text("••••  ••••  ••••  4242", color = Muted); Spacer(Modifier.height(12.dp)); Button(onClick = { vm.notice = "Payment actions are not supported by the Meta Ads Graph API in this demo." }, modifier = Modifier.fillMaxWidth()) { Text("Open billing settings") } }
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Reach", "—", "Connect Graph API for real insights", Modifier.fillMaxWidth(), Icons.Default.Groups)
                MetricCard("Impressions", "—", "No live analytics loaded", Modifier.fillMaxWidth(), Icons.Default.Visibility)
                MetricCard("Spend trend", "—", "Chart populates when insight series is available", Modifier.fillMaxWidth(), Icons.Default.ShowChart)
            }
        }
    }
}

@Composable private fun WebShortcut(tab: Int, onBusinessSuite: () -> Unit, onPages: () -> Unit, onWhatsApp: () -> Unit) {
    val (title, subtitle, action, icon) = when(tab) {
        1 -> listOf("Business Suite", "Manage business assets in Meta's secure web experience.", onBusinessSuite, Icons.Default.Dashboard)
        2 -> listOf("Facebook Pages", "Open your linked Pages and profiles.", onPages, Icons.Default.Flag)
        else -> listOf("WhatsApp Direct", "Send a campaign performance summary to a client.", onWhatsApp, Icons.Default.Chat)
    }
    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(36.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 7.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, null, tint = MetaBlue, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(title as String, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(subtitle as String, color = Muted)
                Spacer(Modifier.height(22.dp))
                Button(onClick = { (action as (() -> Unit))() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Open $title") }
            }
        }
    }
}

@Composable private fun BottomDock(vm: AdsViewModel) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        val items = listOf("Meta Ads", "Business", "Pages", "WhatsApp")
        val icons = listOf(Icons.Default.Campaign, Icons.Default.Dashboard, Icons.Default.Flag, Icons.Default.Chat)
        items.forEachIndexed { index, label ->
            NavigationBarItem(selected = vm.bottomTab == index, onClick = {
                if (vm.selectedAccount != null) vm.selectedAccount = null
                vm.bottomTab = index
            }, icon = { Icon(icons[index], null) }, label = { Text(label, fontSize = 10.sp) })
        }
    }
}
