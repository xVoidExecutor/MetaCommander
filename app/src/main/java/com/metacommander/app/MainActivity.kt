package com.metacommander.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.metacommander.app.data.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ───────────────────────── Theme ─────────────────────────

private val AppBg = Color(0xFFF1F5F9)
private val Ink = Color(0xFF0F172A)
private val MetaBlue = Color(0xFF0668E1)
private val Muted = Color(0xFF64748B)
private val Good = Color(0xFF16A34A)
private val Bad = Color(0xFFDC2626)
private val WaGreen = Color(0xFF128C7E)
private const val BDT_RATE = 120.0

private val LightColors = lightColorScheme(
    primary = MetaBlue, onPrimary = Color.White,
    background = AppBg, onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFE2E8F0), onSurfaceVariant = Muted,
    primaryContainer = Color(0xFFD9FDD3), onPrimaryContainer = Ink,
    secondaryContainer = Color(0xFFDCEAFE), onSecondaryContainer = Ink,
    outline = Color(0xFFCBD5E1)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4A9BFF), onPrimary = Color(0xFF04122B),
    background = Color(0xFF0B1220), onBackground = Color(0xFFE5EAF3),
    surface = Color(0xFF162033), onSurface = Color(0xFFE5EAF3),
    surfaceVariant = Color(0xFF223049), onSurfaceVariant = Color(0xFF94A3B8),
    primaryContainer = Color(0xFF1E4D3A), onPrimaryContainer = Color(0xFFE5EAF3),
    secondaryContainer = Color(0xFF1E3A66), onSecondaryContainer = Color(0xFFE5EAF3),
    outline = Color(0xFF475569)
)

/** Soft, layered white-card shadow (#0F172A at ~10%). */
private fun Modifier.soft(shape: Shape, elevation: Dp = 6.dp): Modifier =
    this.shadow(elevation, shape, clip = false, ambientColor = Ink.copy(alpha = 0.10f), spotColor = Ink.copy(alpha = 0.10f))

// ───────────────────────── Data ─────────────────────────

data class AdAccount(val name: String, val id: String, val active: Boolean, val limit: Double)
data class CampaignItem(
    val id: Int, val accountId: String, val name: String, val active: Boolean, val budget: Double,
    val cpr: Double, val ctr: Double, val cpc: Double, val spend: Double, val totalBudget: Double, val used: Double
)
data class Msg(val text: String, val mine: Boolean)
data class ClientChat(val name: String, val phone: String, val accountId: String, val unread: Int, val messages: List<Msg>)

private val toolItems = listOf(
    "Alerts" to "🔔", "Auto Rules" to "⚙️", "Client Reports" to "📑",
    "Leads Inbox" to "🧲", "Creatives" to "🎨", "Budget Planner" to "🧮",
    "Invoices" to "🧾", "Team Access" to "👥", "Settings" to "🔐"
)

class AdsViewModel : ViewModel() {
    val accounts = mutableStateListOf(
        AdAccount("Dhaka Fashion House", "act_104829371", true, 500.0),
        AdAccount("Rahim Electronics", "act_208817452", true, 300.0),
        AdAccount("Sylhet Tea Co.", "act_377120098", false, 250.0),
        AdAccount("Nila Beauty Studio", "act_459032716", true, 200.0)
    )
    val campaigns = mutableStateListOf(
        CampaignItem(1, "act_104829371", "Eid Collection – Conversions", true, 90.0, .42, 3.1, .18, 78.20, 300.0, 212.4),
        CampaignItem(2, "act_104829371", "Retargeting – Cart Viewers", true, 60.0, .31, 4.4, .12, 51.60, 200.0, 141.9),
        CampaignItem(3, "act_104829371", "Page Likes – Dhaka", false, 20.0, .09, 1.8, .05, 0.0, 100.0, 38.5),
        CampaignItem(4, "act_104829371", "Messages – WhatsApp Leads", true, 70.0, .55, 2.7, .21, 52.60, 250.0, 177.6),
        CampaignItem(5, "act_208817452", "Laptop Deals – Traffic", true, 60.0, .38, 2.9, .16, 44.50, 250.0, 160.2),
        CampaignItem(6, "act_208817452", "Phone Accessories – Sales", true, 40.0, .47, 3.4, .19, 31.20, 150.0, 88.7),
        CampaignItem(7, "act_208817452", "Brand Awareness – Reach", true, 30.0, .06, 1.2, .04, 20.40, 120.0, 64.3),
        CampaignItem(8, "act_459032716", "Bridal Makeup – Leads", true, 45.0, .52, 3.6, .22, 33.90, 180.0, 120.5),
        CampaignItem(9, "act_459032716", "Skincare – Conversions", true, 30.0, .44, 2.8, .17, 20.90, 140.0, 76.8)
    )
    val chats = mutableStateListOf(
        ClientChat("Rahim – Dhaka Fashion", "+880 1712-345678", "act_104829371", 2,
            listOf(Msg("Bhai, ad ta ajke kemon cholche?", false), Msg("Ajke ad bhalo cholche, report pathacchi.", true), Msg("Budget ektu barate chai", false))),
        ClientChat("Karim – Rahim Electronics", "+880 1819-556677", "act_208817452", 0,
            listOf(Msg("Weekly report pathiye diyechi.", true), Msg("Thanks, dekhlam. Good results!", false))),
        ClientChat("Tea Co. Office", "+880 1711-902030", "act_377120098", 1,
            listOf(Msg("Amader account disable keno?", false))),
        ClientChat("Nila Apu", "+880 1914-778899", "act_459032716", 0,
            listOf(Msg("Apnar new ad live hoyeche ✅", true), Msg("Dhonnobad!", false)))
    )

    var initialized = false
    var unlocked by mutableStateOf(false)
    var pin by mutableStateOf("")
    var firstPin by mutableStateOf<String?>(null)
    var storedHash by mutableStateOf<String?>(null)
    var pinLoaded by mutableStateOf(false)
    var nav by mutableIntStateOf(0)
    var selectedAccount by mutableIntStateOf(-1)
    var accountTab by mutableIntStateOf(0)
    var selectedTool by mutableStateOf("")
    var selectedChat by mutableIntStateOf(-1)
    var search by mutableStateOf("")
    var notice by mutableStateOf<String?>(null)
    var budgetCampaignId by mutableIntStateOf(-1)
    var budgetText by mutableStateOf("")
    var plannerDaily by mutableStateOf("25")
    var plannerDays by mutableStateOf("7")
    var newChatName by mutableStateOf("")
    var newChatPhone by mutableStateOf("")
    var showNewChat by mutableStateOf(false)
    var darkTheme by mutableStateOf(false)
    var biometricEnabled by mutableStateOf(true)
    var showBdt by mutableStateOf(false)
    val rules = mutableStateListOf(true, true, false, false)

    fun campaignsOf(accountId: String) = campaigns.filter { it.accountId == accountId }
    fun spendOf(accountId: String) = campaignsOf(accountId).sumOf { it.spend }
    fun usedOf(accountId: String) = campaignsOf(accountId).sumOf { it.used }
    fun activeCount(accountId: String) = campaignsOf(accountId).count { it.active }

    fun toggleCampaign(id: Int, on: Boolean) {
        val i = campaigns.indexOfFirst { it.id == id }
        if (i >= 0) campaigns[i] = campaigns[i].copy(active = on)
    }
    fun setBudget(id: Int, value: Double) {
        val i = campaigns.indexOfFirst { it.id == id }
        if (i >= 0) campaigns[i] = campaigns[i].copy(budget = value)
    }
    fun openChat(i: Int) {
        if (i in chats.indices) { selectedChat = i; chats[i] = chats[i].copy(unread = 0) }
    }
    fun addMessage(i: Int, text: String, mine: Boolean = true) {
        if (i in chats.indices) { val c = chats[i]; chats[i] = c.copy(messages = c.messages + Msg(text, mine)) }
    }
    fun back() {
        when {
            nav == 3 -> if (selectedChat >= 0) selectedChat = -1 else nav = 0
            nav in 1..2 -> nav = 0
            selectedTool.isNotEmpty() -> selectedTool = ""
            selectedAccount >= 0 -> selectedAccount = -1
        }
    }
}

// ───────────────────────── Storage ─────────────────────────

class AppStore(private val ctx: Context) {
    private val secure: SecurePrefs? by lazy { runCatching { SecurePrefs(ctx) }.getOrNull() }
    private val ui: SharedPreferences = ctx.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)
    fun pinHash(): String? = runCatching { secure?.getPinHash() }.getOrNull()?.takeIf { it.isNotBlank() }
    fun savePin(hash: String) { runCatching { secure?.putPinHash(hash) } }
    fun flag(key: String, default: Boolean) = ui.getBoolean(key, default)
    fun setFlag(key: String, value: Boolean) { ui.edit().putBoolean(key, value).apply() }
}

private class Actions(
    val meta: () -> Unit,
    val business: () -> Unit,
    val pages: () -> Unit,
    val billing: () -> Unit,
    val whatsApp: (String, String) -> Unit,
    val biometric: () -> Unit,
    val savePdf: (String, String, List<String>) -> Unit
)

// ───────────────────────── Activity ─────────────────────────

class MainActivity : FragmentActivity() {
    private val store by lazy { AppStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this)[AdsViewModel::class.java]
        if (!vm.initialized) {
            vm.initialized = true
            vm.darkTheme = store.flag("dark", false)
            vm.biometricEnabled = store.flag("bio", true)
            vm.showBdt = store.flag("bdt", false)
        }
        val actions = Actions(
            meta = { openUrl("https://www.facebook.com/adsmanager/") },
            business = { openUrl("https://business.facebook.com/") },
            pages = { openUrl("https://www.facebook.com/pages/") },
            billing = { openUrl("https://business.facebook.com/billing_hub/") },
            whatsApp = { phone, message -> openWhatsApp(phone, message) },
            biometric = { showBiometricPrompt(vm) },
            savePdf = { file, title, lines -> savePdf(vm, file, title, lines) }
        )
        setContent { MetaCommanderApp(vm, store, actions) }
    }

    private fun openUrl(url: String) {
        try { CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, Uri.parse(url)) }
        catch (_: Exception) {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) { }
        }
    }

    private fun openWhatsApp(phone: String, message: String) {
        val digits = phone.filter { it.isDigit() }
        val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
        try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { openUrl(uri.toString()) }
    }

    private fun showBiometricPrompt(vm: AdsViewModel) {
        val authenticators = if (Build.VERSION.SDK_INT >= 30)
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        else BiometricManager.Authenticators.BIOMETRIC_WEAK
        when (BiometricManager.from(this).canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {}
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                vm.notice = "No fingerprint or face is set up on this phone. Add one in phone Settings, or use your PIN."
                return
            }
            else -> {
                vm.notice = "Biometric unlock isn't available on this device. Please use your PIN."
                return
            }
        }
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                vm.pin = ""
                vm.unlocked = true
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED) vm.notice = errString.toString()
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock MetaCommander")
            .setSubtitle("Verify your identity")
            .setAllowedAuthenticators(authenticators)
        if (Build.VERSION.SDK_INT < 30) info.setNegativeButtonText("Use PIN")
        BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback).authenticate(info.build())
    }

    private fun savePdf(vm: AdsViewModel, fileName: String, title: String, lines: List<String>) {
        vm.notice = runCatching {
            val doc = PdfDocument()
            val body = Paint().apply { textSize = 12f; isAntiAlias = true }
            val head = Paint().apply { textSize = 18f; isFakeBoldText = true; isAntiAlias = true }
            var pageNo = 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
            var y = 64f
            page.canvas.drawText(title, 40f, y, head)
            y += 36f
            for (line in lines.flatMap { if (it.isEmpty()) listOf("") else it.chunked(85) }) {
                if (y > 800f) {
                    doc.finishPage(page)
                    pageNo++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
                    y = 64f
                }
                page.canvas.drawText(line, 40f, y, body)
                y += 20f
            }
            doc.finishPage(page)
            val stream: java.io.OutputStream
            val location: String
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("could not create file")
                stream = contentResolver.openOutputStream(uri) ?: error("could not open file")
                location = "Downloads/$fileName"
            } else {
                val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: filesDir
                val f = java.io.File(dir, fileName)
                stream = java.io.FileOutputStream(f)
                location = f.absolutePath
            }
            stream.use { doc.writeTo(it) }
            doc.close()
            "PDF saved: $location"
        }.getOrElse { "Couldn't save PDF: ${it.message}" }
    }
}

// ───────────────────────── Helpers ─────────────────────────

private fun money(v: Double) = String.format(Locale.US, "%.2f", v)
private fun usd(v: Double, bdt: Boolean = false): String =
    "\$" + money(v) + (if (bdt) "  (৳" + String.format(Locale.US, "%,.0f", v * BDT_RATE) + ")" else "")
private fun today(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
private fun hashPin(pin: String): String =
    MessageDigest.getInstance("SHA-256").digest(("metacommander:$pin").toByteArray()).joinToString("") { "%02x".format(it) }

private fun reportMessage(vm: AdsViewModel, account: AdAccount?): String {
    val sb = StringBuilder()
    sb.append("📊 *Campaign Performance Summary*\n").append(today()).append("\n\n")
    val list = if (account != null) listOf(account) else vm.accounts.toList()
    list.forEach { acc ->
        val cs = vm.campaignsOf(acc.id)
        val spend = vm.spendOf(acc.id)
        sb.append("*${acc.name}* (${if (acc.active) "Active" else "Disabled"})\n")
        sb.append("• Today's spend: ${usd(spend)}\n")
        sb.append("• Active campaigns: ${vm.activeCount(acc.id)}/${cs.size}\n")
        if (cs.isNotEmpty()) {
            sb.append("• Avg CTR: ${money(cs.map { it.ctr }.average())}% | Avg CPR: ${usd(cs.map { it.cpr }.average())}\n")
        }
        sb.append("• Remaining limit: ${usd(acc.limit - spend)}\n\n")
    }
    sb.append("— Sent via MetaCommander")
    return sb.toString()
}

// ───────────────────────── App shell ─────────────────────────

@Composable
private fun MetaCommanderApp(vm: AdsViewModel, store: AppStore, act: Actions) {
    val activity = LocalContext.current as ComponentActivity
    val dark = vm.darkTheme
    LaunchedEffect(dark) {
        val clear = android.graphics.Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(clear) else SystemBarStyle.light(clear, clear)
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    LaunchedEffect(Unit) {
        vm.storedHash = withContext(Dispatchers.IO) { store.pinHash() }
        vm.pinLoaded = true
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.notice) {
        val msg = vm.notice
        if (msg != null) {
            vm.notice = null
            snackbar.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }
    BackHandler(
        enabled = vm.unlocked && (vm.nav != 0 || vm.selectedTool.isNotEmpty() || vm.selectedAccount >= 0)
    ) { vm.back() }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { if (vm.unlocked && !imeVisible) BottomDock(vm) }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                    when {
                        !vm.unlocked -> LoginScreen(vm, store, act)
                        vm.nav == 3 -> WhatsAppScreen(vm, act)
                        vm.nav == 1 -> ShortcutScreen("Business Suite", "Manage business assets in Meta’s secure web experience.", "Open Business Suite", "🗂️", act.business)
                        vm.nav == 2 -> ShortcutScreen("Facebook Pages", "Open your linked Pages and profiles.", "Open my Pages", "📄", act.pages)
                        vm.selectedTool.isNotEmpty() -> ToolScreen(vm, store, act)
                        vm.selectedAccount >= 0 -> AccountScreen(vm, act)
                        else -> DashboardScreen(vm, act)
                    }
                }
            }
        }

        val editing = vm.campaigns.firstOrNull { it.id == vm.budgetCampaignId }
        if (editing != null) AlertDialog(
            onDismissRequest = { vm.budgetCampaignId = -1 },
            title = { Text("Edit daily budget") },
            text = {
                Column {
                    Text(editing.name, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = vm.budgetText,
                        onValueChange = { vm.budgetText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Daily budget (USD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = vm.budgetText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) vm.notice = "Enter a valid budget greater than zero."
                    else {
                        vm.setBudget(editing.id, amount)
                        vm.notice = "Daily budget updated (demo preview — not sent to Meta)."
                        vm.budgetCampaignId = -1
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { vm.budgetCampaignId = -1 }) { Text("Cancel") } }
        )

        if (vm.showNewChat) AlertDialog(
            onDismissRequest = { vm.showNewChat = false },
            title = { Text("New client chat") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(vm.newChatName, { vm.newChatName = it }, label = { Text("Client name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        vm.newChatPhone, { vm.newChatPhone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' } },
                        label = { Text("Phone (with country code)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (vm.newChatName.isBlank() || vm.newChatPhone.count { it.isDigit() } < 8) {
                        vm.notice = "Enter a client name and a valid phone number."
                    } else {
                        vm.chats.add(ClientChat(vm.newChatName.trim(), vm.newChatPhone.trim(), "", 0, emptyList()))
                        vm.openChat(vm.chats.lastIndex)
                        vm.newChatName = ""; vm.newChatPhone = ""; vm.showNewChat = false
                    }
                }) { Text("Start chat") }
            },
            dismissButton = { TextButton(onClick = { vm.showNewChat = false }) { Text("Cancel") } }
        )
    }
}

// ───────────────────────── Login ─────────────────────────

private fun submitPin(vm: AdsViewModel, store: AppStore) {
    val entered = vm.pin
    val saved = vm.storedHash
    when {
        saved != null -> {
            vm.pin = ""
            if (hashPin(entered) == saved) vm.unlocked = true else vm.notice = "Wrong PIN. Please try again."
        }
        vm.firstPin == null -> { vm.firstPin = entered; vm.pin = "" }
        entered == vm.firstPin -> {
            val h = hashPin(entered)
            store.savePin(h)
            vm.storedHash = h; vm.firstPin = null; vm.pin = ""; vm.unlocked = true
            vm.notice = "PIN saved securely on this device."
        }
        else -> { vm.firstPin = null; vm.pin = ""; vm.notice = "PINs didn't match. Please start again." }
    }
}

@Composable
private fun LoginScreen(vm: AdsViewModel, store: AppStore, act: Actions) {
    if (!vm.pinLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        val title = when {
            vm.storedHash != null -> "Enter your PIN"
            vm.firstPin == null -> "Create a 4-digit PIN"
            else -> "Confirm your PIN"
        }
        val sub = when {
            vm.storedHash != null -> "Unlock Meta Ads Manager"
            vm.firstPin == null -> "This PIN protects the app on this phone"
            else -> "Enter the same PIN once more"
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(Modifier.widthIn(max = 340.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(72.dp).soft(CircleShape)) {
                        Box(contentAlignment = Alignment.Center) { Text("∞", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = MetaBlue) }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Meta Ads Manager", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                    Text(sub, fontSize = 13.sp, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 20.dp)) {
                        repeat(4) { i ->
                            Box(Modifier.size(14.dp).background(if (i < vm.pin.length) MetaBlue else MaterialTheme.colorScheme.outline, CircleShape))
                        }
                    }
                    val press: (String) -> Unit = { d ->
                        if (vm.pin.length < 4) {
                            vm.pin += d
                            if (vm.pin.length == 4) submitPin(vm, store)
                        }
                    }
                    listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("bio", "0", "del")).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            row.forEach { key ->
                                when (key) {
                                    "bio" -> if (vm.biometricEnabled) PadKey(null, Icons.Default.Fingerprint, "Unlock with fingerprint or face", Modifier.weight(1f)) { act.biometric() }
                                             else Spacer(Modifier.weight(1f))
                                    "del" -> PadKey(null, Icons.Default.Backspace, "Delete", Modifier.weight(1f)) { vm.pin = vm.pin.dropLast(1) }
                                    else -> PadKey(key, null, key, Modifier.weight(1f)) { press(key) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val shape = RoundedCornerShape(18.dp)
                    Surface(
                        onClick = act.meta, shape = shape, color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).soft(shape)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text("∞", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MetaBlue)
                            Spacer(Modifier.width(10.dp))
                            Text("Login with Facebook / Meta Ads", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Text("Your PIN is stored encrypted on this device.", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 14.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun PadKey(label: String?, icon: ImageVector?, description: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Surface(onClick = onClick, shape = shape, color = MaterialTheme.colorScheme.surface, modifier = modifier.height(60.dp).soft(shape, 4.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (label != null) Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (icon != null) Icon(icon, description, tint = MetaBlue)
        }
    }
}

// ───────────────────────── Dashboard ─────────────────────────

@Composable
private fun DashboardScreen(vm: AdsViewModel, act: Actions) {
    val b = vm.showBdt
    val filtered = vm.accounts.withIndex().filter { it.value.name.contains(vm.search, true) || it.value.id.contains(vm.search, true) }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Meta Ads Manager", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Campaign performance at a glance", fontSize = 13.sp, color = Muted)
                }
                Surface(
                    onClick = { vm.selectedTool = "Settings" }, shape = CircleShape, color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(48.dp).soft(CircleShape, 4.dp)
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, "Profile and settings", tint = MetaBlue) } }
            }
            OutlinedTextField(
                vm.search, { vm.search = it },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true, shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Search ad accounts") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (vm.search.isNotEmpty()) IconButton(onClick = { vm.search = "" }) { Icon(Icons.Default.Close, "Clear search") } }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("Active accounts", vm.accounts.count { it.active }.toString(), "of ${vm.accounts.size} accounts", Modifier.weight(1f))
                Metric("Active campaigns", vm.campaigns.count { it.active }.toString(), "of ${vm.campaigns.size} campaigns", Modifier.weight(1f))
            }
        }
        item { Metric("Today's total spend", usd(vm.campaigns.sumOf { it.spend }, b), "Demo data • not live Meta data", Modifier.fillMaxWidth()) }
        item { Text("Tools", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
        items(toolItems.chunked(3)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (name, emoji) ->
                    val shape = RoundedCornerShape(18.dp)
                    Surface(
                        onClick = { vm.selectedTool = name }, shape = shape, color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f).height(92.dp).soft(shape, 4.dp)
                    ) {
                        Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(emoji, fontSize = 26.sp)
                            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ad accounts", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("DEMO DATA", fontSize = 11.sp, color = MetaBlue, fontWeight = FontWeight.Bold)
            }
        }
        if (filtered.isEmpty()) item { Text("No accounts match “${vm.search}”.", color = Muted, modifier = Modifier.padding(vertical = 8.dp)) }
        items(filtered, key = { it.value.id }) { iv ->
            val account = iv.value
            val spend = vm.spendOf(account.id)
            CardTile(onClick = { vm.selectedAccount = iv.index; vm.accountTab = 0 }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MetaBlue.copy(alpha = 0.12f), modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Campaign, null, tint = MetaBlue) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(account.id, fontSize = 12.sp, color = Muted)
                    }
                    Pill(account.active)
                }
                Spacer(Modifier.height(14.dp))
                Row {
                    Mini("Today's spend", usd(spend, b), Modifier.weight(1f))
                    Mini("Remaining limit", usd(account.limit - spend, b), Modifier.weight(1f))
                }
            }
        }
        item {
            val shape = RoundedCornerShape(18.dp)
            Surface(onClick = act.meta, shape = shape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).soft(shape, 4.dp)) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Login, null, tint = MetaBlue)
                    Spacer(Modifier.width(10.dp))
                    Text("Connect / open Meta Ads Manager", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ───────────────────────── Account details ─────────────────────────

@Composable
private fun AccountScreen(vm: AdsViewModel, act: Actions) {
    val a = vm.accounts.getOrNull(vm.selectedAccount) ?: return
    val b = vm.showBdt
    val cs = vm.campaignsOf(a.id)
    val spend = vm.spendOf(a.id)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.selectedAccount = -1 }) { Icon(Icons.Default.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text(a.name, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(a.id, fontSize = 12.sp, color = Muted)
            }
            Pill(a.active)
            Spacer(Modifier.width(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Campaigns", "Billing & Limit", "Analytics").forEachIndexed { i, label ->
                val shape = RoundedCornerShape(14.dp)
                val sel = vm.accountTab == i
                Surface(
                    onClick = { vm.accountTab = i }, shape = shape,
                    color = if (sel) MetaBlue else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f).height(46.dp).soft(shape, 3.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        when (vm.accountTab) {
            0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                item {
                    val cap = cs.filter { it.active }.sumOf { it.budget }
                    CardTile {
                        Text("Live budget tracker", fontWeight = FontWeight.Bold)
                        Text("${usd(spend, b)} of ${usd(cap, b)} daily budget", fontSize = 13.sp, color = Muted)
                        LinearProgressIndicator(
                            progress = { if (cap > 0) (spend / cap).toFloat().coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                        )
                        Row {
                            Mini("Today", usd(spend, b), Modifier.weight(1f))
                            Mini("Lifetime", usd(vm.usedOf(a.id), b), Modifier.weight(1f))
                            Mini("Active campaigns", "${vm.activeCount(a.id)} / ${cs.size}", Modifier.weight(1f))
                        }
                    }
                }
                if (cs.isEmpty()) item {
                    CardTile { Text("No campaigns in this account.", color = Muted) }
                }
                items(cs, key = { it.id }) { c ->
                    CardTile {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.Bold)
                                Text("Spend today ${usd(c.spend, b)}", fontSize = 12.sp, color = Muted)
                            }
                            Switch(checked = c.active, onCheckedChange = {
                                vm.toggleCampaign(c.id, it)
                                vm.notice = if (it) "Campaign ON (demo preview — not sent to Meta)." else "Campaign OFF (demo preview — not sent to Meta)."
                            })
                        }
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Mini("CPR", usd(c.cpr), Modifier.weight(1f))
                            Mini("CTR", c.ctr.toString() + "%", Modifier.weight(1f))
                            Mini("CPC", usd(c.cpc), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Text("Total budget", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(usd(c.totalBudget, b), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { (c.used / c.totalBudget).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                        Row {
                            Mini("Used", usd(c.used, b), Modifier.weight(1f))
                            Mini("Remaining", usd(c.totalBudget - c.used, b), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Daily budget", fontSize = 12.sp, color = Muted)
                                Text(usd(c.budget, b), fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(onClick = { vm.budgetCampaignId = c.id; vm.budgetText = money(c.budget) }) { Text("Edit budget") }
                        }
                    }
                }
            }
            1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                item { Metric("Current threshold", usd(50.0, b), "Sample only • not live billing", Modifier.fillMaxWidth()) }
                item {
                    CardTile {
                        Text("Payment method", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Visa •••• 4417", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Expiry 08/28 • demo data", fontSize = 12.sp, color = Muted)
                    }
                }
                item {
                    CardTile {
                        Text("Ad account limit", fontWeight = FontWeight.Bold)
                        Text("${usd(spend, b)} of ${usd(a.limit, b)} used", fontSize = 13.sp, color = Muted)
                        LinearProgressIndicator(
                            progress = { (spend / a.limit).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                        )
                        Text("Remaining: ${usd(a.limit - spend, b)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                item {
                    Button(onClick = act.billing, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Icon(Icons.Default.OpenInNew, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pay Now", fontWeight = FontWeight.Bold)
                    }
                }
                item { Text("Payments are completed on Meta’s secure billing page — this opens it in the app.", fontSize = 12.sp, color = Muted) }
            }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("Reach", "48.2K", "Sample metric", Modifier.weight(1f))
                        Metric("Impressions", "126K", "Sample metric", Modifier.weight(1f))
                    }
                }
                item {
                    CardTile {
                        Text("Spend, last 7 days", fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth().height(130.dp).padding(top = 12.dp)
                        ) {
                            listOf(22, 31, 27, 44, 38, 52, 48).forEachIndexed { index, value ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                    Box(Modifier.fillMaxWidth().height((value * 1.5).dp).background(MetaBlue, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                                    Text("MTWTFSS"[index].toString(), fontSize = 12.sp, color = Muted)
                                }
                            }
                        }
                    }
                }
                item {
                    CardTile {
                        Text("Audience by age", fontWeight = FontWeight.Bold)
                        listOf("18–24" to 34, "25–34" to 41, "35–44" to 17, "45+" to 8).forEach { (label, pct) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                                Text(label, fontSize = 13.sp, modifier = Modifier.width(52.dp))
                                LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.weight(1f))
                                Text("$pct%", fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }
                }
                item {
                    CardTile {
                        Text("Audience by gender", fontWeight = FontWeight.Bold)
                        listOf("Women" to 58, "Men" to 40, "Other" to 2).forEach { (label, pct) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                                Text(label, fontSize = 13.sp, modifier = Modifier.width(52.dp))
                                LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.weight(1f))
                                Text("$pct%", fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                        Text("Illustrative sample data.", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}

// ───────────────────────── Tools ─────────────────────────

@Composable
private fun ToolScreen(vm: AdsViewModel, store: AppStore, act: Actions) {
    val title = vm.selectedTool
    val b = vm.showBdt
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.selectedTool = "" }) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        }
        val fill = Modifier.weight(1f)
        val pad = PaddingValues(16.dp)
        val gap = Arrangement.spacedBy(12.dp)
        when (title) {
            "Alerts" -> LazyColumn(contentPadding = pad, verticalArrangement = gap, modifier = fill) {
                items(listOf(
                    Triple("🔴  Sylhet Tea Co. is disabled", "Payment failed · 2h ago", 2),
                    Triple("🟠  Dhaka Fashion: CPR up 38%", "Eid Collection campaign · 5h ago", 0),
                    Triple("🟡  Rahim Electronics near limit", "Add funds soon · 1d ago", 1)
                )) { (h, s, idx) ->
                    CardTile(onClick = { vm.selectedTool = ""; vm.selectedAccount = idx; vm.accountTab = if (idx == 2) 1 else 0 }) {
                        Text(h, fontWeight = FontWeight.Bold)
                        Text(s, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
                        Text("Tap to open account", fontSize = 12.sp, color = MetaBlue, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            "Auto Rules" -> LazyColumn(contentPadding = pad, verticalArrangement = gap, modifier = fill) {
                itemsIndexed(listOf(
                    "Pause campaign if CPR goes above \$0.60", "Alert me at 80% of total budget",
                    "Pause ads when total budget is used", "Raise daily budget 20% when CTR is above 4%"
                )) { i, s ->
                    CardTile {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s, fontWeight = FontWeight.SemiBold)
                                Text("Preview rule · checked every 15 minutes", fontSize = 12.sp, color = Muted)
                            }
                            Switch(vm.rules[i], onCheckedChange = {
                                vm.rules[i] = it
                                vm.notice = "Rule ${if (it) "enabled" else "disabled"} (local preview — not connected to Meta)."
                            })
                        }
                    }
                }
            }
            "Client Reports" -> LazyColumn(contentPadding = pad, verticalArrangement = gap, modifier = fill) {
                items(vm.accounts, key = { it.id }) { a ->
                    val cs = vm.campaignsOf(a.id)
                    val spend = vm.spendOf(a.id)
                    CardTile {
                        Text(a.name, fontWeight = FontWeight.Bold)
                        Text(
                            "Spend today: ${usd(spend, b)}\nActive campaigns: ${vm.activeCount(a.id)}/${cs.size}\nRemaining limit: ${usd(a.limit - spend, b)}" +
                                if (cs.isNotEmpty()) "\nAvg CTR ${money(cs.map { it.ctr }.average())}% · Avg CPR ${usd(cs.map { it.cpr }.average())}" else "",
                            fontSize = 13.sp, color = Muted, modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val name = "MetaCommander_" + a.name.replace(Regex("[^A-Za-z0-9]+"), "_") + ".pdf"
                                act.savePdf(name, "Campaign Report – ${a.name}", reportMessage(vm, a).replace("*", "").lines())
                            }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                Icon(Icons.Default.PictureAsPdf, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save PDF")
                            }
                            Button(onClick = {
                                act.whatsApp(vm.chats.firstOrNull { it.accountId == a.id }?.phone ?: "", reportMessage(vm, a))
                            }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("WhatsApp") }
                        }
                    }
                }
            }
            "Leads Inbox" -> LazyColumn(contentPadding = pad, verticalArrangement = gap, modifier = fill) {
                items(listOf(
                    Triple("Sabbir Ahmed", "Eid Collection", "+880 1811-220011"),
                    Triple("Tania Islam", "WhatsApp Leads", "+880 1911-884422"),
                    Triple("Mahmud Hasan", "Cart Retargeting", "+880 1611-009977")
                )) { (n, campaign, phone) ->
                    CardTile {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(n, fontWeight = FontWeight.Bold)
                                Text("$campaign · $phone", fontSize = 12.sp, color = Muted)
                            }
                            FilledTonalButton(onClick = { act.whatsApp(phone, "Hello $n, following up on your enquiry.") }) { Text("Chat") }
                        }
                    }
                }
            }
            "Creatives" -> LazyColumn(contentPadding = pad, verticalArrangement = gap, modifier = fill) {
                items(listOf("Eid Banner" to "CTR 4.2% · ROAS 3.8", "Cart Reminder" to "CTR 3.6% · ROAS 4.1", "Video Reel" to "CTR 5.1% · ROAS 2.9", "Page Likes" to "CTR 1.8% · ROAS 1.2")) { (n, s) ->
                    CardTile {
                        Box(Modifier.fillMaxWidth().height(80.dp).background(MetaBlue.copy(alpha = 0.12f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, null, tint = MetaBlue, modifier = Modifier.size(36.dp))
                        }
                        Text(n, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                        Text("$s · sample data", fontSize = 12.sp, color = Muted)
                    }
                }
            }
            "Budget Planner" -> Column(fill.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = gap) {
                CardTile {
                    OutlinedTextField(
                        vm.plannerDaily, { vm.plannerDaily = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Daily budget (USD)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        vm.plannerDays, { vm.plannerDays = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Number of days") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                val daily = vm.plannerDaily.toDoubleOrNull() ?: 0.0
                val days = vm.plannerDays.toIntOrNull() ?: 0
                CardTile {
                    Text("Estimated results", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Mini("Total spend", usd(daily * days, b), Modifier.weight(1f))
                        Mini("Est. reach", String.format(Locale.US, "%,d", (daily * days * 190).toInt()), Modifier.weight(1f))
                        Mini("Est. results", String.format(Locale.US, "%,d", (daily * days / .42).toInt()), Modifier.weight(1f))
                    }
                    Text("Forecast is illustrative, not a Meta prediction.", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 10.dp))
                }
            }
            "Invoices" -> LazyColumn(contentPadding = pad, verticalArrangement = gap, modifier = fill) {
                items(listOf(Triple("Oct 14, 2026", 50.0, "Paid"), Triple("Oct 7, 2026", 50.0, "Paid"), Triple("Sep 29, 2026", 50.0, "Failed"), Triple("Sep 21, 2026", 50.0, "Paid"))) { (date, amount, status) ->
                    CardTile {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(date, fontWeight = FontWeight.Bold)
                                Text("Visa •••• 4417 · sample invoice", fontSize = 12.sp, color = Muted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(usd(amount, b), fontWeight = FontWeight.Bold)
                                Text(status, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (status == "Paid") Good else Bad)
                            }
                        }
                    }
                }
                item {
                    Button(onClick = act.billing, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("View billing records on Meta", fontWeight = FontWeight.Bold) }
                }
            }
            "Team Access" -> LazyColumn(contentPadding = pad, verticalArrangement = gap, modifier = fill) {
                items(listOf("You" to "Owner", "Sadia Rahman" to "Analyst", "Imran Hossain" to "Ad editor", "Farhana Akter" to "Viewer")) { (n, role) ->
                    CardTile {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(n, fontWeight = FontWeight.Bold)
                                Text("Meta Ads access · sample", fontSize = 12.sp, color = Muted)
                            }
                            AssistChip(onClick = { vm.notice = "Changing team roles needs a connected backend and verified permissions." }, label = { Text(role) })
                        }
                    }
                }
            }
            "Settings" -> Column(fill.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = gap) {
                CardTile {
                    Text("Security", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Biometric unlock", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(vm.biometricEnabled, onCheckedChange = { vm.biometricEnabled = it; store.setFlag("bio", it) })
                    }
                    OutlinedButton(
                        onClick = {
                            vm.storedHash = null; store.savePin(""); vm.firstPin = null; vm.pin = ""
                            vm.unlocked = false; vm.selectedTool = ""
                            vm.notice = "Create a new 4-digit PIN."
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp)
                    ) { Text("Change PIN") }
                }
                CardTile {
                    Text("Display", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Dark theme", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(vm.darkTheme, onCheckedChange = { vm.darkTheme = it; store.setFlag("dark", it) })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show amounts in BDT too", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(vm.showBdt, onCheckedChange = { vm.showBdt = it; store.setFlag("bdt", it) })
                    }
                    Text("BDT uses a fixed sample rate of 1 USD = ৳120.", fontSize = 12.sp, color = Muted)
                }
                Button(
                    onClick = { vm.unlocked = false; vm.pin = ""; vm.selectedTool = "" },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Icon(Icons.Default.Lock, null); Spacer(Modifier.width(8.dp)); Text("Lock app", fontWeight = FontWeight.Bold) }
            }
            else -> CardTile(modifier = Modifier.padding(16.dp)) { Text("This section is a preview.", color = Muted) }
        }
    }
}

// ───────────────────────── WhatsApp ─────────────────────────

@Composable
private fun WhatsAppScreen(vm: AdsViewModel, act: Actions) {
    val chat = vm.chats.getOrNull(vm.selectedChat)
    if (chat == null) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("WhatsApp Direct", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = WaGreen)
                    Text("Message clients with performance summaries", fontSize = 12.sp, color = Muted)
                }
                FilledTonalIconButton(onClick = { vm.showNewChat = true }) { Icon(Icons.Default.Add, "New chat") }
            }
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                itemsIndexed(vm.chats) { i, c ->
                    CardTile(onClick = { vm.openChat(i) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = WaGreen, modifier = Modifier.size(44.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text(c.name.take(1), color = Color.White, fontWeight = FontWeight.Bold) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(c.messages.lastOrNull()?.text?.lines()?.firstOrNull() ?: "No messages yet", fontSize = 13.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (c.unread > 0) Badge { Text(c.unread.toString()) }
                        }
                    }
                }
            }
        }
    } else {
        var draft by remember(vm.selectedChat) { mutableStateOf("") }
        val listState = rememberLazyListState()
        val account = vm.accounts.firstOrNull { it.id == chat.accountId }
        LaunchedEffect(chat.messages.size) {
            if (chat.messages.isNotEmpty()) listState.animateScrollToItem(chat.messages.lastIndex)
        }
        fun sendOut(text: String) {
            vm.addMessage(vm.selectedChat, text, true)
            act.whatsApp(chat.phone, text)
        }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.selectedChat = -1 }) { Icon(Icons.Default.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text(chat.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(chat.phone, fontSize = 12.sp, color = Muted)
                }
                TextButton(onClick = { act.whatsApp(chat.phone, "") }) { Text("Open WhatsApp") }
            }
            LazyColumn(
                state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (chat.messages.isEmpty()) item { Text("No messages yet. Send a report or type a message below.", color = Muted, fontSize = 13.sp) }
                items(chat.messages) { m ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (m.mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp, modifier = Modifier.widthIn(max = 300.dp)
                        ) { Text(m.text, modifier = Modifier.padding(12.dp), fontSize = 14.sp) }
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { sendOut(reportMessage(vm, account)) }, label = { Text("📊 Send report") })
                AssistChip(onClick = {
                    sendOut("💳 *Payment reminder*\nHello ${chat.name.substringBefore(" –")}, please review your Meta Ads billing so your campaigns keep running.")
                }, label = { Text("💳 Payment reminder") })
            }
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    draft, { draft = it }, modifier = Modifier.weight(1f), maxLines = 4,
                    placeholder = { Text("Type a message") }, shape = RoundedCornerShape(22.dp)
                )
                IconButton(onClick = { if (draft.isNotBlank()) { sendOut(draft.trim()); draft = "" } }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.Send, "Send via WhatsApp", tint = WaGreen)
                }
            }
        }
    }
}

// ───────────────────────── Shortcut tabs ─────────────────────────

@Composable
private fun ShortcutScreen(title: String, subtitle: String, button: String, emoji: String, action: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CardTile {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(emoji, fontSize = 44.sp)
                Spacer(Modifier.height(12.dp))
                Text(title, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
                Button(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(button, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ───────────────────────── Shared components ─────────────────────────

@Composable
private fun BottomDock(vm: AdsViewModel) {
    val shape = RoundedCornerShape(26.dp)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Surface(shape = shape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().soft(shape, 10.dp)) {
            NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp, windowInsets = WindowInsets(0, 0, 0, 0)) {
                val labels = listOf("Meta Ads", "Business", "Pages", "WhatsApp")
                val icons = listOf(Icons.Default.Campaign, Icons.Default.Dashboard, Icons.Default.Flag, Icons.Default.Chat)
                labels.forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = vm.nav == i,
                        onClick = { vm.nav = i; vm.selectedAccount = -1; vm.selectedTool = ""; vm.selectedChat = -1 },
                        icon = { Icon(icons[i], label) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MetaBlue, selectedTextColor = MetaBlue,
                            indicatorColor = MetaBlue.copy(alpha = 0.14f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CardTile(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val m = modifier.fillMaxWidth().soft(shape)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) Card(onClick = onClick, modifier = m, shape = shape, colors = colors) { Column(Modifier.padding(16.dp), content = content) }
    else Card(modifier = m, shape = shape, colors = colors) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
private fun Metric(title: String, value: String, caption: String, modifier: Modifier) {
    CardTile(modifier = modifier) {
        Icon(Icons.Default.Insights, null, tint = MetaBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(caption, fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun Mini(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = Muted)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Pill(active: Boolean) {
    val c = if (active) Good else Bad
    Surface(shape = RoundedCornerShape(50), color = c.copy(alpha = 0.12f)) {
        Text(if (active) "Active" else "Disabled", color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}
