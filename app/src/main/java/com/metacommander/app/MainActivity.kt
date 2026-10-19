package com.metacommander.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.browser.customtabs.CustomTabsIntent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private val AppBg = Color(0xFFF1F5F9)
private val Ink = Color(0xFF0F172A)
private val MetaBlue = Color(0xFF0668E1)
private val Muted = Color(0xFF64748B)
private val Good = Color(0xFF16A34A)
private val Bad = Color(0xFFDC2626)

data class AdAccount(val name:String,val id:String,var active:Boolean,val spend:Double,val limit:Double,val campaignCount:Int)
data class CampaignItem(val name:String,var active:Boolean,var budget:Double,val cpr:Double,val ctr:Double,val cpc:Double,val spend:Double,val totalBudget:Double,val used:Double)
data class ClientChat(val name:String,val phone:String,var unread:Int,var messages:List<String>)

private val toolItems = listOf(
    "Alerts" to "🔔", "Auto Rules" to "⚙️", "Client Reports" to "📑",
    "Leads Inbox" to "🧲", "Creatives" to "🎨", "Budget Planner" to "🧮",
    "Invoices" to "🧾", "Team Access" to "👥", "Settings" to "🔐"
)

class AdsViewModel : ViewModel() {
    val accounts = mutableStateListOf(
        AdAccount("Dhaka Fashion House","act_104829371",true,182.40,500.0,4),
        AdAccount("Rahim Electronics","act_208817452",true,96.10,300.0,3),
        AdAccount("Sylhet Tea Co.","act_377120098",false,0.0,250.0,0),
        AdAccount("Nila Beauty Studio","act_459032716",true,54.80,200.0,2)
    )
    val campaigns = mutableStateListOf(
        CampaignItem("Eid Collection – Conversions",true,25.0,.42,3.1,.18,48.2,300.0,212.4),
        CampaignItem("Retargeting – Cart Viewers",true,15.0,.31,4.4,.12,31.6,200.0,141.9),
        CampaignItem("Page Likes – Dhaka",false,10.0,.09,1.8,.05,0.0,100.0,38.5),
        CampaignItem("Messages – WhatsApp Leads",true,20.0,.55,2.7,.21,42.7,250.0,177.6)
    )
    val chats = mutableStateListOf(
        ClientChat("Rahim – Dhaka Fashion","+880 1712-345678",2,listOf("Bhai, ad ta ajke kemon cholche?","Ajke 4 ta campaign active, spend $182.","Budget ektu barate chai")),
        ClientChat("Karim – Rahim Electronics","+880 1819-556677",0,listOf("Weekly report pathiye diyechi.","Thanks, dekhlam. Good results!")),
        ClientChat("Tea Co. Office","+880 1711-902030",1,listOf("Amader account disable keno?")),
        ClientChat("Nila Apu","+880 1914-778899",0,listOf("Apnar new ad live hoyeche ✅","Dhonnobad!"))
    )
    var unlocked by mutableStateOf(false)
    var pin by mutableStateOf("")
    var nav by mutableIntStateOf(0)
    var selectedAccount by mutableIntStateOf(-1)
    var accountTab by mutableIntStateOf(0)
    var selectedTool by mutableStateOf("")
    var selectedChat by mutableIntStateOf(-1)
    var search by mutableStateOf("")
    var notice by mutableStateOf<String?>(null)
    var budgetIndex by mutableIntStateOf(-1)
    var budgetText by mutableStateOf("25")
    var plannerDaily by mutableStateOf("25")
    var plannerDays by mutableStateOf("7")
    var newChatName by mutableStateOf("")
    var newChatPhone by mutableStateOf("")
    var showNewChat by mutableStateOf(false)
    var darkTheme by mutableStateOf(false)
    val rules = mutableStateListOf(true,true,false,false)
    val settings = mutableStateListOf(true,true,false)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AdsViewModel = viewModel()
            MetaCommanderApp(vm,
                openMeta = { openUrl("https://www.facebook.com/adsmanager/") },
                openBusiness = { openUrl("https://business.facebook.com/") },
                openPages = { openUrl("https://www.facebook.com/pages/") },
                openBilling = { openUrl("https://business.facebook.com/billing_hub/") },
                openWhatsApp = { phone, message -> openUrl("https://wa.me/${phone.filter { it.isDigit() }}?text=${Uri.encode(message)}") },
                biometric = { showBiometricPrompt { vm.unlocked = true } }
            )
        }
    }
    private fun openUrl(url:String) {
        try { CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, Uri.parse(url)) }
        catch (_:Exception) { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }
    }
    private fun showBiometricPrompt(onSuccess:()->Unit) {
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) return
        val executor = ContextCompat.getMainExecutor(this)
        BiometricPrompt(this,executor,object:BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){ onSuccess() }
        }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock MetaCommander")
            .setSubtitle("Verify your identity").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL).build())
    }
}

@Composable
private fun MetaCommanderApp(vm:AdsViewModel,openMeta:()->Unit,openBusiness:()->Unit,openPages:()->Unit,
    openBilling:()->Unit,openWhatsApp:(String,String)->Unit,biometric:()->Unit) {
    MaterialTheme(colorScheme=if(vm.darkTheme) darkColorScheme(primary=Color(0xFF4A9BFF),background=Color(0xFF0B1220),surface=Color(0xFF162033)) else lightColorScheme(primary=MetaBlue,background=AppBg,surface=Color.White,onSurface=Ink)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            if(!vm.unlocked) LoginScreen(vm,openMeta,biometric)
            else Column(Modifier.fillMaxSize()) {
                when {
                    vm.nav==3 -> WhatsAppScreen(vm,openWhatsApp)
                    vm.nav==1 -> ShortcutScreen("Business Suite","Manage business assets in Meta’s secure web experience.","Open Business Suite","🗂️",openBusiness)
                    vm.nav==2 -> ShortcutScreen("Facebook Pages","Open your linked Pages and profiles.","Open my Pages","📄",openPages)
                    vm.selectedTool.isNotEmpty() -> ToolScreen(vm,openWhatsApp)
                    vm.selectedAccount>=0 -> AccountScreen(vm,openBilling)
                    else -> DashboardScreen(vm,openMeta,biometric,openBilling)
                }
                BottomDock(vm)
            }
        }
        if(vm.notice!=null) AlertDialog(onDismissRequest={vm.notice=null},title={Text("MetaCommander")},text={Text(vm.notice ?: "")},confirmButton={TextButton(onClick={vm.notice=null}){Text("OK")}})
        if(vm.budgetIndex>=0) AlertDialog(onDismissRequest={vm.budgetIndex=-1},title={Text("Edit daily budget")},text={
            Column { Text(vm.campaigns[vm.budgetIndex].name); Spacer(Modifier.height(8.dp)); OutlinedTextField(value=vm.budgetText,onValueChange={vm.budgetText=it},label={Text("Daily budget (USD)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true) }
        },confirmButton={TextButton(onClick={
            val amount=vm.budgetText.toDoubleOrNull()
            if(amount==null || amount<=0) vm.notice="Enter a valid budget greater than zero."
            else { vm.notice="Preview updated locally only. Connect Meta Marketing API before applying live changes."; vm.campaigns[vm.budgetIndex]=vm.campaigns[vm.budgetIndex].copy(budget=amount) }
            vm.budgetIndex=-1
        }){Text("Save preview")}},dismissButton={TextButton(onClick={vm.budgetIndex=-1}){Text("Cancel")}})
        if(vm.showNewChat) AlertDialog(onDismissRequest={vm.showNewChat=false},title={Text("New client chat")},text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(vm.newChatName,{vm.newChatName=it},label={Text("Client name")},singleLine=true)
                OutlinedTextField(vm.newChatPhone,{vm.newChatPhone=it},label={Text("Phone number")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone))
            }
        },confirmButton={TextButton(onClick={
            if(vm.newChatName.isNotBlank() && vm.newChatPhone.isNotBlank()) {
                vm.chats.add(ClientChat(vm.newChatName,vm.newChatPhone,0,listOf("Hello! 👋")))
                vm.selectedChat=vm.chats.lastIndex
            } else vm.notice="Enter client name and phone number."
            vm.showNewChat=false
        }){Text("Start chat")}},dismissButton={TextButton(onClick={vm.showNewChat=false}){Text("Cancel")}})
    }
}

@Composable private fun LoginScreen(vm:AdsViewModel,openMeta:()->Unit,biometric:()->Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal=24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Surface(shape=CircleShape,color=Color.White,shadowElevation=6.dp,modifier=Modifier.size(72.dp)) { Box(contentAlignment=Alignment.Center){Text("∞",fontSize=42.sp,fontWeight=FontWeight.ExtraBold,color=MetaBlue)} }
        Spacer(Modifier.height(16.dp)); Text("Meta Ads Manager",fontSize=25.sp,fontWeight=FontWeight.ExtraBold)
        Text("Enter your 4-digit PIN to unlock",fontSize=13.sp,color=Muted)
        Row(horizontalArrangement=Arrangement.spacedBy(16.dp),modifier=Modifier.padding(vertical=22.dp)) { repeat(4){i-> Surface(shape=CircleShape,color=if(i<vm.pin.length) MetaBlue else Color(0xFFCBD5E1),modifier=Modifier.size(14.dp)){} } }
        val keys=listOf("1","2","3","4","5","6","7","8","9","⌫","0","✓")
        keys.chunked(3).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxWidth()) {
            row.forEach { key -> Surface(shape=RoundedCornerShape(18.dp),color=Color.White,shadowElevation=3.dp,modifier=Modifier.weight(1f).height(58.dp).padding(1.dp).clickable {
                when(key) {
                    "⌫" -> vm.pin=vm.pin.dropLast(1)
                    "✓" -> { if(vm.pin.length==4) vm.unlocked=true else vm.notice="Enter four digits first." }
                    else -> { if(vm.pin.length<4) vm.pin+=key }
                }
            }) { Box(contentAlignment=Alignment.Center){Text(key,fontSize=21.sp,fontWeight=FontWeight.Bold)} }
            }
        }; Spacer(Modifier.height(10.dp)) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick=biometric,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Fingerprint,null);Spacer(Modifier.width(8.dp));Text("Unlock with fingerprint or face")}
        Button(onClick=openMeta,modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Text("Login with Facebook / Meta Ads")}
        Text("Prototype PIN screen • secure PIN storage is not configured",fontSize=11.sp,color=Muted,modifier=Modifier.padding(top=12.dp))
    }
}

@Composable private fun DashboardScreen(vm:AdsViewModel,openMeta:()->Unit,biometric:()->Unit,openBilling:()->Unit) {
    Column {
        Column(Modifier.padding(horizontal=16.dp, vertical=12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)){Text("Meta Ads Manager",fontSize=23.sp,fontWeight=FontWeight.ExtraBold);Text("Campaign performance at a glance",fontSize=12.sp,color=Muted)}
                Surface(shape=CircleShape,color=Color.White,shadowElevation=3.dp,modifier=Modifier.size(42.dp).clickable{vm.selectedTool="Settings"}){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.Person,null,tint=MetaBlue)}}
            }
            OutlinedTextField(vm.search,{vm.search=it},modifier=Modifier.fillMaxWidth().padding(top=10.dp),singleLine=true,shape=RoundedCornerShape(14.dp),placeholder={Text("Search ad accounts")},leadingIcon={Icon(Icons.Default.Search,null)})
        }
        LazyColumn(contentPadding=PaddingValues(horizontal=16.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.weight(1f)) {
            item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Metric("Active accounts",vm.accounts.count{it.active}.toString(),"Accounts",Modifier.weight(1f))
                Metric("Active campaigns",vm.campaigns.count{it.active}.toString(),"Sample campaigns",Modifier.weight(1f))
            }; Spacer(Modifier.height(10.dp)); Metric("Today's total spend","$"+money(vm.accounts.sumOf{it.spend}),"DEMO DATA • not live Meta data",Modifier.fillMaxWidth()) }
            item { Text("Tools",fontSize=17.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.padding(vertical=2.dp)) }
            items(toolItems.chunked(3)) { row -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { row.forEach { (name,emoji)-> Surface(shape=RoundedCornerShape(16.dp),color=Color.White,shadowElevation=3.dp,modifier=Modifier.weight(1f).clickable{vm.selectedTool=name}.height(94.dp)) { Column(Modifier.fillMaxSize().padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(emoji,fontSize=24.sp);Text(name,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,textAlign=androidx.compose.ui.text.style.TextAlign.Center)} } }; repeat(3-row.size){Spacer(Modifier.weight(1f))} } }
            item { Row(verticalAlignment=Alignment.CenterVertically){Text("Ad accounts",fontSize=17.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f));Text("DEMO DATA",fontSize=10.sp,color=MetaBlue,fontWeight=FontWeight.Bold)} }
            items(vm.accounts.filter{it.name.contains(vm.search,true)||it.id.contains(vm.search,true)}) { account ->
                CardTile(onClick={vm.selectedAccount=vm.accounts.indexOf(account);vm.accountTab=0}) {
                    Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(12.dp),color=Color(0xFFEAF2FF),modifier=Modifier.size(42.dp)){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.Campaign,null,tint=MetaBlue)}};Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(account.name,fontWeight=FontWeight.Bold);Text(account.id,fontSize=11.sp,color=Muted)};Pill(account.active)}
                    Spacer(Modifier.height(14.dp));Row{Mini("Today's spend","$"+money(account.spend),Modifier.weight(1f));Mini("Remaining limit","$"+money(account.limit-account.spend),Modifier.weight(1f))}
                }
            }
            item { OutlinedButton(onClick=openMeta,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Login,null);Spacer(Modifier.width(8.dp));Text("Login with Facebook / Meta Ads")} }
            item { TextButton(onClick=biometric,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Fingerprint,null);Spacer(Modifier.width(6.dp));Text("Unlock with biometrics")} }
        }
    }
}

@Composable private fun AccountScreen(vm:AdsViewModel,openBilling:()->Unit) {
    val a=vm.accounts[vm.selectedAccount]
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={vm.selectedAccount=-1}){Icon(Icons.Default.ArrowBack,"Back")}
            Column(Modifier.weight(1f)){Text(a.name,fontSize=19.sp,fontWeight=FontWeight.ExtraBold);Text(a.id,fontSize=11.sp,color=Muted)}
            Pill(a.active)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("Campaigns","Billing & Limit","Analytics").forEachIndexed { i,label ->
                Surface(shape=RoundedCornerShape(13.dp),color=if(vm.accountTab==i) MetaBlue else Color.White,shadowElevation=2.dp,modifier=Modifier.weight(1f).clickable{vm.accountTab=i}) { Text(label,color=if(vm.accountTab==i) Color.White else Ink,fontSize=11.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(vertical=12.dp,horizontal=2.dp)) }
            }
        }
        when(vm.accountTab) {
            0 -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.weight(1f)) {
                item { CardTile { Row { Mini("Today","$"+money(a.spend),Modifier.weight(1f));Mini("Lifetime (sample)","$"+money(a.spend*38+120),Modifier.weight(1f)) };Text("Campaign budget overview • DEMO DATA",fontSize=11.sp,color=Muted,modifier=Modifier.padding(top=12.dp)) } }
                items(vm.campaigns) { c ->
                    CardTile {
                        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(c.name,fontWeight=FontWeight.Bold);Text("Spend $"+money(c.spend),fontSize=12.sp,color=Muted)};Switch(checked=c.active,onCheckedChange={c.active=it;vm.notice="Demo preview changed only. No live campaign status was changed."})}
                        Spacer(Modifier.height(10.dp));Row{Mini("CPR","$"+money(c.cpr),Modifier.weight(1f));Mini("CTR",c.ctr.toString()+"%",Modifier.weight(1f));Mini("CPC","$"+money(c.cpc),Modifier.weight(1f))}
                        Spacer(Modifier.height(12.dp));Text("Total budget  $"+money(c.totalBudget),fontSize=12.sp,fontWeight=FontWeight.Bold);LinearProgressIndicator(progress={(c.used/c.totalBudget).toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(vertical=5.dp));Row{Mini("Used","$"+money(c.used),Modifier.weight(1f));Mini("Remaining","$"+money(c.totalBudget-c.used),Modifier.weight(1f))}
                        Row(verticalAlignment=Alignment.CenterVertically){Text("Daily budget  $"+money(c.budget),fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));TextButton(onClick={vm.budgetIndex=vm.campaigns.indexOf(c);vm.budgetText=c.budget.toString()}){Text("Edit budget")}}
                    }
                }
            }
            1 -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.weight(1f)) {
                item { Metric("Current threshold","$50.00","Sample only • not live billing",Modifier.fillMaxWidth()) }
                item { CardTile { Text("Payment method",fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text("Visa •••• 4417",fontSize=17.sp,fontWeight=FontWeight.Bold);Text("Expiry 08/28 • DEMO DATA",fontSize=11.sp,color=Muted) } }
                item { CardTile { Text("Ad account limit",fontWeight=FontWeight.Bold);Text("$"+money(a.spend)+" of $"+money(a.limit)+" used",fontSize=13.sp,color=Muted);LinearProgressIndicator(progress={(a.spend/a.limit).toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp));Text("Billing details are not loaded from Meta.",fontSize=12.sp,color=Muted) } }
                item { Button(onClick=openBilling,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.OpenInNew,null);Spacer(Modifier.width(8.dp));Text("Open Meta Billing & Payments")} }
                item { Text("Payment-method editing and payments are not simulated here. Use Meta's secure billing page.",fontSize=12.sp,color=Muted) }
            }
            else -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.weight(1f)) {
                item { Metric("Reach","48.2K","Illustrative sample metric",Modifier.fillMaxWidth()) }
                item { Metric("Impressions","126K","Illustrative sample metric",Modifier.fillMaxWidth()) }
                item { CardTile { Text("Spend, last 7 days",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.Bottom,modifier=Modifier.fillMaxWidth().height(100.dp).padding(top=12.dp)){listOf(22,31,27,44,38,52,48).forEachIndexed{index,value->Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom){Box(Modifier.fillMaxWidth().height((value*1.25).dp).background(MetaBlue,RoundedCornerShape(topStart=5.dp,topEnd=5.dp)));Text("MTWTFSS"[index].toString(),fontSize=10.sp,color=Muted)}}} } }
                item { CardTile { Text("Audience by age",fontWeight=FontWeight.Bold);listOf("18–24" to 34,"25–34" to 41,"35–44" to 17,"45+" to 8).forEach{(label,pct)->Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(top=10.dp)){Text(label,fontSize=12.sp,modifier=Modifier.width(48.dp));LinearProgressIndicator(progress={pct/100f},modifier=Modifier.weight(1f));Text("$pct%",fontSize=12.sp,modifier=Modifier.padding(start=8.dp))}} } }
            }
        }
    }
}

@Composable private fun ToolScreen(vm:AdsViewModel,openWhatsApp:(String,String)->Unit) {
    val title=vm.selectedTool
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={vm.selectedTool=""}){Icon(Icons.Default.ArrowBack,"Back")};Text(title,fontSize=21.sp,fontWeight=FontWeight.ExtraBold)}
        when(title) {
            "Alerts" -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(listOf("🔴  Sylhet Tea Co. is disabled" to "Payment failed · 2h ago","🟠  Dhaka Fashion: CPR up 38%" to "Eid Collection campaign · 5h ago","🟡  Rahim Electronics used 90% of limit" to "Add funds soon · 1d ago")){(h,s)->CardTile{Text(h,fontWeight=FontWeight.Bold);Text(s,fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=5.dp))}}
            }
            "Auto Rules" -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(listOf("Pause campaign if CPR goes above $0.60","Alert me at 80% of total budget","Pause ads when total budget is used","Raise daily budget 20% when CTR is above 4%").mapIndexed{i,s->i to s}){(i,s)->CardTile{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(s,fontWeight=FontWeight.SemiBold);Text("Preview rule · checked every 15 minutes",fontSize=11.sp,color=Muted)};Switch(vm.rules[i],onCheckedChange={vm.rules[i]=it;vm.notice="Rule setting saved in this local preview only. Automation is not connected to Meta."})}}}
            }
            "Client Reports" -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(vm.accounts){a->CardTile{Text(a.name,fontWeight=FontWeight.Bold);Text("Spend today: $"+money(a.spend)+"\nActive campaigns: "+a.campaignCount+"\nLimit remaining: $"+money(a.limit-a.spend)+"\nCTR 3.4% · CPR $0.42",fontSize=12.sp,color=Muted,modifier=Modifier.padding(vertical=8.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={vm.notice="PDF export will be enabled in the next implementation step."},modifier=Modifier.weight(1f)){Text("Save as PDF")};Button(onClick={openWhatsApp("+8801712345678","Report for ${a.name}: spend $"+money(a.spend)+", active campaigns "+a.campaignCount)},modifier=Modifier.weight(1f)){Text("Send WhatsApp")}}}}
            }
            "Leads Inbox" -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(listOf("Sabbir Ahmed" to "Eid Collection · +880 1811-220011","Tania Islam" to "WhatsApp Leads · +880 1911-884422","Mahmud Hasan" to "Cart Retargeting · +880 1611-009977")){(n,s)->CardTile{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(n,fontWeight=FontWeight.Bold);Text(s,fontSize=12.sp,color=Muted)};TextButton(onClick={openWhatsApp(s.substringAfter("· ").trim(),"Hello $n, following up on your enquiry.")}){Text("Chat")}}}}
            }
            "Creatives" -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(listOf("Eid Banner" to "CTR 4.2% · ROAS 3.8","Cart Reminder" to "CTR 3.6% · ROAS 4.1","Video Reel" to "CTR 5.1% · ROAS 2.9","Page Likes" to "CTR 1.8% · ROAS 1.2")){(n,s)->CardTile{Box(Modifier.fillMaxWidth().height(72.dp).background(Color(0xFFEAF2FF),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Image,null,tint=MetaBlue,modifier=Modifier.size(34.dp))};Text(n,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp));Text("$s · sample creative data",fontSize=12.sp,color=Muted)}}
            }
            "Budget Planner" -> Column(Modifier.padding(14.dp)) {
                CardTile { OutlinedTextField(vm.plannerDaily,{vm.plannerDaily=it},label={Text("Daily budget (USD)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true);Spacer(Modifier.height(8.dp));OutlinedTextField(vm.plannerDays,{vm.plannerDays=it},label={Text("Number of days")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true) }
                val daily=vm.plannerDaily.toDoubleOrNull()?:0.0;val days=vm.plannerDays.toIntOrNull()?:0;CardTile { Text("Estimated results",fontWeight=FontWeight.Bold);Row{Mini("Total spend","$"+money(daily*days),Modifier.weight(1f));Mini("Est. reach",(daily*days*190).toInt().toString(),Modifier.weight(1f));Mini("Est. results",(daily*days/.42).toInt().toString(),Modifier.weight(1f))};Text("Forecast is illustrative, not a Meta prediction.",fontSize=11.sp,color=Muted,modifier=Modifier.padding(top=8.dp)) }
            }
            "Invoices" -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(listOf(Triple("Oct 14, 2026","$50.00","Paid"),Triple("Oct 7, 2026","$50.00","Paid"),Triple("Sep 29, 2026","$50.00","Failed"),Triple("Sep 21, 2026","$50.00","Paid"))){(date,amount,status)->CardTile{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(date,fontWeight=FontWeight.Bold);Text("Visa •••• 4417 · sample invoice",fontSize=11.sp,color=Muted)};Column(horizontalAlignment=Alignment.End){Text(amount,fontWeight=FontWeight.Bold);Text(status,fontSize=11.sp,color=if(status=="Paid")Good else Bad)}}}}
                item { Button(onClick={vm.notice="Invoice records are sample data. Live invoices are not connected yet."},modifier=Modifier.fillMaxWidth()){Text("View billing records on Meta")} }
            }
            "Team Access" -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(listOf("You" to "Owner","Sadia Rahman" to "Analyst","Imran Hossain" to "Ad editor","Farhana Akter" to "Viewer")){(n,role)->CardTile{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(n,fontWeight=FontWeight.Bold);Text("Meta Ads access · sample",fontSize=11.sp,color=Muted)};AssistChip(onClick={vm.notice="Team-role changes require a connected backend and verified permissions."},label={Text(role)})}}}
            }
            "Settings" -> LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(listOf("PIN lock","Biometric unlock","Show amounts in BDT too").mapIndexed{i,s->i to s}){(i,s)->CardTile{Row(verticalAlignment=Alignment.CenterVertically){Text(s,modifier=Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Switch(vm.settings[i],onCheckedChange={vm.settings[i]=it;vm.notice=if(i==0)"PIN lock preference preview changed; secure PIN storage is not yet configured." else "Setting saved in this local preview."})}}}
                item { CardTile { Text("Appearance",fontWeight=FontWeight.Bold);Row(verticalAlignment=Alignment.CenterVertically){Text("Dark theme",modifier=Modifier.weight(1f));Switch(vm.darkTheme,onCheckedChange={vm.darkTheme=it})} };Button(onClick={vm.unlocked=false;vm.pin=""},modifier=Modifier.fillMaxWidth()){Text("Lock app")} }
            }
            else -> CardTile { Text("This section is a preview. Live account integration is not configured.",color=Muted) }
        }
    }
}

@Composable private fun WhatsAppScreen(vm:AdsViewModel,openWhatsApp:(String,String)->Unit) {
    if(vm.selectedChat<0) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("WhatsApp Business",fontSize=22.sp,fontWeight=FontWeight.ExtraBold,color=Color(0xFF128C7E));Text("Client conversations • sample inbox",fontSize=11.sp,color=Muted)};FilledTonalIconButton(onClick={vm.showNewChat=true}){Icon(Icons.Default.Add,null)}}
            LazyColumn(contentPadding=PaddingValues(horizontal=14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.weight(1f)) {
                items(vm.chats){chat->CardTile(onClick={vm.selectedChat=vm.chats.indexOf(chat);chat.unread=0}){Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=CircleShape,color=Color(0xFF128C7E),modifier=Modifier.size(42.dp)){Box(contentAlignment=Alignment.Center){Text(chat.name.take(1),color=Color.White,fontWeight=FontWeight.Bold)}};Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(chat.name,fontWeight=FontWeight.Bold);Text(chat.messages.lastOrNull()?: "",fontSize=12.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)};if(chat.unread>0)Badge{Text(chat.unread.toString())}}}}
            }
        }
    } else {
        val chat=vm.chats[vm.selectedChat]
        var draft by remember(vm.selectedChat){mutableStateOf("")}
        Column {
            Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={vm.selectedChat=-1}){Icon(Icons.Default.ArrowBack,"Back")};Column(Modifier.weight(1f)){Text(chat.name,fontWeight=FontWeight.Bold);Text(chat.phone,fontSize=11.sp,color=Muted)};TextButton(onClick={openWhatsApp(chat.phone,"Hello from MetaCommander")}){Text("Open WhatsApp")}}
            LazyColumn(modifier=Modifier.weight(1f).fillMaxWidth().padding(horizontal=12.dp),contentPadding=PaddingValues(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(chat.messages){msg->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(msg.startsWith("You:"))Arrangement.End else Arrangement.Start){Surface(shape=RoundedCornerShape(14.dp),color=if(msg.startsWith("You:"))Color(0xFFD9FDD3) else MaterialTheme.colorScheme.surface,shadowElevation=1.dp,modifier=Modifier.widthIn(max=300.dp)){Text(msg.removePrefix("You: "),modifier=Modifier.padding(12.dp),fontSize=14.sp)}}}
            }
            Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(draft,{draft=it},modifier=Modifier.weight(1f),placeholder={Text("Type a message")},shape=RoundedCornerShape(20.dp));IconButton(onClick={if(draft.isNotBlank()){chat.messages=chat.messages+("You: "+draft);draft=""}}){Icon(Icons.Default.Send,null,tint=MetaBlue)}}
            Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){TextButton(onClick={chat.messages=chat.messages+("You: "+ "Report: sample spend today $182.40; 4 campaigns active.")}){Text("Send report")};TextButton(onClick={chat.messages=chat.messages+"You: Payment reminder: please review your Meta Ads billing."}){Text("Payment reminder")};TextButton(onClick={openWhatsApp(chat.phone,chat.messages.lastOrNull()?: "Hello")}){Text("Send externally")}}
        }
    }
}

@Composable private fun ShortcutScreen(title:String,subtitle:String,button:String,emoji:String,action:()->Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        CardTile { Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth().padding(12.dp)){Text(emoji,fontSize=42.sp);Spacer(Modifier.height(12.dp));Text(title,fontSize=23.sp,fontWeight=FontWeight.ExtraBold);Text(subtitle,color=Muted,modifier=Modifier.padding(vertical=10.dp));Button(onClick=action,modifier=Modifier.fillMaxWidth()){Text(button)}} }
    }
}

@Composable private fun BottomDock(vm:AdsViewModel) {
    NavigationBar(containerColor=MaterialTheme.colorScheme.surface,tonalElevation=8.dp) {
        val labels=listOf("Meta Ads","Business","Pages","WhatsApp")
        val icons=listOf(Icons.Default.Campaign,Icons.Default.Dashboard,Icons.Default.Flag,Icons.Default.Chat)
        labels.forEachIndexed{i,label->NavigationBarItem(selected=vm.nav==i,onClick={vm.nav=i;vm.selectedAccount=-1;vm.selectedTool="";vm.selectedChat=-1},icon={Icon(icons[i],null)},label={Text(label,fontSize=10.sp)})}
    }
}

@Composable private fun Metric(title:String,value:String,caption:String,modifier:Modifier) {
    Card(modifier=modifier,shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(3.dp)) { Column(Modifier.padding(14.dp)){Icon(Icons.Default.Insights,null,tint=MetaBlue,modifier=Modifier.size(21.dp));Spacer(Modifier.height(7.dp));Text(value,fontSize=22.sp,fontWeight=FontWeight.ExtraBold);Text(title,fontSize=12.sp,fontWeight=FontWeight.SemiBold);Text(caption,fontSize=10.sp,color=Muted)} }
}
@Composable private fun Mini(label:String,value:String,modifier:Modifier) { Column(modifier){Text(label,fontSize=11.sp,color=Muted);Text(value,fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)} }
@Composable private fun Pill(active:Boolean) { Surface(shape=RoundedCornerShape(50),color=if(active)Color(0xFFE7F8EF) else Color(0xFFFFF0F0)){Text(if(active)"Active" else "Disabled",color=if(active)Good else Bad,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp))} }
@Composable private fun CardTile(onClick:(()->Unit)?=null,content:@Composable ColumnScope.()->Unit) { Card(modifier=Modifier.fillMaxWidth().then(if(onClick!=null)Modifier.clickable{onClick()}else Modifier),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(3.dp)){Column(Modifier.padding(14.dp),content=content)} }
private fun money(value:Double)=String.format(java.util.Locale.US,"%.2f",value)
