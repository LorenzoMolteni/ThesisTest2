package com.example.thesistest2.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.example.thesistest2.R
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*
import kotlin.concurrent.schedule

@SuppressLint("StaticFieldLeak")
object WidgetNudge2 {
    private val TAG = "MYWIDGETNUDGE2"

    private var frameLayout: FrameLayout? = null

    fun fillWidget(context: Context, isScroll: Boolean, type: Int) {
        if(frameLayout == null){
            Log.d(TAG, "Creating widget from scratch")
            val mWindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
            frameLayout = FrameLayout(context)
            val inflater = LayoutInflater.from(context)
            inflater.inflate(R.layout.widget_nudge_2, frameLayout)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, //can use also TYPE_APPLICATION_OVERLAY
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            //Specify the view position -> starting from bottom right, move up of y and left of x
            params.gravity =
                Gravity.BOTTOM or Gravity.RIGHT
            params.x = context.resources.getDimension(R.dimen.widget_margin_end).toInt()
            params.y = context.resources.getDimension(R.dimen.widget_margin_bottom).toInt()

            setImageResource(context, isScroll, type)

            mWindowManager.addView(frameLayout, params)
        }
        else {
            frameLayout?.visibility = View.VISIBLE
            setImageResource(context, isScroll, type)
        }
    }

    private fun setImageResource(context: Context, isScroll: Boolean, type: Int){
        //change image depending on isScroll and type
        val image = frameLayout!!.findViewById<CircleImageView>(R.id.widget_image)
        if(isScroll){
            //type = 0 -> normal, 1 -> fast, 2 -> really fast
            when(type){
                0 -> {
                    image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_scrolling_low, context.theme))
                }
                1 -> {
                    image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_scrolling_mid, context.theme))
                }
                else -> {
                    image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_scrolling_high, context.theme))
                }
            }
        }
        else {
            //type = 0 -> normal, 1 -> frequent, 2 -> really frequent
            when(type){
                0 -> image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_pulling_low, context.theme))
                1 -> image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_pulling_mid, context.theme))
                else -> image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_pulling_high, context.theme))
            }
        }

    }

    fun showWidget() {

    }

    fun hideWidget() {
        frameLayout?.visibility = View.GONE
        Log.d(TAG, "Widget hidden")
    }
}