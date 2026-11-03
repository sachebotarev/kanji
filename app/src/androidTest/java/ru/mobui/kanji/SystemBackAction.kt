package ru.mobui.kanji

import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot

/** Target the dialog window explicitly, waiting for its focus before dispatching Back. */
internal fun pressSystemBack() {
    onView(isRoot()).inRoot(isDialog()).perform(pressKey(KeyEvent.KEYCODE_BACK))
}
