package com.g00fy2.developerwidget.activities.widgetconfig

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.g00fy2.developerwidget.R
import com.g00fy2.developerwidget.activities.about.AboutActivity
import com.g00fy2.developerwidget.base.BaseActivity
import com.g00fy2.developerwidget.base.BaseContract.BasePresenter
import com.g00fy2.developerwidget.data.DeviceDataItem
import com.g00fy2.developerwidget.receiver.widget.WidgetProviderImpl
import kotlinx.android.synthetic.main.activity_widget_config.*
import javax.inject.Inject

class WidgetConfigActivity : BaseActivity(R.layout.activity_widget_config), WidgetConfigContract.WidgetConfigView {

  @Inject
  lateinit var presenter: WidgetConfigContract.WidgetConfigPresenter

  private var updateExistingWidget = false
  private var launchedFromAppLauncher = true
  private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
  private lateinit var adapter: DeviceDataAdapter
  private val editDrawable by lazy {
    ResourcesCompat.getDrawable(resources, R.drawable.ic_edit, null)?.apply {
      setColorFilter(ResourcesCompat.getColor(resources, R.color.dividerGrey, null), PorterDuff.Mode.SRC_IN)
      setBounds(0, 0, this.intrinsicWidth, this.intrinsicHeight)
    }
  }
  private val closeConfigureActivityReceiver by lazy {
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        presenter.showHomescreen()
        this@WidgetConfigActivity.finish()
      }
    }
  }

  override fun providePresenter(): BasePresenter = presenter

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    registerReceiver(closeConfigureActivityReceiver, IntentFilter(EXTRA_APPWIDGET_CLOSE_CONFIGURE))
    setResult(Activity.RESULT_CANCELED)

    intent.extras?.let {
      widgetId = it.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
      updateExistingWidget = it.getBoolean(EXTRA_APPWIDGET_UPDATE_EXISTING)
      launchedFromAppLauncher = !(widgetId != AppWidgetManager.INVALID_APPWIDGET_ID || updateExistingWidget)
    }

    if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !launchedFromAppLauncher) {
      finish()
      return
    }

    setActionbarElevationListener(widget_config_root_scrollview)

    adapter = DeviceDataAdapter()
    recyclerview.setHasFixedSize(false)
    recyclerview.layoutManager = LinearLayoutManager(this)
    recyclerview.isNestedScrollingEnabled = false
    recyclerview.adapter = adapter

    // set up webview pre oreo to get implementation information
    if (VERSION.SDK_INT in VERSION_CODES.LOLLIPOP until VERSION_CODES.O) {
      WebView(this)
    }
    initViews()
  }

  override fun onResume() {
    super.onResume()
    initViews()
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(closeConfigureActivityReceiver)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.configuration_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.about_button -> {
        startActivity(Intent(this, AboutActivity::class.java))
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun showDeviceData(data: List<Pair<String, DeviceDataItem>>) = adapter.submitList(data)

  override fun setDeviceTitle(title: String) {
    device_title_textview.text = title
    device_title_textview.visibility = View.VISIBLE
    device_title_edittextview.setText(title)
    device_title_edittextview.setSelection(device_title_edittextview.text.length)
  }

  override fun setDeviceTitleHint(hint: String) {
    device_title_edittextview.hint = hint
  }

  override fun setSubtitle(data: Pair<String, String>) {
    device_subtitle_textview.text = getString(R.string.subtitle).format(data.first, data.second)
  }

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    if (event.action == MotionEvent.ACTION_DOWN) {
      currentFocus.let {
        if (it != null && it == device_title_edittextview) {
          Rect().let { rect ->
            it.getGlobalVisibleRect(rect)
            if (!rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
              it.clearFocus()
            }
          }
        }
      }
    }
    return super.dispatchTouchEvent(event)
  }

  private fun initViews() {
    val showAddWidget = !launchedFromAppLauncher || widgetCount() < 1
    if (showAddWidget) {
      apply_button.apply {
        visibility = View.VISIBLE
        if (updateExistingWidget) setText(R.string.update_widget)
        setOnClickListener {
          when {
            launchedFromAppLauncher -> initPinAppWidget()
            updateExistingWidget -> updateWidgetAndFinish(true)
            else -> updateWidgetAndFinish(false)
          }
        }
      }
    } else {
      apply_button.visibility = View.GONE
    }
    device_title_textview.apply {
      setOnClickListener { toggleDeviceNameEdit(true) }
      if (showAddWidget) {
        isClickable = true
        setCompoundDrawables(null, null, editDrawable, null)
        setPadding(
          paddingLeft,
          paddingTop,
          (compoundDrawablePadding * 2) + (editDrawable?.intrinsicWidth ?: 0),
          paddingBottom
        )
      } else {
        isClickable = false
        setCompoundDrawables(null, null, null, null)
        setPadding(paddingLeft, paddingTop, (16 * resources.displayMetrics.density).toInt(), paddingBottom)
      }
    }
    device_title_edittextview.apply {
      setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
          presenter.setCustomDeviceName(device_title_edittextview.text.toString())
          toggleDeviceNameEdit(false)
        }
      }
      setOnEditorActionListener { v, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
          v.clearFocus()
        }
        true
      }
    }
  }

  private fun toggleDeviceNameEdit(editable: Boolean) {
    device_title_textview.visibility = if (editable) View.INVISIBLE else View.VISIBLE
    device_title_edittextview.visibility = if (editable) View.VISIBLE else View.INVISIBLE
    if (editable) {
      device_title_edittextview.requestFocus()
      showKeyboard(device_title_edittextview)
    } else {
      hideKeyboard(device_title_edittextview)
    }
  }

  private fun updateWidgetAndFinish(existing: Boolean) {
    presenter.setCustomDeviceName(device_title_edittextview.text.toString(), true)
    if (!existing) {
      setResult(Activity.RESULT_OK, Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId) })
    }
    sendBroadcast(Intent(applicationContext, WidgetProviderImpl::class.java).apply {
      action = WidgetProviderImpl.UPDATE_WIDGET_ACTION
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
    })
    finish()
  }

  private fun initPinAppWidget() {
    if (VERSION.SDK_INT >= VERSION_CODES.O && !isNokiaLauncher()) {
      getSystemService<AppWidgetManager>()?.let { appWidgetManager ->
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
          val successCallback = PendingIntent.getBroadcast(
            this, 0, Intent(applicationContext, WidgetProviderImpl::class.java).apply {
              putExtra(EXTRA_APPWIDGET_FROM_PIN_APP, true)
              putExtra(EXTRA_APPWIDGET_CUSTOM_DEVICE_NAME, device_title_edittextview.text.toString())
            },
            PendingIntent.FLAG_UPDATE_CURRENT
          )
          appWidgetManager.requestPinAppWidget(
            ComponentName(applicationContext, WidgetProviderImpl::class.java),
            null,
            successCallback
          )
        } else {
          presenter.showManuallyAddWidgetNotice()
        }
      }
    } else {
      presenter.showManuallyAddWidgetNotice()
    }
  }

  // current HMD Global / Nokia launcher crashes when using app widget pinning
  private fun isNokiaLauncher(): Boolean {
    if (!Build.MANUFACTURER.startsWith("HMD")) return false
    return packageManager.resolveActivity(
      Intent("android.intent.action.MAIN").apply { addCategory("android.intent.category.HOME") },
      PackageManager.MATCH_DEFAULT_ONLY
    ).activityInfo.packageName == "com.android.launcher3"
  }

  private fun widgetCount() = AppWidgetManager.getInstance(this).getAppWidgetIds(
    ComponentName(
      applicationContext,
      WidgetProviderImpl::class.java
    )
  ).size

  companion object {
    const val EXTRA_APPWIDGET_CLOSE_CONFIGURE = "EXTRA_APPWIDGET_CLOSE_CONFIGURE"
    const val EXTRA_APPWIDGET_UPDATE_EXISTING = "UPDATE_EXISTING_WIDGET"
    const val EXTRA_APPWIDGET_FROM_PIN_APP = "EXTRA_APPWIDGET_FROM_PIN_APP"
    const val EXTRA_APPWIDGET_CUSTOM_DEVICE_NAME = "EXTRA_APPWIDGET_CUSTOM_DEVICE_NAME"
  }
}
