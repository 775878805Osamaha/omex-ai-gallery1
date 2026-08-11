package com.omex.gallery.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main Room database instance for OMEX AI Gallery.
 */
@Database(
    entities = [
        MediaItemEntity::class,
        ImageClassificationEntity::class,
        DetectedObjectEntity::class,
        DetectedFaceEntity::class,
        FaceEmbeddingEntity::class,
        DuplicateGroupEntity::class,
        DuplicateMemberEntity::class,
        ImageMetadataEntity::class,
        OcrTextEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        MediaCategoryEntity::class,
        MediaItemCategoryCrossRef::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mediaDao(): MediaDao
    abstract fun aiDao(): AiDao
    abstract fun chatDao(): ChatDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ocr_text_results` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mediaId` INTEGER NOT NULL,
                        `extractedText` TEXT NOT NULL,
                        `language` TEXT,
                        `processingStatus` TEXT NOT NULL,
                        `modelVersion` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_text_results_mediaId` ON `ocr_text_results` (`mediaId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_text_results_processingStatus` ON `ocr_text_results` (`processingStatus`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chat_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `session_id` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`session_id`) REFERENCES `chat_sessions`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_session_id` ON `chat_messages` (`session_id`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `media_categories` (
                        `categoryId` TEXT NOT NULL,
                        `nameArabic` TEXT NOT NULL,
                        `iconName` TEXT,
                        PRIMARY KEY(`categoryId`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `media_item_category_cross_ref` (
                        `mediaId` INTEGER NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        PRIMARY KEY(`mediaId`, `categoryId`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_item_category_cross_ref_mediaId` ON `media_item_category_cross_ref` (`mediaId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_item_category_cross_ref_categoryId` ON `media_item_category_cross_ref` (`categoryId`)")

                // Pre-populate default virtual categories
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('PERSON', 'الأشخاص', 'person')")
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('PRODUCT', 'المنتجات', 'shopping_bag')")
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('TRADING', 'التداول', 'show_chart')")
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('SCREENSHOT', 'لقطات الشاشة', 'crop_free')")
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('DOCUMENT', 'المستندات', 'description')")
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('CAR', 'السيارات', 'directions_car')")
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('FOOD', 'الطعام', 'restaurant')")
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('NATURE', 'الطبيعة', 'park')")
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('TRAVEL', 'السفر', 'flight')")
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('WORK', 'صور العمل', 'work')")
                db.execSQL("INSERT OR IGNORE INTO `media_categories` (`categoryId`, `nameArabic`, `iconName`) VALUES ('OTHER', 'أخرى', 'category')")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omex_ai_gallery.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
