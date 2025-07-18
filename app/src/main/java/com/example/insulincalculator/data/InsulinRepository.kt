package com.example.insulincalculator.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

interface InsulinRepository {
    suspend fun saveEntry(entry: InsulinEntry)
    fun getAllEntries(): Flow<List<InsulinEntry>>
    suspend fun clearEntries()
    suspend fun deleteEntry(entry: InsulinEntry)
}

class InsulinRepositoryImpl(private val context: Context) : InsulinRepository {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("insulin_data", Context.MODE_PRIVATE)
    private val gson = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create()
    private val entriesFlow = MutableStateFlow<List<InsulinEntry>>(emptyList())

    init {
        loadEntries()
    }

    private fun loadEntries() {
        val entriesJson = sharedPreferences.getString("history", "[]")
        val type = object : TypeToken<List<InsulinEntry>>() {}.type
        val entries = gson.fromJson<List<InsulinEntry>>(entriesJson, type) ?: emptyList()
        entriesFlow.value = entries
    }

    override suspend fun saveEntry(entry: InsulinEntry) {
        val currentEntries = entriesFlow.value.toMutableList()
        currentEntries.add(entry)
        entriesFlow.value = currentEntries
        
        sharedPreferences.edit().apply {
            putString("history", gson.toJson(currentEntries))
            apply()
        }
    }

    override fun getAllEntries(): Flow<List<InsulinEntry>> = entriesFlow.asStateFlow()

    override suspend fun clearEntries() {
        entriesFlow.value = emptyList()
        sharedPreferences.edit().clear().apply()
    }

    override suspend fun deleteEntry(entry: InsulinEntry) {
        val currentEntries = entriesFlow.value.toMutableList()
        currentEntries.removeAll { it.timestamp == entry.timestamp }
        entriesFlow.value = currentEntries
        sharedPreferences.edit().apply {
            putString("history", gson.toJson(currentEntries))
            apply()
        }
    }
} 