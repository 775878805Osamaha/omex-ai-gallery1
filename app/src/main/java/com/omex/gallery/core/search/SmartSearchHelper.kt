package com.omex.gallery.core.search

object SmartSearchHelper {

    /**
     * Map of Arabic and English keywords / synonyms to category IDs.
     */
    private val categoryKeywordMap = mapOf(
        // TRADING
        "trading" to "TRADING",
        "trader" to "TRADING",
        "trade" to "TRADING",
        "crypto" to "TRADING",
        "cryptocurrency" to "TRADING",
        "btc" to "TRADING",
        "eth" to "TRADING",
        "forex" to "TRADING",
        "candlestick" to "TRADING",
        "chart" to "TRADING",
        "stock" to "TRADING",
        "stocks" to "TRADING",
        "market" to "TRADING",
        "binance" to "TRADING",
        "metatrader" to "TRADING",
        "tradingview" to "TRADING",
        "تداول" to "TRADING",
        "تداولات" to "TRADING",
        "التداول" to "TRADING",
        "المتداول" to "TRADING",
        "فوركس" to "TRADING",
        "بيتكوين" to "TRADING",
        "ايثريوم" to "TRADING",
        "إيثريوم" to "TRADING",
        "كريبتو" to "TRADING",
        "عملات رقمية" to "TRADING",
        "شموع" to "TRADING",
        "شارت" to "TRADING",
        "تحليل فني" to "TRADING",
        "سوق مالي" to "TRADING",
        "بورصة" to "TRADING",

        // PRODUCT
        "product" to "PRODUCT",
        "products" to "PRODUCT",
        "merchandise" to "PRODUCT",
        "item" to "PRODUCT",
        "goods" to "PRODUCT",
        "shoe" to "PRODUCT",
        "sneaker" to "PRODUCT",
        "watch" to "PRODUCT",
        "perfume" to "PRODUCT",
        "smartphone" to "PRODUCT",
        "منتج" to "PRODUCT",
        "منتجات" to "PRODUCT",
        "المنتج" to "PRODUCT",
        "المنتجات" to "PRODUCT",
        "سلعة" to "PRODUCT",
        "سلع" to "PRODUCT",
        "بضاعة" to "PRODUCT",
        "بضائع" to "PRODUCT",
        "عطر" to "PRODUCT",
        "ساعة" to "PRODUCT",
        "حذاء" to "PRODUCT",
        "هاتف" to "PRODUCT",

        // DOCUMENT
        "document" to "DOCUMENT",
        "documents" to "DOCUMENT",
        "doc" to "DOCUMENT",
        "pdf" to "DOCUMENT",
        "receipt" to "DOCUMENT",
        "invoice" to "DOCUMENT",
        "bill" to "DOCUMENT",
        "contract" to "DOCUMENT",
        "paper" to "DOCUMENT",
        "report" to "DOCUMENT",
        "statement" to "DOCUMENT",
        "certificate" to "DOCUMENT",
        "prescription" to "DOCUMENT",
        "مستند" to "DOCUMENT",
        "مستندات" to "DOCUMENT",
        "المستند" to "DOCUMENT",
        "المستندات" to "DOCUMENT",
        "فاتورة" to "DOCUMENT",
        "فواتير" to "DOCUMENT",
        "الفاتورة" to "DOCUMENT",
        "ايصال" to "DOCUMENT",
        "إيصال" to "DOCUMENT",
        "عقد" to "DOCUMENT",
        "شهادة" to "DOCUMENT",
        "روشتة" to "DOCUMENT",
        "تقرير" to "DOCUMENT",
        "تقرير طبي" to "DOCUMENT",
        "كشف حساب" to "DOCUMENT",
        "ورقة" to "DOCUMENT",

        // CAR
        "car" to "CAR",
        "cars" to "CAR",
        "vehicle" to "CAR",
        "vehicles" to "CAR",
        "automobile" to "CAR",
        "auto" to "CAR",
        "truck" to "CAR",
        "suv" to "CAR",
        "motorcycle" to "CAR",
        "سيارة" to "CAR",
        "سيارات" to "CAR",
        "السيارة" to "CAR",
        "السيارات" to "CAR",
        "مركبة" to "CAR",
        "مركبات" to "CAR",
        "شاحنة" to "CAR",
        "دراجة" to "CAR",

        // FOOD
        "food" to "FOOD",
        "meal" to "FOOD",
        "dish" to "FOOD",
        "cuisine" to "FOOD",
        "pizza" to "FOOD",
        "burger" to "FOOD",
        "restaurant" to "FOOD",
        "dessert" to "FOOD",
        "drink" to "FOOD",
        "coffee" to "FOOD",
        "طعام" to "FOOD",
        "الطعام" to "FOOD",
        "أكل" to "FOOD",
        "اكل" to "FOOD",
        "وجبة" to "FOOD",
        "وجبات" to "FOOD",
        "مطعم" to "FOOD",
        "مشروب" to "FOOD",
        "قهوة" to "FOOD",
        "حلوى" to "FOOD",
        "بيتزا" to "FOOD",
        "برجر" to "FOOD",

        // NATURE
        "nature" to "NATURE",
        "mountain" to "NATURE",
        "mountains" to "NATURE",
        "beach" to "NATURE",
        "sea" to "NATURE",
        "ocean" to "NATURE",
        "forest" to "NATURE",
        "tree" to "NATURE",
        "flower" to "NATURE",
        "landscape" to "NATURE",
        "sunset" to "NATURE",
        "طبيعة" to "NATURE",
        "الطبيعة" to "NATURE",
        "طبيعه" to "NATURE",
        "جبل" to "NATURE",
        "جبال" to "NATURE",
        "بحر" to "NATURE",
        "شاطئ" to "NATURE",
        "غابة" to "NATURE",
        "شجر" to "NATURE",
        "زهور" to "NATURE",
        "ورد" to "NATURE",
        "منظر طبيعي" to "NATURE",
        "غروب" to "NATURE",

        // TRAVEL
        "travel" to "TRAVEL",
        "trip" to "TRAVEL",
        "tour" to "TRAVEL",
        "tourist" to "TRAVEL",
        "flight" to "TRAVEL",
        "plane" to "TRAVEL",
        "airport" to "TRAVEL",
        "hotel" to "TRAVEL",
        "resort" to "TRAVEL",
        "vacation" to "TRAVEL",
        "سفر" to "TRAVEL",
        "السفر" to "TRAVEL",
        "رحلة" to "TRAVEL",
        "رحلات" to "TRAVEL",
        "سياحة" to "TRAVEL",
        "فندق" to "TRAVEL",
        "مطار" to "TRAVEL",
        "طائرة" to "TRAVEL",
        "طيارة" to "TRAVEL",
        "إجازة" to "TRAVEL",
        "اجازة" to "TRAVEL",

        // PERSON
        "person" to "PERSON",
        "people" to "PERSON",
        "human" to "PERSON",
        "man" to "PERSON",
        "woman" to "PERSON",
        "child" to "PERSON",
        "portrait" to "PERSON",
        "selfie" to "PERSON",
        "face" to "PERSON",
        "faces" to "PERSON",
        "شخص" to "PERSON",
        "أشخاص" to "PERSON",
        "اشخاص" to "PERSON",
        "الأشخاص" to "PERSON",
        "ناس" to "PERSON",
        "بورتريه" to "PERSON",
        "سيلفي" to "PERSON",
        "وجوه" to "PERSON",
        "وجه" to "PERSON",
        "رجل" to "PERSON",
        "امرأة" to "PERSON",
        "طفل" to "PERSON",

        // SCREENSHOT
        "screenshot" to "SCREENSHOT",
        "screenshots" to "SCREENSHOT",
        "screen" to "SCREENSHOT",
        "screen_shot" to "SCREENSHOT",
        "لقطة" to "SCREENSHOT",
        "لقطات" to "SCREENSHOT",
        "لقطة شاشة" to "SCREENSHOT",
        "لقطات الشاشة" to "SCREENSHOT",
        "شاشة" to "SCREENSHOT",

        // WORK
        "work" to "WORK",
        "office" to "WORK",
        "meeting" to "WORK",
        "presentation" to "WORK",
        "code" to "WORK",
        "desk" to "WORK",
        "عمل" to "WORK",
        "العمل" to "WORK",
        "صور العمل" to "WORK",
        "شغل" to "WORK",
        "مكتب" to "WORK",
        "اجتماع" to "WORK",
        "عرض تقديمي" to "WORK"
    )

    /**
     * Finds a matching category ID for the given query, if any.
     */
    fun getCategoryAlias(query: String): String {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return ""

        // Direct check
        categoryKeywordMap[trimmed]?.let { return it }

        // Normalized Arabic check (e.g. strip "ال", normalize hamzas)
        val normalized = normalizeArabic(trimmed)
        categoryKeywordMap[normalized]?.let { return it }

        // Substring / token matching
        for ((keyword, categoryId) in categoryKeywordMap) {
            if (trimmed == keyword || normalized == keyword) {
                return categoryId
            }
            if (keyword.length >= 3 && (trimmed.contains(keyword) || normalized.contains(keyword))) {
                return categoryId
            }
        }

        return ""
    }

    /**
     * Normalizes Arabic text for flexible matching.
     */
    fun normalizeArabic(text: String): String {
        var result = text.trim().lowercase()
        // Strip Arabic diacritics / tashkeel
        result = result.replace("[\\u064B-\\u065F]".toRegex(), "")
        // Normalize Alef variants
        result = result.replace("[إأآا]".toRegex(), "ا")
        // Normalize Taa Marbuta & Haa
        result = result.replace("ة", "ه")
        // Normalize Yaa variants
        result = result.replace("ى", "ي")
        // Strip leading "ال" if the remaining word is >= 3 chars
        if (result.startsWith("ال") && result.length > 4) {
            result = result.substring(2)
        }
        return result
    }

    /**
     * Returns the Arabic display name for a category ID.
     */
    fun getCategoryNameArabic(categoryId: String): String {
        return when (categoryId.uppercase()) {
            "TRADING" -> "التداول"
            "PRODUCT" -> "المنتجات"
            "DOCUMENT" -> "المستندات"
            "CAR" -> "السيارات"
            "FOOD" -> "الطعام"
            "NATURE" -> "الطبيعة"
            "TRAVEL" -> "السفر"
            "PERSON" -> "الأشخاص"
            "SCREENSHOT" -> "لقطات الشاشة"
            "WORK" -> "صور العمل"
            "OTHER" -> "أخرى"
            else -> categoryId
        }
    }

    /**
     * Checks if the query indicates a photo filter.
     */
    fun isPhotoQuery(query: String): Boolean {
        val q = query.trim().lowercase()
        return q == "photo" || q == "photos" || q == "image" || q == "images" ||
                q == "صورة" || q == "صور" || q == "الصور" || q == "الصورة"
    }

    /**
     * Checks if the query indicates a video filter.
     */
    fun isVideoQuery(query: String): Boolean {
        val q = query.trim().lowercase()
        return q == "video" || q == "videos" || q == "movie" || q == "clip" ||
                q == "فيديو" || q == "فيديوهات" || q == "الفيديو" || q == "الفيديوهات" || q == "مقطع" || q == "مقاطع"
    }

    /**
     * Checks if the query indicates a favorite filter.
     */
    fun isFavoriteQuery(query: String): Boolean {
        val q = query.trim().lowercase()
        return q == "favorite" || q == "favorites" || q == "fav" || q == "star" ||
                q == "مفضل" || q == "مفضلة" || q == "المفضلة" || q == "المفضلات" || q == "نجمة"
    }
}
