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

/** قوانین کامل بازی به زبان فارسی */
class RulesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = ScrollView(this)
        root.setBackgroundColor(0xFF101828.toInt())
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 24))
        root.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val title = TextView(this)
        title.text = "قوانین بازی دنیای روپولی"
        title.textSize = 22f
        title.typeface = Ui.persianFont(this)
        title.setTextColor(0xFFFFD4A017.toInt())
        title.gravity = android.view.Gravity.CENTER
        content.addView(title)

        fun h(text: String) {
            val t = TextView(this)
            t.text = text
            t.textSize = 17f
            t.typeface = Ui.persianFont(this)
            t.setTextColor(0xFFFFD4A017.toInt())
            t.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 4))
            content.addView(t)
        }
        fun p(text: String) {
            val t = TextView(this)
            t.text = text
            t.textSize = 14f
            t.typeface = Ui.persianFont(this)
            t.setTextColor(0xFFF5EFE0.toInt())
            t.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2))
            t.setLineSpacing(0f, 1.2f)
            content.addView(t)
        }

        h("هدف بازی")
        p("بازیکنان به نوبت تاس می‌ریزند، در صفحه حرکت می‌کنند و املاک می‌خرند. هدف نهایی این است که با دریافت اجاره و ساخت خانه و هتل، ثروت خود را افزایش دهید و حریفان را ورشکست کنید. بازیکنی که در پایان تنها بازیکن باقی‌مانده باشد برنده است.")

        h("شروع بازی")
        p("هر بازیکن با ۱٬۵۰۰٬۰۰۰ تومان از خانه‌ی «شروع» بازی را آغاز می‌کند. همه به نوبت دو تاس می‌ریزند و به همان تعداد قدم جلو می‌روند. با گذر از خانه‌ی «شروع» یا فرود آمدن روی آن، ۲۰۰٬۰۰۰ تومان حقوق می‌گیرید.")

        h("خرید ملک و حراج")
        p("اگر روی ملک بدون مالک فرود بیایید، می‌توانید آن را با قیمت اعلام‌شده بخرید. اگر نخرید، بانک ملک را به حراج می‌گذارد؛ همه‌ی بازیکنان (حتی شما) می‌توانند پیشنهاد بدهند. بالاترین پیشنهاددهنده مالک ملک می‌شود. اگر کسی پیشنهاد ندهد، ملک نزد بانک می‌ماند.")

        h("اجاره")
        p("اگر روی ملک دیگری فرود بیایید، اجاره به مالک می‌پردازید. اجاره‌ی خیابان‌های بدون خانه در حالتی که مالک، کل گروه رنگی را داشته باشد دو برابر می‌شود. با هر خانه اجاره به‌شدت افزایش می‌یابد و اجاره‌ی هتل بسیار سنگین است. اجاره‌ی ایستگاه‌های حمل‌ونقل به تعداد ایستگاه‌های مالک بستگی دارد و اجاره‌ی شرکت خدمات چهار برابر تاس (ده برابر با هر دو شرکت) است. روی املاک رهنی اجاره تعلق نمی‌گیرد.")

        h("ساخت خانه و هتل")
        p("وقتی همه‌ی املاک یک گروه رنگی را مالک باشید و هیچ‌کدام رهن نباشند، می‌توانید بسازید. ساخت باید یکنواخت باشد: تفاوت تعداد خانه‌های ملک‌های یک گروه نباید بیش از یک باشد. با چهار خانه می‌توانید آن را به هتل ارتقا دهید. خانه و هتل را می‌توانید با نصف قیمت به بانک بفروشید؛ فروش هم باید یکنواخت باشد. تعداد خانه و هتل بانک محدود است (۳۲ خانه و ۱۲ هتل).")

        h("رهن")
        p("می‌توانید ملک بدون ساختمان را به بانک رهن بگذارید و به اندازه‌ی ارزش رهن پول بگیرید. تا زمانی که رهن است اجاره نمی‌گیرید و نمی‌توانید در آن بسازید. آزادسازی رهن هزینه‌اش برابر ارزش رهن به‌علاوه‌ی ۱۰٪ بهره است.")

        h("کارت شانس و اتفاقات عمومی")
        p("روی این خانه‌ها کارت می‌کشید: پول بگیرید، جریمه شوید، جابه‌جا شوید، به زندان بروید یا کارت خروج از زندان بگیرید. کارت خروج از زندان نگه‌داشتنی است و در معامله هم قابل داد‌وستد است.")

        h("زندان")
        p("با خانه‌ی «رفتن به زندان»، کارت زندان یا سه تاس جفت پیاپی به زندان می‌افتید. برای خروج: تاس جفت بیاورید (سه نوبت فرصت دارید)، ۵۰٬۰۰۰ تومان جریمه بپردازید یا کارت خروج از زندان استفاده کنید. در زندان می‌توانید اجاره بگیرید، بسازید و معامله کنید.")

        h("ورشکستگی و برنده")
        p("اگر نتوانید بدهی خود را با پول نقد بپردازید، باید ساختمان بفروشید یا ملک رهن بگذارید. اگر حتی با فروش همه‌ی دارایی‌ها هم نتوانید بدهی را بپردازید، ورشکست می‌شوید: املاک به طلبکار منتقل می‌شود (اگر طلبکار بانک باشد، املاک به حراج گذاشته می‌شوند). بازیکنی که در پایان تنها بازیکن باقی بماند برنده است.")

        h("قوانین تکمیلی")
        p("• سه تاس جفت پیاپی یعنی رفتن به زندان.\n• تاس جفت به شما اجازه‌ی یک نوبت اضافه می‌دهد.\n• جریمه‌ها و مالیات‌ها به صندوق «پارکینگ آزاد» می‌رود و بازیکنی که روی آن فرود بیاید صندوق را برمی‌دارد (قانون قابل تنظیم).\n• مالیات درآمد ۲۰۰٬۰۰۰ و مالیات تجملات ۱۰۰٬۰۰۰ تومان است.\n• خرید فقط با پول نقد کافی ممکن است؛ پول منفی وجود ندارد.")

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
