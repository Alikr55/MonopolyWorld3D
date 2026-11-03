package com.monopoly.world3d.engine

/**
 * صفحه‌ی بازی: ۴۰ خانه با تم شهر تهران.
 * مبالغ بر پایه‌ی اقتصاد کلاسیک روپولی و بر حسب تومان هستند (مقادیر کلاسیک × ۱٬۰۰۰).
 */
object Board {

    const val BOARD_SIZE = 40
    const val GO_POS = 0
    const val JAIL_POS = 10
    const val FREE_PARKING_POS = 20
    const val GO_TO_JAIL_POS = 30

    /** قیمت پایه حمل‌ونقل‌ها (اجاره بر اساس تعداد مالکیت: ۲۵/۵۰/۱۰۰/۲۰۰ هزار تومان) */
    const val RAILROAD_PRICE = 200_000L
    val RAILROAD_RENTS = listOf(25_000L, 50_000L, 100_000L, 200_000L)

    /** قیمت خدمات (اجاره = ۴× تاس با یکی، ۱۰× تاس با هر دو) */
    const val UTILITY_PRICE = 150_000L

    fun createSpaces(): List<Space> = listOf(
        // ۰ تا ۱۰ — ضلع جنوبی
        Space(0, SpaceType.GO, "شروع", null),
        street(1, "خیابان ری", Group.BROWN, 60, listOf(2, 10, 30, 90, 160, 250), 50),
        Space(2, SpaceType.CHEST, "کارت اتفاقات عمومی", null),
        street(3, "خیابان طالقانی", Group.BROWN, 60, listOf(4, 20, 60, 180, 320, 450), 50),
        Space(4, SpaceType.TAX, "مالیات درآمد", null, taxAmount = 200_000),
        Space(5, SpaceType.RAILROAD, "ایستگاه راه‌آهن تهران", Group.RAILROAD, RAILROAD_PRICE),
        street(6, "خیابان انقلاب", Group.LIGHT_BLUE, 100, listOf(6, 30, 90, 270, 400, 550), 50),
        Space(7, SpaceType.CHANCE, "کارت شانس", null),
        street(8, "خیابان آزادی", Group.LIGHT_BLUE, 100, listOf(6, 30, 90, 270, 400, 550), 50),
        street(9, "خیابان کارگر", Group.LIGHT_BLUE, 120, listOf(8, 40, 100, 300, 450, 600), 50),
        Space(10, SpaceType.JAIL, "زندان (فقط بازدید)", null),
        // ۱۱ تا ۱۹ — ضلع غربی
        street(11, "خیابان ولیعصر", Group.PINK, 140, listOf(10, 50, 150, 450, 625, 750), 100),
        Space(12, SpaceType.UTILITY, "شرکت برق", Group.UTILITY, UTILITY_PRICE),
        street(13, "خیابان کریم‌خان", Group.PINK, 140, listOf(10, 50, 150, 450, 625, 750), 100),
        street(14, "خیابان شریعتی", Group.PINK, 160, listOf(12, 60, 180, 500, 700, 900), 100),
        Space(15, SpaceType.RAILROAD, "ترمینال مسافربری غرب", Group.RAILROAD, RAILROAD_PRICE),
        street(16, "خیابان سعدی", Group.ORANGE, 180, listOf(14, 70, 200, 550, 750, 950), 100),
        Space(17, SpaceType.CHEST, "کارت اتفاقات عمومی", null),
        street(18, "خیابان فردوسی", Group.ORANGE, 180, listOf(14, 70, 200, 550, 750, 950), 100),
        street(19, "خیابان جمهوری", Group.ORANGE, 200, listOf(16, 80, 220, 600, 800, 1000), 100),
        // ۲۰ — گوشه
        Space(20, SpaceType.FREE_PARKING, "پارکینگ آزاد", null),
        // ۲۱ تا ۲۹ — ضلع شمالی
        street(21, "خیابان دولت", Group.RED, 220, listOf(18, 90, 250, 700, 875, 1050), 150),
        Space(22, SpaceType.CHANCE, "کارت شانس", null),
        street(23, "خیابان پاسداران", Group.RED, 220, listOf(18, 90, 250, 700, 875, 1050), 150),
        street(24, "خیابان فرشته", Group.RED, 240, listOf(20, 100, 300, 750, 925, 1100), 150),
        Space(25, SpaceType.RAILROAD, "فرودگاه مهرآباد", Group.RAILROAD, RAILROAD_PRICE),
        street(26, "بلوار فردوس", Group.YELLOW, 260, listOf(22, 110, 330, 800, 975, 1150), 150),
        street(27, "بلوار کشاورز", Group.YELLOW, 260, listOf(22, 110, 330, 800, 975, 1150), 150),
        Space(28, SpaceType.UTILITY, "شرکت آب و فاضلاب", Group.UTILITY, UTILITY_PRICE),
        street(29, "شهرک غرب", Group.YELLOW, 280, listOf(24, 120, 360, 850, 1025, 1200), 150),
        // ۳۰ — گوشه
        Space(30, SpaceType.GO_TO_JAIL, "رفتن به زندان", null),
        // ۳۱ تا ۳۹ — ضلع شرقی
        street(31, "خیابان نیاوران", Group.GREEN, 300, listOf(26, 130, 450, 900, 1100, 1275), 200),
        street(32, "خیابان ظفر", Group.GREEN, 300, listOf(26, 130, 450, 900, 1100, 1275), 200),
        Space(33, SpaceType.CHEST, "کارت اتفاقات عمومی", null),
        street(34, "دهکده الماس الهیه", Group.GREEN, 320, listOf(28, 150, 450, 1000, 1200, 1400), 200),
        Space(35, SpaceType.RAILROAD, "اسکله بندرعباس", Group.RAILROAD, RAILROAD_PRICE),
        Space(36, SpaceType.CHANCE, "کارت شانس", null),
        street(37, "خیابان تجریش", Group.DARK_BLUE, 350, listOf(35, 175, 500, 1100, 1300, 1500), 200),
        Space(38, SpaceType.TAX, "مالیات تجملات", null, taxAmount = 100_000),
        street(39, "خیابان اقدسیه", Group.DARK_BLUE, 400, listOf(50, 200, 600, 1400, 1700, 2000), 200)
    )

    /** ساخت خیابان — همه‌ی مبالغ × ۱٬۰۰۰ تومان */
    private fun street(id: Int, name: String, group: Group, priceK: Long, rentsK: List<Long>, houseCostK: Long): Space =
        Space(
            id, SpaceType.STREET, name, group,
            price = priceK * 1000,
            houseCost = houseCostK * 1000,
            rents = rentsK.map { it * 1000 }
        )

    /** آیا این گروه از نوع توسعه‌پذیر (خیابانی) است؟ */
    fun isStreetGroup(g: Group?): Boolean =
        g == Group.BROWN || g == Group.LIGHT_BLUE || g == Group.PINK || g == Group.ORANGE ||
                g == Group.RED || g == Group.YELLOW || g == Group.GREEN || g == Group.DARK_BLUE
}
