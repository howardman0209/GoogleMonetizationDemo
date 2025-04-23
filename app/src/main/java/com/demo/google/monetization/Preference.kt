package com.demo.google.monetization

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Preference {
    private const val PREFERENCE_FILENAME = "PREF"
    private const val PREF_KEY_NO_ADS = "NoAds"
    private const val PREF_KEY_NUMBER_OF_COIN = "NumberOfCoin"

    private fun getPref(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE)
    }

    fun saveNoAds(context: Context, accessible: Boolean) {
        val pref = getPref(context)
        pref.edit {
            putBoolean(PREF_KEY_NO_ADS, accessible)
        }
    }

    fun getNoAds(context: Context): Boolean {
        val pref = getPref(context)
        return pref.getBoolean(PREF_KEY_NO_ADS, false)
    }

    fun getNumberOfCoin(context: Context): Int {
        val pref = getPref(context)
        return pref.getInt(PREF_KEY_NUMBER_OF_COIN, 0)
    }

    fun incrementAndGetNumberOfCoin(context: Context, increment: Int): Int {
        val pref = getPref(context)
        val current = pref.getInt(PREF_KEY_NUMBER_OF_COIN, 0)
        pref.edit {
            putInt(PREF_KEY_NUMBER_OF_COIN, current + increment)
        }
        return current + increment
    }

    fun decrementAndGetNumberOfCoin(context: Context, decrement: Int): Int {
        val pref = getPref(context)
        val current = pref.getInt(PREF_KEY_NUMBER_OF_COIN, 0)
        pref.edit {
            putInt(PREF_KEY_NUMBER_OF_COIN, current - decrement)
        }
        return current - decrement
    }
}