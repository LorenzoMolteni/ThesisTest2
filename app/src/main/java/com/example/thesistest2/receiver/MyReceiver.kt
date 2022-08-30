package com.example.thesistest2.receiver


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.thesistest2.activity.MainActivity

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MYRECEIVER", "started")
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
            val myIntent = Intent(context, MainActivity::class.java)
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            //on my xiaomi, this works only if the user has granted the autostart permission
            context.startActivity(myIntent)
            //context.startForegroundService(myIntent)

        }

    }
}