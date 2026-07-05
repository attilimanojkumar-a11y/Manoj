package com.example.data.repository

import com.example.data.dao.AppSettingDao
import com.example.data.dao.LogEntryDao
import com.example.data.model.AppSetting
import com.example.data.model.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class LogRepository(
    private val logEntryDao: LogEntryDao,
    private val appSettingDao: AppSettingDao
) {
    // Log entries
    val allLogs: Flow<List<LogEntry>> = logEntryDao.getAllLogs()

    suspend fun getLogsInDateRangeList(startDate: Long, endDate: Long): List<LogEntry> {
        return logEntryDao.getAllLogsList().filter { it.date in startDate..endDate }
    }

    suspend fun insertLog(log: LogEntry) = logEntryDao.insertLog(log)

    suspend fun updateLog(log: LogEntry) = logEntryDao.updateLog(log)

    suspend fun deleteLog(log: LogEntry) = logEntryDao.deleteLog(log)

    suspend fun deleteAllLogs() = logEntryDao.deleteAllLogs()

    // Settings helpers
    val allSettingsFlow: Flow<List<AppSetting>> = appSettingDao.getAllSettingsFlow()

    suspend fun getSetting(key: String, defaultValue: String): String {
        return appSettingDao.getSettingByKey(key)?.value ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        appSettingDao.insertSetting(AppSetting(key, value))
    }

    suspend fun deleteSetting(key: String) {
        appSettingDao.deleteSetting(key)
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String {
        val allLogsList = logEntryDao.getAllLogsList()
        val allSettingsList = appSettingDao.getAllSettingsFlow().first()

        val rootObj = JSONObject()
        
        // Serialize Logs
        val logsArray = JSONArray()
        for (log in allLogsList) {
            val logObj = JSONObject()
            logObj.put("id", log.id)
            logObj.put("date", log.date)
            logObj.put("vehicleNumber", log.vehicleNumber)
            logObj.put("driverName", log.driverName)
            logObj.put("startingOdometer", log.startingOdometer)
            logObj.put("endingOdometer", log.endingOdometer)
            logObj.put("dieselFilled", log.dieselFilled)
            logObj.put("dieselCost", log.dieselCost)
            logObj.put("purpose", log.purpose)
            logObj.put("fromLocation", log.fromLocation)
            logObj.put("toLocation", log.toLocation)
            logObj.put("notes", log.notes)
            logsArray.put(logObj)
        }
        rootObj.put("logs", logsArray)

        // Serialize Settings
        val settingsArray = JSONArray()
        for (setting in allSettingsList) {
            val settingObj = JSONObject()
            settingObj.put("key", setting.key)
            settingObj.put("value", setting.value)
            settingsArray.put(settingObj)
        }
        rootObj.put("settings", settingsArray)

        return rootObj.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        return try {
            val rootObj = JSONObject(jsonString)
            
            // Clear existing data
            logEntryDao.deleteAllLogs()

            // Restore Logs
            if (rootObj.has("logs")) {
                val logsArray = rootObj.getJSONArray("logs")
                for (i in 0 until logsArray.length()) {
                    val logObj = logsArray.getJSONObject(i)
                    val log = LogEntry(
                        date = logObj.optLong("date", System.currentTimeMillis()),
                        vehicleNumber = logObj.optString("vehicleNumber", ""),
                        driverName = logObj.optString("driverName", ""),
                        startingOdometer = logObj.optDouble("startingOdometer", 0.0),
                        endingOdometer = logObj.optDouble("endingOdometer", 0.0),
                        dieselFilled = logObj.optDouble("dieselFilled", 0.0),
                        dieselCost = logObj.optDouble("dieselCost", 0.0),
                        purpose = logObj.optString("purpose", ""),
                        fromLocation = logObj.optString("fromLocation", ""),
                        toLocation = logObj.optString("toLocation", ""),
                        notes = logObj.optString("notes", "")
                    )
                    logEntryDao.insertLog(log)
                }
            }

            // Restore Settings
            if (rootObj.has("settings")) {
                val settingsArray = rootObj.getJSONArray("settings")
                for (i in 0 until settingsArray.length()) {
                    val settingObj = settingsArray.getJSONObject(i)
                    val key = settingObj.optString("key", "")
                    val value = settingObj.optString("value", "")
                    if (key.isNotEmpty()) {
                        appSettingDao.insertSetting(AppSetting(key, value))
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
