package de.g00fy2.developerwidget.util

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import de.g00fy2.developerwidget.base.BaseActivity

class UiUtils {

  companion object {
    fun hideKeyboard(activity: BaseActivity) {
      activity.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(
        (if (activity.currentFocus == null) View(activity) else activity.currentFocus).windowToken,
        0
      )
    }
  }
}