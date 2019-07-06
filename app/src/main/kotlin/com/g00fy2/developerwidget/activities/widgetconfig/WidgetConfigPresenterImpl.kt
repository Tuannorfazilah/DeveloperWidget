package com.g00fy2.developerwidget.activities.widgetconfig

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.OnLifecycleEvent
import com.g00fy2.developerwidget.R
import com.g00fy2.developerwidget.base.BasePresenterImpl
import com.g00fy2.developerwidget.controllers.IntentController
import com.g00fy2.developerwidget.controllers.StringController
import com.g00fy2.developerwidget.controllers.ToastController
import com.g00fy2.developerwidget.controllers.WidgetPreferenceController
import com.g00fy2.developerwidget.data.DeviceDataItem
import com.g00fy2.developerwidget.data.DeviceDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class WidgetConfigPresenterImpl @Inject constructor() : BasePresenterImpl(),
  WidgetConfigContract.WidgetConfigPresenter {

  @Inject
  lateinit var view: WidgetConfigContract.WidgetConfigView
  @Inject
  lateinit var deviceDataSource: DeviceDataSource
  @Inject
  lateinit var stringController: StringController
  @Inject
  lateinit var widgetPreferenceController: WidgetPreferenceController
  @Inject
  lateinit var toastController: ToastController
  @Inject
  lateinit var intentController: IntentController

  private val defaultDeviceName by lazy { deviceDataSource.getCombinedDeviceName() }

  @OnLifecycleEvent(Event.ON_CREATE)
  override fun loadDeviceData() {
    launch {
      withContext(Dispatchers.IO) {
        getDeviceData()
      }.let {
        view.showDeviceData(it)
      }
    }
  }

  @OnLifecycleEvent(Event.ON_CREATE)
  override fun loadCustomDeviceName() {
    launch {
      withContext(Dispatchers.IO) {
        widgetPreferenceController.getCustomDeviceName()
      }.let {
        if (it.isNotEmpty()) {
          view.setDeviceTitle(it)
        } else {
          view.setDeviceTitle(defaultDeviceName)
        }
        view.setSubtitle(deviceDataSource.getVersionAndSDK())
        view.setDeviceTitleHint(defaultDeviceName)
      }
    }
  }

  override fun setCustomDeviceName(deviceName: String, persistent: Boolean): Boolean {
    if (deviceName.isNotEmpty()) {
      view.setDeviceTitle(deviceName)
    } else {
      view.setDeviceTitle(defaultDeviceName)
    }
    if (persistent) {
      return if (deviceName == defaultDeviceName) {
        widgetPreferenceController.saveCustomDeviceName("")
      } else {
        widgetPreferenceController.saveCustomDeviceName(deviceName)
      }
    }
    return true
  }

  override fun showHomescreen() = intentController.showHomescreen()

  private suspend fun getDeviceData(): List<Pair<String, DeviceDataItem>> {
    return deviceDataSource
      .getStaticDeviceData()
      .plus(deviceDataSource.getHardwareData())
      .plus(deviceDataSource.getSoftwareInfo())
      .plus(deviceDataSource.getHeaderItems())
      .toList()
      .filter { (_, value) -> (value.value.isNotBlank() || value.isHeader) && !value.value.equals("unknown", true) }
      .sortedWith(
        compareBy(
          { it.second.category.ordinal },
          { !it.second.isHeader },
          { stringController.getString(it.second.title) })
      )
  }

  override fun shareDeviceData() {
    launch {
      withContext(Dispatchers.IO) {
        getDeviceData()
      }.let { intentController.shareDeviceData(formatDeviceDataString(it)) }
    }
  }

  private fun formatDeviceDataString(data: List<Pair<String, DeviceDataItem>>): String {
    return data.joinToString("") {
      if (it.second.isHeader) {
        "\n" + stringController.getString(it.second.title) + "\n"
      } else {
        stringController.getString(it.second.title) + ": \t" + it.second.value.replace("\n", " ") + "\n"
      }
    }.removeSurrounding("\n")
  }


  override fun showManuallyAddWidgetNotice() = toastController.showToast(R.string.manually_add_widget)
}