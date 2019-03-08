package de.g00fy2.developerwidget.activities.apkinstall

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Environment
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.OnLifecycleEvent
import de.g00fy2.developerwidget.base.BasePresenterImpl
import de.g00fy2.developerwidget.controllers.IntentController
import de.g00fy2.developerwidget.controllers.PermissionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ApkPresenterImpl @Inject constructor() : BasePresenterImpl(), ApkContract.ApkPresenter {

  @Inject lateinit var view: ApkContract.ApkView
  @Inject lateinit var intentController: IntentController
  @Inject lateinit var permissionController: PermissionController
  @Inject lateinit var apkFileBuilder: ApkFile.ApkFileBuilder

  @OnLifecycleEvent(ON_CREATE)
  @TargetApi(VERSION_CODES.JELLY_BEAN)
  fun requestPermission() {
    permissionController.requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
  }

  @OnLifecycleEvent(ON_RESUME)
  fun checkPermissionAndScanApks() {
    if (permissionController.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      launch {
        val apkFiles = searchAPKs(Environment.getExternalStorageDirectory())
        view.toggleResultView(apkFiles, false)
      }
    } else {
      view.toggleResultView(ArrayList(), missingPermissions = true)
    }
  }

  private suspend fun searchAPKs(dir: File): List<ApkFile> {
    return withContext(Dispatchers.IO) {
      dir.walk()
        .filter { !it.isDirectory }
        .filter { it.extension.equals(APK_EXTENSION, true) }
        .map { apkFileBuilder.build(it) }
        .filter { it.valid }
        .sorted()
        .toList()
    }
  }

  override fun installApk(fileUri: Uri?) {
    fileUri?.let {
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(it, APK_MIME_TYPE)
        flags =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Intent.FLAG_GRANT_READ_URI_PERMISSION else Intent.FLAG_ACTIVITY_NEW_TASK
      }.let { intent -> intentController.startActivity(intent) }
    }
  }

  companion object {
    const val APK_EXTENSION = "apk"
    const val APK_MIME_TYPE = "application/vnd.android.package-archive"
  }

}