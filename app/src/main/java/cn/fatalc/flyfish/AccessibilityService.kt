package cn.fatalc.flyfish

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalTime

class AccessibilityService : AccessibilityService() {
    private var preferences: UserPreferences = UserPreferences()
    private lateinit var toast: Toast
    private var currentPackage = CurrentPackage("")
    private var info = AccessibilityServiceInfo()

    override fun onServiceConnected() {
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {
            UserPreferencesRepository(dataStore).mutablePreferencesFlow.collect {
                preferences = it.toUserPreferences()
                Log.d("updated", "current setting %s".format(preferences))
            }
        }

        info.apply {
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            eventTypes =
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        }
        this.serviceInfo = info
        this.toast = Toast.makeText(this, "\uD83D\uDC4C", Toast.LENGTH_SHORT)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName.isNullOrEmpty()
            || event.className.isNullOrEmpty()
            || preferences.ignorePackages.contains(event.packageName)
            || event.packageName == packageName
        ) {
            return
        }

        event
            .also {
                it.takeIf {
                    it.canPerformAction()
                }?.apply {
                    source?.performSkipIfNeeded()
                }
            }.also {
                it.takeIf {
                    it.isNewLaunch()
                }?.apply {
                    resetCurrent(event)
                }
            }
    }

    private fun AccessibilityNodeInfo.performSkipIfNeeded() {
        preferences.keyword.forEach {
            findAccessibilityNodeInfosByText(it).forEach { info1 ->
                info1?.also {
                    Log.d(
                        "found ad like",
                        "package: %s text: %s content description: %s".format(
                            info1.packageName,
                            info1.text,
                            info1.contentDescription
                        )
                    )
                }?.performClick(
                    this@AccessibilityService
                ).takeIf {
                    true
                }?.apply {
                    toast.show()
                    Log.d("perform", "clicked %s".format(info1))
                    this@AccessibilityService.currentPackage.countInc()
                }
            }
        }
    }

    private fun AccessibilityNodeInfo.performClick(service: AccessibilityService): Boolean {
        return when (isClickable) {
            true -> {
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            false -> Rect()
                .apply {
                    this@performClick.getBoundsInScreen(this)
                    Log.v("gesture", "use gesture on %s".format(this))
                }.let {
                    Path().apply { moveTo(it.exactCenterX(), it.exactCenterY()) }
                }.let {
                    GestureDescription.Builder().addStroke(StrokeDescription(it, 0, 20)).build()
                }?.let {
                    service.dispatchGesture(it, null, null)
                } ?: false
        }
    }

    private fun AccessibilityEvent.canPerformAction(): Boolean {
        return packageName == currentPackage.packageName
                && currentPackage.performCount < preferences.maxPerformActions
                && currentPackage.startAt.plusSeconds(preferences.ignoreAfterSeconds)
            .isAfter(LocalTime.now())
    }

    private fun AccessibilityEvent.isNewLaunch(): Boolean {
        return (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
                || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
                && isEnabled
                && packageName.isNotBlank() && className.isNotBlank()
                && currentPackage.packageName != packageName
    }

    private fun resetCurrent(event: AccessibilityEvent) {
        Log.v("current", "package %s".format(event.packageName))
        currentPackage = CurrentPackage(event.packageName.toString())
    }

    override fun onInterrupt() {
        Log.d("interrupt", "on interrupt")
    }

    data class CurrentPackage(val packageName: String, var performCount: Int = 0) {
        val startAt: LocalTime = LocalTime.now()

        fun countInc() {
            performCount = performCount.inc()
        }
    }
}