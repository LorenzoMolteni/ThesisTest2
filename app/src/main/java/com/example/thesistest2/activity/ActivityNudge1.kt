package com.example.thesistest2.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.example.thesistest2.R
import com.example.thesistest2.db.Db
import pl.droidsonroids.gif.GifImageView


class ActivityNudge1 : Activity() {
    private val TAG = "MY_TEXTUAL_NUDGE_1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.textual_nudge_1)

        //getting openedAppLabel from intent
        val openedAppLabel = intent.getStringExtra("openedAppLabel")
        findViewById<TextView>(R.id.openedAppLabel).text = openedAppLabel

        //getting nudgeType from intent
        val nudgeTYpe = intent.getIntExtra("nudgeType", 0)
        //set gif depending on nudge type
        val gif = findViewById<GifImageView>(R.id.gif)
        when(nudgeTYpe){
            0 -> gif.setImageResource(R.drawable.infinite_scrolling_gif)
            else -> gif.setImageResource(R.drawable.pull_to_refresh_gif)
        }

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
        val openText = sharedPref.getString("openText", "")
        val backText = sharedPref.getString("backText", "")

        val backBtn = findViewById<Button>(R.id.back_btn)
        val openBtn = findViewById<Button>(R.id.open_btn)

        //set text saved in preferences inside the button
        if(switchOpenActive && !(openText.isNullOrBlank()) )
            openBtn.text = openText
        if(switchBackActive && !(backText.isNullOrBlank()) )
            backBtn.text = backText

        openBtn.setOnClickListener { handleOpenBtn() }
        backBtn.setOnClickListener { handleBackButton() }
    }

    private fun handleOpenBtn() {
        //get shared preferences of service
        val sharedPref = getSharedPreferences(getString(R.string.system_preference_file_key), Context.MODE_PRIVATE)
        var clicksOnOpen = sharedPref.getInt("clicksOnOpen", 0)
        clicksOnOpen++
        //write to shared preferences
        with (sharedPref.edit()) {
            putInt("clicksOnOpen", clicksOnOpen)
            apply()
        }

        //also increment in db
        Db.incrementClicksOnOpen()
        finish()
    }

    private fun handleBackButton() {
        //get shared preferences of service
        val sharedPref = getSharedPreferences(getString(R.string.system_preference_file_key), Context.MODE_PRIVATE)
        var clicksOnBack = sharedPref.getInt("clicksOnBack", 0)
        clicksOnBack++
        //write to shared preferences
        with (sharedPref.edit()) {
            putInt("clicksOnBack", clicksOnBack)
            apply()
        }
        //also increment in db
        Db.incrementClicksOnBack()

        //go to launcher
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }
}