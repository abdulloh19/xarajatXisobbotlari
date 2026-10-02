# Hisobchi Bot 📊

**Hisobchi Bot** — bu Java 21, Spring Boot 3.3, PostgreSQL va AI/NLP texnologiyalari asosida qurilgan, production darajasidagi professional shaxsiy va biznes moliyaviy hisobchi Telegram boti.

U foydalanuvchiga kunlik xarajat va daromadlarni oddiy matn, menyu tugmalari yoki **ovozli xabar (voice message)** orqali kiritish, ovozdan summa, kategoriya va izohni avtomatik aniqlash, kunlik, haftalik, oylik hisobotlarni ko‘rish hamda kun yakunida sof foydani hisoblab kunni yopish imkonini beradi.

---

## 🚀 Asosiy imkoniyatlar

- 🎙 **Ovozli xabarlarni tushunish:** Telegram voice message (`OGG/OPUS`) qabul qilinadi, `SpeechToTextService` orqali matnga aylantiriladi va gibrid NLP parser summa, kategoriya va izohni ajratib oladi.
- 💵 **Murakkab O'zbek tili summa parseri:**
  - `15 ming`, `15 ming so‘m`, `15k`, `15000`, `15 000`
  - `100 ming`, `yuz ming`, `o‘n besh ming`
  - `1 million`, `1.2 million`, `1 mln 250 ming`, `bir million ikki yuz ming`
  - `2 yarim million`, `yarim million`, `500 ming`
- 🛡 **Qat'iy tasdiqlash (Draft Confirmation) tizimi:** Moliyaviy operatsiya hech qachon foydalanuvchi tasdig‘isiz (`✅ Saqlash` bosilmasdan) yakuniy bazaga saqlanmaydi.
- 📂 **Kategoriyalarni avtomatik tanish va shaxsiy kategoriyalar:** 17 ta default xarajat va 6 ta daromad kategoriyasi, shuningdek foydalanuvchi o‘zi yangi kategoriya qo‘shishi mumkin.
- ❓ **Noaniqlikni hal qilish (Smart Follow-up):** Agar xabarda summa bo‘lib, kategoriya noaniq bo‘lsa (masalan: *"50 ming berdim"*), bot avval kategoriya tugmalarini chiqaradi.
- ⏰ **Avtomatik Eslatmalar & Davriy Hisobotlar:**
  - Har kuni 18:00 va 21:00 da foydani kiritish eslatmalari.
  - Kunlik (23:00), Haftalik (Yakshanba 22:00), 2 haftalik (14 kun), 3 haftalik (21 kun) va Oylik avtomatik hisobotlar.
  - Eslatmalar sozlamalari (`🔔 Eslatmalar`) orqali har bir bildirishnomani yoqish/o'chirish.
- 💰 **Foydani kiritish va Daromad hisoblash qoidasi:**
  - Foydalanuvchi naqd va kartadagi qolgan foydasini kiritadi.
  - Barcha davriy hisobotlarda: `JAMI ISHLANGAN = XARAJAT + FOYDA`.
  - Foydasi kiritilmagan kunlarni aniqlash va hisobotda ko'rsatish.
- 🤝 **Qarzlar Moduli (Debt Management — Sections 75-100):**
  - **Asosiy menyu tugmasi:** `🤝 Qarzlar`
  - **Submenu:** `💵 Qarz oldim`, `💰 Qarz berdim`, `📋 Faol qarzlar`, `✅ Yopilgan qarzlar`, `📊 Qarz statistikasi`.
  - **Qarz turlari:** `BORROWED` (foydalanuvchi olgan) va `LENT` (foydalanuvchi bergan) — bir-biridan qat'iy ajratilgan.
  - **Bosqichma-bosqich manual flow & Ovozli kiritish:** Summa, shaxs, muddat (`UzbekDateParser`) va to'lov turi (`💵 Naqd` / `💳 Karta`) orqali tasdiqlash kartasi bilan kiritish.
  - **Avtomatik eslatmalar:**
    - `BORROWED`: Qaytarishga 2 kun qolganda (Sahih al-Buxoriy 2393 hadisi bilan) hamda muddat kuni (09:00, 13:00, 17:00, 21:00 da). `✅ To‘ladim` bosilgach qolgan eslatmalar to'xtaydi.
    - `LENT`: 2 kun qolganda va muddat kuni (09:00, 14:00, 19:00 da) muloyim eslatma (Sahih Muslim 1563a muddat berish fazilati hadisi bilan). `✅ Qaytarib oldim` bosilgach status `RECEIVED` bo'ladi.
    - `OVERDUE`: Muddati o'tgan qarzlarni avtomatik aniqlash va kuniga 1 marta muloyim eslatish.
  - **Muddatni uzaytirish:** Yangi sanani matn orqali kiritish va tasdiqlash (`debt:conf_ext`).
  - **Qisman va to‘liq to‘lovlar (Partial & Full Payments):** Qarzni bo‘lib-bo‘lib to‘lash yoki to‘liq yopish imkoniyati. Qolgan summa (`remaining_amount`) va to‘langan qism (`paid_amount`) doimiy kuzatiladi.
  - **To‘lovlar tarixi (Debt Payments History):** Har bir to‘lov miqdori, usuli (`Naqd`/`Karta`), sanasi va manbasi bilan `debt_payments` da saqlanadi.
  - **Kassa va karta balansi (User Balances):** Qarz olinganda, berilganda yoki to‘langanda foydalanuvchining kassa/karta qoldig‘i (`user_balances`) avtomatik to‘g‘rilanadi.
  - **Ovozli va matnli NLP orqali to‘lov:** *"Rustam akaga qarzimdan 500 ming berdim"*, *"Rustam akaga qarzimni hammasini to'ladim"*, *"Javlon hamma qarzini qaytardi"* kabi iboralarni to‘liq tushunish.
  - **Qat'iy moliyaviy ajralish (Section 81):** Qarzlar kundalik taksi sof foydasi, xarajati va daromadi hisob-kitobiga aralashmaydi!
- 📈 **Sof foyda va statistikalar:**
  - Bugungi, haftalik va oylik batafsil hisobotlar (PostgreSQL agregatsiyalari bilan, N+1 muammosisiz).
- 🔐 **Kunni yopish va qayta ochish:** Kun yakunida yakuniy hisobot saqlanadi (`DailySummary`). Yopilgan kunga yangi operatsiya qo‘shilganda bot qayta ochishni so‘raydi.
- 📜 **Tarix, Tahrirlash va O‘chirish:** Istalgan operatsiyani ko‘rish, summasini, kategoriyasini, izohini tahrirlash yoki o‘chirish mumkin.
- 🔒 **Xavfsizlik va Idempotentlik:**
  - Har bir foydalanuvchi faqat o‘z ma'lumotlarini ko‘radi va o‘zgartiradi (`user_id` tekshiruvi).
  - Double-click va takroriy Telegram update'lar hamda takroriy eslatmalar/hisobotlar oldi olingan (`ProcessedUpdate`, `ReminderDeliveryLog`, `ReportDeliveryLog`, `DebtReminderLog`).
  - Ovozli xabarlar tahlildan so‘ng darhol o‘chirib yuboriladi (maxfiylik kafolati).

---

## 🛠 Texnologiyalar

- **Java 21 LTS**
- **Spring Boot 3.3.4**
- **Spring Data JPA & Hibernate**
- **PostgreSQL 16+**
- **Flyway Database Migrations** (12 ta to'liq migratsiya)
- **Telegram Bot API** (Toza, yuqori unumdor REST Client)
- **OpenAI Whisper STT & Built-in Voice Engine**
- **Lombok**
- **JUnit 5 & Mockito** (50 ta unit va integratsiya testlar)
- **Docker & Docker Compose**

---

## 📋 Tizim talablari

- Java 21 (JDK 21)
- Apache Maven 3.9+
- PostgreSQL 14+ (yoki Docker)
- Telegram Bot Token ([@BotFather](https://t.me/BotFather) orqali olinadi)
- OpenAI API Key (ixtiyoriy, ovozni matnga aylantirish uchun)

---

## ⚙️ Sozlash va Ishga tushirish

### 1. PostgreSQL ma'lumotlar bazasini tayyorlash

PostgreSQL'da yangi baza yarating:

```sql
CREATE DATABASE hisobchi_bot;
```

### 2. Environment Variables (.env)

Loyihaning ildiz papkasida `.env` faylini yarating (yoki `.env.example` dan nusxa oling):

```env
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrSTUvwxYZ
TELEGRAM_BOT_USERNAME=hisobchi_bot

# Ma'lumotlar bazasi
DB_HOST=localhost
DB_PORT=5432
DB_NAME=hisobchi_bot
DB_USERNAME=postgres
DB_PASSWORD=postgres

# AI / Ovoz xizmati (OpenAI Whisper)
AI_API_KEY=sk-proj-your-openai-api-key
AI_BASE_URL=https://api.openai.com/v1
AI_MODEL=gpt-4o-mini
SPEECH_TO_TEXT_MODEL=whisper-1
AI_ENABLED=true

# Vaqt zonasi
APP_TIMEZONE=Asia/Tashkent
```

### 3. Loyihani kompilyatsiya qilish va testlarni yurgizish

Windows (PowerShell / CMD):
```powershell
.\mvnw.cmd clean test
```

Linux / macOS:
```bash
./mvnw clean test
```

Barcha 26 ta unit va integratsiya testlari (Summa parser, Kategoriya aniqlash, Foyda formulasi, Xavfsizlik, Idempotentlik, Spring Context) muvaffaqiyatli o'tadi.

### 4. Mahalliy (Local) ishga tushirish

```powershell
.\mvnw.cmd spring-boot:run
```

Loyiha ishga tushishi bilan Flyway avtomatik ravishda barcha kerakli jadvallar va indekslarni yaratadi.

---

## 🐳 Docker orqali ishga tushirish

Docker va Docker Compose o'rnatilgan bo'lsa, butun tizimni bitta buyruq bilan ishga tushirish mumkin:

```bash
docker compose up -d --build
```

Bu buyruq:
1. `hisobchi-postgres` konteynerini ko'taradi va healthcheck orqali tayyor bo'lishini kutadi.
2. `hisobchi-bot-app` dasturini ko'p bosqichli (multi-stage) Dockerfile orqali yig'adi va ishga tushiradi.

Loglarni ko'rish:
```bash
docker compose logs -f app
```

To'xtatish:
```bash
docker compose down
```

---

## ☁️ Render.com (Bepul Free Tier) serveriga joylash

Hisobchi Bot Render.com platformasining bepul (`Free`) tarifida ishlash uchun to'liq moslashtirilgan. Loyihada tayyor `render.yaml` (Blueprint) mavjud.

### 1-usul: Blueprint orqali avtomatik joylash (Tavsiya etiladi)
1. [Render.com](https://render.com) ga kiring va GitHub akkauntingiz bilan avtorizatsiyadan o'ting.
2. Dashboard'da **New +** tugmasini bosing va **Blueprint** ni tanlang.
3. Ushbu `xarajatXisobbotlari` GitHub omborini (repository) tanlang.
4. Render avtomatik ravishda `render.yaml` faylini aniqlaydi:
   - Bepul **PostgreSQL** ma'lumotlar bazasi (`hisobchi-db`) yaratiladi.
   - Bepul **Web Service** (`hisobchi-bot`) yaratiladi va baza bilan ulanadi.
5. So'ralgan environment variable'larga quyidagilarni kiriting:
   - `TELEGRAM_BOT_TOKEN`: Telegram bot tokeningiz ([@BotFather](https://t.me/BotFather) dan).
   - `AI_API_KEY`: OpenAI API key (ixtiyoriy, agar Whisper STT ishlatilsa).
6. **Apply** tugmasini bosing. Render butun dasturni yig'adi, Flyway migratsiyalarni yurgizadi va botni ishga tushiradi!

### 2-usul: Qo'lda (Manual) Web Service yaratish
1. Render'da **New +** ➡️ **PostgreSQL** yarating (nomi: `hisobchi_bot`).
2. Keyin **New +** ➡️ **Web Service** yarating, GitHub omborni tanlang va **Docker** runtime'ni tanlang.
3. Environment variables bo'limida bazaning `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` va `TELEGRAM_BOT_TOKEN` qiymatlarini kiriting.
4. **Deploy Web Service** tugmasini bosing.

---

## 📱 Botdan foydalanish qo'llanmasi

1. Telegramda botingizga `/start` yuboring.
2. Bot asosiy menyuni chiqaradi:
   - **💸 Xarajat qo‘shish** — Summa va kategoriyani bosqichma-bosqich kiritish.
   - **💰 Daromad qo‘shish** — Daromad summasi va manbasini kiritish.
   - **💵 Foydani kiritish** — Kun oxirida qo'ldagi naqd va karta foydasini kiritish.
   - **📋 Qarzlar** — Olingan va berilgan qarzlarni ko'rish, yangi qarz kiritish, qaytarish.
   - **🎙 Ovoz bilan kiritish** — Mikrofon tugmasini bosib, ovozli xabar yuborish.
   - **📊 Bugungi statistika** — Bugungi daromad, xarajat, sof foyda va kategoriyalar taqsimoti.
   - **📊 Hisobotlar** — Bugun, 7 kun, 14 kun, 21 kun, oylik hisobotlarni ko'rish.
   - **📅 Haftalik statistika** — Dushanbadan Yakshanbagacha haftalik tahlil va eng katta xarajat kategoriyasi.
   - **🗓 Oylik statistika** — Oylik umumiy natija, o'rtacha kunlik daromad va sof foyda.
   - **📜 Tarix** — Bugun, kecha, oxirgi 7 kun va shu oydagi operatsiyalar ro'yxati.
   - **🔔 Eslatmalar** — Avtomatik eslatmalar va davriy hisobotlarni yoqish/o'chirish.
   - **🔐 Kunni yopish** — Kun yakunini tasdiqlab, kunlik hisobotni arxivlash.

### Misollar:

- **Ovoz (Xarajat):** *"15 ming obedga"* ➡️ Xarajat: 15 000 so‘m | Kategoriya: Ovqat
- **Ovoz (Xarajat):** *"250 ming mashinaga benzin quydim"* ➡️ Xarajat: 250 000 so‘m | Kategoriya: Yoqilg‘i
- **Ovoz (Material):** *"Mis truba bilan kabelga 1 million 400 ming ketdi"* ➡️ Xarajat: 1 400 000 so‘m | Kategoriya: Material
- **Ovoz (Daromad):** *"Bugun montajdan 3 million ishladim"* ➡️ Daromad: 3 000 000 so‘m | Manba: Xizmat
- **Ovoz (Qarz olish):** *"2 million 500 ming Rustam akadan oldim, keyingi haftaga berishim kerak"* ➡️ Qarz turi: BORROWED | Summa: 2 500 000 so'm | Shaxs: Rustam Aka | Muddat: 08.10.2026
- **Ovoz (Qarz berish):** *"Sherzodga 3 million qarz berdim, keyingi dushanba qaytaradi"* ➡️ Qarz turi: LENT | Summa: 3 000 000 so'm | Shaxs: Sherzod
- **Matn:** *"300 ming benzin"* ➡️ Bot darhol tasdiqlash oynasini chiqaradi.

---

## 🏛 Loyiha arxitekturasi

```text
com.hisobchi.bot/
├── HisobchiBotApplication.java       # Spring Boot main class
├── config/                           # Konfiguratsiyalar (BotConfig, AppConfig)
├── common/
│   ├── exception/                    # Global istisnolar (Validation, Unauthorized, EntityNotFound)
│   ├── formatter/                    # MoneyFormatter (15 000 so'm, sof foyda)
│   └── util/                         # DateTimeUtils, UzbekDateParser (Sanalarni tahlil qilish)
├── user/                             # Foydalanuvchi va holatlar (UserState)
├── category/                         # Default va maxsus kategoriyalar
├── transaction/                      # Tranzaksiyalar, qoralama (Draft) tizimi
├── profit/                           # Kunlik naqd/karta foydasi (DailyProfit, DailyProfitService)
├── debt/                             # Qarzlar boshqaruvi va qoralamasi (Debt, DebtDraft, DebtService)
├── summary/                          # Kunlik xulosalar (DailySummary, Kunni yopish/ochish)
├── statistics/                       # Yuqori unumdor SQL agregatsiyali statistika
├── report/                           # Davriy hisobotlar (ReportService, ReportData: Jami ishlangan = Xarajat + Foyda)
├── notification/                     # Avtomatik eslatmalar (18:00, 21:00, 23:00, haftalik, oylik) va loglar
├── idempotency/                      # Takroriy so'rovlar va double-click himoyasi
├── ai/
│   ├── service/                      # SpeechToTextService, UzbekAmountParser, CategoryMatcher, DebtNlpService, TransactionNlpService
│   └── dto/                          # ParsedTransaction, TranscriptionResult, ParsedDebt
├── voice/                            # Ovoz yuklab olish va tozalash (AudioFileDownloader, VoiceProcessingService)
└── telegram/
    ├── client/                       # Telegram REST API Client va modellari
    ├── keyboard/                     # ReplyKeyboardFactory, InlineKeyboardFactory
    ├── handler/                      # UpdateDispatcher, CommandHandler, TextHandler, VoiceHandler, CallbackHandler
    └── polling/                      # TelegramPollingRunner (Long-polling worker)
```

---

## 📄 Litsenziya

MIT License.
