package com.example.thesistest2.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import com.example.thesistest2.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


class MainActivity : Activity() {
    private val TAG = "MYACTIVITY"
    private val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 2323 //needed for Android version 10 or higher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onResume() {
        super.onResume()

        //restore preferences
        val sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val switchOpen = findViewById<SwitchMaterial>(R.id.switch_open)
        val switchBack = findViewById<SwitchMaterial>(R.id.switch_back)
        val openText = findViewById<TextInputEditText>(R.id.edit_text_open)
        val backText = findViewById<TextInputEditText>(R.id.edit_text_back)

        switchOpen.isChecked = sharedPref.getBoolean("switchOpenActive", false)
        switchBack.isChecked = sharedPref.getBoolean("switchBackActive", false)
        openText.text = Editable.Factory.getInstance().newEditable(sharedPref.getString("openText", ""))
        backText.text = Editable.Factory.getInstance().newEditable(sharedPref.getString("backText", ""))


        //check if app needs permissions (Android >10 and not already given)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Settings.canDrawOverlays(this)){
            //app needs permissions
            showLayoutPermission()
        }
        else{
            //app has permissions
            showLayoutSettings()
        }
    }

    override fun onPause() {
        super.onPause()

        //save preferences
        val sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val switchOpen = findViewById<SwitchMaterial>(R.id.switch_open)
        val switchBack = findViewById<SwitchMaterial>(R.id.switch_back)
        val openText = findViewById<TextInputEditText>(R.id.edit_text_open)
        val backText = findViewById<TextInputEditText>(R.id.edit_text_back)

        with (sharedPref.edit()) {
            putBoolean("switchOpenActive", switchOpen.isChecked)
            putBoolean("switchBackActive", switchBack.isChecked)
            putString("openText", openText.text.toString())
            putString("backText", backText.text.toString())
            apply()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                //permission not granted
                showErrorToast()
                showLayoutPermission()
            }
            else {
                // permission Granted
                showSuccessToast()
                showLayoutSettings()
            }

        }
    }

    private fun requestPermission() {
        //TODO (check normal permission if phone is not xiaomi)

        //if phone is xiaomi, user need also to manually enable the "display pop-up windows while running in the background" permission
        if ("xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)) {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
            intent.setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            intent.putExtra("extra_pkgname", packageName)
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)

        } else {
            val overlaySettings = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(overlaySettings, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    private fun showLayoutPermission(){
        //hide components for settings
        val settingsLayout = findViewById<ConstraintLayout>(R.id.layout_settings)
        settingsLayout.visibility = View.GONE

        //display layout permissions
        val give_permission_layout = findViewById<ConstraintLayout>(R.id.layout_permissions)
        give_permission_layout.visibility = View.VISIBLE

        //add listener to btn
        val givePermissionBtn = findViewById<Button>(R.id.give_permissions_btn)
        givePermissionBtn.setOnClickListener { requestPermission() }

    }

    private fun showLayoutSettings(){
        //hide components for requesting permissions
        val give_permission_layout = findViewById<ConstraintLayout>(R.id.layout_permissions)
        give_permission_layout.visibility = View.GONE

        //display settings
        val settingsLayout = findViewById<ConstraintLayout>(R.id.layout_settings)
        settingsLayout.visibility = View.VISIBLE


        val switch_open = findViewById<SwitchMaterial>(R.id.switch_open)
        val switch_back = findViewById<SwitchMaterial>(R.id.switch_back)
        val openButton = findViewById<AppCompatButton>(R.id.button3)
        val backButton = findViewById<AppCompatButton>(R.id.button2)

        //when phone rotated, show correct input texts and preview
        updateUI(switch_open, switch_back)

        //add listener to switches
        switch_open.setOnCheckedChangeListener { compoundButton, isChecked ->
            updateUI(switch_open, switch_back)

            if(isChecked) {
                //show layout for text
                findViewById<TextInputLayout>(R.id.text_field_open).visibility = View.VISIBLE
            }
            else {
                //hide input for text
                findViewById<TextInputLayout>(R.id.text_field_open).visibility = View.GONE

                //update button3 in preview with default
                openButton.text = resources.getString(R.string.open_button_label)
            }
        }
        switch_back.setOnCheckedChangeListener { compoundButton, isChecked ->
            updateUI(switch_open, switch_back)
            if(isChecked) {
                //show layout for text
                findViewById<TextInputLayout>(R.id.text_field_back).visibility = View.VISIBLE
            }
            else {
                //hide input for text
                findViewById<TextInputLayout>(R.id.text_field_back).visibility = View.GONE

                //update button2 in preview with default
                backButton.text = resources.getString(R.string.back_button_label)
            }
        }

        //add text change listener for open text input to update preview
        findViewById<TextInputEditText>(R.id.edit_text_open).doAfterTextChanged {
            //update Button3 in preview
            if(!it.isNullOrBlank())
                openButton.text = it
            else openButton.text = resources.getString(R.string.open_button_label)
        }

        //add text change listener for back text input to update preview
        findViewById<TextInputEditText>(R.id.edit_text_back).doAfterTextChanged {
            //update backButton in preview
            if(!it.isNullOrBlank())
                backButton.text = it
            else backButton.text = resources.getString(R.string.back_button_label)
        }


    }

    private fun updateUI(switch_open: SwitchMaterial, switch_back: SwitchMaterial) {
        if(switch_open.isChecked){
            //show layout for text
            findViewById<TextInputLayout>(R.id.text_field_open).visibility = View.VISIBLE
            //update Button3 in preview
            val txt = findViewById<TextInputEditText>(R.id.edit_text_open).text
            if(!txt.isNullOrBlank())
                findViewById<AppCompatButton>(R.id.button3).text = txt
            else
                findViewById<AppCompatButton>(R.id.button3).text = resources.getString(R.string.open_button_label)

        }
        if(switch_back.isChecked){
            //show layout for text
            findViewById<TextInputLayout>(R.id.text_field_back).visibility = View.VISIBLE
            //update Button2 in preview
            val txt = findViewById<TextInputEditText>(R.id.edit_text_back).text
            if(!txt.isNullOrBlank())
                findViewById<AppCompatButton>(R.id.button2).text = txt
            else
                findViewById<AppCompatButton>(R.id.button2).text = resources.getString(R.string.back_button_label)
        }
    }

    private fun showSuccessToast() {
        Toast.makeText(baseContext, R.string.permissions_granted_message,Toast.LENGTH_LONG).show()
    }
    private fun showErrorToast() {
        Toast.makeText(baseContext, R.string.permissions_not_granted_message,Toast.LENGTH_LONG).show()
    }
}