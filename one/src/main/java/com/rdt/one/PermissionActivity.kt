package com.rdt.one

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.rdt.one.MyUtil.Companion.showToast

open class PermissionActivity : AppCompatActivity() {

    private val TAG = "PermissionActivity"
    private lateinit var mPermissions: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPermissions = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != RequestCode.PERMISSION.i) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                showToast(R.string.permissionDenied)
            }
        }
    }

    //
    // PRIVATE FUN
    //
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val notGrantedList = arrayListOf<String>()
        var askUser = false
        for (permission in mPermissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                notGrantedList.add(permission)
                askUser = askUser || shouldShowRequestPermissionRationale(permission)
            }
        }
        if (notGrantedList.size > 0) {
            requestPermission(notGrantedList.toArray(arrayOfNulls<String>(notGrantedList.size)), askUser)
        }
        return notGrantedList.size == 0
    }

    private fun requestPermission(permissions: Array<String>, askUser: Boolean) {
        when (askUser) {
            true -> {
                val builder = AlertDialog.Builder(this).create()
                builder.setTitle(R.string.permissionTitle)
                builder.setMessage(getString(R.string.permissionMessage))
                builder.setCancelable(false)
                builder.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok)) {
                    _ , _ -> ActivityCompat.requestPermissions(this, permissions, RequestCode.PERMISSION.i)
                }
            }
        }
    }

}