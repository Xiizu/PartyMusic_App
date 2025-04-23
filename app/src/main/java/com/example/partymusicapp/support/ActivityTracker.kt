package com.example.partymusicapp.support

import android.app.Activity
import kotlin.Boolean

object ActivityTracker {

    val openActivities = mutableListOf<Activity>()

    fun isLastActivity() : Boolean {
        return openActivities.size <= 1
    }

    fun register(activity: Activity) {
        openActivities.add(activity)
    }

    fun unregister(activity: Activity) {
        openActivities.remove(activity)
    }
}
