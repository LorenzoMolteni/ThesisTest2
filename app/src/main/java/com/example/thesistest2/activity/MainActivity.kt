package com.example.thesistest2.activity

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import com.example.thesistest2.R
import com.example.thesistest2.db.Db
import com.example.thesistest2.service.MyService
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : Activity() {
    private val TAG = "MYACTIVITY"
    private val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 2323 //needed for Android version 10 or higher
    private var alreadySeenTutorial = false

    //GOOGLE SIGN IN VARIABLES
    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private val REQ_ONE_TAP = 200
    //END GOOGLE SIGN IN VARIABLES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = Firebase.auth
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

        alreadySeenTutorial = sharedPref.getBoolean("alreadySeenTutorial", false)

        updateLayout()

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

            putBoolean("alreadySeenTutorial", alreadySeenTutorial)
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
                showLayoutEnableService()
            }

        }
        else if(requestCode == REQ_ONE_TAP){
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                when {
                    idToken != null -> {
                        // Got an ID token from Google. Use it to authenticate
                        // with Firebase.
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    Log.d(TAG, "signInWithCredential:success, ${user}")
                                    Db.createUserIfNotExists()
                                    updateLayout()
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                                    Toast.makeText(this, resources.getString(R.string.login_failed_label), Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                    else -> {
                        // Shouldn't happen.
                        Log.d(TAG, "No ID token or password!")
                    }
                }
            } catch (e: ApiException) {
                when (e.statusCode) {
                    CommonStatusCodes.CANCELED -> {
                        Log.d(TAG, "One-tap dialog was closed.")
                        // Don't re-prompt the user.
                        // showOneTapUI = false
                    }
                    CommonStatusCodes.NETWORK_ERROR -> {
                        Log.d(TAG, "One-tap encountered a network error.")
                        // Try again or just ignore.
                    }
                    else -> {
                        Log.d(TAG, "Couldn't get credential from result." +
                                " (${e.localizedMessage})")
                    }
                }
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
        //hide other layouts
        val settingsLayout = findViewById<ConstraintLayout>(R.id.layout_settings)
        settingsLayout.visibility = View.GONE

        val enable_service_layout = findViewById<ConstraintLayout>(R.id.layout_enable_service)
        enable_service_layout.visibility = View.GONE

        val tutorial_layout = findViewById<ConstraintLayout>(R.id.layout_tutorial)
        tutorial_layout.visibility = View.GONE

        val login_layout = findViewById<ConstraintLayout>(R.id.layout_login)
        login_layout.visibility = View.GONE


        //display layout permissions
        val give_permission_layout = findViewById<ConstraintLayout>(R.id.layout_permissions)
        give_permission_layout.visibility = View.VISIBLE

        //add listener to btn
        val givePermissionBtn = findViewById<Button>(R.id.give_permissions_btn)
        givePermissionBtn.setOnClickListener { requestPermission() }

    }
    private fun showLayoutEnableService(){
        //hide other layouts
        val settingsLayout = findViewById<ConstraintLayout>(R.id.layout_settings)
        settingsLayout.visibility = View.GONE

        val tutorial_layout = findViewById<ConstraintLayout>(R.id.layout_tutorial)
        tutorial_layout.visibility = View.GONE

        val give_permission_layout = findViewById<ConstraintLayout>(R.id.layout_permissions)
        give_permission_layout.visibility = View.GONE

        val login_layout = findViewById<ConstraintLayout>(R.id.layout_login)
        login_layout.visibility = View.GONE

        //display enable service layout
        val enable_service_layout = findViewById<ConstraintLayout>(R.id.layout_enable_service)
        enable_service_layout.visibility = View.VISIBLE

        //add listener to btn in order to open accessibility menu
        val accessibilityButton = findViewById<AppCompatButton>(R.id.accessibility_button)
        accessibilityButton.visibility = View.VISIBLE
        //add click listener on button for going to accessibility menu
        accessibilityButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }
    private fun showLayoutTutorial() {
        //hide other layouts
        val settingsLayout = findViewById<ConstraintLayout>(R.id.layout_settings)
        settingsLayout.visibility = View.GONE

        val enable_service_layout = findViewById<ConstraintLayout>(R.id.layout_enable_service)
        enable_service_layout.visibility = View.GONE

        val give_permission_layout = findViewById<ConstraintLayout>(R.id.layout_permissions)
        give_permission_layout.visibility = View.GONE

        val login_layout = findViewById<ConstraintLayout>(R.id.layout_login)
        login_layout.visibility = View.GONE

        //display layout tutorial
        val tutorial_layout = findViewById<ConstraintLayout>(R.id.layout_tutorial)
        tutorial_layout.visibility = View.VISIBLE


        val btn = findViewById<Button>(R.id.tutorial_button)

        btn.setOnClickListener {
            alreadySeenTutorial = true
            showLayoutSettings()
        }


    }

    private fun showLayoutLogin() {
        //hide other layouts
        val settingsLayout = findViewById<ConstraintLayout>(R.id.layout_settings)
        settingsLayout.visibility = View.GONE

        val enable_service_layout = findViewById<ConstraintLayout>(R.id.layout_enable_service)
        enable_service_layout.visibility = View.GONE

        val give_permission_layout = findViewById<ConstraintLayout>(R.id.layout_permissions)
        give_permission_layout.visibility = View.GONE

        val tutorial_layout = findViewById<ConstraintLayout>(R.id.layout_tutorial)
        tutorial_layout.visibility = View.GONE

        //show layout login
        val login_layout = findViewById<ConstraintLayout>(R.id.layout_login)
        login_layout.visibility = View.VISIBLE

        //GOOGLE SIGN IN


        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(false)
            .build()

        //click listener for the button
        val signInButton = findViewById<com.google.android.gms.common.SignInButton>(R.id.sign_in_button)
        signInButton.setOnClickListener {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        startIntentSenderForResult(
                            result.pendingIntent.intentSender, REQ_ONE_TAP,
                            null, 0, 0, 0, null)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                    }
                }
                .addOnFailureListener(this) { e ->
                    // No saved credentials found.
                    // Do nothing and continue presenting the signed-out UI.
                    Log.d(TAG, "Unable to login ${e.localizedMessage}")
                    //showMessageLoginFailed(R.string.message_no_account)
                    Toast.makeText(this, resources.getString(R.string.no_google_account_found), Toast.LENGTH_LONG).show()
                }
        }

    }

    private fun showLayoutSettings(){
        //hide other layouts
        val give_permission_layout = findViewById<ConstraintLayout>(R.id.layout_permissions)
        give_permission_layout.visibility = View.GONE

        val enable_service_layout = findViewById<ConstraintLayout>(R.id.layout_enable_service)
        enable_service_layout.visibility = View.GONE

        val tutorial_layout = findViewById<ConstraintLayout>(R.id.layout_tutorial)
        tutorial_layout.visibility = View.GONE

        val login_layout = findViewById<ConstraintLayout>(R.id.layout_login)
        login_layout.visibility = View.GONE


        //display settings
        val settingsLayout = findViewById<ConstraintLayout>(R.id.layout_settings)
        settingsLayout.visibility = View.VISIBLE

        //display button for going back to tutorial
        val tutorialBtn = findViewById<AppCompatButton>(R.id.see_tutorial_again_button)
        tutorialBtn.visibility = View.VISIBLE
        tutorialBtn.setOnClickListener {
            //go back to tutorial and hide btn
            tutorialBtn.visibility = View.GONE
            showLayoutTutorial()
        }


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

        //add listener for button4 for closing app
        val btn = findViewById<Button>(R.id.button4)
        btn.setOnClickListener {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
        }

    }

    private fun updateLayout() {
        val currentUser = auth.getCurrentUser()
        if(currentUser == null){
            showLayoutLogin()
        }
        else {
            //check if app needs permissions (Android >10 and not already given)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Settings.canDrawOverlays(this)) {
                //app needs permissions
                showLayoutPermission()
            } else {
                //app has permissions
                //showLayoutSettings()
                if (isAccessibilityServiceEnabled(this, MyService::class.java)) {
                    if (alreadySeenTutorial) {
                        showLayoutSettings()
                    } else
                        showLayoutTutorial()
                } else {
                    showLayoutEnableService()
                }

            }
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

    fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService?>
    ): Boolean {
        val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val enabledServiceInfo: ServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName.equals(context.packageName) && enabledServiceInfo.name.equals(service.name) )
                return true
        }
        return false
    }
}