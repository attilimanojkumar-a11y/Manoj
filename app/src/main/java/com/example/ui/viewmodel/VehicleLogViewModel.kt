package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.LogEntry
import com.example.data.repository.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VehicleLogViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = LogRepository(database.logEntryDao(), database.appSettingDao())

    // All logs
    val allLogs: StateFlow<List<LogEntry>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All settings in key-value map
    private val _settingsMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val settingsMap = _settingsMap.asStateFlow()

    // PIN lock state
    var isAppLocked by mutableStateOf(false)
        private set
    var enteredPin by mutableStateOf("")
    var pinError by mutableStateOf(false)

    // Current navigation screen state
    var currentScreen by mutableStateOf("dashboard") // dashboard, logs, reports, charts, settings, add_edit
    var editingLog by mutableStateOf<LogEntry?>(null)

    // Form states
    var formDate by mutableStateOf(System.currentTimeMillis())
    var formVehicleNumber by mutableStateOf("")
    var formDriverName by mutableStateOf("")
    var formStartingOdometer by mutableStateOf("")
    var formEndingOdometer by mutableStateOf("")
    var formDieselFilled by mutableStateOf("")
    var formDieselCost by mutableStateOf("")
    var formPurpose by mutableStateOf("")
    var formFromLocation by mutableStateOf("")
    var formToLocation by mutableStateOf("")
    var formNotes by mutableStateOf("")

    // Search & filter states
    var searchQuery by mutableStateOf("")
    var filterVehicle by mutableStateOf("")
    var filterDriver by mutableStateOf("")
    var filterMonth by mutableStateOf(-1) // 0-11, -1 for all
    var filterYear by mutableStateOf(-1) // e.g. 2026, -1 for all

    // Setting values with defaults
    val darkThemeEnabled: Boolean
        get() = _settingsMap.value["dark_mode"] == "true"
    val defaultFuelPrice: Double
        get() = _settingsMap.value["fuel_price"]?.toDoubleOrNull() ?: 1.50
    val defaultMileage: Double
        get() = _settingsMap.value["default_mileage"]?.toDoubleOrNull() ?: 12.0
    val pinCode: String
        get() = _settingsMap.value["pin_code"] ?: ""
    val notificationsEnabled: Boolean
        get() = _settingsMap.value["notifications_enabled"] != "false"

    init {
        loadSettings()
        checkAppLock()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.allSettingsFlow.collect { list ->
                val map = list.associate { it.key to it.value }
                _settingsMap.value = map
                // Re-verify app lock if pin changed
                checkAppLock()
            }
        }
    }

    fun checkAppLock() {
        val activePin = pinCode
        isAppLocked = activePin.isNotEmpty()
    }

    fun unlockApp(pin: String): Boolean {
        return if (pin == pinCode) {
            isAppLocked = false
            pinError = false
            enteredPin = ""
            true
        } else {
            pinError = true
            false
        }
    }

    fun lockAppManually() {
        if (pinCode.isNotEmpty()) {
            isAppLocked = true
        }
    }

    // Settings actions
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("dark_mode", enabled.toString())
        }
    }

    fun setFuelPrice(price: Double) {
        viewModelScope.launch {
            repository.saveSetting("fuel_price", price.toString())
        }
    }

    fun setDefaultMileage(mileage: Double) {
        viewModelScope.launch {
            repository.saveSetting("default_mileage", mileage.toString())
        }
    }

    fun setPinCode(pin: String) {
        viewModelScope.launch {
            repository.saveSetting("pin_code", pin)
            isAppLocked = pin.isNotEmpty()
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("notifications_enabled", enabled.toString())
        }
    }

    // Form Actions
    fun openAddLog() {
        editingLog = null
        // Prepopulate with defaults
        formDate = System.currentTimeMillis()
        
        // Find last vehicle and driver to save typing
        val logs = allLogs.value
        if (logs.isNotEmpty()) {
            formVehicleNumber = logs.first().vehicleNumber
            formDriverName = logs.first().driverName
            formStartingOdometer = logs.first().endingOdometer.toString()
        } else {
            formVehicleNumber = ""
            formDriverName = ""
            formStartingOdometer = ""
        }
        formEndingOdometer = ""
        formDieselFilled = ""
        formDieselCost = ""
        formPurpose = ""
        formFromLocation = ""
        formToLocation = ""
        formNotes = ""
        currentScreen = "add_edit"
    }

    fun openEditLog(log: LogEntry) {
        editingLog = log
        formDate = log.date
        formVehicleNumber = log.vehicleNumber
        formDriverName = log.driverName
        formStartingOdometer = log.startingOdometer.toString()
        formEndingOdometer = log.endingOdometer.toString()
        formDieselFilled = if (log.dieselFilled > 0) log.dieselFilled.toString() else ""
        formDieselCost = if (log.dieselCost > 0) log.dieselCost.toString() else ""
        formPurpose = log.purpose
        formFromLocation = log.fromLocation
        formToLocation = log.toLocation
        formNotes = log.notes
        currentScreen = "add_edit"
    }

    fun saveLog(onComplete: () -> Unit) {
        val startOdo = formStartingOdometer.toDoubleOrNull() ?: 0.0
        val endOdo = formEndingOdometer.toDoubleOrNull() ?: startOdo
        val diesel = formDieselFilled.toDoubleOrNull() ?: 0.0
        val cost = formDieselCost.toDoubleOrNull() ?: 0.0

        if (formVehicleNumber.isBlank()) {
            Toast.makeText(getApplication(), "Vehicle Number is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (formDriverName.isBlank()) {
            Toast.makeText(getApplication(), "Driver Name is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (endOdo < startOdo) {
            Toast.makeText(getApplication(), "Ending odometer cannot be less than starting odometer", Toast.LENGTH_SHORT).show()
            return
        }

        val log = LogEntry(
            id = editingLog?.id ?: 0,
            date = formDate,
            vehicleNumber = formVehicleNumber.trim().uppercase(),
            driverName = formDriverName.trim(),
            startingOdometer = startOdo,
            endingOdometer = endOdo,
            dieselFilled = diesel,
            dieselCost = cost,
            purpose = formPurpose.trim(),
            fromLocation = formFromLocation.trim(),
            toLocation = formToLocation.trim(),
            notes = formNotes.trim()
        )

        viewModelScope.launch {
            if (editingLog == null) {
                repository.insertLog(log)
            } else {
                repository.updateLog(log)
            }
            onComplete()
        }
    }

    fun deleteLog(log: LogEntry) {
        viewModelScope.launch {
            repository.deleteLog(log)
            Toast.makeText(getApplication(), "Entry deleted", Toast.LENGTH_SHORT).show()
        }
    }

    // Stats calculations
    fun getFilteredLogs(): List<LogEntry> {
        return allLogs.value.filter { log ->
            val matchesSearch = searchQuery.isBlank() || 
                log.vehicleNumber.contains(searchQuery, ignoreCase = true) ||
                log.driverName.contains(searchQuery, ignoreCase = true) ||
                log.purpose.contains(searchQuery, ignoreCase = true) ||
                log.fromLocation.contains(searchQuery, ignoreCase = true) ||
                log.toLocation.contains(searchQuery, ignoreCase = true)

            val matchesVehicle = filterVehicle.isBlank() || log.vehicleNumber.equals(filterVehicle, ignoreCase = true)
            val matchesDriver = filterDriver.isBlank() || log.driverName.equals(filterDriver, ignoreCase = true)

            val calendar = Calendar.getInstance().apply { timeInMillis = log.date }
            val matchesMonth = filterMonth == -1 || calendar.get(Calendar.MONTH) == filterMonth
            val matchesYear = filterYear == -1 || calendar.get(Calendar.YEAR) == filterYear

            matchesSearch && matchesVehicle && matchesDriver && matchesMonth && matchesYear
        }
    }

    // Daily and monthly computations
    fun getStats(): AppStats {
        val logs = allLogs.value
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var kmToday = 0.0
        var kmThisMonth = 0.0
        var totalKm = 0.0
        var currentOdometer = 0.0
        var totalDieselFilled = 0.0
        var totalCost = 0.0
        var monthlyDieselFilled = 0.0
        var monthlyCost = 0.0

        for (log in logs) {
            val dist = log.distance
            totalKm += dist
            totalDieselFilled += log.dieselFilled
            totalCost += log.dieselCost

            if (log.endingOdometer > currentOdometer) {
                currentOdometer = log.endingOdometer
            }
            if (log.startingOdometer > currentOdometer) {
                currentOdometer = log.startingOdometer
            }

            if (log.date >= todayStart) {
                kmToday += dist
            }
            if (log.date >= monthStart) {
                kmThisMonth += dist
                monthlyDieselFilled += log.dieselFilled
                monthlyCost += log.dieselCost
            }
        }

        // Calculate average mileage
        // Active mileage is total distance divided by diesel filled
        val avgMileage = if (totalDieselFilled > 0) totalKm / totalDieselFilled else defaultMileage
        val costPerKm = if (totalKm > 0) totalCost / totalKm else 0.0

        // Smart Fuel Predictions
        // Last refuel entry
        val lastRefuel = logs.firstOrNull { it.dieselFilled > 0 }
        val remainingFuel: Double
        val estRemainingDistance: Double

        if (lastRefuel != null) {
            // Find distance driven since the last refuel
            val currentOdo = if (logs.isNotEmpty()) logs.first().endingOdometer else lastRefuel.endingOdometer
            val drivenSinceRefuel = (currentOdo - lastRefuel.endingOdometer).coerceAtLeast(0.0)
            
            // Remaining fuel = diesel filled - (driven / avgMileage)
            val consumed = if (avgMileage > 0) drivenSinceRefuel / avgMileage else 0.0
            remainingFuel = (lastRefuel.dieselFilled - consumed).coerceIn(0.0, lastRefuel.dieselFilled)
            estRemainingDistance = remainingFuel * avgMileage
        } else {
            // Default guess
            remainingFuel = 15.0
            estRemainingDistance = remainingFuel * defaultMileage
        }

        val nextRefuelOdometer = currentOdometer + estRemainingDistance
        
        // Next refuel date estimation based on average daily driving
        val dailyAvgDistance = if (logs.size > 1) {
            val earliest = logs.last().date
            val latest = logs.first().date
            val daysDiff = ((latest - earliest) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
            totalKm / daysDiff
        } else {
            30.0 // Default 30km/day
        }
        val daysToRefuel = if (dailyAvgDistance > 0) estRemainingDistance / dailyAvgDistance else 14.0
        val nextRefuelDate = System.currentTimeMillis() + (daysToRefuel * 24 * 60 * 60 * 1000).toLong()

        // Reminders
        val reminders = mutableListOf<String>()
        if (remainingFuel < 10.0) {
            reminders.add("Low fuel alert! Less than 10L remaining (${String.format("%.1f", remainingFuel)}L)")
        }
        if (estRemainingDistance < 50.0) {
            reminders.add("Refuel soon! Remaining range is under 50 km (${String.format("%.0f", estRemainingDistance)} km)")
        }
        val oneDayFromRefuel = nextRefuelDate - System.currentTimeMillis() <= 24 * 60 * 60 * 1000
        if (oneDayFromRefuel && estRemainingDistance < 100.0) {
            reminders.add("Refuel predicted tomorrow based on driving patterns.")
        }

        return AppStats(
            kmToday = kmToday,
            kmThisMonth = kmThisMonth,
            currentOdometer = currentOdometer,
            avgMileage = avgMileage,
            costPerKm = costPerKm,
            remainingFuel = remainingFuel,
            estRemainingDistance = estRemainingDistance,
            nextRefuelOdometer = nextRefuelOdometer,
            nextRefuelDate = nextRefuelDate,
            totalMonthlyDiesel = monthlyDieselFilled,
            totalMonthlyCost = monthlyCost,
            reminders = reminders
        )
    }

    // Reports data generations
    fun getReportsData(type: String): ReportSummary {
        val logs = allLogs.value
        val now = Calendar.getInstance()
        val filterCalendar = Calendar.getInstance()

        val filteredLogs = logs.filter { log ->
            filterCalendar.timeInMillis = log.date
            when (type.lowercase()) {
                "daily" -> {
                    now.get(Calendar.YEAR) == filterCalendar.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == filterCalendar.get(Calendar.DAY_OF_YEAR)
                }
                "weekly" -> {
                    now.get(Calendar.YEAR) == filterCalendar.get(Calendar.YEAR) &&
                    now.get(Calendar.WEEK_OF_YEAR) == filterCalendar.get(Calendar.WEEK_OF_YEAR)
                }
                "monthly" -> {
                    now.get(Calendar.YEAR) == filterCalendar.get(Calendar.YEAR) &&
                    now.get(Calendar.MONTH) == filterCalendar.get(Calendar.MONTH)
                }
                "yearly" -> {
                    now.get(Calendar.YEAR) == filterCalendar.get(Calendar.YEAR)
                }
                else -> true
            }
        }

        var dist = 0.0
        var fuel = 0.0
        var cost = 0.0
        for (log in filteredLogs) {
            dist += log.distance
            fuel += log.dieselFilled
            cost += log.dieselCost
        }

        val avgMil = if (fuel > 0) dist / fuel else defaultMileage

        return ReportSummary(
            totalDistance = dist,
            totalFuel = fuel,
            totalCost = cost,
            avgMileage = avgMil,
            logs = filteredLogs
        )
    }

    // Sharing / Export to CSV
    fun generateCsvData(logs: List<LogEntry>): String {
        val sb = java.lang.StringBuilder()
        sb.append("ID,Date,Vehicle Number,Driver,Starting Odometer,Ending Odometer,Distance,Diesel Filled (L),Cost,From,To,Purpose,Notes\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (log in logs) {
            sb.append("${log.id},")
            sb.append("${sdf.format(Date(log.date))},")
            sb.append("\"${log.vehicleNumber.replace("\"", "\"\"")}\",")
            sb.append("\"${log.driverName.replace("\"", "\"\"")}\",")
            sb.append("${log.startingOdometer},")
            sb.append("${log.endingOdometer},")
            sb.append("${log.distance},")
            sb.append("${log.dieselFilled},")
            sb.append("${log.dieselCost},")
            sb.append("\"${log.fromLocation.replace("\"", "\"\"")}\",")
            sb.append("\"${log.toLocation.replace("\"", "\"\"")}\",")
            sb.append("\"${log.purpose.replace("\"", "\"\"")}\",")
            sb.append("\"${log.notes.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    fun shareReportCsv(context: Context, reportType: String, logs: List<LogEntry>) {
        try {
            val csvText = generateCsvData(logs)
            val fileName = "vehicle_log_${reportType.lowercase()}_report.csv"
            val file = File(context.cacheDir, fileName)
            file.writeText(csvText)

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Vehicle Log Book - $reportType Report")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share CSV Report"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export report: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Share as Formatted Text
    fun shareReportText(context: Context, reportType: String, summary: ReportSummary) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("📋 *VEHICLE LOG BOOK - ${reportType.uppercase()} REPORT*\n")
        sb.append("Generated on: ${sdf.format(Date())}\n\n")
        sb.append("• *Total Distance:* ${String.format("%.1f", summary.totalDistance)} km\n")
        sb.append("• *Total Diesel Filled:* ${String.format("%.1f", summary.totalFuel)} L\n")
        sb.append("• *Total Fuel Cost:* $${String.format("%.2f", summary.totalCost)}\n")
        sb.append("• *Average Mileage:* ${String.format("%.2f", summary.avgMileage)} km/L\n")
        sb.append("• *Entries Count:* ${summary.logs.size}\n\n")
        sb.append("Driven Logs Summary:\n")
        for (log in summary.logs.take(10)) {
            sb.append("- ${sdf.format(Date(log.date))} | ${log.vehicleNumber} | ${log.distance} km | By ${log.driverName}\n")
        }
        if (summary.logs.size > 10) {
            sb.append("- ...and ${summary.logs.size - 10} more entries.")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Vehicle Log Book - $reportType Report")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Report via"))
    }

    // Backup import/export action
    fun exportBackup(context: Context) {
        viewModelScope.launch {
            try {
                val backupJson = repository.exportBackupJson()
                val file = File(context.cacheDir, "vehicle_log_backup.json")
                file.writeText(backupJson)

                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "Vehicle Log Book - Database Backup")
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Database Backup"))
            } catch (e: Exception) {
                Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importBackup(context: Context, jsonString: String) {
        viewModelScope.launch {
            val success = repository.importBackupJson(jsonString)
            if (success) {
                Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_SHORT).show()
                loadSettings()
            } else {
                Toast.makeText(context, "Invalid backup format or error during import.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class AppStats(
    val kmToday: Double,
    val kmThisMonth: Double,
    val currentOdometer: Double,
    val avgMileage: Double,
    val costPerKm: Double,
    val remainingFuel: Double,
    val estRemainingDistance: Double,
    val nextRefuelOdometer: Double,
    val nextRefuelDate: Long,
    val totalMonthlyDiesel: Double,
    val totalMonthlyCost: Double,
    val reminders: List<String>
)

data class ReportSummary(
    val totalDistance: Double,
    val totalFuel: Double,
    val totalCost: Double,
    val avgMileage: Double,
    val logs: List<LogEntry>
)
