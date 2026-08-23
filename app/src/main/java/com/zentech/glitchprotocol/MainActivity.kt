package com.zentech.glitchprotocol

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.JavascriptInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        setContent {
            GlitchProtocolMasterApp(vibrator)
        }
    }
}

// OS Farbpaletten
val KaliBgDark = Color(0xFF07090F)
val KaliPanelBg = Color(0xFF101422)
val KaliBlue = Color(0xFF1793D1)
val TermGreen = Color(0xFF2ECC71)
val ErrorRed = Color(0xFFE74C3C)
val WarningAmber = Color(0xFFF39C12)

val Win11Taskbar = Color(0xEE141724)
val Win11Blue = Color(0xFF0078D4)

data class SoftwareTool(
    val id: String,
    val name: String,
    val command: String,
    val category: String,
    val priceEur: Int,
    val isFreeRepo: Boolean,
    val desc: String,
    var isInstalled: Boolean = false
)

data class HardwareItem(
    val id: String,
    val name: String,
    val category: String,
    val priceEur: Int,
    val hashSpeedMhs: Double,
    val powerWatt: Int,
    val desc: String,
    var isBought: Boolean = false
)

data class ExploitPayloadItem(
    val id: String,
    val name: String,
    val seller: String,
    val priceEur: Int,
    val desc: String,
    val targetIp: String,
    var isBought: Boolean = false
)

data class Hideout(
    val id: String,
    val name: String,
    val district: String,
    val rentDayEur: Int,
    val depositEur: Int,
    val secLevel: Int,
    val desc: String,
    val imageResId: Int = R.drawable.hideout_keller,
    val lat: Double = 52.5480,
    val lng: Double = 13.3600
)

data class CompanyTarget(
    val id: String,
    val name: String,
    val sector: String,
    val address: String,
    val ip: String,
    val bountyEur: Int,
    val vulnType: String,
    val vulnPort: String,
    val requiredTools: String,
    val difficulty: String,
    val lat: Double,
    val lng: Double,
    val spawnDay: Int = 1,
    var breachStage: Int = 0,
    var isPwned: Boolean = false
)

data class ChatMessage(
    val sender: String,
    val text: String,
    val time: String,
    val isOffer: Boolean = false,
    val rewardEur: Int = 0
)

data class DesktopAppEntry(
    val id: String,
    val name: String,
    val icon: String,
    val color: Color
)

// FIRMEN-GENERATOR FÜR TAGESABHÄNGIGE SKALIERUNG
val BERLIN_COMPANY_NAMES = listOf(
    "Spree Logistics", "Kudamm Luxury Brands", "Alex Finanzen AG", "Kreuzberg StartUp Hub",
    "Potsdamer Platz Real Estate", "Brandenburger Tor Security", "Neukölln Krypto Exchange",
    "Tiergarten Cloud Systems", "Friedrichshain VR Labs", "Charlottenburg Quantum AI",
    "Moabit Solar Networks", "Tempelhof Drone Delivery", "Prenzlauer Berg Bio Tech",
    "Wannsee Datacenter AG", "Schöneberg Cyber Defense", "Lichtenberg Telecom Hub",
    "Adlershof Photonics", "Treptow Media Stream", "Marzahn Smart Factory", "Steglitz Legal Cloud"
)
val BERLIN_STREETS = listOf(
    "Friedrichstraße", "Unter den Linden", "Kurfürstendamm", "Karl-Marx-Allee", "Torstraße",
    "Schönhauser Allee", "Oranienstraße", "Kantstraße", "Warschauer Straße", "Bernauer Straße"
)
val SECTORS = listOf("Finanzen", "E-Commerce", "Industrie", "Medizin", "Kryptowährung", "Cloud-Hosting", "Gastronomie")
val VULNS = listOf(
    Triple("SQL-Injection in Auth-Portal", "Port 443 (HTTPS)", "sqlmap"),
    Triple("Schwacher WPA2/WPA3 Handshake", "Port 80/WLAN", "airodump-ng / hashcat"),
    Triple("Offener RDP Server ohne 2FA", "Port 3389 (RDP)", "hydra"),
    Triple("Apache Path Traversal Zero-Day", "Port 80 (HTTP)", "msfconsole / 0day"),
    Triple("SSH Brute-Force Default Passwords", "Port 22 (SSH)", "hydra"),
    Triple("Unverschlüsselte API Tokens", "Port 8080 (API)", "wireshark")
)

fun getCompanyCountForDay(day: Int): Int {
    return when {
        day <= 10 -> Random.nextInt(3, 6)    // max 5
        day <= 20 -> Random.nextInt(6, 11)   // max 10
        day <= 30 -> Random.nextInt(12, 21)  // max 20
        else -> Random.nextInt(20, 31)       // max 30
    }
}

fun generateCompaniesForDay(day: Int): List<CompanyTarget> {
    val count = getCompanyCountForDay(day)
    val list = mutableListOf<CompanyTarget>()
    for (i in 1..count) {
        val name = "${BERLIN_COMPANY_NAMES.random()} #$day-$i"
        val street = "${BERLIN_STREETS.random()} ${Random.nextInt(1, 199)}"
        val sector = SECTORS.random()
        val vuln = VULNS.random()
        // Fair balancierte Belohnungen je nach Tag und Sektor
        val bounty = when (sector) {
            "Finanzen", "Kryptowährung" -> (450 + (day * 35) + Random.nextInt(50, 200))
            "Medizin", "Cloud-Hosting" -> (280 + (day * 25) + Random.nextInt(30, 150))
            else -> (90 + (day * 15) + Random.nextInt(15, 60))
        }
        val diff = if (bounty > 600) "Schwer" else if (bounty > 300) "Mittel" else "Einfach"
        val ip = "${Random.nextInt(45, 218)}.${Random.nextInt(10, 240)}.${Random.nextInt(1, 254)}.${Random.nextInt(2, 250)}"
        // Echte GPS Koordinaten rund um Berlin Zentrum (Alexanderplatz, Mitte, Kreuzberg, Charlottenburg)
        val lat = 52.4800 + (Random.nextDouble() * 0.0800)
        val lng = 13.3200 + (Random.nextDouble() * 0.1400)

        list.add(
            CompanyTarget(
                id = "comp_d${day}_$i",
                name = name,
                sector = sector,
                address = street,
                ip = ip,
                bountyEur = bounty,
                vulnType = vuln.first,
                vulnPort = vuln.second,
                requiredTools = vuln.third,
                difficulty = diff,
                lat = lat,
                lng = lng,
                spawnDay = day
            )
        )
    }
    return list
}

@Composable
fun GlitchProtocolMasterApp(vibrator: Vibrator?) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("glitch_protocol_save", Context.MODE_PRIVATE) }

    val isSavedGame = prefs.getBoolean("has_saved_game", false)

    var gameStateStep by remember { mutableIntStateOf(if (isSavedGame) 4 else -1) }
    var playerName by remember { mutableStateOf(prefs.getString("player_name", "") ?: "") }
    var hackerAlias by remember { mutableStateOf(prefs.getString("hacker_alias", "zer0_day") ?: "zer0_day") }

    var activeOS by remember { mutableStateOf(prefs.getString("active_os", "Kali Linux") ?: "Kali Linux") }
    var hasDualBootUpgrade by remember { mutableStateOf(prefs.getBoolean("has_dualboot", false)) }
    var isTorInstalled by remember { mutableStateOf(prefs.getBoolean("is_tor_installed", false)) }

    // MINER FREISCHALT-STATUS (SOFTWARE & HARDWARE-TEILE)
    var isMinerSoftwareInstalled by remember { mutableStateOf(prefs.getBoolean("is_miner_installed", false)) }
    var hasMiningRigFrame by remember { mutableStateOf(prefs.getBoolean("has_rig_frame", false)) }
    var hasMiningPsu by remember { mutableStateOf(prefs.getBoolean("has_mining_psu", false)) }
    var hasDedicatedGpu by remember { mutableStateOf(prefs.getBoolean("has_dedicated_gpu", false)) }

    var balanceEur by remember { mutableIntStateOf(prefs.getInt("balance_eur", 180)) }
    var btcBalance by remember { mutableDoubleStateOf(prefs.getFloat("btc_balance", 0.0f).toDouble()) }
    var unconfirmedBtc by remember { mutableDoubleStateOf(0.0) }
    var totalMinedBlocks by remember { mutableIntStateOf(0) }
    var isMiningActive by remember { mutableStateOf(false) }

    var heatLevel by remember { mutableIntStateOf(prefs.getInt("heat_level", 0)) }
    var currentDay by remember { mutableIntStateOf(prefs.getInt("current_day", 1)) }

    // DEUTSCHE BERLIN UHRZEIT
    val berlinTimeZone = remember { TimeZone.getTimeZone("Europe/Berlin") }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.GERMANY).apply { timeZone = berlinTimeZone } }
    var berlinTimeStr by remember { mutableStateOf(timeFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            berlinTimeStr = timeFormat.format(Date())
            delay(1000)
        }
    }

    // Rig Status
    var rigName by remember { mutableStateOf(prefs.getString("rig_name", "Lenovo ThinkPad T480") ?: "Lenovo ThinkPad T480") }
    var currentCpu by remember { mutableStateOf(prefs.getString("current_cpu", "Intel Core i5-8250U") ?: "Intel Core i5-8250U") }
    var currentGpu by remember { mutableStateOf(prefs.getString("current_gpu", "Intel UHD 620 (Integriert)") ?: "Intel UHD 620 (Integriert)") }
    var hashrateMhs by remember { mutableDoubleStateOf(prefs.getFloat("hashrate_mhs", 0.0f).toDouble()) }
    var powerWatts by remember { mutableIntStateOf(prefs.getInt("power_watts", 0)) }
    var wifiAdapter by remember { mutableStateOf(prefs.getString("wifi_adapter", "Internes Wi-Fi (Kein Monitor-Mode)") ?: "Internes Wi-Fi (Kein Monitor-Mode)") }
    var hasMonitorMode by remember { mutableStateOf(prefs.getBoolean("has_monitor_mode", false)) }

    // REAL MINING TICK LOOP (NUR WENN MINER INSTALLIERT UND HARDWARE BEREIT)
    var currentMiningNonce by remember { mutableLongStateOf(0L) }
    var currentMiningHash by remember { mutableStateOf("0000a3f892c9b4e781...") }
    var btcPriceEur by remember { mutableIntStateOf(65400) }

    val isRigHardwareComplete = hasMiningRigFrame && hasMiningPsu && hasDedicatedGpu

    LaunchedEffect(isMiningActive, hashrateMhs, isRigHardwareComplete) {
        while (isMiningActive && isRigHardwareComplete && hashrateMhs > 0.0) {
            delay(800)
            currentMiningNonce += (hashrateMhs * 12500).toLong()
            currentMiningHash = "0000" + UUID.randomUUID().toString().replace("-", "").take(14)
            val btcReward = (hashrateMhs * 0.00000008)
            unconfirmedBtc += btcReward
        }
    }

    var currentHideoutId by remember { mutableStateOf(prefs.getString("hideout_id", "keller") ?: "keller") }

    val hideoutList = remember {
        listOf(
            Hideout("keller", "Feuchtes Keller-Zimmer", "Berlin-Wedding", 12, 150, 1, "Dein Start-Unterschlupf. Spartanisch, aber unauffällig vor der Polizei.", R.drawable.hideout_keller, 52.5480, 13.3600),
            Hideout("garage", "Industrie-Schraubergarage", "Berlin-Spandau", 28, 400, 3, "Starkstrom-Anschluss für Mining-Rigs. Senkt Polizei-Heat um 30%.", R.drawable.hideout_garage, 52.5350, 13.2000),
            Hideout("penthouse", "Kugelsicheres Luxus-Penthouse", "Berlin-Mitte", 95, 1800, 5, "Glasfaser-Standleitung, Krypto-Vault & eigene Notstrom-Generatoren.", R.drawable.hideout_penthouse, 52.5180, 13.3950)
        )
    }

    val currentHideout = hideoutList.find { it.id == currentHideoutId } ?: hideoutList[0]

    val companyTargets = remember {
        mutableStateListOf<CompanyTarget>().apply {
            addAll(generateCompaniesForDay(1))
        }
    }

    fun advanceToNextDay() {
        currentDay += 1
        val newCompanies = generateCompaniesForDay(currentDay)
        companyTargets.addAll(newCompanies)
        prefs.edit().putInt("current_day", currentDay).apply()
    }

    fun saveGame() {
        prefs.edit().apply {
            putBoolean("has_saved_game", true)
            putString("player_name", playerName)
            putString("hacker_alias", hackerAlias)
            putString("active_os", activeOS)
            putBoolean("has_dualboot", hasDualBootUpgrade)
            putBoolean("is_tor_installed", isTorInstalled)
            putBoolean("is_miner_installed", isMinerSoftwareInstalled)
            putBoolean("has_rig_frame", hasMiningRigFrame)
            putBoolean("has_mining_psu", hasMiningPsu)
            putBoolean("has_dedicated_gpu", hasDedicatedGpu)
            putInt("balance_eur", balanceEur)
            putFloat("btc_balance", btcBalance.toFloat())
            putInt("heat_level", heatLevel)
            putInt("current_day", currentDay)
            putString("rig_name", rigName)
            putString("current_cpu", currentCpu)
            putString("current_gpu", currentGpu)
            putFloat("hashrate_mhs", hashrateMhs.toFloat())
            putInt("power_watts", powerWatts)
            putString("wifi_adapter", wifiAdapter)
            putBoolean("has_monitor_mode", hasMonitorMode)
            putString("hideout_id", currentHideoutId)
            apply()
        }
    }

    // Windows & Desktop State
    var isTermOpen by remember { mutableStateOf(true) }
    var isMapOpen by remember { mutableStateOf(false) }
    var isBrowserOpen by remember { mutableStateOf(false) }
    var isTorBrowserOpen by remember { mutableStateOf(false) }
    var isSoftwareCenterOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isChatOpen by remember { mutableStateOf(false) }
    var isMinerOpen by remember { mutableStateOf(false) }

    // Start-Positionen der Fenster
    var termWinOffset by remember { mutableStateOf(Offset(240f, 20f)) }
    var mapWinOffset by remember { mutableStateOf(Offset(250f, 25f)) }
    var browserWinOffset by remember { mutableStateOf(Offset(260f, 30f)) }
    var torWinOffset by remember { mutableStateOf(Offset(270f, 35f)) }
    var softwareWinOffset by remember { mutableStateOf(Offset(280f, 40f)) }
    var settingsWinOffset by remember { mutableStateOf(Offset(290f, 45f)) }
    var chatWinOffset by remember { mutableStateOf(Offset(300f, 50f)) }
    var minerWinOffset by remember { mutableStateOf(Offset(310f, 55f)) }

    // Browser URLs
    var clearWebUrl by remember { mutableStateOf("about:home") }
    var clearWebInputUrl by remember { mutableStateOf("about:home") }
    var torWebUrl by remember { mutableStateOf("about:tor") }
    var torWebInputUrl by remember { mutableStateOf("about:tor") }

    fun vibrate(ms: Long) {
        vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("Schatten_030", "Willkommen in Berlin. Ich habe deine IP auf Shodan gesehen.", "23:48"),
            ChatMessage("KryptoBroker", "Um Bitcoin zu minen, musst du im Web Shop ein Rig-Gestell, 1200W Netzteil & GPUs kaufen und die Miner Software im Software Center installieren!", "23:51", true, 1400),
            ChatMessage("Ghost_Bln", "Lösche Logs mit 'shred -u /var/log/auth.log', falls die Polizei aufmerksam wird.", "23:55")
        )
    }

    var selectedTarget by remember { mutableStateOf<CompanyTarget?>(null) }

    val softwareCatalog = remember {
        mutableStateListOf(
            SoftwareTool("miner_soft", "Stratum V2 Bitcoin Miner Node (GUI)", "sudo apt install btc-miner-gui", "Krypto / Mining", 150, false, "Ermöglicht das Anschließen von Mining-Hardware an das Bitcoin-Mainnet.", isInstalled = isMinerSoftwareInstalled),
            SoftwareTool("tor", "Tor Anonymity Service & Browser", "sudo apt install tor", "Netzwerk / Darknet", 0, true, "Ermöglicht Zugriff auf .onion Darknet-Märkte & Zero-Day Schwarzmärkte.", isInstalled = isTorInstalled),
            SoftwareTool("sqlmap", "sqlmap Automatic SQLi Tool", "sudo apt install sqlmap", "Web Exploitation", 0, true, "Automatisiert das Injizieren und Extrahieren von Firmen-Datenbanken.", isInstalled = prefs.getBoolean("tool_sqlmap", false)),
            SoftwareTool("wireshark", "Wireshark Packet Sniffer", "sudo apt install wireshark", "Netzwerkanalyse", 0, true, "Fängt Passwörter und HTTP-Sessions im lokalen WLAN ab.", isInstalled = prefs.getBoolean("tool_wireshark", false)),
            SoftwareTool("hydra", "THC Hydra Fast Login Cracker", "sudo apt install hydra", "Brute-Force", 0, true, "Paralleles Brute-Forcing von SSH-, FTP- und RDP-Logins.", isInstalled = prefs.getBoolean("tool_hydra", false)),
            SoftwareTool("burp_pro", "Burp Suite Professional 2026", "burpsuite_pro", "Web-Sicherheit", 180, false, "Profi-Proxy mit Schwachstellen-Scanner für Enterprise-APIs.", isInstalled = prefs.getBoolean("tool_burp", false)),
            SoftwareTool("cobalt", "Cobalt Strike C2 Framework", "cobaltstrike", "Militärisch / C2", 650, false, "Militärisches Command & Control Kit für dauerhafte Persistenz.", isInstalled = prefs.getBoolean("tool_cobalt", false))
        )
    }

    val hardwareShop = remember {
        mutableStateListOf(
            HardwareItem("rig_frame", "Aluminium Open-Air Mining Frame (6-GPU)", "Mining-Hardware", 85, 0.0, 0, "Grundgerüst mit Riser-Karten zur Montage von Mining-Grafikkarten.", isBought = hasMiningRigFrame),
            HardwareItem("mining_psu", "Corsair 1200W Titanium Mining Netzteil", "Mining-Hardware", 160, 0.0, 0, "Liefert stabilen Strom für Hochleistungs-GPUs und Mining-Rigs.", isBought = hasMiningPsu),
            HardwareItem("rtx4070", "NVIDIA GeForce RTX 4070 Mining Edition", "Grafikkarte", 380, 24.5, 140, "24.5 MH/s Mining & Brute-Force Power.", isBought = currentGpu.contains("4070")),
            HardwareItem("rtx4090", "NVIDIA GeForce RTX 4090 Monster Rig", "Grafikkarte", 750, 78.0, 350, "78.0 MH/s Hashrate! Ultimative Mining-Power.", isBought = currentGpu.contains("4090")),
            HardwareItem("asic_s19", "Antminer S19 Pro Bitcoin ASIC Miner", "ASIC Miner", 1250, 210.0, 850, "210.0 MH/s dedizierte SHA-256 Mining-Maschine für Profis.", isBought = currentGpu.contains("Antminer")),
            HardwareItem("dualboot_lic", "Dual-Boot SSD & VM-Hypervisor Lizenz", "Software / OS", 120, 0.0, 0, "Ermöglicht nahtloses Umschalten zwischen Kali Linux und Windows 11.", isBought = hasDualBootUpgrade),
            HardwareItem("alfa", "Alfa Network AWUS036ACH USB-Adapter", "WLAN-Antenne", 45, 0.0, 5, "Unterstützt 2.4/5GHz Monitor Mode & Packet Injection für airodump-ng.", isBought = hasMonitorMode)
        )
    }

    val darknetExploitShop = remember {
        mutableStateListOf(
            ExploitPayloadItem("0day_apache", "0-Day Apache Path Traversal RCE", "ShadowBroker_030", 350, "Volle Remote Code Execution auf Linux-Webservern.", "185.220.101.5", isBought = prefs.getBoolean("payload_0day", false)),
            ExploitPayloadItem("db_alex_dump", "Deutsche Bank Alex Pass-The-Hash Kit", "AnonymousBerlin", 500, "Enthält NTLM-Hashes aus dem Alexanderplatz-Filialnetz.", "212.58.244.70", isBought = prefs.getBoolean("payload_db", false)),
            ExploitPayloadItem("bka_cleaner", "BKA / Polizei Datenbank Cleaner Script", "GhostInTheShell", 220, "Löscht Ermittlungsakten und setzt den Polizei-Heat sofort auf 0%.", "127.0.0.1")
        )
    }

    val terminalHistory = remember {
        mutableStateListOf(
            "┌──($hackerAlias㉿thinkpad)-[~]",
            "└─$ neofetch",
            "OS: $activeOS 2026.2 x86_64",
            "Host: Lenovo ThinkPad T480 (Berlin Node 030)",
            "Berlin Time: $berlinTimeStr (Europe/Berlin)",
            "Tippe 'help' oder öffne die 'Berlin Map' um Ziele zu scannen."
        )
    }
    var terminalInput by remember { mutableStateOf("") }

    fun isToolInstalled(id: String): Boolean {
        return softwareCatalog.find { it.id == id }?.isInstalled == true
    }

    fun hasExploitBought(id: String): Boolean {
        return darknetExploitShop.find { it.id == id }?.isBought == true
    }

    fun simulateAptInstall(packageName: String, onComplete: () -> Unit) {
        coroutineScope.launch {
            terminalHistory.add("Paketlisten werden gelesen… Fertig")
            delay(250)
            terminalHistory.add("Abhängigkeitsbaum wird aufgebaut… Fertig")
            delay(250)
            terminalHistory.add("Die folgenden NEUEN Pakete werden installiert: $packageName libssl3 ca-certificates")
            delay(350)
            terminalHistory.add("Es müssen 14,2 MB an Archiven heruntergeladen werden.")
            delay(400)
            terminalHistory.add("Holen:1 http://http.kali.org/kali kali-rolling/main amd64 $packageName [14,2 MB]")
            delay(500)
            terminalHistory.add("14,2 MB in 1s geholt (12,4 MB/s)")
            delay(300)
            terminalHistory.add("Entpacken und Einrichten von $packageName (amd64) …")
            delay(400)
            terminalHistory.add("[✓] $packageName erfolgreich installiert und betriebsbereit.")
            vibrate(60)
            onComplete()
        }
    }

    fun parseLinuxCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        terminalHistory.add("┌──($hackerAlias㉿thinkpad)-[~]\n└─$ $trimmed")
        terminalInput = ""
        vibrate(25)

        val parts = trimmed.split(" ")
        val mainCmd = parts[0].lowercase()

        if (mainCmd == "apt" || (mainCmd == "sudo" && parts.size > 1 && parts[1].lowercase() == "apt")) {
            val isInstall = parts.contains("install")
            if (isInstall) {
                if (parts.contains("tor")) {
                    simulateAptInstall("tor") {
                        isTorInstalled = true
                        softwareCatalog.find { it.id == "tor" }?.isInstalled = true
                        terminalHistory.add("[✓] Tor Browser im Cyber-Dock freigeschaltet!")
                        saveGame()
                    }
                    return
                } else if (parts.contains("btc-miner-gui") || parts.contains("miner")) {
                    if (balanceEur >= 150) {
                        balanceEur -= 150
                        simulateAptInstall("btc-miner-gui") {
                            isMinerSoftwareInstalled = true
                            softwareCatalog.find { it.id == "miner_soft" }?.isInstalled = true
                            terminalHistory.add("[✓] Bitcoin Miner Node auf dem Desktop freigeschaltet!")
                            saveGame()
                        }
                    } else {
                        terminalHistory.add("[-] Error: Nicht genügend Guthaben für Miner-Lizenz (150€ nötig)!")
                    }
                    return
                } else if (parts.contains("sqlmap")) {
                    simulateAptInstall("sqlmap") {
                        softwareCatalog.find { it.id == "sqlmap" }?.isInstalled = true
                        saveGame()
                    }
                    return
                } else if (parts.contains("hydra")) {
                    simulateAptInstall("hydra") {
                        softwareCatalog.find { it.id == "hydra" }?.isInstalled = true
                        saveGame()
                    }
                    return
                }
            }
        }

        when (mainCmd) {
            "help" -> {
                terminalHistory.add("--- KALI CYBER-KILL-CHAIN COMMANDS ---")
                terminalHistory.add("• nmap -sV [IP]             : Portscan & Schwachstellen-Analyse")
                terminalHistory.add("• sqlmap -u [URL] --dbs     : SQL-Injection auf API ausführen")
                terminalHistory.add("• hydra -l admin -P pass.txt [IP] rdp : RDP/SSH Login Brute-Force")
                terminalHistory.add("• airodump-ng wlan0mon       : WLAN Handshakes sniffen")
                terminalHistory.add("• hashcat -m 22000 [file]   : GPU Handshake Cracker starten")
                terminalHistory.add("• exfiltrate [target_id/IP] : Beute auf dein Bankkonto transferieren")
                terminalHistory.add("• nextday                   : Zum nächsten Tag springen (+neue Firmen)")
                terminalHistory.add("• shred -u /var/log/auth.log: Spuren vernichten (Polizei-Heat = 0)")
                terminalHistory.add("• clear                     : Terminal leeren")
            }
            "clear" -> terminalHistory.clear()
            "nextday" -> {
                advanceToNextDay()
                terminalHistory.add("[+] Tag $currentDay in Berlin angebrochen! Neue Firmen haben eröffnet!")
                vibrate(60)
            }
            "nmap" -> {
                val ip = if (parts.size > 2) parts[2] else if (parts.size > 1) parts[1] else ""
                val target = companyTargets.find { it.ip == ip }

                if (target == null) {
                    terminalHistory.add("Syntax: nmap -sV <IP>  (Klicke ein Ziel auf der Berlin Map an)")
                } else if (target.isPwned) {
                    terminalHistory.add("[!] ${target.name} ($ip) wurde bereits erfolgreich gehackt!")
                } else {
                    coroutineScope.launch {
                        terminalHistory.add("Starting Nmap 7.94 scan on $ip (${target.name})...")
                        delay(400)
                        terminalHistory.add("Host is up (0.0038s latency). Sektor: ${target.sector}")
                        terminalHistory.add("PORT: ${target.vulnPort} | Schwachstelle: ${target.vulnType}")
                        terminalHistory.add("[+] Empfohlenes Angriffs-Tool: ${target.requiredTools}")
                        if (target.breachStage == 0) {
                            target.breachStage = 1
                            saveGame()
                        }
                        vibrate(40)
                    }
                }
            }
            "sqlmap" -> {
                val target = companyTargets.find { it.requiredTools.contains("sqlmap") && !it.isPwned }
                if (target == null) {
                    terminalHistory.add("[-] Kein aktives SQL-Injection Ziel gefunden. Scanne Firmen auf der Map!")
                } else {
                    coroutineScope.launch {
                        terminalHistory.add("[sqlmap] Scanning ${target.name} (${target.ip})...")
                        delay(500)
                        terminalHistory.add("[✓] Datenbank 'production_data' erfolgreich kompromittiert!")
                        terminalHistory.add("[+] Exfiltriere Beute mit: 'exfiltrate ${target.id}'")
                        target.breachStage = 3
                        saveGame()
                        vibrate(60)
                    }
                }
            }
            "hydra" -> {
                val target = companyTargets.find { it.requiredTools.contains("hydra") && !it.isPwned }
                if (target == null) {
                    terminalHistory.add("[-] Kein aktives RDP/SSH Brute-Force Ziel gefunden.")
                } else {
                    coroutineScope.launch {
                        terminalHistory.add("[hydra] Starte 32 parallele Threads gegen ${target.ip}...")
                        delay(600)
                        terminalHistory.add("[✓] LOGIN ERFOLGREICH! user: admin  password: Berlin#Secure2026")
                        terminalHistory.add("[+] Exfiltriere Beute mit: 'exfiltrate ${target.id}'")
                        target.breachStage = 3
                        saveGame()
                        vibrate(60)
                    }
                }
            }
            "airmon-ng" -> {
                if (!hasMonitorMode && !wifiAdapter.contains("Alfa")) {
                    terminalHistory.add("[-] Error: Internes Wi-Fi unterstützt keinen Monitor Mode! Kaufe Alfa-Adapter im Shop.")
                } else {
                    hasMonitorMode = true
                    saveGame()
                    terminalHistory.add("[✓] Interface wlan0mon switched to MONITOR MODE.")
                }
            }
            "airodump-ng" -> {
                val target = companyTargets.find { it.requiredTools.contains("airodump") && !it.isPwned }
                if (target == null) {
                    terminalHistory.add("[-] Kein ungesichertes WLAN in Reichweite gefunden.")
                } else {
                    coroutineScope.launch {
                        terminalHistory.add("CH 6  BSSID: E4:8D:8C:11:22:33  ESSID: '${target.name}'")
                        delay(400)
                        terminalHistory.add("[✓] WPA2 4-Way Handshake erfasst -> handshake.hc22000")
                        terminalHistory.add("[+] Nächster Schritt: 'hashcat -m 22000 handshake.hc22000'")
                        target.breachStage = 2
                        saveGame()
                        vibrate(40)
                    }
                }
            }
            "hashcat" -> {
                val target = companyTargets.find { it.requiredTools.contains("hashcat") && !it.isPwned }
                if (target == null || target.breachStage < 2) {
                    terminalHistory.add("[-] Error: Kein Handshake erfasst! Vorher: airodump-ng wlan0mon")
                } else {
                    coroutineScope.launch {
                        terminalHistory.add("[hashcat] Initialisiere GPU ($currentGpu)...")
                        delay(500)
                        terminalHistory.add("[✓] STATUS: CRACKED! Key: 'berlin2026!'")
                        terminalHistory.add("[+] Exfiltriere Beute mit: 'exfiltrate ${target.id}'")
                        target.breachStage = 3
                        saveGame()
                        vibrate(70)
                    }
                }
            }
            "exfiltrate" -> {
                val targetId = if (parts.size > 1) parts[1] else ""
                val target = companyTargets.find { it.id == targetId || it.ip == targetId }

                if (target == null) {
                    terminalHistory.add("Syntax: exfiltrate <target_id oder IP>")
                } else if (target.isPwned) {
                    terminalHistory.add("[-] Error: Diese Firma wurde bereits vollständig gehackt und ausbezahlt!")
                } else if (target.breachStage < 3) {
                    terminalHistory.add("[-] Error: Noch kein vollständiger Root-Zugriff auf ${target.name}!")
                } else {
                    target.isPwned = true
                    balanceEur += target.bountyEur
                    heatLevel = (heatLevel + 18).coerceAtMost(100)
                    saveGame()
                    vibrate(90)
                    terminalHistory.add("==========================================")
                    terminalHistory.add("[$$$] EXFILTRATION ERFOLGREICH!")
                    terminalHistory.add("[+] +${target.bountyEur}€ auf dein Konto überwiesen!")
                    terminalHistory.add("[!] Polizei-Heat: $heatLevel% | Lösche Logs mit 'shred -u /var/log/auth.log'")
                    terminalHistory.add("==========================================")
                }
            }
            "shred" -> {
                terminalHistory.add("[✓] 3-Pass Zero-Fill abgeschlossen. Alle Logs gelöscht. Polizei-Heat = 0%!")
                heatLevel = 0
                saveGame()
                vibrate(40)
            }
            else -> terminalHistory.add("Befehl '$mainCmd' nicht gefunden. Tippe 'help' für alle Befehle.")
        }
    }

    when (gameStateStep) {
        -1 -> SplashScreenView { gameStateStep = 0; vibrate(40) }
        0 -> NameInputScreen(
            playerName = playerName,
            onPlayerNameChange = { playerName = it },
            hackerAlias = hackerAlias,
            onHackerAliasChange = { hackerAlias = it },
            onNext = { gameStateStep = 1; vibrate(40) }
        )
        1 -> OSSelectionScreen { selectedOS ->
            activeOS = selectedOS
            gameStateStep = 2
            vibrate(40)
        }
        2 -> StoryScreen(playerName = playerName, hackerAlias = hackerAlias, activeOS = activeOS) {
            gameStateStep = 3
            vibrate(40)
        }
        3 -> ApartmentScreen(balanceEur = balanceEur) {
            balanceEur -= 150
            gameStateStep = 4
            saveGame()
            vibrate(50)
        }
        4 -> DesktopScreen(
            activeOS = activeOS,
            hackerAlias = hackerAlias,
            balanceEur = balanceEur,
            btcBalance = btcBalance,
            heatLevel = heatLevel,
            currentDay = currentDay,
            berlinTimeStr = berlinTimeStr,
            onNextDay = { advanceToNextDay(); vibrate(50) },
            hasDualBootUpgrade = hasDualBootUpgrade,
            onToggleOS = {
                activeOS = if (activeOS == "Kali Linux") "Windows 11" else "Kali Linux"
                saveGame()
                vibrate(35)
            },
            isTermOpen = isTermOpen,
            onTermToggle = { isTermOpen = it },
            isMapOpen = isMapOpen,
            onMapToggle = { isMapOpen = it },
            isBrowserOpen = isBrowserOpen,
            onBrowserToggle = { isBrowserOpen = it },
            isTorBrowserOpen = isTorBrowserOpen,
            onTorBrowserToggle = { isTorBrowserOpen = it },
            isSoftwareCenterOpen = isSoftwareCenterOpen,
            onSoftwareCenterToggle = { isSoftwareCenterOpen = it },
            isSettingsOpen = isSettingsOpen,
            onSettingsToggle = { isSettingsOpen = it },
            isChatOpen = isChatOpen,
            onChatToggle = { isChatOpen = it },
            isMinerOpen = isMinerOpen,
            onMinerToggle = { isMinerOpen = it },
            isTorInstalled = isTorInstalled,
            isMinerSoftwareInstalled = isMinerSoftwareInstalled,
            hasMiningRigFrame = hasMiningRigFrame,
            hasMiningPsu = hasMiningPsu,
            hasDedicatedGpu = hasDedicatedGpu,
            termWinOffset = termWinOffset,
            onTermWinOffsetChange = { termWinOffset = it },
            mapWinOffset = mapWinOffset,
            onMapWinOffsetChange = { mapWinOffset = it },
            browserWinOffset = browserWinOffset,
            onBrowserWinOffsetChange = { browserWinOffset = it },
            torWinOffset = torWinOffset,
            onTorWinOffsetChange = { torWinOffset = it },
            softwareWinOffset = softwareWinOffset,
            onSoftwareWinOffsetChange = { softwareWinOffset = it },
            settingsWinOffset = settingsWinOffset,
            onSettingsWinOffsetChange = { settingsWinOffset = it },
            chatWinOffset = chatWinOffset,
            onChatWinOffsetChange = { chatWinOffset = it },
            minerWinOffset = minerWinOffset,
            onMinerWinOffsetChange = { minerWinOffset = it },
            currentHideout = currentHideout,
            companyTargets = companyTargets,
            selectedTarget = selectedTarget,
            onSelectTarget = { selectedTarget = it },
            terminalHistory = terminalHistory,
            terminalInput = terminalInput,
            onTerminalInputChange = { terminalInput = it },
            onRunCommand = { parseLinuxCommand(it) },
            chatMessages = chatMessages,
            softwareCatalog = softwareCatalog,
            hardwareShop = hardwareShop,
            darknetExploitShop = darknetExploitShop,
            clearWebUrl = clearWebUrl,
            clearWebInputUrl = clearWebInputUrl,
            onClearWebUrlChange = { clearWebUrl = it },
            onClearWebInputUrlChange = { clearWebInputUrl = it },
            torWebUrl = torWebUrl,
            torWebInputUrl = torWebInputUrl,
            onTorWebUrlChange = { torWebUrl = it },
            onTorWebInputUrlChange = { torWebInputUrl = it },
            hideoutList = hideoutList,
            playerName = playerName,
            rigName = rigName,
            currentCpu = currentCpu,
            currentGpu = currentGpu,
            wifiAdapter = wifiAdapter,
            hashrateMhs = hashrateMhs,
            powerWatts = powerWatts,
            isMiningActive = isMiningActive,
            onToggleMining = { isMiningActive = !isMiningActive; vibrate(30) },
            unconfirmedBtc = unconfirmedBtc,
            currentMiningNonce = currentMiningNonce,
            currentMiningHash = currentMiningHash,
            btcPriceEur = btcPriceEur,
            onTransferMiningRewards = {
                btcBalance += unconfirmedBtc
                unconfirmedBtc = 0.0
                totalMinedBlocks += 1
                saveGame()
                vibrate(50)
            },
            onSellBtc = { amountBtc ->
                if (btcBalance >= amountBtc && amountBtc > 0.0) {
                    btcBalance -= amountBtc
                    val euroGain = (amountBtc * btcPriceEur).toInt()
                    balanceEur += euroGain
                    saveGame()
                    vibrate(60)
                }
            },
            onBuySoftware = { tool ->
                if (tool.id == "tor") isTorInstalled = true
                if (tool.id == "miner_soft") isMinerSoftwareInstalled = true
                if (tool.isFreeRepo || balanceEur >= tool.priceEur) {
                    if (!tool.isFreeRepo) balanceEur -= tool.priceEur
                    val index = softwareCatalog.indexOfFirst { it.id == tool.id }
                    if (index != -1) {
                        softwareCatalog[index] = softwareCatalog[index].copy(isInstalled = true)
                    }
                    saveGame()
                    vibrate(50)
                }
            },
            onBuyHardware = { item ->
                if (balanceEur >= item.priceEur && !item.isBought) {
                    balanceEur -= item.priceEur
                    val index = hardwareShop.indexOfFirst { it.id == item.id }
                    if (index != -1) {
                        hardwareShop[index] = hardwareShop[index].copy(isBought = true)
                    }
                    if (item.id == "dualboot_lic") hasDualBootUpgrade = true
                    else if (item.id == "alfa") { wifiAdapter = item.name; hasMonitorMode = true }
                    else if (item.id == "rig_frame") { hasMiningRigFrame = true }
                    else if (item.id == "mining_psu") { hasMiningPsu = true }
                    else {
                        hasDedicatedGpu = true
                        currentGpu = item.name
                        hashrateMhs += item.hashSpeedMhs
                        powerWatts += item.powerWatt
                    }
                    saveGame()
                    vibrate(60)
                }
            },
            onBuyDarknetExploit = { exploit ->
                if (balanceEur >= exploit.priceEur && !exploit.isBought) {
                    balanceEur -= exploit.priceEur
                    val index = darknetExploitShop.indexOfFirst { it.id == exploit.id }
                    if (index != -1) {
                        darknetExploitShop[index] = darknetExploitShop[index].copy(isBought = true)
                    }
                    if (exploit.id == "bka_cleaner") heatLevel = 0
                    saveGame()
                    vibrate(70)
                }
            },
            onChangeHideout = { h ->
                if (balanceEur >= h.depositEur) {
                    balanceEur -= h.depositEur
                    currentHideoutId = h.id
                    saveGame()
                    vibrate(60)
                }
            },
            vibrate = { vibrate(it) }
        )
    }
}

// ------------------- SUB-SCREENS -------------------

@Composable
fun SplashScreenView(onBoot: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(id = R.drawable.splashscreen),
            contentDescription = "Splash Screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x99000000), Color(0xF0000000)),
                        startY = 400f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("GLITCH PROTOCOL : BREACH OS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
            Text("BERLIN UNDERGROUND CYBERNETIC OS", color = KaliBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))

            Button(
                onClick = onBoot,
                colors = ButtonDefaults.buttonColors(containerColor = KaliBlue),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth().height(42.dp)
            ) {
                Text("SYSTEM BOOTEN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun NameInputScreen(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    hackerAlias: String,
    onHackerAliasChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF07090E)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Logo",
            modifier = Modifier.size(72.dp).padding(bottom = 12.dp)
        )
        Text("GLITCH PROTOCOL : BREACH OS", color = KaliBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Gib deine Identität für das System ein:", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

        Column(modifier = Modifier.widthIn(max = 420.dp)) {
            TextField(
                value = playerName,
                onValueChange = onPlayerNameChange,
                placeholder = { Text("Dein bürgerlicher Name (z.B. Alex Müller)", color = Color.DarkGray, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF141724), unfocusedContainerColor = Color(0xFF141724), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            TextField(
                value = hackerAlias,
                onValueChange = onHackerAliasChange,
                placeholder = { Text("Dein Hacker-Alias (z.B. zer0_day)", color = Color.DarkGray, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF141724), unfocusedContainerColor = Color(0xFF141724), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Button(
                onClick = onNext,
                enabled = playerName.isNotBlank() && hackerAlias.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = KaliBlue),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text("WEITER: BETRIEBSSYSTEM WÄHLEN", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun OSSelectionScreen(onSelectOS: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF07090E)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("BETRIEBSSYSTEM WÄHLEN", color = KaliBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Wähle das Betriebssystem für dein erstes ThinkPad-Rig:", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))

        Row(modifier = Modifier.widthIn(max = 700.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(
                modifier = Modifier.weight(1f).clickable { onSelectOS("Kali Linux") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131724)),
                border = BorderStroke(1.dp, KaliBlue)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🐉 KALI LINUX 2026", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("• Vorinstallierte Hacking-Tools (Aircrack, Hashcat, Nmap)\n• Schneller Terminal-Workflow & minimaler RAM-Bedarf\n• Volle Kontrolle über Wi-Fi Interfaces", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Card(
                modifier = Modifier.weight(1f).clickable { onSelectOS("Windows 11") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131724)),
                border = BorderStroke(1.dp, Win11Blue)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🪟 WINDOWS 11 PRO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("• PowerShell & Mimikatz NTLM-Dumping\n• Windows Defender & GUI-Tools\n• Dual-Boot mit Kali Linux kann später erworben werden", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun StoryScreen(playerName: String, hackerAlias: String, activeOS: String, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF07090E)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 580.dp)) {
            Text("KAPITEL 1: ANKUNFT IN BERLIN", color = WarningAmber, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Es ist 23:45 Uhr. Der Zug rollt im verregneten Berliner Hauptbahnhof ein.\n\n" +
                        "Du, $playerName (alias '$hackerAlias'), hast alles hinter dir gelassen. " +
                        "In deinem Rucksack: Ein gebrauchtes Lenovo ThinkPad T480 (mit $activeOS), ein Ladekabel und genau 450€ Bargeld in der Tasche.\n\n" +
                        "Dein Ziel: Du willst dir in der Berliner Cyber-Unterwelt einen Namen machen und dein eigenes Hacking-Imperium aufbauen.\n\n" +
                        "Doch zuerst brauchst du ein Dach über dem Kopf, um dein Rig sicher vor der Polizei aufzubauen...",
                color = Color.LightGray,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = TermGreen),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text("ERSTE WOHNUNG IN BERLIN MIETEN", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ApartmentScreen(balanceEur: Int, onRent: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF07090E)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 560.dp)) {
            Text("IMMOBILIEN-AUSWAHL BERLIN", color = KaliBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Verfügbares Budget: $balanceEur€", color = TermGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141724)),
                border = BorderStroke(1.dp, KaliBlue)
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.hideout_keller),
                        contentDescription = "Keller Wedding",
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Keller-Zimmer (Berlin-Wedding)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("• Kaution: 150€ (wird sofort fällig)\n• Miete: 12€ / Tag\n• Zustand: Feucht, spartanisch, aber keine Nachbarn und abhörsicher.", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 6.dp))
                        Button(
                            onClick = onRent,
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("ZIMMER MIETEN (-150€)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ------------------- MAIN DESKTOP -------------------

@Composable
fun DesktopScreen(
    activeOS: String,
    hackerAlias: String,
    balanceEur: Int,
    btcBalance: Double,
    heatLevel: Int,
    currentDay: Int,
    berlinTimeStr: String,
    onNextDay: () -> Unit,
    hasDualBootUpgrade: Boolean,
    onToggleOS: () -> Unit,
    isTermOpen: Boolean,
    onTermToggle: (Boolean) -> Unit,
    isMapOpen: Boolean,
    onMapToggle: (Boolean) -> Unit,
    isBrowserOpen: Boolean,
    onBrowserToggle: (Boolean) -> Unit,
    isTorBrowserOpen: Boolean,
    onTorBrowserToggle: (Boolean) -> Unit,
    isSoftwareCenterOpen: Boolean,
    onSoftwareCenterToggle: (Boolean) -> Unit,
    isSettingsOpen: Boolean,
    onSettingsToggle: (Boolean) -> Unit,
    isChatOpen: Boolean,
    onChatToggle: (Boolean) -> Unit,
    isMinerOpen: Boolean,
    onMinerToggle: (Boolean) -> Unit,
    isTorInstalled: Boolean,
    isMinerSoftwareInstalled: Boolean,
    hasMiningRigFrame: Boolean,
    hasMiningPsu: Boolean,
    hasDedicatedGpu: Boolean,
    termWinOffset: Offset,
    onTermWinOffsetChange: (Offset) -> Unit,
    mapWinOffset: Offset,
    onMapWinOffsetChange: (Offset) -> Unit,
    browserWinOffset: Offset,
    onBrowserWinOffsetChange: (Offset) -> Unit,
    torWinOffset: Offset,
    onTorWinOffsetChange: (Offset) -> Unit,
    softwareWinOffset: Offset,
    onSoftwareWinOffsetChange: (Offset) -> Unit,
    settingsWinOffset: Offset,
    onSettingsWinOffsetChange: (Offset) -> Unit,
    chatWinOffset: Offset,
    onChatWinOffsetChange: (Offset) -> Unit,
    minerWinOffset: Offset,
    onMinerWinOffsetChange: (Offset) -> Unit,
    currentHideout: Hideout,
    companyTargets: List<CompanyTarget>,
    selectedTarget: CompanyTarget?,
    onSelectTarget: (CompanyTarget?) -> Unit,
    terminalHistory: List<String>,
    terminalInput: String,
    onTerminalInputChange: (String) -> Unit,
    onRunCommand: (String) -> Unit,
    chatMessages: List<ChatMessage>,
    softwareCatalog: List<SoftwareTool>,
    hardwareShop: List<HardwareItem>,
    darknetExploitShop: List<ExploitPayloadItem>,
    clearWebUrl: String,
    clearWebInputUrl: String,
    onClearWebUrlChange: (String) -> Unit,
    onClearWebInputUrlChange: (String) -> Unit,
    torWebUrl: String,
    torWebInputUrl: String,
    onTorWebUrlChange: (String) -> Unit,
    onTorWebInputUrlChange: (String) -> Unit,
    hideoutList: List<Hideout>,
    playerName: String,
    rigName: String,
    currentCpu: String,
    currentGpu: String,
    wifiAdapter: String,
    hashrateMhs: Double,
    powerWatts: Int,
    isMiningActive: Boolean,
    onToggleMining: () -> Unit,
    unconfirmedBtc: Double,
    currentMiningNonce: Long,
    currentMiningHash: String,
    btcPriceEur: Int,
    onTransferMiningRewards: () -> Unit,
    onSellBtc: (Double) -> Unit,
    onBuySoftware: (SoftwareTool) -> Unit,
    onBuyHardware: (HardwareItem) -> Unit,
    onBuyDarknetExploit: (ExploitPayloadItem) -> Unit,
    onChangeHideout: (Hideout) -> Unit,
    vibrate: (Long) -> Unit
) {
    // DOCK APPS: BTC MINER WIRD NUR ANGEZEIGT WENN DIE SOFTWARE GEKAUFT WURDE!
    val desktopApps = remember(isTorInstalled, isMinerSoftwareInstalled) {
        mutableStateListOf(
            DesktopAppEntry("term", "Terminal", "💻", TermGreen),
            DesktopAppEntry("map", "Maps", "🗺️", Color(0xFF3498DB)),
            DesktopAppEntry("chat", "Chat", "💬", Color(0xFF3498DB)),
            DesktopAppEntry("software", "Software", "📦", WarningAmber),
            DesktopAppEntry("browser", "Browser", "🌐", KaliBlue),
            DesktopAppEntry("rig", "Mein Rig", "🛠️", Color(0xFF1ABC9C))
        ).apply {
            if (isMinerSoftwareInstalled) {
                add(2, DesktopAppEntry("miner", "BTC Miner", "⛏️", Color(0xFFF1C40F)))
            }
            if (isTorInstalled && none { it.id == "tor" }) {
                add(DesktopAppEntry("tor", "Tor Browser", "🧅", Color(0xFF9B59B6)))
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (activeOS == "Kali Linux") KaliBgDark else Color(0xFF070B14))
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val windowWidth = (screenWidth * 0.74f).coerceIn(440.dp, 840.dp)
        val windowHeight = (screenHeight * 0.80f).coerceIn(340.dp, 660.dp)

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP STATUS PANEL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (activeOS == "Kali Linux") KaliPanelBg else Color(0xFF0F1424))
                    .border(BorderStroke(1.dp, Color(0xFF1C2236)))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (activeOS == "Kali Linux") "🐉 KALI LINUX" else "🪟 WINDOWS 11",
                        color = if (activeOS == "Kali Linux") KaliBlue else Win11Blue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("($hackerAlias)", color = Color.Gray, fontSize = 11.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💵 $balanceEur€", color = TermGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("₿ ${String.format("%.5f", btcBalance)}", color = Color(0xFFF1C40F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("🔥 $heatLevel%", color = if (heatLevel > 50) ErrorRed else WarningAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp)

                    Surface(color = Color(0xFF192033), shape = RoundedCornerShape(4.dp)) {
                        Text("🕒 $berlinTimeStr (Berlin)", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }

                    Button(
                        onClick = onNextDay,
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("☀️ Tag $currentDay beenden", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (hasDualBootUpgrade) {
                    Button(
                        onClick = onToggleOS,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222838)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("🔄 DUAL-BOOT", fontSize = 10.sp, color = Color.White)
                    }
                }
            }

            // 2. DESKTOP WORKSPACE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // CYBER-WALLPAPER
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF10162B), Color(0xFF060810)),
                                radius = 1200f
                            )
                        )
                )

                // DESKTOP DOCK (LINKE SEITE)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(96.dp)
                        .background(Color(0x880D111E))
                        .border(BorderStroke(1.dp, Color(0x3328324E)))
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    desktopApps.forEach { app ->
                        CleanDesktopIcon(
                            name = app.name,
                            icon = app.icon,
                            accentColor = app.color,
                            onClick = {
                                vibrate(20)
                                when (app.id) {
                                    "term" -> onTermToggle(true)
                                    "map" -> onMapToggle(true)
                                    "miner" -> onMinerToggle(true)
                                    "chat" -> onChatToggle(true)
                                    "software" -> onSoftwareCenterToggle(true)
                                    "browser" -> onBrowserToggle(true)
                                    "tor" -> onTorBrowserToggle(true)
                                    "rig" -> onSettingsToggle(true)
                                }
                            }
                        )
                    }
                }

                // ================= FREI VERSCHIEBBARE MULTI-WINDOWS =================

                // 1. MAPS FENSTER (DEUTSCHLANDKARTE MIT ZIELEN & NAVIGATION)
                if (isMapOpen) {
                    SmoothMovableWindow(
                        title = "🗺️ Maps - Deutschland (${companyTargets.size} Ziele aktiv)",
                        initialOffset = mapWinOffset,
                        width = windowWidth,
                        height = windowHeight,
                        borderColor = Color(0xFF3498DB),
                        onPositionChange = onMapWinOffsetChange,
                        onClose = { onMapToggle(false); vibrate(20) }
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .fillMaxHeight()
                                    .background(Color(0xFFE8ECEF))
                            ) {
                                var mapScale by remember { mutableFloatStateOf(1.0f) }
                                var mapPanOffset by remember { mutableStateOf(Offset.Zero) }

                                BoxWithConstraints(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                mapPanOffset += dragAmount
                                            }
                                        }
                                ) {
                                    val mapW = maxWidth
                                    val mapH = maxHeight

                                    // Container der gemeinsam mit Bild und Markern gezoomt und verschoben wird
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .offset { IntOffset(mapPanOffset.x.roundToInt(), mapPanOffset.y.roundToInt()) }
                                    ) {
                                        // 1. Saubere Deutschlandkarte (Vollflächig im Content-Bereich)
                                        Image(
                                            painter = painterResource(id = R.drawable.germany_map),
                                            contentDescription = "Maps Deutschland",
                                            modifier = Modifier
                                                .fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // 🏠 Eigener Standort (Berlin-Bereich)
                                        Box(
                                            modifier = Modifier
                                                .offset(
                                                    x = mapW * 0.40f - 16.dp,
                                                    y = mapH * 0.32f - 16.dp
                                                )
                                                .size(32.dp)
                                                .background(KaliBlue, CircleShape)
                                                .border(2.dp, Color.White, CircleShape)
                                                .clickable { onSelectTarget(null); vibrate(20) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("📍", fontSize = 16.sp)
                                        }

                                        // 🏢 Firmen-Marker auf der Deutschlandkarte
                                        companyTargets.forEachIndexed { idx, company ->
                                            val isSel = selectedTarget?.id == company.id
                                            val xOffset = 0.15f + ((idx * 0.083f) % 0.65f)
                                            val yOffset = 0.18f + (((idx * 0.137f) + (idx * 0.03f)) % 0.62f)

                                            Box(
                                                modifier = Modifier
                                                    .offset(
                                                        x = mapW * xOffset - 14.dp,
                                                        y = mapH * yOffset - 14.dp
                                                    )
                                                    .size(if (isSel) 32.dp else 26.dp)
                                                    .background(if (company.isPwned) Color(0xFF2ECC71) else if (isSel) Color(0xFFE74C3C) else Color(0xFF2C3E50), CircleShape)
                                                    .border(if (isSel) 2.dp else 1.dp, Color.White, CircleShape)
                                                    .clickable { onSelectTarget(company); vibrate(25) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (company.isPwned) "✓" else if (company.sector == "Finanzen") "🏦" else if (company.sector == "Medizin") "🧪" else "🏢",
                                                    fontSize = if (isSel) 14.sp else 11.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    // 2. Zoom & Reset Buttons
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            color = Color(0xEEFFFFFF),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, Color.LightGray),
                                            modifier = Modifier.clickable {
                                                mapScale = (mapScale + 0.2f).coerceAtMost(2.5f)
                                                vibrate(15)
                                            }
                                        ) {
                                            Text(" ➕ ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
                                        }
                                        Surface(
                                            color = Color(0xEEFFFFFF),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, Color.LightGray),
                                            modifier = Modifier.clickable {
                                                mapScale = (mapScale - 0.2f).coerceAtLeast(0.8f)
                                                vibrate(15)
                                            }
                                        ) {
                                            Text(" ➖ ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
                                        }
                                        Surface(
                                            color = Color(0xEEFFFFFF),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, Color.LightGray),
                                            modifier = Modifier.clickable {
                                                mapScale = 1.0f
                                                mapPanOffset = Offset.Zero
                                                vibrate(15)
                                            }
                                        ) {
                                            Text(" ⟲ ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
                                        }
                                    }
                                }
                            }

                            // Intel Scanner Sidepanel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF0F1524))
                                    .padding(12.dp)
                            ) {
                                if (selectedTarget != null) {
                                    val t = selectedTarget
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Text("🎯 TARGET INTEL SCANNER", color = WarningAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(t.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                                        Text("Sektor: ${t.sector} | ${t.address}", color = Color.Gray, fontSize = 10.sp)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("💰 Belohnung: +${t.bountyEur}€", color = TermGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("🌐 Host/IP: ${t.ip}", color = KaliBlue, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                        Text("⚠️ Lücke: ${t.vulnType}", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                        Text("🛠️ Tool: ${t.requiredTools}", color = WarningAmber, fontSize = 10.sp)

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (t.isPwned) "STATUS: [PWNED / BEUTE GESICHERT]"
                                            else when (t.breachStage) {
                                                0 -> "STATUS: [UNGEÖFFNET] - Portscan starten"
                                                1 -> "STATUS: [GESCANNT] - Exploit ausführen"
                                                2 -> "STATUS: [EXPLOITED] - Hash crashen / Root holen"
                                                else -> "STATUS: [ROOT ZUGRIFF] - 'exfiltrate ${t.id}'"
                                            },
                                            color = if (t.isPwned) TermGreen else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                onTermToggle(true)
                                                if (t.isPwned) {
                                                    onRunCommand("echo '[!] ${t.name} bereits gehackt!'")
                                                } else if (t.breachStage == 0) {
                                                    onRunCommand("nmap -sV ${t.ip}")
                                                } else if (t.breachStage == 3) {
                                                    onRunCommand("exfiltrate ${t.id}")
                                                } else {
                                                    onRunCommand(if (t.requiredTools.contains("sqlmap")) "sqlmap -u http://${t.ip} --dbs" else "hydra -l admin -P pass.txt ${t.ip} rdp")
                                                }
                                                vibrate(40)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (t.isPwned) Color.DarkGray else KaliBlue),
                                            enabled = !t.isPwned,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (t.isPwned) "BEREITS GEHACKT" else "TERMINAL ATTACK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        item {
                                            Text("🏢 BERLINER FIRMEN (Tag $currentDay)", color = KaliBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Tages-Spawns nach Fortschritt:", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
                                        }
                                        items(companyTargets) { comp ->
                                            Surface(
                                                color = if (comp.isPwned) Color(0xFF10281A) else Color(0xFF151C2C),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable {
                                                    onSelectTarget(comp)
                                                    vibrate(20)
                                                }
                                            ) {
                                                Row(modifier = Modifier.padding(6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("• ${comp.name}", color = if (comp.isPwned) TermGreen else KaliBlue, fontSize = 10.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                                    Text(if (comp.isPwned) "✓" else "+${comp.bountyEur}€", color = if (comp.isPwned) TermGreen else WarningAmber, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. MODULARES BITCOIN MINING RIG FENSTER
                if (isMinerOpen) {
                    SmoothMovableWindow(
                        title = "⛏️ Stratum V2 Bitcoin Mining Node",
                        initialOffset = minerWinOffset,
                        width = windowWidth,
                        height = windowHeight,
                        borderColor = Color(0xFFF1C40F),
                        onPositionChange = onMinerWinOffsetChange,
                        onClose = { onMinerToggle(false); vibrate(20) }
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                            // HARDWARE SETUP STATUS CHECK
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF121726)),
                                border = BorderStroke(1.dp, if (hasMiningRigFrame && hasMiningPsu && hasDedicatedGpu) TermGreen else WarningAmber)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("🔧 MINING-RIG HARDWARE SETUP:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("1. Open-Air Frame: " + if (hasMiningRigFrame) "✓ INSTALLIERT" else "❌ FEHLT (Web Shop)", color = if (hasMiningRigFrame) TermGreen else ErrorRed, fontSize = 11.sp)
                                        Text("2. 1200W Netzteil: " + if (hasMiningPsu) "✓ INSTALLIERT" else "❌ FEHLT (Web Shop)", color = if (hasMiningPsu) TermGreen else ErrorRed, fontSize = 11.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("3. Mining GPU/ASIC: " + if (hasDedicatedGpu) "✓ $currentGpu" else "❌ FEHLT (Web Shop)", color = if (hasDedicatedGpu) TermGreen else ErrorRed, fontSize = 11.sp)
                                        Text("4. Miner Node Software: ✓ AKTIV", color = TermGreen, fontSize = 11.sp)
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Hashrate: ${String.format("%.2f", hashrateMhs)} MH/s | Verbrauch: $powerWatts Watt", color = TermGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Rig Status: " + if (hasMiningRigFrame && hasMiningPsu && hasDedicatedGpu) "BEREIT" else "UNVOLLSTÄNDIG (Kaufe Teile im Web Shop!)", color = if (hasMiningRigFrame && hasMiningPsu && hasDedicatedGpu) Color.LightGray else WarningAmber, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = onToggleMining,
                                    enabled = hasMiningRigFrame && hasMiningPsu && hasDedicatedGpu,
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isMiningActive) ErrorRed else TermGreen)
                                ) {
                                    Text(if (isMiningActive) "⏹️ STOP MINING" else "▶️ START MINING", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // LIVE MINING CONSOLE
                            Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF07090F)), border = BorderStroke(1.dp, Color(0xFF28324E))) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("STATUS: " + if (isMiningActive) "🟢 MINING POOL AKTIV (Berlin Stratum V2)" else "🔴 GESTOPPT", color = if (isMiningActive) TermGreen else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Current Nonce: $currentMiningNonce", color = KaliBlue, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    Text("Target Difficulty Hash: $currentMiningHash", color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("⚡ Unbestätigte Rewards: ₿ ${String.format("%.8f", unconfirmedBtc)} (${(unconfirmedBtc * btcPriceEur).toInt()}€)", color = Color(0xFFF1C40F), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("📈 Aktueller BTC Kurs: 1 BTC = $btcPriceEur€", color = Color.Gray, fontSize = 11.sp)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = onTransferMiningRewards,
                                            enabled = unconfirmedBtc > 0.0,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1C40F)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("IN WALLET ÜBERTRAGEN", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { onSellBtc(0.001) },
                                            enabled = btcBalance >= 0.001,
                                            colors = ButtonDefaults.buttonColors(containerColor = TermGreen),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("0.001 BTC VERKAUFEN (+${(0.001 * btcPriceEur).toInt()}€)", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. TERMINAL FENSTER
                if (isTermOpen) {
                    SmoothMovableWindow(
                        title = if (activeOS == "Kali Linux") "$hackerAlias@thinkpad: ~ (zsh)" else "Administrator: Windows PowerShell",
                        initialOffset = termWinOffset,
                        width = windowWidth,
                        height = windowHeight,
                        borderColor = Color(0xFF282C3D),
                        onPositionChange = onTermWinOffsetChange,
                        onClose = { onTermToggle(false); vibrate(20) }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                                items(terminalHistory) { line ->
                                    Text(
                                        text = line,
                                        color = if (line.startsWith("┌──") || line.startsWith("└─$")) KaliBlue
                                        else if (line.contains("[✓]") || line.contains("[+]") || line.contains("[$$$]")) TermGreen
                                        else if (line.contains("[-]")) ErrorRed
                                        else if (line.contains("[!]")) WarningAmber
                                        else Color(0xFFD6D6D6),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color(0xFF0B0C12)).padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = terminalInput,
                                    onValueChange = onTerminalInputChange,
                                    placeholder = { Text("Befehl eingeben...", fontSize = 11.sp, color = Color.Gray) },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    singleLine = true
                                )
                                Button(
                                    onClick = { onRunCommand(terminalInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = KaliBlue),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text("RUN", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // 4. CHAT FENSTER
                if (isChatOpen) {
                    SmoothMovableWindow(
                        title = "💬 Signal / Matrix - Hacker Bounties",
                        initialOffset = chatWinOffset,
                        width = (windowWidth * 0.85f),
                        height = windowHeight,
                        borderColor = Color(0xFF3498DB),
                        onPositionChange = onChatWinOffsetChange,
                        onClose = { onChatToggle(false); vibrate(20) }
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            items(chatMessages) { msg ->
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF151C2C))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(msg.sender, color = KaliBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(msg.time, color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Text(msg.text, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. SOFTWARE CENTER FENSTER (CYBERPUNK REDESIGN)
                if (isSoftwareCenterOpen) {
                    SmoothMovableWindow(
                        title = "📦 APT Software Repository & Cyber Tools",
                        initialOffset = softwareWinOffset,
                        width = windowWidth,
                        height = windowHeight,
                        borderColor = WarningAmber,
                        onPositionChange = onSoftwareWinOffsetChange,
                        onClose = { onSoftwareCenterToggle(false); vibrate(20) }
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("📦 Kali Rolling Repositories", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Offizielle Hacking-, Exploit- & Krypto-Werkzeuge", color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Surface(color = Color(0xFF141D30), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, KaliBlue)) {
                                        Text("Guthaben: $balanceEur€", color = TermGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            items(softwareCatalog) { tool ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141928)),
                                    border = BorderStroke(1.dp, if (tool.isInstalled) TermGreen.copy(alpha = 0.5f) else Color(0xFF26314A))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = if (tool.isInstalled) Color(0xFF133621) else Color(0xFF1C253B),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.size(32.dp),
                                                    border = BorderStroke(1.dp, if (tool.isInstalled) TermGreen else KaliBlue)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(if (tool.isInstalled) "✓" else "⚙️", fontSize = 14.sp, color = if (tool.isInstalled) TermGreen else KaliBlue)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(tool.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Kategorie: ${tool.category}", color = KaliBlue, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                            Surface(
                                                color = if (tool.isFreeRepo) Color(0xFF0F3020) else Color(0xFF33230F),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(1.dp, if (tool.isFreeRepo) TermGreen else WarningAmber)
                                            ) {
                                                Text(
                                                    text = if (tool.isFreeRepo) "FREE (apt)" else "${tool.priceEur}€",
                                                    color = if (tool.isFreeRepo) TermGreen else WarningAmber,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(tool.desc, color = Color(0xFFB0B9D0), fontSize = 11.sp, modifier = Modifier.padding(vertical = 6.dp), lineHeight = 16.sp)
                                        Text("Terminal-Befehl: ${tool.command}", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { onBuySoftware(tool) },
                                            enabled = !tool.isInstalled && (tool.isFreeRepo || balanceEur >= tool.priceEur),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (tool.isInstalled) Color(0xFF183B25) else if (tool.isFreeRepo) KaliBlue else TermGreen,
                                                disabledContainerColor = Color(0xFF182030)
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().height(38.dp)
                                        ) {
                                            Text(
                                                text = if (tool.isInstalled) "✓ IM BETRIEBSSYSTEM INSTALLIERT" else "SOFTWARE INSTALLIEREN (${if (tool.isFreeRepo) "Kostenlos" else "${tool.priceEur}€"})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tool.isInstalled) TermGreen else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. CLEAR-WEB BROWSER
                if (isBrowserOpen) {
                    SmoothMovableWindow(
                        title = "🌐 Web Browser - ClearWeb Portal",
                        initialOffset = browserWinOffset,
                        width = windowWidth,
                        height = windowHeight,
                        borderColor = KaliBlue,
                        onPositionChange = onBrowserWinOffsetChange,
                        onClose = { onBrowserToggle(false); vibrate(20) }
                    ) {
                        ClearWebBrowserView(
                            currentUrl = clearWebUrl,
                            inputUrl = clearWebInputUrl,
                            onUrlChange = { 
                                onClearWebUrlChange(it)
                                onClearWebInputUrlChange(it)
                            },
                            onInputUrlChange = onClearWebInputUrlChange,
                            balanceEur = balanceEur,
                            hardwareShop = hardwareShop,
                            onBuyHardware = onBuyHardware,
                            vibrate = vibrate
                        )
                    }
                }

                // 7. TOR BROWSER
                if (isTorBrowserOpen) {
                    SmoothMovableWindow(
                        title = "🧅 Tor Browser (.onion Onion Routing)",
                        initialOffset = torWinOffset,
                        width = windowWidth,
                        height = windowHeight,
                        borderColor = Color(0xFF9B59B6),
                        onPositionChange = onTorWinOffsetChange,
                        onClose = { onTorBrowserToggle(false); vibrate(20) }
                    ) {
                        TorBrowserView(
                            currentUrl = torWebUrl,
                            inputUrl = torWebInputUrl,
                            onUrlChange = { 
                                onTorWebUrlChange(it)
                                onTorWebInputUrlChange(it)
                            },
                            onInputUrlChange = onTorWebInputUrlChange,
                            balanceEur = balanceEur,
                            hideoutList = hideoutList,
                            currentHideout = currentHideout,
                            onChangeHideout = onChangeHideout,
                            darknetExploitShop = darknetExploitShop,
                            onBuyDarknetExploit = onBuyDarknetExploit,
                            vibrate = vibrate
                        )
                    }
                }

                // 8. RIG SPECS FENSTER
                if (isSettingsOpen) {
                    SmoothMovableWindow(
                        title = "🛠️ Rig Specs & Hardware-Status",
                        initialOffset = settingsWinOffset,
                        width = (windowWidth * 0.75f),
                        height = (windowHeight * 0.85f),
                        borderColor = Color(0xFF9B59B6),
                        onPositionChange = onSettingsWinOffsetChange,
                        onClose = { onSettingsToggle(false); vibrate(20) }
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("• Spieler: $playerName ($hackerAlias)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("• Host: $rigName", color = Color.LightGray, fontSize = 12.sp)
                            Text("• OS: $activeOS", color = KaliBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("• CPU: $currentCpu", color = Color.LightGray, fontSize = 12.sp)
                            Text("• GPU: $currentGpu", color = Color.LightGray, fontSize = 12.sp)
                            Text("• Wi-Fi: $wifiAdapter", color = Color.LightGray, fontSize = 12.sp)
                            Text("• Mining Power: ${String.format("%.2f", hashrateMhs)} MH/s ($powerWatts Watt)", color = TermGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("🏠 Versteck: ${currentHideout.name} (${currentHideout.district})", color = WarningAmber, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 3. BOTTOM TASKBAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (activeOS == "Kali Linux") KaliPanelBg else Win11Taskbar)
                    .border(BorderStroke(1.dp, Color(0xFF1C2236)))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isTermOpen) TaskbarAppIcon(icon = "💻", label = "Terminal", isActive = true) { onTermToggle(false) }
                    if (isMapOpen) TaskbarAppIcon(icon = "🗺️", label = "Map", isActive = true) { onMapToggle(false) }
                    if (isMinerSoftwareInstalled && isMinerOpen) TaskbarAppIcon(icon = "⛏️", label = "Miner", isActive = true) { onMinerToggle(false) }
                    if (isChatOpen) TaskbarAppIcon(icon = "💬", label = "Chat", isActive = true) { onChatToggle(false) }
                    if (isSoftwareCenterOpen) TaskbarAppIcon(icon = "📦", label = "Software", isActive = true) { onSoftwareCenterToggle(false) }
                    if (isBrowserOpen) TaskbarAppIcon(icon = "🌐", label = "Browser", isActive = true) { onBrowserToggle(false) }
                    if (isTorBrowserOpen) TaskbarAppIcon(icon = "🧅", label = "Tor", isActive = true) { onTorBrowserToggle(false) }
                    if (isSettingsOpen) TaskbarAppIcon(icon = "🛠️", label = "Rig", isActive = true) { onSettingsToggle(false) }
                }
                Text("Tag $currentDay • $berlinTimeStr", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ------------------- BROWSER VIEWS -------------------

@Composable
fun ClearWebBrowserView(
    currentUrl: String,
    inputUrl: String,
    onUrlChange: (String) -> Unit,
    onInputUrlChange: (String) -> Unit,
    balanceEur: Int,
    hardwareShop: List<HardwareItem>,
    onBuyHardware: (HardwareItem) -> Unit,
    vibrate: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B2B))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onUrlChange("about:home"); vibrate(20) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242C44)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text("🏠 Start", fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            TextField(
                value = inputUrl,
                onValueChange = onInputUrlChange,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF0C0F1A), unfocusedContainerColor = Color(0xFF0C0F1A), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = { onUrlChange(inputUrl); vibrate(20) },
                colors = ButtonDefaults.buttonColors(containerColor = KaliBlue)
            ) {
                Text("GO", fontSize = 11.sp)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0D111E))) {
            when {
                currentUrl == "about:home" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        item {
                            Text("🌐 NetStart Berlin - Offizielles Web Portal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Wähle einen Online-Shop oder Dienst aus dem Berliner Clear-Web:", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onUrlChange("https://hardware-direct.de"); vibrate(25) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF171E30)),
                                border = BorderStroke(1.dp, KaliBlue)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🛒", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                                    Column {
                                        Text("HardwareDirect Berlin (hardware-direct.de)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Mining-Frames, 1200W Netzteile, RTX 4070/4090 GPUs & ASIC Mining Rigs.", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onUrlChange("https://berlin-news.de"); vibrate(25) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF171E30)),
                                border = BorderStroke(1.dp, Color(0xFF28324E))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("📰", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                                    Column {
                                        Text("Berliner Morgenpost Cyber-Ticker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Aktuelle Meldungen über Hackerangriffe, BKA-Razzien und FinTech-Vorfälle.", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                currentUrl.contains("hardware-direct") -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        item {
                            Text("🛒 HardwareDirect.de - Offizieller Cyber & Mining Store", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Guthaben: $balanceEur€", color = TermGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                        }
                        items(hardwareShop) { item ->
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF182033))) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${item.priceEur}€", color = WarningAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Text(item.desc, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                                    Button(
                                        onClick = { onBuyHardware(item) },
                                        enabled = !item.isBought && balanceEur >= item.priceEur,
                                        colors = ButtonDefaults.buttonColors(containerColor = TermGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (item.isBought) "✓ BEREITS INSTALLIERT" else "KAUFEN (${item.priceEur}€)", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        item {
                            Text("📰 Berliner Morgenpost - Cyber-Ticker 2026", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("🚨 POLIZEI WARNT VOR NEUER HACKER-WELLE IN BERLIN\n" +
                                    "Das LKA Berlin meldet massive Angriffe auf Berliner Firmennetze. " +
                                    "Täglich werden Dutzende neue Unternehmen registriert und stehen unter Beschuss.",
                                color = Color.LightGray, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TorBrowserView(
    currentUrl: String,
    inputUrl: String,
    onUrlChange: (String) -> Unit,
    onInputUrlChange: (String) -> Unit,
    balanceEur: Int,
    hideoutList: List<Hideout>,
    currentHideout: Hideout,
    onChangeHideout: (Hideout) -> Unit,
    darknetExploitShop: List<ExploitPayloadItem>,
    onBuyDarknetExploit: (ExploitPayloadItem) -> Unit,
    vibrate: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF191226))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onUrlChange("about:tor"); vibrate(20) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF321E52)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text("🧅 Tor Portal", fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            TextField(
                value = inputUrl,
                onValueChange = onInputUrlChange,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF0C0816), unfocusedContainerColor = Color(0xFF0C0816), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = { onUrlChange(inputUrl); vibrate(20) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6))
            ) {
                Text("ROUTE", fontSize = 11.sp)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0D0818))) {
            when {
                currentUrl == "about:tor" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        item {
                            Text("🧅 Tor Onion Hidden Services Index", color = Color(0xFF9B59B6), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Verschlüsselte .onion Knotenpunkte für Unterwelt-Hacks & Bunkerverstecke:", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onUrlChange("dark-immo.onion"); vibrate(25) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1430)),
                                border = BorderStroke(1.dp, Color(0xFF9B59B6))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏚️", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                                    Column {
                                        Text("DarkImmo Untergrund-Verstecke (dark-immo.onion)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Geheime Bunker, Industrie-Garagen & Penthouses in Berlin mieten.", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onUrlChange("shadow-zero.onion"); vibrate(25) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1430)),
                                border = BorderStroke(1.dp, ErrorRed)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("☣️", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                                    Column {
                                        Text("ShadowZero Day Exploits & Dumps (shadow-zero.onion)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Kaufen von Zero-Day RCE Payloads & BKA-Cleanern.", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                currentUrl.contains("dark-immo") -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        item {
                            Text("🏚️ DarkImmo.onion - Geheime Berliner Verstecke", color = Color(0xFF9B59B6), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Aktueller Ort: ${currentHideout.name} (${currentHideout.district})", color = WarningAmber, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                        }
                        items(hideoutList) { h ->
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF22163A))) {
                                Column {
                                    Image(
                                        painter = painterResource(id = h.imageResId),
                                        contentDescription = h.name,
                                        modifier = Modifier.fillMaxWidth().height(150.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(h.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${h.rentDayEur}€ / Tag", color = WarningAmber, fontSize = 12.sp)
                                        }
                                        Text("Sicherheitsstufe: ★★★★★".take(h.secLevel + 18), color = Color(0xFFF1C40F), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                        Text(h.desc, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                                        Button(
                                            onClick = { onChangeHideout(h) },
                                            enabled = currentHideout.id != h.id && balanceEur >= h.depositEur,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (currentHideout.id == h.id) "✓ AKTUELLER UNTERSCHLUPF" else "UMZIEHEN (${h.depositEur}€ Kaution)", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                currentUrl.contains("shadow-zero") -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        item {
                            Text("☣️ ShadowZero.onion - Zero-Day Exploits & Bounties", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Guthaben: $balanceEur€", color = TermGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                        }
                        items(darknetExploitShop) { exp ->
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF261220))) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(exp.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${exp.priceEur}€", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Text("Verkäufer: ${exp.seller} | Target: ${exp.targetIp}", color = Color.Gray, fontSize = 10.sp)
                                    Text(exp.desc, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                                    Button(
                                        onClick = { onBuyDarknetExploit(exp) },
                                        enabled = !exp.isBought && balanceEur >= exp.priceEur,
                                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (exp.isBought) "✓ IM SYSTEM GELADEN" else "PAYLOAD KAUFEN (${exp.priceEur}€)", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("404 Onionsite Not Found: Circuit Failed", color = ErrorRed, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ------------------- UI COMPONENTS -------------------

@Composable
fun CleanDesktopIcon(
    name: String,
    icon: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color(0xFF131828), RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Text(
            text = name,
            color = Color(0xFFD0D6E8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 3.dp),
            maxLines = 1
        )
    }
}

@Composable
fun SmoothMovableWindow(
    title: String,
    initialOffset: Offset,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    borderColor: Color,
    onPositionChange: (Offset) -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    var winOffset by remember { mutableStateOf(initialOffset) }

    Card(
        modifier = Modifier
            .offset { IntOffset(winOffset.x.roundToInt(), winOffset.y.roundToInt()) }
            .size(width, height)
            .zIndex(10f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1018)),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161A26))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val next = winOffset + dragAmount
                            winOffset = next
                            onPositionChange(next)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                WindowCloseButton(onClose)
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun WindowCloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(Color(0xFF33151D), RoundedCornerShape(4.dp))
            .border(1.dp, ErrorRed, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("✕", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TaskbarAppIcon(icon: String, label: String, isActive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(if (isActive) Color(0xFF1C243B) else Color(0xFF121522), RoundedCornerShape(6.dp))
            .border(1.dp, if (isActive) KaliBlue else Color(0xFF232B44), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 14.sp)
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
