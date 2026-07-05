package com.example.data.dao

import androidx.room.*
import com.example.data.model.LogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {
    @Query("SELECT * FROM log_entries ORDER BY date DESC, id DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries ORDER BY date DESC, id DESC")
    suspend fun getAllLogsList(): List<LogEntry>

    @Query("SELECT * FROM log_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getLogsInDateRange(startDate: Long, endDate: Long): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntry): Long

    @Update
    suspend fun updateLog(log: LogEntry)

    @Delete
    suspend fun deleteLog(log: LogEntry)

    @Query("DELETE FROM log_entries")
    suspend fun deleteAllLogs()
}
