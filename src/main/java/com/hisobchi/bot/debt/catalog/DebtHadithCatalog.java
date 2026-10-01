package com.hisobchi.bot.debt.catalog;

/**
 * Verified Hadith Catalog for Debt Module.
 * Strict Rule (Section 96):
 * - Never fetch unverified hadith from internet at runtime.
 * - Only verified references: Sahih al-Bukhari 2393 and Sahih Muslim 1563a.
 */
public final class DebtHadithCatalog {

    private DebtHadithCatalog() {
    }

    /**
     * Hadith for BORROWED reminders (encouraging timely and good repayment).
     * Reference: Sahih al-Buxoriy, 2393.
     */
    public static final String BORROWED_HADITH = 
            "📖 <b>Hadis:</b>\n" +
            "<i>“Eng yaxshilaringiz — qarzni eng yaxshi ado etadiganlaringizdir.”</i>\n\n" +
            "— <b>Sahih al-Buxoriy, 2393</b>";

    /**
     * Hadith for LENT reminders (encouraging leniency and granting respite to someone in difficulty).
     * Reference: Sahih Muslim, 1563a.
     */
    public static final String LENT_RESPITE_HADITH = 
            "🤲 <i>Agar qarzdor haqiqatan ham qiynalayotgan bo‘lsa, unga muddat berish katta yaxshilikdir.</i>\n\n" +
            "📖 <b>Rasululloh ﷺ</b> moliyaviy qiyinchilikdagi qarzdorga yengillik yoki muddat bergan kishining fazilati haqida xabar berganlar.\n\n" +
            "— <b>Sahih Muslim, 1563a</b>";

    public static String getBorrowedHadith() {
        return BORROWED_HADITH;
    }

    public static String getLentRespiteHadith() {
        return LENT_RESPITE_HADITH;
    }
}
