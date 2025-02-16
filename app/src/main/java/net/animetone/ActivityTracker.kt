package net.animetone

import android.app.Activity
import android.app.Application
import android.os.Bundle

class ActivityTracker : Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null
    private var isActivityInBackground = true

    // Called when an activity is created
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // No changes here
    }

    // Called when an activity is resumed
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        isActivityInBackground = false // Activity is in the foreground
    }

    // Called when an activity is paused
    override fun onActivityPaused(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null // Activity is no longer in the foreground
        }
    }

    // Called when an activity is stopped
    override fun onActivityStopped(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null // Activity is no longer in the foreground
        }
        isActivityInBackground = true // Activity may be in the background
    }

    // Called when an activity is destroyed
    override fun onActivityDestroyed(activity: Activity) {
        // No changes here
    }

    // Other lifecycle methods you can leave empty if not needed
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    // Get current activity
    fun getCurrentActivity(): Activity? = currentActivity

    // Check if activity is in background
    fun isActivityInBackground(): Boolean = isActivityInBackground
}

