package com.monopoly.world3d.engine

/**
 * کارت‌های بازی — دو دسته‌ی «کارت شانس» و «کارت اتفاقات عمومی».
 * هر کارت جلوه‌ی واقعی در موتور بازی دارد.
 */

sealed class CardEffect {
    /** دریافت پول از بانک */
    class Gain(val amount: Long) : CardEffect()
    /** پرداخت جریمه به بانک/صندوق */
    class PayFine(val amount: Long) : CardEffect()
    /** پرداخت به هر بازیکن */
    class PayEachPlayer(val amount: Long) : CardEffect()
    /** دریافت از هر بازیکن */
    class CollectFromEach(val amount: Long) : CardEffect()
    /** انتقال به خانه‌ی مشخص */
    class MoveTo(val spaceId: Int, val collectGo: Boolean = true) : CardEffect()
    /** نزدیک‌ترین حمل‌ونقل؛ اگر مالک داشته باشد اجاره دو برابر */
    class MoveToNearestRailroad : CardEffect()
    /** نزدیک‌ترین خدمات؛ پرداخت ۱۰ برابر تاس */
    class MoveToNearestUtility : CardEffect()
    /** عقب رفتن */
    class MoveBack(val steps: Int) : CardEffect()
    /** جلو رفتن */
    class MoveForward(val steps: Int) : CardEffect()
    /** رفتن به زندان */
    class GoToJail : CardEffect()
    /** کارت خروج از زندان (اگر زندانی است بلافاصله استفاده، وگرنه نگه می‌دارد) */
    class JailFree : CardEffect()
    /** تعمیرات ساختمان */
    class BuildingRepairs(val perHouse: Long, val perHotel: Long) : CardEffect()
}

class Card(
    val id: Int,
    val deck: Int,          // 0 = شانس، 1 = اتفاقات عمومی
    val title: String,
    val description: String,
    val effect: CardEffect
)

object Cards {

    const val DECK_CHANCE = 0
    const val DECK_CHEST = 1

    private val chance: List<Card> = listOf(
        Card(100, DECK_CHANCE, "رفتن به شروع", "به خانه‌ی «شروع» بروید و حقوق دریافت کنید.", CardEffect.MoveTo(0)),
        Card(101, DECK_CHANCE, "سفر به اقدسیه", "به «خیابان اقدسیه» بروید.", CardEffect.MoveTo(39)),
        Card(102, DECK_CHANCE, "سفر به تجریش", "به «خیابان تجریش» بروید.", CardEffect.MoveTo(37)),
        Card(103, DECK_CHANCE, "قطار بعدی", "به نزدیک‌ترین ایستگاه حمل‌ونقل بروید و اگر مالک دارد، دو برابر اجاره بپردازید.", CardEffect.MoveToNearestRailroad()),
        Card(104, DECK_CHANCE, "قطار سریع‌السیر", "به نزدیک‌ترین ایستگاه حمل‌ونقل بروید و اگر مالک دارد، دو برابر اجاره بپردازید.", CardEffect.MoveToNearestRailroad()),
        Card(105, DECK_CHANCE, "خدمات شهری", "به نزدیک‌ترین شرکت خدمات بروید؛ اگر مالک دارد، ۱۰ برابر تاس بپردازید.", CardEffect.MoveToNearestUtility()),
        Card(106, DECK_CHANCE, "خدمات مورد نیاز", "به نزدیک‌ترین شرکت خدمات بروید؛ اگر مالک دارد، ۱۰ برابر تاس بپردازید.", CardEffect.MoveToNearestUtility()),
        Card(107, DECK_CHANCE, "برگشت به عقب", "سه قدم به عقب برگردید.", CardEffect.MoveBack(3)),
        Card(108, DECK_CHANCE, "جریمه‌ی رانندگی", "برای سرعت غیرمجاز ${PersianNumbers.money(15_000)} جریمه شوید.", CardEffect.PayFine(15_000)),
        Card(109, DECK_CHANCE, "بلیت یک‌طرفه", "به «ترمینال مسافربری غرب» بروید.", CardEffect.MoveTo(15)),
        Card(110, DECK_CHANCE, "پرواز داخلی", "به «فرودگاه مهرآباد» بروید.", CardEffect.MoveTo(25)),
        Card(111, DECK_CHANCE, "وام بازرگانی", "وام بازرگانی گرفته‌اید: ${PersianNumbers.money(45_000)} بگیرید.", CardEffect.Gain(45_000)),
        Card(112, DECK_CHANCE, "جایزه‌ی زیبایی", "برنده‌ی جایزه‌ی زیبایی شدید: ${PersianNumbers.money(10_000)} بگیرید.", CardEffect.Gain(10_000)),
        Card(113, DECK_CHANCE, "بازگشت مالیات", "بازگشت مالیات دریافت کردید: ${PersianNumbers.money(20_000)}.", CardEffect.Gain(20_000)),
        Card(114, DECK_CHANCE, "ارث", "ارثیه به شما رسید: ${PersianNumbers.money(100_000)} بگیرید.", CardEffect.Gain(100_000)),
        Card(115, DECK_CHANCE, "مسابقه‌ی شعر", "برنده‌ی مسابقه‌ی شعر شدید: ${PersianNumbers.money(12_000)}.", CardEffect.Gain(12_000)),
        Card(116, DECK_CHANCE, "سود سهام", "سود سهام شرکت دریافت کردید: ${PersianNumbers.money(50_000)}.", CardEffect.Gain(50_000)),
        Card(117, DECK_CHANCE, "به زندان بروید", "مستقیماً به زندان بروید و از حقوق شروع صرف‌نظر کنید.", CardEffect.GoToJail()),
        Card(118, DECK_CHANCE, "کارت آزادی", "از زندان آزاد شوید. این کارت را تا زمان نیاز نگه دارید.", CardEffect.JailFree()),
        Card(119, DECK_CHANCE, "پارک کنید", "به «پارکینگ آزاد» بروید.", CardEffect.MoveTo(20)),
        Card(120, DECK_CHANCE, "سفر به نیاوران", "به «خیابان نیاوران» بروید.", CardEffect.MoveTo(31)),
        Card(121, DECK_CHANCE, "تعمیرات جزئی", "برای هر خانه ${PersianNumbers.money(25_000)} و برای هر هتل ${PersianNumbers.money(100_000)} بپردازید.", CardEffect.BuildingRepairs(25_000, 100_000)),
        Card(122, DECK_CHANCE, "جریمه‌ی مستندی", "برای مستندات ناقص ${PersianNumbers.money(30_000)} بپردازید.", CardEffect.PayFine(30_000)),
        Card(123, DECK_CHANCE, "قرعه‌کشی بانکی", "برنده‌ی قرعه‌کشی بانک شدید: ${PersianNumbers.money(50_000)} بگیرید.", CardEffect.Gain(50_000)),
        Card(124, DECK_CHANCE, "کلاس ویژه", "درآمد تدریس کلاس ویژه: ${PersianNumbers.money(150_000)}.", CardEffect.Gain(150_000)),
        Card(125, DECK_CHANCE, "دو قدم جلو", "دو قدم به جلو حرکت کنید.", CardEffect.MoveForward(2)),
        Card(126, DECK_CHANCE, "شهریه‌ی فرزندان", "شهریه‌ی فرزندانتان را از هر بازیکن ${PersianNumbers.money(25_000)} بگیرید.", CardEffect.CollectFromEach(25_000)),
        Card(127, DECK_CHANCE, "عیدی", "عیدی از هر بازیکن ${PersianNumbers.money(10_000)} بگیرید.", CardEffect.CollectFromEach(10_000)),
        Card(128, DECK_CHANCE, "هزینه‌ی بیمارستان", "هزینه‌ی بیمارستان: ${PersianNumbers.money(100_000)} بپردازید.", CardEffect.PayFine(100_000)),
        Card(129, DECK_CHANCE, "جریمه‌ی دیرکرد", "جریمه‌ی دیرکرد پرداخت: ${PersianNumbers.money(20_000)}.", CardEffect.PayFine(20_000)),
        Card(130, DECK_CHANCE, "ایستگاه مرکزی", "به «ایستگاه راه‌آهن تهران» بروید.", CardEffect.MoveTo(5)),
        Card(131, DECK_CHANCE, "تعمیرات خیابان", "تعمیرات خیابان: برای هر خانه ${PersianNumbers.money(40_000)} و برای هر هتل ${PersianNumbers.money(115_000)} بپردازید.", CardEffect.BuildingRepairs(40_000, 115_000)),
        Card(132, DECK_CHANCE, "فروش خودرو", "خودروی قدیمی فروختید: ${PersianNumbers.money(35_000)} بگیرید.", CardEffect.Gain(35_000)),
        Card(133, DECK_CHANCE, "مهمانی خانوادگی", "هزینه‌ی مهمانی: به هر بازیکن ${PersianNumbers.money(20_000)} بدهید.", CardEffect.PayEachPlayer(20_000)),
        Card(134, DECK_CHANCE, "اجاره‌ی انباری", "درآمد اجاره‌ی انباری: ${PersianNumbers.money(18_000)}.", CardEffect.Gain(18_000)),
        Card(135, DECK_CHANCE, "جشن تولد", "هدیه‌ی جشن تولد: ${PersianNumbers.money(25_000)} بگیرید.", CardEffect.Gain(25_000)),
        Card(136, DECK_CHANCE, "جایزه‌ی ورزشی", "برنده‌ی جایزه‌ی ورزشی شدید: ${PersianNumbers.money(80_000)}.", CardEffect.Gain(80_000))
    )

    private val chest: List<Card> = listOf(
        Card(200, DECK_CHEST, "خطای بانکی", "خطای بانک به نفع شما بود: ${PersianNumbers.money(200_000)} بگیرید.", CardEffect.Gain(200_000)),
        Card(201, DECK_CHEST, "هزینه‌ی پزشکی", "هزینه‌ی پزشکی: ${PersianNumbers.money(50_000)} بپردازید.", CardEffect.PayFine(50_000)),
        Card(202, DECK_CHEST, "فروش سهام", "سهام بورس فروختید: ${PersianNumbers.money(50_000)} بگیرید.", CardEffect.Gain(50_000)),
        Card(203, DECK_CHEST, "رفتن به شروع", "به خانه‌ی «شروع» بروید و حقوق دریافت کنید.", CardEffect.MoveTo(0)),
        Card(204, DECK_CHEST, "به زندان بروید", "مستقیماً به زندان بروید و از حقوق شروع صرف‌نظر کنید.", CardEffect.GoToJail()),
        Card(205, DECK_CHEST, "کارت آزادی", "از زندان آزاد شوید. این کارت را تا زمان نیاز نگه دارید.", CardEffect.JailFree()),
        Card(206, DECK_CHEST, "بیمه‌ی عمر", "بیمه‌ی عمر بالغ شد: ${PersianNumbers.money(100_000)} بگیرید.", CardEffect.Gain(100_000)),
        Card(207, DECK_CHEST, "هزینه‌ی بیمارستان", "هزینه‌ی بیمارستان: ${PersianNumbers.money(100_000)} بپردازید.", CardEffect.PayFine(100_000)),
        Card(208, DECK_CHEST, "دندانپزشکی", "هزینه‌ی درمان دندان: ${PersianNumbers.money(50_000)} بپردازید.", CardEffect.PayFine(50_000)),
        Card(209, DECK_CHEST, "بازگشت مالیات", "بازگشت مالیات بر ارزش افزوده: ${PersianNumbers.money(20_000)}.", CardEffect.Gain(20_000)),
        Card(210, DECK_CHEST, "ارثیه", "ارثیه‌ی پدری به شما رسید: ${PersianNumbers.money(100_000)}.", CardEffect.Gain(100_000)),
        Card(211, DECK_CHEST, "عیدی پایان سال", "عیدی پایان سال از هر بازیکن ${PersianNumbers.money(10_000)} بگیرید.", CardEffect.CollectFromEach(10_000)),
        Card(212, DECK_CHEST, "فروش کتاب", "کتاب نوشتید و فروختید: ${PersianNumbers.money(25_000)}.", CardEffect.Gain(25_000)),
        Card(213, DECK_CHEST, "بهترین باغ", "جایزه‌ی بهترین باغ محله: ${PersianNumbers.money(40_000)}.", CardEffect.Gain(40_000)),
        Card(214, DECK_CHEST, "جریمه‌ی آموزشی", "جریمه‌ی کلاس‌های ناتمام: ${PersianNumbers.money(15_000)} بپردازید.", CardEffect.PayFine(15_000)),
        Card(215, DECK_CHEST, "همایش علمی", "درآمد همایش علمی: ${PersianNumbers.money(35_000)} بگیرید.", CardEffect.Gain(35_000)),
        Card(216, DECK_CHEST, "استراحت", "به «پارکینگ آزاد» بروید و استراحت کنید.", CardEffect.MoveTo(20)),
        Card(217, DECK_CHEST, "تعمیرات ساختمان", "تعمیر کامل ساختمان‌ها: هر خانه ${PersianNumbers.money(40_000)} و هر هتل ${PersianNumbers.money(115_000)} بپردازید.", CardEffect.BuildingRepairs(40_000, 115_000)),
        Card(218, DECK_CHEST, "عوارض شهرداری", "عوارض شهرداری از هر بازیکن ${PersianNumbers.money(30_000)} بگیرید.", CardEffect.CollectFromEach(30_000)),
        Card(219, DECK_CHEST, "کمک به خیریه", "به خیریه کمک کنید: به هر بازیکن ${PersianNumbers.money(15_000)} بدهید.", CardEffect.PayEachPlayer(15_000)),
        Card(220, DECK_CHEST, "وام دانشجویی", "وام دانشجویی گرفتید: ${PersianNumbers.money(60_000)} بگیرید.", CardEffect.Gain(60_000)),
        Card(221, DECK_CHEST, "قرعه‌کشی خودرو", "برنده‌ی قرعه‌کشی خودرو شدید: ${PersianNumbers.money(100_000)} بگیرید.", CardEffect.Gain(100_000)),
        Card(222, DECK_CHEST, "نظافت شهر", "سهمیه‌ی نظافت شهر: ${PersianNumbers.money(30_000)} بپردازید.", CardEffect.PayFine(30_000)),
        Card(223, DECK_CHEST, "اوراق قرضه", "سود اوراق قرضه: ${PersianNumbers.money(75_000)} بگیرید.", CardEffect.Gain(75_000)),
        Card(224, DECK_CHEST, "ایستگاه تهران", "به «ایستگاه راه‌آهن تهران» بروید و حقوق دریافت کنید.", CardEffect.MoveTo(5)),
        Card(225, DECK_CHEST, "جریمه‌ی بسته‌بندی", "جریمه‌ی بسته‌بندی غیراستاندارد: ${PersianNumbers.money(25_000)} بپردازید.", CardEffect.PayFine(25_000)),
        Card(226, DECK_CHEST, "پاداش بازنشستگی", "پاداش بازنشستگی: ${PersianNumbers.money(90_000)} بگیرید.", CardEffect.Gain(90_000)),
        Card(227, DECK_CHEST, "هزینه‌ی مجالس", "هزینه‌ی مجالس: به هر بازیکن ${PersianNumbers.money(20_000)} بدهید.", CardEffect.PayEachPlayer(20_000)),
        Card(228, DECK_CHEST, "وسایل اضافی", "وسایل اضافی فروختید: ${PersianNumbers.money(12_000)} بگیرید.", CardEffect.Gain(12_000)),
        Card(229, DECK_CHEST, "کارت هدیه", "کارت هدیه: از هر بازیکن ${PersianNumbers.money(5_000)} بگیرید.", CardEffect.CollectFromEach(5_000)),
        Card(230, DECK_CHEST, "سه قدم عقب", "سه قدم به عقب برگردید.", CardEffect.MoveBack(3)),
        Card(231, DECK_CHEST, "جریمه‌ی پارک", "جریمه‌ی پارک غیرمجاز: ${PersianNumbers.money(10_000)} بپردازید.", CardEffect.PayFine(10_000)),
        Card(232, DECK_CHEST, "پروژه‌ی جانبی", "درآمد پروژه‌ی جانبی: ${PersianNumbers.money(35_000)} بگیرید.", CardEffect.Gain(35_000)),
        Card(233, DECK_CHEST, "قبض آب و برق", "قبض آب و برق: ${PersianNumbers.money(40_000)} بپردازید.", CardEffect.PayFine(40_000))
    )

    fun chanceIds(): List<Int> = chance.map { it.id }
    fun chestIds(): List<Int> = chest.map { it.id }
    fun byId(id: Int): Card = (chance + chest).first { it.id == id }

    fun deckName(deck: Int): String =
        if (deck == DECK_CHANCE) "کارت شانس" else "کارت اتفاقات عمومی"
}
