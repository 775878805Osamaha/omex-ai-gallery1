package com.omex.gallery.core.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history_prefs")

/**
 * DataStore-backed SearchHistoryRepository for managing recent local search queries.
 */
class SearchHistoryRepository(private val context: Context) {

    private val recentQueriesKey = stringSetPreferencesKey("recent_search_queries")
    private val maxHistorySize = 10

    fun getRecentQueries(): Flow<List<String>> {
        return context.searchHistoryDataStore.data.map { prefs ->
            val set = prefs[recentQueriesKey] ?: emptySet()
            set.toList().reversed()
        }
    }

    suspend fun addQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        context.searchHistoryDataStore.edit { prefs ->
            val currentList = (prefs[recentQueriesKey] ?: emptySet()).toMutableList()
            currentList.remove(trimmed)
            currentList.add(trimmed)
            while (currentList.size > maxHistorySize) {
                currentList.removeAt(0)
            }
            prefs[recentQueriesKey] = currentList.toSet()
        }
    }

    suspend fun removeQuery(query: String) {
        context.searchHistoryDataStore.edit { prefs ->
            val currentSet = (prefs[recentQueriesKey] ?: emptySet()).toMutableSet()
            currentSet.remove(query)
            prefs[recentQueriesKey] = currentSet
        }
    }

    suspend fun clearHistory() {
        context.searchHistoryDataStore.edit { prefs ->
            prefs.remove(recentQueriesKey)
        }
    }
}
