package com.g00fy2.developerwidget.activities.apkinstall

import android.Manifest
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import com.g00fy2.developerwidget.base.BasePresenterImpl
import com.g00fy2.developerwidget.controllers.IntentController
import com.g00fy2.developerwidget.controllers.PermissionController
import com.g00fy2.developerwidget.controllers.StorageDirsController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ApkPresenterImpl @Inject constructor() : BasePresenterImpl(), ApkContract.ApkPresenter {

  @Inject
  lateinit var view: ApkContract.ApkView
  @Inject
  lateinit var intentController: IntentController
  @Inject
  lateinit var permissionController: PermissionController
  @Inject
  lateinit var storageDirsController: StorageDirsController
  @Inject
  lateinit var apkFileBuilder: ApkFile.ApkFileBuilder

  @OnLifecycleEvent(Event.ON_CREATE)
  fun requestPermission() {
    permissionController.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
  }

  @OnLifecycleEvent(Event.ON_RESUME)
  fun scanStorageForApks() {
    if (permissionController.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      view.lifecycleScope.launch {
        withContext(Dispatchers.IO) {
          mutableSetOf<ApkFile>().apply {
            for (dir in storageDirsController.getStorageDirectories()) {
              addAll(searchAPKs(dir))
            }
          }
        }.let {
          view.toggleResultView(it.sorted(), false)
        }
      }
    } else {
      view.toggleResultView(emptyList(), true)
    }
  }

  private fun searchAPKs(dir: File): Collection<ApkFile> {
    return dir.walk()
      .filter { !it.isDirectory }
      .filter { it.extension.equals("apk", true) }
      .map { apkFileBuilder.build(it) }
      .filter { it.valid }
      .toList()
  }

  override fun installApk(apkFile: ApkFile?) {
    apkFile?.let { intentController.installApk(it) }
  }

  override fun deleteApkFiles(apkFiles: List<ApkFile>?) {
    if (permissionController.hasPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
    ) {
      view.lifecycleScope.launch {
        withContext(Dispatchers.IO) {
          apkFiles?.let { files ->
            for (apkFile in files) {
              File(apkFile.filePath).delete()
            }
          }
        }
        scanStorageForApks()
      }
    }
  }
}