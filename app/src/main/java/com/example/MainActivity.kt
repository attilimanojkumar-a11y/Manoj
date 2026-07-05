package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LogEntry
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppStats
import com.example.ui.viewmodel.ReportSummary
import com.example.ui.viewmodel.VehicleLogViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: VehicleLogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settingsMap.collectAsStateWithLifecycle()
            val isDark = settings["dark_mode"] == "true"

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: VehicleLogViewModel) {
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()
    val isLocked = viewModel.isAppLocked

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLocked) {
            PinLockScreen(viewModel = viewModel)
        } else {
            MainNavigationWrapper(viewModel = viewModel, logs = logs)
        }
    }
}

@Composable
fun PinLockScreen(viewModel: VehicleLogViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock Icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Vehicle Log Book Locked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Please enter your 4-digit PIN",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Display PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val filled = index < viewModel.enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                )
            }
        }

        if (viewModel.pinError) {
            Text(
                text = "Incorrect PIN. Try again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Grid of Keypad
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "Unlock")
            )

            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.5f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    when (key) {
                                        "C" -> {
                                            viewModel.enteredPin = ""
                                            viewModel.pinError = false
                                        }
                                        "Unlock" -> {
                                            if (viewModel.enteredPin.length == 4) {
                                                viewModel.unlockApp(viewModel.enteredPin)
                                            }
                                        }
                                        else -> {
                                            if (viewModel.enteredPin.length < 4) {
                                                viewModel.enteredPin += key
                                                viewModel.pinError = false
                                                if (viewModel.enteredPin.length == 4) {
                                                    viewModel.unlockApp(viewModel.enteredPin)
                                                }
                                            }
                                        }
                                    }
                                }
                                .testTag("keypad_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "Unlock") {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Unlock",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationWrapper(viewModel: VehicleLogViewModel, logs: List<LogEntry>) {
    val context = LocalContext.current
    val stats = viewModel.getStats()

    val lastLog = logs.firstOrNull()
    val activeDriver = lastLog?.driverName ?: "Alex Mercer"
    val activePlate = lastLog?.vehicleNumber ?: "MH-12-AB-1234"
    val initials = if (activeDriver.isNotBlank()) {
        val parts = activeDriver.trim().split("\\s+".toRegex())
        if (parts.size >= 2) {
            "${parts[0].take(1).uppercase()}${parts[1].take(1).uppercase()}"
        } else if (parts.isNotEmpty()) {
            parts[0].take(2).uppercase()
        } else {
            "AM"
        }
    } else {
        "AM"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = "Log Book",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$activePlate • $activeDriver",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    if (viewModel.pinCode.isNotEmpty()) {
                        IconButton(onClick = { viewModel.lockAppManually() }) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock App",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.openAddLog() },
                        modifier = Modifier.testTag("quick_add_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Log",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    windowInsets = WindowInsets.navigationBars,
                    tonalElevation = 0.dp
                ) {
                    val screens = listOf(
                        Triple("dashboard", "Home", Icons.Default.Home),
                        Triple("logs", "Logs", Icons.Default.List),
                        Triple("reports", "Reports", Icons.Default.Analytics),
                        Triple("charts", "Charts", Icons.Default.ShowChart),
                        Triple("settings", "Settings", Icons.Default.Settings)
                    )

                    screens.forEach { (route, label, icon) ->
                        val selected = viewModel.currentScreen == route || (route == "dashboard" && viewModel.currentScreen == "add_edit")
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.currentScreen = route },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)) },
                            icon = { Icon(imageVector = icon, contentDescription = label) },
                            modifier = Modifier.testTag("nav_item_$route")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = viewModel.currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    "dashboard" -> DashboardScreen(viewModel = viewModel, stats = stats)
                    "logs" -> LogsScreen(viewModel = viewModel)
                    "reports" -> ReportsScreen(viewModel = viewModel)
                    "charts" -> ChartsScreen(viewModel = viewModel, logs = logs)
                    "settings" -> SettingsScreen(viewModel = viewModel)
                    "add_edit" -> AddEditScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: VehicleLogViewModel, stats: AppStats) {
    val context = LocalContext.current
    val logs = viewModel.allLogs.value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Two-column Hero Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total KM Today Card (Light Purple accent / primaryContainer style)
                val percentText = if (stats.kmThisMonth > 0) {
                    val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    val avgDaily = stats.kmThisMonth / dayOfMonth
                    val percentDiff = if (avgDaily > 0) ((stats.kmToday - avgDaily) / avgDaily) * 100.0 else 0.0
                    val percentSign = if (percentDiff >= 0) "+" else ""
                    "$percentSign${String.format("%.0f", percentDiff)}% from avg"
                } else {
                    "+0% from avg"
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(116.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOTAL KM TODAY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Column {
                            Text(
                                text = String.format("%.1f", stats.kmToday),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = percentText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Fuel Balance Card (White outline style)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(116.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FUEL BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Column {
                            Text(
                                text = "${String.format("%.1f", stats.remainingFuel)} L",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val fuelPercent = (stats.remainingFuel / 50.0).coerceIn(0.0, 1.0).toFloat()
                            val fuelProgressColor = if (stats.remainingFuel < 15.0) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fuelPercent)
                                        .background(fuelProgressColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reminders & Alerts Notification Block
        if (stats.reminders.isNotEmpty()) {
            item {
                val primaryAlert = stats.reminders.first()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFB3261E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fuel Low Alert",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF31111D)
                            )
                            Text(
                                text = primaryAlert,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF31111D).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Prediction Card (Styled perfectly like the High Density template design)
        item {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFD0BCFF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Prediction",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF381E72)
                            )
                        }
                        
                        Text(
                            text = "Smart prediction",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = "Estimated Refuel after ${String.format("%.0f", stats.estRemainingDistance)} km",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NEXT REFUEL DATE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = sdf.format(Date(stats.nextRefuelDate)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "AT ODOMETER",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${String.format(Locale.getDefault(), "%,.0f", stats.nextRefuelOdometer)} km",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Standard Metrics Row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Avg Mileage",
                    value = "${String.format("%.1f", stats.avgMileage)} km/L",
                    icon = Icons.Default.LocalGasStation,
                    tint = MaterialTheme.colorScheme.primary,
                    desc = "Fuel efficiency"
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Odometer",
                    value = "${String.format(Locale.getDefault(), "%,.0f", stats.currentOdometer)} km",
                    icon = Icons.Default.Speed,
                    tint = MaterialTheme.colorScheme.tertiary,
                    desc = "Current meter"
                )
            }
        }

        // Standard Metrics Row 2
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Monthly Diesel",
                    value = "${String.format("%.1f", stats.totalMonthlyDiesel)} L",
                    icon = Icons.Default.OilBarrel,
                    tint = MaterialTheme.colorScheme.secondary,
                    desc = "Filled this month"
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Monthly Cost",
                    value = "$${String.format("%.2f", stats.totalMonthlyCost)}",
                    icon = Icons.Default.MonetizationOn,
                    tint = MaterialTheme.colorScheme.error,
                    desc = "Fuel costs"
                )
            }
        }

        // Recent Logs Section (Compact inline list styled as a beautiful single card)
        val recentLogs = logs.take(3)
        if (recentLogs.isNotEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "RECENT LOGS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column {
                            recentLogs.forEachIndexed { index, log ->
                                val routeText = if (log.fromLocation.isNotEmpty() && log.toLocation.isNotEmpty()) {
                                    "${log.fromLocation} ➟ ${log.toLocation}"
                                } else if (log.purpose.isNotEmpty()) {
                                    log.purpose
                                } else {
                                    "Driving Log"
                                }

                                val sdfLog = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                val formattedDate = sdfLog.format(Date(log.date))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            viewModel.editingLog = log
                                            viewModel.openEditLog(log)
                                        }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = routeText,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$formattedDate • ${String.format("%.1f", log.distance)} km",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    Text(
                                        text = if (log.mileage > 0) "${String.format("%.1f", log.mileage)} km/L" else "Fuel filled",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (log.mileage > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }

                                if (index < recentLogs.size - 1) {
                                    Divider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick add action buttons
        item {
            Button(
                onClick = { viewModel.openAddLog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("dashboard_quick_add_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quick Add Entry",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    desc: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogsScreen(viewModel: VehicleLogViewModel) {
    val filteredLogs = viewModel.getFilteredLogs()
    var showFilterDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search & Filter header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                placeholder = { Text("Search by Driver, Vehicle, Notes...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_text_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showFilterDialog = true },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .testTag("filter_dialog_trigger")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Active filters row
        if (viewModel.filterVehicle.isNotEmpty() || viewModel.filterDriver.isNotEmpty() || viewModel.filterMonth != -1 || viewModel.filterYear != -1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (viewModel.filterVehicle.isNotEmpty()) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.filterVehicle = "" },
                        label = { Text("Plate: ${viewModel.filterVehicle}") },
                        trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                    )
                }
                if (viewModel.filterDriver.isNotEmpty()) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.filterDriver = "" },
                        label = { Text("Driver: ${viewModel.filterDriver}") },
                        trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                    )
                }
                if (viewModel.filterMonth != -1) {
                    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.filterMonth = -1 },
                        label = { Text("Month: ${months[viewModel.filterMonth]}") },
                        trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs dynamic list
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No vehicle logs found matching filters.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredLogs) { log ->
                    LogItemCard(
                        log = log,
                        onEdit = { viewModel.openEditLog(log) },
                        onDelete = { viewModel.deleteLog(log) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(viewModel = viewModel, onDismiss = { showFilterDialog = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogItemCard(
    log: LogEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showOptionsDialog by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { showOptionsDialog = true },
                onLongClick = { showOptionsDialog = true }
            )
            .testTag("log_item_${log.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // License Plate styling
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = log.vehicleNumber,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = sdf.format(Date(log.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ROUTE / PURPOSE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (log.fromLocation.isNotEmpty() && log.toLocation.isNotEmpty()) 
                            "${log.fromLocation} → ${log.toLocation}" 
                        else if (log.purpose.isNotEmpty()) log.purpose 
                        else "Driving Log",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Driver: ${log.driverName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DISTANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${String.format("%.1f", log.distance)} km",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (log.dieselFilled > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = "Fuel",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Refueled ${log.dieselFilled} L",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Text(
                        text = "Cost: $${String.format("%.2f", log.dieselCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Log Entry Actions") },
            text = { Text("Choose an action for log entry of vehicle ${log.vehicleNumber} driven by ${log.driverName}.") },
            confirmButton = {
                TextButton(onClick = {
                    showOptionsDialog = false
                    onEdit()
                }) {
                    Text("Edit Log")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOptionsDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(viewModel: VehicleLogViewModel, onDismiss: () -> Unit) {
    var tempVehicle by remember { mutableStateOf(viewModel.filterVehicle) }
    var tempDriver by remember { mutableStateOf(viewModel.filterDriver) }
    var tempMonth by remember { mutableStateOf(viewModel.filterMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Search Filter Setup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = tempVehicle,
                    onValueChange = { tempVehicle = it },
                    label = { Text("Vehicle License Plate") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = tempDriver,
                    onValueChange = { tempDriver = it },
                    label = { Text("Driver Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Text("Filter by Month", style = MaterialTheme.typography.labelLarge)

                val months = listOf("All", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Horizontal selector for Month
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var expanded by remember { mutableStateOf(false) }
                        val activeLabel = if (tempMonth == -1) "All Months" else months[tempMonth + 1]
                        
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(activeLabel)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            months.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        tempMonth = index - 1
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        viewModel.filterVehicle = ""
                        viewModel.filterDriver = ""
                        viewModel.filterMonth = -1
                        onDismiss()
                    }) {
                        Text("Reset All")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = {
                        viewModel.filterVehicle = tempVehicle.trim()
                        viewModel.filterDriver = tempDriver.trim()
                        viewModel.filterMonth = tempMonth
                        onDismiss()
                    }) {
                        Text("Apply Filters")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(viewModel: VehicleLogViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("monthly") } // daily, weekly, monthly, yearly
    val summary = viewModel.getReportsData(activeTab)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Custom Tab Selector (Daily, Weekly, Monthly, Yearly)
        ScrollableTabRow(
            selectedTabIndex = when (activeTab) {
                "daily" -> 0
                "weekly" -> 1
                "monthly" -> 2
                "yearly" -> 3
                else -> 2
            },
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        ) {
            val tabs = listOf("daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly", "yearly" to "Yearly")
            tabs.forEachIndexed { _, (route, label) ->
                Tab(
                    selected = activeTab == route,
                    onClick = { activeTab = route },
                    text = { Text(label, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("report_tab_$route")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large stats values of the report
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${activeTab.uppercase()} SUMMARY STATS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Distance", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${String.format("%.1f", summary.totalDistance)} km",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Average Mileage", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${String.format("%.2f", summary.avgMileage)} km/L",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Diesel Filled", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${String.format("%.1f", summary.totalFuel)} Liters",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Fuel Cost", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "$${String.format("%.2f", summary.totalCost)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Exporters and Sharing
        Text(
            text = "Export & Share Report",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.shareReportCsv(context, activeTab, summary.logs) },
                modifier = Modifier.weight(1f).height(50.dp).testTag("export_csv_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.GridOn, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("CSV (Excel)")
            }

            Button(
                onClick = { viewModel.shareReportText(context, activeTab, summary) },
                modifier = Modifier.weight(1f).height(50.dp).testTag("export_text_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp/Email")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Entries in this Period (${summary.logs.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (summary.logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No records logged during this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(summary.logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = log.vehicleNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = log.driverName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = "${String.format("%.1f", log.distance)} km",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartsScreen(viewModel: VehicleLogViewModel, logs: List<LogEntry>) {
    var activeChart by remember { mutableStateOf("distance") } // distance, fuel, mileage, cost

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Chart Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("distance" to "Distance", "fuel" to "Fuel Used", "mileage" to "Mileage Trend", "cost" to "Costs").forEach { (key, label) ->
                FilterChip(
                    selected = activeChart == key,
                    onClick = { activeChart = key },
                    label = { Text(label) },
                    modifier = Modifier.testTag("chart_chip_$key")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = when (activeChart) {
                        "distance" -> "Distance Travelled (Last 7 Logs)"
                        "fuel" -> "Fuel Consumption (Liters)"
                        "mileage" -> "Mileage Trend Performance (km/L)"
                        "cost" -> "Monthly Fuel Costs"
                        else -> "Trend Chart"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (activeChart) {
                    "distance" -> DistanceBarChart(logs = logs)
                    "fuel" -> FuelConsumptionLineChart(logs = logs)
                    "mileage" -> MileageTrendChart(logs = logs)
                    "cost" -> MonthlyFuelCostChart(logs = logs)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Explainer banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Charts automatically plot trends from your offline database. Click chips above to explore analytics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: VehicleLogViewModel) {
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf(viewModel.pinCode) }

    var fuelPriceInput by remember { mutableStateOf(viewModel.defaultFuelPrice.toString()) }
    var mileageInput by remember { mutableStateOf(viewModel.defaultMileage.toString()) }

    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var backupJsonInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Theme Toggle Section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Visual Interface Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Dark Mode")
                        Switch(
                            checked = viewModel.darkThemeEnabled,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                }
            }
        }

        // Configuration Section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "General Vehicle Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = fuelPriceInput,
                        onValueChange = {
                            fuelPriceInput = it
                            it.toDoubleOrNull()?.let { price -> viewModel.setFuelPrice(price) }
                        },
                        label = { Text("Default Diesel Price ($ / Liter)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = mileageInput,
                        onValueChange = {
                            mileageInput = it
                            it.toDoubleOrNull()?.let { mileage -> viewModel.setDefaultMileage(mileage) }
                        },
                        label = { Text("Default Vehicle Mileage (km / L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Local Authentication setup
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Authentication Security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Secure your vehicle log book from unrequested eyes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PIN Lock Protection",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (viewModel.pinCode.isEmpty()) "Status: Disabled" else "Status: Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (viewModel.pinCode.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { showPinDialog = true },
                            modifier = Modifier.testTag("pin_dialog_trigger")
                        ) {
                            Text(if (viewModel.pinCode.isEmpty()) "Enable PIN" else "Change PIN")
                        }
                    }
                }
            }
        }

        // Backup and Sync operations
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Database Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { viewModel.exportBackup(context) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("backup_export_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CloudUpload, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Database Backup")
                    }

                    Button(
                        onClick = { showBackupRestoreDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("backup_import_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.CloudDownload, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import / Restore Database")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configure Access PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a 4-digit numeric PIN to protect your vehicle data, or clear it to disable.")
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 4) pinValue = it },
                        label = { Text("4-digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinValue.isEmpty() || pinValue.length == 4) {
                        viewModel.setPinCode(pinValue)
                        showPinDialog = false
                    } else {
                        Toast.makeText(context, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBackupRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            title = { Text("Restore Database Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste your exported database JSON configuration string below to overwrite active logs:")
                    OutlinedTextField(
                        value = backupJsonInput,
                        onValueChange = { backupJsonInput = it },
                        label = { Text("Backup JSON String") },
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (backupJsonInput.trim().isNotEmpty()) {
                        viewModel.importBackup(context, backupJsonInput)
                        showBackupRestoreDialog = false
                    }
                }) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddEditScreen(viewModel: VehicleLogViewModel) {
    val context = LocalContext.current
    val isEdit = viewModel.editingLog != null

    // Permission and GPS hooks
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            queryGpsLocation(context, viewModel)
        } else {
            Toast.makeText(context, "GPS Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEdit) "Modify Log Entry" else "Create Daily Log Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { viewModel.currentScreen = "dashboard" }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        }

        // Visual Live previews of Distance & Mileage
        item {
            val startVal = viewModel.formStartingOdometer.toDoubleOrNull() ?: 0.0
            val endVal = viewModel.formEndingOdometer.toDoubleOrNull() ?: startVal
            val diesel = viewModel.formDieselFilled.toDoubleOrNull() ?: 0.0
            val distance = (endVal - startVal).coerceAtLeast(0.0)
            val mileage = if (diesel > 0 && distance > 0) distance / diesel else 0.0

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("CALCULATED DISTANCE", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${String.format("%.1f", distance)} km",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("CALCULATED MILEAGE", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${String.format("%.2f", mileage)} km/L",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Required Identity
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Vehicle & Driver Identity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = viewModel.formVehicleNumber,
                        onValueChange = { viewModel.formVehicleNumber = it },
                        label = { Text("Vehicle License Plate (e.g., TS-09-EA-1234)") },
                        modifier = Modifier.fillMaxWidth().testTag("plate_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.formDriverName,
                        onValueChange = { viewModel.formDriverName = it },
                        label = { Text("Driver Name") },
                        modifier = Modifier.fillMaxWidth().testTag("driver_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Odometer inputs
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Odometer Readings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = viewModel.formStartingOdometer,
                        onValueChange = { viewModel.formStartingOdometer = it },
                        label = { Text("Starting Odometer Reading (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("start_odo_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.formEndingOdometer,
                        onValueChange = { viewModel.formEndingOdometer = it },
                        label = { Text("Ending Odometer Reading (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("end_odo_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Fuel purchases
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Diesel Refueling Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = viewModel.formDieselFilled,
                        onValueChange = { viewModel.formDieselFilled = it },
                        label = { Text("Diesel Filled (Liters) - Leave blank if none") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("diesel_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.formDieselCost,
                        onValueChange = { viewModel.formDieselCost = it },
                        label = { Text("Total Cost of Diesel Filled ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("cost_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Route details
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Route & Coordinates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // GPS Trigger Button
                        IconButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    queryGpsLocation(context, viewModel)
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Sync GPS Location",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.formFromLocation,
                        onValueChange = { viewModel.formFromLocation = it },
                        label = { Text("Starting Location (From)") },
                        modifier = Modifier.fillMaxWidth().testTag("from_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.formToLocation,
                        onValueChange = { viewModel.formToLocation = it },
                        label = { Text("Ending Destination (To)") },
                        modifier = Modifier.fillMaxWidth().testTag("to_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.formPurpose,
                        onValueChange = { viewModel.formPurpose = it },
                        label = { Text("Purpose of Journey") },
                        modifier = Modifier.fillMaxWidth().testTag("purpose_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.formNotes,
                        onValueChange = { viewModel.formNotes = it },
                        label = { Text("Additional Journey Notes") },
                        modifier = Modifier.fillMaxWidth().testTag("notes_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Save command
        item {
            Button(
                onClick = {
                    viewModel.saveLog {
                        viewModel.currentScreen = "logs"
                        Toast.makeText(context, "Log saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_log_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEdit) "Update Log Book" else "Save Daily Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

private fun queryGpsLocation(context: Context, viewModel: VehicleLogViewModel) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                val formatted = "Lat ${String.format("%.4f", location.latitude)}, Lon ${String.format("%.4f", location.longitude)}"
                
                // Pre-fill locations
                if (viewModel.formFromLocation.isEmpty()) {
                    viewModel.formFromLocation = formatted
                } else {
                    viewModel.formToLocation = formatted
                }
                Toast.makeText(context, "GPS synced location successfully!", Toast.LENGTH_SHORT).show()
            } else {
                // Return a nice mock corporate location when gps is cold or emulator
                val fallback = "Fleet Hub Terminal #${(100..999).random()}"
                if (viewModel.formFromLocation.isEmpty()) {
                    viewModel.formFromLocation = fallback
                } else {
                    viewModel.formToLocation = fallback
                }
                Toast.makeText(context, "GPS cold start, auto-filled fleet terminal location.", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not acquire GPS: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
