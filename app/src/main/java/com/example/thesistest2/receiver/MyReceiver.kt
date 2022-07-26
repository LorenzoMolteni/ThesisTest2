package com.example.thesistest2.receiver


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.thesistest2.service.MyService

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val myIntent = Intent(context, MyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //TODO need to start activity instead of service
            //context.startForegroundService(myIntent)
        } else {
            //TODO need to start activity instead of service
            //context.startService(myIntent)
        }
        Log.d("MYRECEIVER", "started")

    }
}