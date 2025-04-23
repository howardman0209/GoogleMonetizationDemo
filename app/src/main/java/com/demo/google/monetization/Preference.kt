package com.demo.google.monetization

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Preference {
    private const val PREFERENCE_FILENAME = "PREF"
    private const val PREF_KEY_FEATURE_A = "FeatureA"
    private const val PREF_KEY_FEATURE_B = "FeatureB"

    private fun getPref(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE)
    }

    fun saveFeatureAAccessible(context: Context, accessible: Boolean) {
        val pref = getPref(context)
        pref.edit {
            putBoolean(PREF_KEY_FEATURE_A, accessible)
        }
    }

    fun getFeatureAAccessible(context: Context): Boolean {
        val pref = getPref(context)
        return pref.getBoolean(PREF_KEY_FEATURE_A, false)
    }

    fun saveFeatureBAccessible(context: Context, accessible: Boolean) {
        val pref = getPref(context)
        pref.edit {
            putBoolean(PREF_KEY_FEATURE_B, accessible)
        }
    }

    fun getFeatureBAccessible(context: Context): Boolean {
        val pref = getPref(context)
        return pref.getBoolean(PREF_KEY_FEATURE_B, false)
    }

}