package com.example.thesistest2.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.example.thesistest2.R


class MainActivity : Activity() {
    private val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 2323 //needed for Android version 10 or higher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //TODO (request normal permission if phone is not xiaomi)
        //TODO (if phone is xiaomi, ask user to allow also additional permission for displaying popups while in background)


        //if the user already granted the permission or the API is below Android 10 no need to ask for permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !Settings.canDrawOverlays(this)) {
            requestPermission()
        }

        if(intent.getStringExtra("test").isNullOrEmpty())
            finish()
        else {
            setContentView(R.layout.activity_main)

        }

    }

    private fun requestPermission() {
        //if phone is xiaomi, user need also to manually enable the "display pop-up windows while running in the background" permission
        if ("xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)) {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
            intent.setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            intent.putExtra("extra_pkgname", packageName)
            startActivity(intent)

        } else {
            val overlaySettings = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(overlaySettings, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    //permission not granted
                    showErrorToast()
                }
                else {
                    // permission Granted
                    showSuccessToast()
                }

            }
        }
    }

    private fun showSuccessToast() {
        Toast.makeText(this, R.string.permissions_granted_message,Toast.LENGTH_LONG).show()
    }
    private fun showErrorToast() {
        Toast.makeText(this, R.string.permissions_not_granted_message,Toast.LENGTH_LONG).show()
    }
}