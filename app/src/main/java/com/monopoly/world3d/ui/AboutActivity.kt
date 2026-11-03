package com.monopoly.world3d.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import com.monopoly.world3d.R
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** درباره‌ی بازی */
class AboutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = ScrollView(this)
        root.setBackgroundColor(0xFF101828.toInt())
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 24))
        root.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val title = TextView(this)
        title.text = "دنیای روپولی"
        title.textSize = 28f
        title.typeface = Ui.persianFont(this)
        title.setTextColor(0xFFFFD4A017.toInt())
        title.gravity = android.view.Gravity.CENTER
        content.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "MONOPOLY WORLD 3D — نسخه‌ی ۱٫۰٫۰"
        subtitle.textSize = 13f
        subtitle.typeface = Ui.persianFont(this)
        subtitle.setTextColor(0xFFC8A96A.toInt())
        subtitle.gravity = android.view.Gravity.CENTER
        subtitle.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12))
        content.addView(subtitle)

        fun p(text: String) {
            val t = TextView(this)
            t.text = text
            t.textSize = 14f
            t.typeface = Ui.persianFont(this)
            t.setTextColor(0xFFF5EFE0.toInt())
            t.setLineSpacing(0f, 1.25f)
            t.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4))
            content.addView(t)
        }

        p("«دنیای روپولی» یک بازی خرید و فروش املاک به سبک کلاسیک روپولی با تم شهر تهران و گرافیک سه‌بعدی سبک برای موبایل است. می‌توانید به‌تنهایی در برابر حریفان هوشمند رایانه بازی کنید یا با دوستانتان روی یک گوشی رقابت کنید.")
        p("ویژگی‌ها:")
        p("• صفحه‌ی کامل ۴۰ خانه‌ای با خیابان‌های تهران\n• ۱ تا ۵ حریف رایانه با سه سطح دشواری و شش شخصیت متفاوت\n• بازی چندنفره‌ی محلی تا ۶ نفر روی یک گوشی\n• خانه و هتل، حراج، رهن، معامله، کارت شانس و اتفاقات\n• ده مهره‌ی سه‌بعدی و ده رنگ\n• ۲۸ دستاورد و آمار کامل\n• ذخیره‌ی خودکار و اسلات‌های دستی\n• کاملاً آفلاین، بدون نیاز به اینترنت")
        p("موسیقی و جلوه‌های صوتی به‌صورت رویه‌ای تولید شده‌اند. همه‌ی گرافیک‌های سه‌بعدی با OpenGL ES رندر می‌شوند.")
        p("مبالغ بازی بر حسب تومان و ارقام فارسی نمایش داده می‌شوند.")

        val back = Button(this)
        back.text = "بازگشت"
        back.typeface = Ui.persianFont(this)
        back.setTextColor(Color.WHITE)
        back.background = getDrawable(R.drawable.btn_action)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46))
        lp.topMargin = Ui.dp(this, 16)
        content.addView(back, lp)
        back.setOnClickListener { finish() }
        Ui.applyFont(root)
        setContentView(root)
    }
}
