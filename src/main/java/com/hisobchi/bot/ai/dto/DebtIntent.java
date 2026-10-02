package com.hisobchi.bot.ai.dto;

public enum DebtIntent {
    BORROW,         // Qarz oldim
    LEND,           // Qarz berdim
    REPAY_PARTIAL,  // Qarzimdan 500 ming berdim
    REPAY_FULL,     // Qarzimni hammasini berdim
    RETURN_PARTIAL, // Javlon 600 ming qarz qaytardi
    RETURN_FULL     // Javlon qarzini hammasini qaytardi
}
