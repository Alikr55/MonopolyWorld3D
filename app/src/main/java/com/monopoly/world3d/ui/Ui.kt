package com.monopoly.world3d.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.monopoly.world3d.engine.PersianNumbers

/** ابزارهای مشترک رابط کاربری فارسی */
object Ui {

    private var persianFont: Typeface? = null

    fun persianFont(context: Context): Typeface {
        if (persianFont == null) {
            persianFont = try {
                Typeface.createFromAsset(context.assets, "fonts/Vazirmatn-Regular.ttf")
            } catch (e: Exception) {
                try {
                    Typeface.createFromAsset(context.assets, "fonts/Vazirmatn-Bold.ttf")
                } catch (e2: Exception) {
                    Typeface.DEFAULT_BOLD
                }
            }
        }
        return persianFont!!
    }

    /** اعمال فونت فارسی روی همه‌ی TextView/Buttonهای یک درخت ویو */
    fun applyFont(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) applyFont(view.getChildAt(i))
        } else if (view is TextView) {
            view.typeface = persianFont(view.context)
        }
    }

    fun money(amount: Long): String = PersianNumbers.money(amount)

    fun toast(context: Context, message: String) {
        val t = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        t.view?.let { v ->
            val tv = v.findViewById<TextView>(android.R.id.message)
            tv?.typeface = persianFont(context)
            tv?.gravity = Gravity.CENTER
        }
        t.show()
    }

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    /** ساخت دکمه‌ی استاندارد منو */
    fun menuButton(context: Context, text: String, onClick: (View) -> Unit): Button {
        val b = Button(context)
        b.text = text
        b.typeface = persianFont(context)
        b.textSize = 17f
        b.setTextColor(Color.WHITE)
        b.background = context.resources.getDrawable(
            context.resources.getIdentifier("btn_menu", "drawable", context.packageName),
            context.theme
        )
        b.setPadding(dp(context, 28), dp(context, 10), dp(context, 28), dp(context, 10))
        b.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        b.setOnClickListener(onClick)
        return b
    }
}
