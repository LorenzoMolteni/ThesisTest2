package com.example.thesistest2.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.example.thesistest2.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText


class TextualNudge1 : Activity() {
    private val TAG = "MY_TEXTUAL_NUDGE_1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.textual_nudge_1)

        //setting nudge in view
        val nudge = intent.getStringExtra("nudge")
        findViewById<TextView>(R.id.textualNudge).text = nudge


    }

    override fun onResume() {
        super.onResume()

        //restore preferences
        val sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val switchOpenActive = sharedPref.getBoolean("switchOpenActive", false)
        val switchBackActive = sharedPref.getBoolean("switchBackActive", false)

        val backBtn = findViewById<Button>(R.id.back_btn)
        val openBtn = findViewById<Button>(R.id.open_btn)

        //set text saved in preferences inside the button
        if(switchOpenActive)
            openBtn.text = sharedPref.getString("openText", "")
        if(switchBackActive)
            backBtn.text = sharedPref.getString("backText", "")

        openBtn.setOnClickListener { handleOpenBtn() }
        backBtn.setOnClickListener { handleBackButton() }
    }

    private fun handleOpenBtn() {
        finish()
    }

    private fun handleBackButton() {
        //go to launcher
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }
}