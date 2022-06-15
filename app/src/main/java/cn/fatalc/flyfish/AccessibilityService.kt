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
import java.time.LocalTime

class AccessibilityService : AccessibilityService() {
    private val keywords = setOf("跳过")
    private val ignoredPackages = setOf("com.android.systemui")
    private lateinit var toast: Toast
    private var lastWindowChange: LocalTime = LocalTime.now()
    private var info = AccessibilityServiceInfo()

    override fun onServiceConnected() {
        info.apply {
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            eventTypes =
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        }
        this.serviceInfo = info
        this.toast = Toast.makeText(this, "\uD83D\uDC4C", Toast.LENGTH_SHORT).apply {
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        event.takeIf {
            event.isEnabled
                    && event.packageName.isNotBlank()
                    && event.className.isNotBlank()
                    && it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && !ignoredPackages.contains(event.packageName)
        }?.apply {
            Log.v(
                "launched", "name %s package %s activity %s "
                    .format(text, packageName, className)
            )
            resetTimeout()
        }
        event.takeIf { isInTimeout() }?.apply { source?.performSkipIfNeeded() }
    }

    private fun AccessibilityNodeInfo.performSkipIfNeeded() {
        keywords.forEach {
            findAccessibilityNodeInfosByText(it).forEach { info1 ->
                Log.d("found ad like", (info1.text ?: "").toString())
                info1.performClick(this@AccessibilityService)
                    .takeIf { true }
                    ?.apply {
                        toast.show()
                    }
            }
        }
    }

    private fun AccessibilityNodeInfo.performClick(service: AccessibilityService): Boolean {
        Log.d("perform", "clicked %s".format(this))
        return when (isClickable) {
            true -> {
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            false -> Rect()
                .apply {
                    this@performClick.getBoundsInScreen(this)
                }.let {
                    Path().apply { moveTo(it.exactCenterX(), it.exactCenterY()) }
                }.let {
                    GestureDescription.Builder().addStroke(StrokeDescription(it, 0, 20)).build()
                }?.let {
                    service.dispatchGesture(it, null, null)
                } ?: false
        }
    }

    private fun foreachParentRecursive(
        nodeInfo: AccessibilityNodeInfo,
        fn: ((item: AccessibilityNodeInfo) -> Boolean)
    ) {
        if (fn(nodeInfo)) {
            return
        }
        nodeInfo.parent?.apply {
            foreachParentRecursive(this, fn)
        }
    }

    private fun foreachRecursive(
        nodeInfo: AccessibilityNodeInfo,
        fn: ((item: AccessibilityNodeInfo) -> Boolean)
    ) {
        if (fn(nodeInfo)) {
            return
        }
        for (i in 0 until nodeInfo.childCount) {
            nodeInfo.getChild(i)?.apply {
                foreachRecursive(this, fn)
            }
        }
    }

    private fun isInTimeout(): Boolean {
        return lastWindowChange.plusSeconds(5).isAfter(LocalTime.now())
    }

    private fun resetTimeout() {
        lastWindowChange = LocalTime.now()
    }

    override fun onInterrupt() {
        Log.d("Event", "interrupt")
    }
}