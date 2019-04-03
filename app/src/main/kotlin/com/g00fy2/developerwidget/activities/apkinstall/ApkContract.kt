package com.g00fy2.developerwidget.activities.apkinstall

import com.g00fy2.developerwidget.base.BaseContract

interface ApkContract {

  interface ApkView : BaseContract.BaseView {

    fun toggleResultView(apkFiles: List<ApkFile>, missingPermissions: Boolean)
  }

  interface ApkPresenter : BaseContract.BasePresenter {

    fun installApk(apkFile: ApkFile?)

    fun deleteApkFiles(apkFiles: List<ApkFile>?)
  }
}