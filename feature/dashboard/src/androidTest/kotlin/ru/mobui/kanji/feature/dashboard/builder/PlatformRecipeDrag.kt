package ru.mobui.kanji.feature.dashboard.builder

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry

/** Real touchscreen events: Compose pointer injection does not drive an Android drag session. */
internal fun platformRecipeDrag(
    rule: ComposeContentTestRule,
    start: Offset,
    end: Offset,
    cancel: Boolean = false,
    holdMillis: Long = 0,
) {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val downTime = SystemClock.uptimeMillis()
    fun touch(action: Int, position: Offset) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, position.x, position.y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try { check(automation.injectInputEvent(event, true)) } finally { event.recycle() }
    }
    rule.mainClock.autoAdvance = false
    try {
        touch(MotionEvent.ACTION_DOWN, start)
        SystemClock.sleep(ViewConfiguration.getLongPressTimeout().toLong() + 200)
        rule.mainClock.advanceTimeBy(ViewConfiguration.getLongPressTimeout().toLong() + 300)
        SystemClock.sleep(100)
        repeat(20) { step ->
            val fraction = (step + 1) / 20f
            touch(MotionEvent.ACTION_MOVE, start + (end - start) * fraction)
            rule.mainClock.advanceTimeBy(32)
            SystemClock.sleep(16)
        }
        repeat((holdMillis / 32).toInt()) {
            rule.mainClock.advanceTimeBy(32)
            SystemClock.sleep(16)
        }
        touch(if (cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP, end)
        SystemClock.sleep(200)
        rule.mainClock.advanceTimeBy(500)
    } finally {
        rule.mainClock.autoAdvance = true
    }
}
