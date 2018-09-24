package de.g00fy2.developerwidget.util

import android.os.Build

class DeviceDataUtils {

  companion object {
    val deviceInfo: String
      get() {
        return if (Build.MODEL.contains(Build.MANUFACTURER)) {
          Build.MODEL.capitalize()
        } else {
          Build.MANUFACTURER.capitalize() + " " + Build.MODEL
        }
      }

    val versionAndCodename: String
      get() = "Android " + Build.VERSION.RELEASE

    val sdkVersion: String
      get() = "API Level " + Integer.toString(Build.VERSION.SDK_INT)
  }
}
