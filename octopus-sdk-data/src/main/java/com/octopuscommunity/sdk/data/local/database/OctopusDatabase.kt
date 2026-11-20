package com.octopuscommunity.sdk.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.octopuscommunity.sdk.data.BuildConfig
import com.octopuscommunity.sdk.data.local.dao.CommentDao
import com.octopuscommunity.sdk.data.local.dao.CommunityConfigDao
import com.octopuscommunity.sdk.data.local.dao.FeedDao
import com.octopuscommunity.sdk.data.local.dao.GamificationProfileDao
import com.octopuscommunity.sdk.data.local.dao.NotificationDao
import com.octopuscommunity.sdk.data.local.dao.PostDao
import com.octopuscommunity.sdk.data.local.dao.ProfileDao
import com.octopuscommunity.sdk.data.local.dao.ReplyDao
import com.octopuscommunity.sdk.data.local.dao.SectionDao
import com.octopuscommunity.sdk.data.local.dao.TopicDao
import com.octopuscommunity.sdk.data.local.dao.TrackerEventDao
import com.octopuscommunity.sdk.data.local.dao.UserConfigDao
import com.octopuscommunity.sdk.data.local.entity.AppSessionEndReceiptEntity
import com.octopuscommunity.sdk.data.local.entity.CommentEntity
import com.octopuscommunity.sdk.data.local.entity.CommunityConfigEntity
import com.octopuscommunity.sdk.data.local.entity.CommunityConfigEntity.GamificationEntity.LevelEntity
import com.octopuscommunity.sdk.data.local.entity.CurrentUserProfileEntity
import com.octopuscommunity.sdk.data.local.entity.CurrentUserProfileEntity.BlockedProfileEntity
import com.octopuscommunity.sdk.data.local.entity.FeedItemEntity
import com.octopuscommunity.sdk.data.local.entity.MediaEntity
import com.octopuscommunity.sdk.data.local.entity.MinimalProfileEntity
import com.octopuscommunity.sdk.data.local.entity.ModerationEntity.ReasonEntity
import com.octopuscommunity.sdk.data.local.entity.NotificationEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity
import com.octopuscommunity.sdk.data.local.entity.ProfileTagEntity
import com.octopuscommunity.sdk.data.local.entity.PublicProfileEntity
import com.octopuscommunity.sdk.data.local.entity.ReactionCountEntity
import com.octopuscommunity.sdk.data.local.entity.ReplyEntity
import com.octopuscommunity.sdk.data.local.entity.RootFeedEntity
import com.octopuscommunity.sdk.data.local.entity.SectionEntity
import com.octopuscommunity.sdk.data.local.entity.TopicEntity
import com.octopuscommunity.sdk.data.local.entity.TrackerEventEntity
import com.octopuscommunity.sdk.data.local.entity.UserConfigEntity
import com.octopuscommunity.sdk.domain.model.Gamification
import kotlinx.serialization.json.Json

@Database(
    entities = [
        AppSessionEndReceiptEntity::class,
        CommentEntity::class,
        CommunityConfigEntity::class,
        CurrentUserProfileEntity::class,
        FeedItemEntity::class,
        NotificationEntity::class,
        PostEntity::class,
        PublicProfileEntity::class,
        ReplyEntity::class,
        RootFeedEntity::class,
        SectionEntity::class,
        TopicEntity::class,
        TrackerEventEntity::class,
        UserConfigEntity::class,
    ],
    version = BuildConfig.DATABASE_VERSION,
    exportSchema = false,
)
@TypeConverters(OctopusDatabase.Converters::class)
abstract class OctopusDatabase : RoomDatabase() {
    abstract fun commentDao(): CommentDao

    abstract fun communityConfigDao(): CommunityConfigDao

    abstract fun feedDao(): FeedDao

    abstract fun notificationDao(): NotificationDao

    abstract fun postDao(): PostDao

    open fun profileDao(): ProfileDao = gamificationProfileDao()

    internal abstract fun gamificationProfileDao(): GamificationProfileDao

    abstract fun replyDao(): ReplyDao

    abstract fun sectionDao(): SectionDao

    abstract fun topicDao(): TopicDao

    abstract fun trackerEventDao(): TrackerEventDao

    abstract fun userConfigDao(): UserConfigDao

    /**
     * Removes user content, keeping API-key configuration and queued analytics.
     * With [keepGuestProfile], also retains the unchanged guest's content and community access:
     * no replacement session is saved to refetch them or trigger the screens' reload.
     */
    suspend fun clearUserTables(keepGuestProfile: Boolean = false) {
        if (keepGuestProfile) return
        withTransaction {
            val database = openHelper.writableDatabase
            for (table in listOf(
                "comments",
                "feed_items",
                "notifications",
                "posts",
                "public_profiles",
                "replies",
                "root_feeds",
                "sections",
                "topics",
                "user_configs",
            )) {
                database.execSQL("DELETE FROM `$table`")
            }
            database.execSQL("DELETE FROM current_user_profiles")
        }
    }

    /**
     * Removes what a closed community must not show nor send at reopening: the content, the notifications and the
     * queued analytics. Keeps the API-key configuration, the community access (overridden while closed) and the
     * current user profile, whose unseen notifications count is set to 0.
     */
    suspend fun clearClosedCommunityTables() {
        withTransaction {
            val database = openHelper.writableDatabase
            for (table in listOf(
                "comments",
                "feed_items",
                "notifications",
                "posts",
                "public_profiles",
                "replies",
                "root_feeds",
                "sections",
                "topics",
                "tracker_events",
            )) {
                database.execSQL("DELETE FROM `$table`")
            }
            database.execSQL("UPDATE current_user_profiles SET notSeenNotificationsCount = 0")
        }
    }

    class Converters {
        private val json = Json { ignoreUnknownKeys = true }

        @TypeConverter
        fun fromMinimalProfileEntityList(value: List<MinimalProfileEntity>) = json.encodeToString(value)

        @TypeConverter
        fun toMinimalProfileEntityList(value: String): List<MinimalProfileEntity> = json.decodeFromString(value)

        @TypeConverter
        fun fromMediaImageEntityList(value: List<MediaEntity.ImageEntity>) = json.encodeToString(value)

        @TypeConverter
        fun toMediaImageEntityList(value: String): List<MediaEntity.ImageEntity> = json.decodeFromString(value)

        @TypeConverter
        fun fromPollAnswerEntityList(value: List<PostEntity.PollEntity.AnswerEntity>) = json.encodeToString(value)

        @TypeConverter
        fun toPollAnswerEntityList(value: String): List<PostEntity.PollEntity.AnswerEntity> = json.decodeFromString(value)

        @TypeConverter
        fun fromPollResultEntityList(value: List<PostEntity.PollEntity.ResultEntity>) = json.encodeToString(value)

        @TypeConverter
        fun toPollAResultEntityList(value: String): List<PostEntity.PollEntity.ResultEntity> = json.decodeFromString(value)

        @TypeConverter
        fun fromReactionsEntityList(value: List<ReactionCountEntity>) = json.encodeToString(value)

        @TypeConverter
        fun toReactionsEntityList(value: String): List<ReactionCountEntity> = json.decodeFromString(value)

        @TypeConverter
        fun fromReasonEntityList(value: List<ReasonEntity>) = json.encodeToString(value)

        @TypeConverter
        fun toReasonEntityList(value: String): List<ReasonEntity> = json.decodeFromString(value)

        @TypeConverter
        fun fromBlockedProfileEntityList(value: List<BlockedProfileEntity>) = json.encodeToString(value)

        @TypeConverter
        fun toBlockedProfileEntityList(value: String): List<BlockedProfileEntity> = json.decodeFromString(value)

        @TypeConverter
        fun fromLevelEntityList(value: List<LevelEntity>) = json.encodeToString(value)

        @TypeConverter
        fun toLevelEntityList(value: String): List<LevelEntity> = json.decodeFromString(value)

        @TypeConverter
        fun fromActionsPointsEntityMap(value: Map<Gamification.Action, Int>) = json.encodeToString(value)

        @TypeConverter
        fun toActionsPointsEntityMap(value: String): Map<Gamification.Action, Int> = json.decodeFromString(value)

        @TypeConverter
        fun fromProfileTagEntityList(value: List<ProfileTagEntity>) = json.encodeToString(value)

        @TypeConverter
        fun toProfileTagEntityList(value: String): List<ProfileTagEntity> = json.decodeFromString(value)

        @TypeConverter
        fun fromMediaSource(value: MediaEntity.ResourceEntity?) = value?.let { json.encodeToString(it) }

        @TypeConverter
        fun toMediaSource(value: String?): MediaEntity.ResourceEntity? = value?.let { json.decodeFromString(it) }

        @TypeConverter
        fun fromStringList(value: List<String>) = json.encodeToString(value)

        @TypeConverter
        fun toStringList(value: String): List<String> = json.decodeFromString(value)
    }

    companion object {
        private const val DATABASE_NAME = "octopus_database"

        // Open the DB eagerly so that any schema/migration failure happens here
        // and can be recovered by wiping the on-disk file and rebuilding. Room's
        // lazy open would otherwise bubble the SQLiteException up to whatever
        // code issues the first query, which crashes the app.
        fun build(context: Context): OctopusDatabase =
            try {
                buildInstance(context).also { it.openHelper.writableDatabase }
            } catch (t: Throwable) {
                context.deleteDatabase(DATABASE_NAME)
                buildInstance(context)
            }

        private fun buildInstance(context: Context): OctopusDatabase =
            Room
                .databaseBuilder(
                    context = context,
                    klass = OctopusDatabase::class.java,
                    name = DATABASE_NAME,
                ).addMigrations(
                    MIGRATION_98_102,
                    MIGRATION_103_104,
                    MIGRATION_104_105,
                    MIGRATION_105_106,
                    MIGRATION_106_107,
                    MIGRATION_107_108,
                    MIGRATION_108_109,
                    MIGRATION_109_110,
                    MIGRATION_110_111,
                    MIGRATION_111_112,
                    MIGRATION_112_113,
                    MIGRATION_113_114,
                    MIGRATION_114_115,
                    MIGRATION_115_116,
                    MIGRATION_116_117,
                    MIGRATION_117_118,
                    MIGRATION_118_119,
                    MIGRATION_119_120,
                    MIGRATION_120_121,
                ).fallbackToDestructiveMigration(true)
                .build()

        // Explicit migration from SDK 1.9.x (DB v98) to SDK 1.10.x (DB v102).
        // Room's destructive fallback has been observed to silently no-op on some
        // MIUI / Xiaomi Android 15 devices, leaving the old schema in place and
        // producing SQLiteException "table ... has no column named ..." on insert.
        // Dropping and recreating the three tables whose schema changed in this
        // window sidesteps that failure mode. Cached data is discarded, which is
        // acceptable (the SDK re-hydrates on next sync).
        private val MIGRATION_98_102 =
            object : Migration(98, 102) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS `community_configs`")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `community_configs` (" +
                            "`id` INTEGER NOT NULL, " +
                            "`forceLoginOnStrongActions` INTEGER NOT NULL, " +
                            "`displayAccountAge` INTEGER NOT NULL, " +
                            "`gamificationpointsName` TEXT, " +
                            "`gamificationshortenPointSingular` TEXT, " +
                            "`gamificationshortenPointPlural` TEXT, " +
                            "`gamificationactionsPoints` TEXT, " +
                            "`gamificationlevels` TEXT, " +
                            "`poweredBytype` TEXT, " +
                            "`poweredBylightLogoUrl` TEXT, " +
                            "`poweredBydarkLogoUrl` TEXT, " +
                            "PRIMARY KEY(`id`))",
                    )

                    db.execSQL("DROP TABLE IF EXISTS `topics`")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `topics` (" +
                            "`id` TEXT NOT NULL, " +
                            "`parentId` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "`updatedAt` INTEGER NOT NULL, " +
                            "`localeLanguage` TEXT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`description` TEXT, " +
                            "`follow_status` TEXT NOT NULL, " +
                            "`sectionIds` TEXT NOT NULL, " +
                            "`status_value` TEXT NOT NULL, " +
                            "`status_reasons` TEXT NOT NULL, " +
                            "`post_feeds_descendingId` TEXT, " +
                            "`post_feeds_ascendingId` TEXT, " +
                            "PRIMARY KEY(`id`))",
                    )

                    db.execSQL("DROP TABLE IF EXISTS `sections`")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `sections` (" +
                            "`id` TEXT NOT NULL, " +
                            "`label` TEXT NOT NULL, " +
                            "PRIMARY KEY(`id`))",
                    )
                }
            }

        // SDK 1.11.x → 1.12.x: internal ticket adds `canCreateChildren` on topics so the create-post
        // topic picker can filter admin-only topics. Existing rows default to `true`, matching
        // the domain-model default and preserving v1.11 behavior until the BE serves the
        // new RequesterCtx field.
        private val MIGRATION_103_104 =
            object : Migration(103, 104) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `topics` ADD COLUMN `canCreateChildren` INTEGER NOT NULL DEFAULT 1",
                    )
                }
            }

        // internal ticket: Topic CTA. Five additive nullable columns embedded under the
        // `custom_action_` prefix, matching `TopicEntity.customAction` /
        // `CustomActionEntity` (ctaText: TranslatableTextEntity + targetUrl).
        // All-nullable defaults to `null`, preserving existing rows; the values
        // hydrate on the next `fetchGroups()` once the BE serves `Topic.cta`.
        private val MIGRATION_104_105 =
            object : Migration(104, 105) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `topics` ADD COLUMN `custom_action_targetUrl` TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE `topics` ADD COLUMN `custom_action_ctaText_original_text` TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE `topics` ADD COLUMN `custom_action_ctaText_original_language` TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE `topics` ADD COLUMN `custom_action_ctaText_translated_text` TEXT DEFAULT NULL")
                    db.execSQL("ALTER TABLE `topics` ADD COLUMN `custom_action_ctaText_translated_language` TEXT DEFAULT NULL")
                }
            }

        // internal ticket: per-field profile lock. Three additive NOT NULL enum columns embedded under the
        // `profileLock` prefix, matching `CommunityConfigEntity.ProfileFieldsLockEntity`. Existing
        // rows default to `EDITABLE`, preserving today's behaviour until the BE serves
        // `ApiKeyConfig.profileFieldsLock`.
        private val MIGRATION_105_106 =
            object : Migration(105, 106) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `community_configs` ADD COLUMN `profileLocknickname` TEXT NOT NULL DEFAULT 'EDITABLE'",
                    )
                    db.execSQL(
                        "ALTER TABLE `community_configs` ADD COLUMN `profileLockavatar` TEXT NOT NULL DEFAULT 'EDITABLE'",
                    )
                    db.execSQL(
                        "ALTER TABLE `community_configs` ADD COLUMN `profileLockbio` TEXT NOT NULL DEFAULT 'EDITABLE'",
                    )
                }
            }

        // internal ticket: per-content-type creation options (Enable pictures / Enable polls). Four additive
        // columns embedded under the `contentOptions` prefix, all NOT NULL DEFAULT 1 (true) so existing
        // rows keep today's behaviour until the BE serves `ApiKeyConfig.contentOptions`.
        private val MIGRATION_106_107 =
            object : Migration(106, 107) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `contentOptionspostEnablePictures` INTEGER NOT NULL DEFAULT 1",
                    )
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `contentOptionspostEnablePolls` INTEGER NOT NULL DEFAULT 1",
                    )
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `contentOptionscommentEnablePictures` INTEGER NOT NULL DEFAULT 1",
                    )
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `contentOptionsreplyEnablePictures` INTEGER NOT NULL DEFAULT 1",
                    )
                }
            }

        // Unified Profile clientUserId consumption (gRPC contract).
        // - MinimalProfile.clientUserId (field 6) rides the embedded author on cached content, so it
        //   becomes `author_clientUserId` on `posts` / `comments` / `replies` (nullable TEXT).
        //   Notifications store their authors as a serialized blob, so they need no column.
        // - PublicProfile.clientUserId (field 50) => `public_profiles.clientUserId` (nullable TEXT).
        // - ApiKeyConfig.exposeClientUserId (field 8) => `community_configs.exposeClientUserId`
        //   (NOT NULL DEFAULT 0, so existing rows keep today's native-profile behaviour).
        private val MIGRATION_107_108 =
            object : Migration(107, 108) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `posts` ADD COLUMN `author_clientUserId` TEXT")
                    db.execSQL("ALTER TABLE `comments` ADD COLUMN `author_clientUserId` TEXT")
                    db.execSQL("ALTER TABLE `replies` ADD COLUMN `author_clientUserId` TEXT")
                    db.execSQL("ALTER TABLE `public_profiles` ADD COLUMN `clientUserId` TEXT")
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `exposeClientUserId` INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        // Explicit terms acceptance (gRPC contract / internal ticket).
        // - ApiKeyConfig.termsAcceptanceMode (field 9) => `community_configs.termsAcceptanceMode`
        //   (Room persists the enum by name; NOT NULL DEFAULT 'IMPLICIT', so existing rows keep
        //   today's behaviour — implicit acceptance, no consent sheet).
        private val MIGRATION_108_109 =
            object : Migration(108, 109) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `termsAcceptanceMode` TEXT NOT NULL DEFAULT 'IMPLICIT'",
                    )
                }
            }

        // Profile "Comments" tab (internal ticket). Two additive nullable-text columns per profile table,
        // embedded under the `comments_feeds_` prefix - mirrors `posts_feeds_descendingId` /
        // `posts_feeds_ascendingId` (both tables), matching `ProfileEntity.commentsFeeds: FeedsEntity`.
        // Existing rows get `NULL` (no cached feed id) until the next profile fetch hydrates them from
        // `PrivateProfile.descCommentFeedId`/`ascCommentFeedId` (own profile) or
        // `PublicProfile.descCommentFeedId`/`ascCommentFeedId` (other profiles).
        private val MIGRATION_109_110 =
            object : Migration(109, 110) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `current_user_profiles` ADD COLUMN `comments_feeds_descendingId` TEXT DEFAULT NULL",
                    )
                    db.execSQL(
                        "ALTER TABLE `current_user_profiles` ADD COLUMN `comments_feeds_ascendingId` TEXT DEFAULT NULL",
                    )
                    db.execSQL(
                        "ALTER TABLE `public_profiles` ADD COLUMN `comments_feeds_descendingId` TEXT DEFAULT NULL",
                    )
                    db.execSQL(
                        "ALTER TABLE `public_profiles` ADD COLUMN `comments_feeds_ascendingId` TEXT DEFAULT NULL",
                    )
                }
            }

        // Comments tab on other members' profiles (internal ticket).
        // - ApiKeyConfig.showCommentsOnOtherProfiles (field 10) => `community_configs.showCommentsOnOtherProfiles`
        //   (Room persists the bool as INTEGER; NOT NULL DEFAULT 0, so existing rows keep today's
        //   behaviour — comments tab only on the connected user's own profile — until the next config
        //   fetch hydrates the real value).
        private val MIGRATION_110_111 =
            object : Migration(110, 111) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `showCommentsOnOtherProfiles` INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        // Optional post text. `ContentOptions.post.textRequired` (PostConfig field 3) is embedded under
        // the `contentOptions` prefix like the other content options: NOT NULL DEFAULT 1 (true), so
        // existing rows keep requiring text until the next config fetch hydrates the real value.
        private val MIGRATION_111_112 =
            object : Migration(111, 112) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `contentOptionspostTextRequired` INTEGER NOT NULL DEFAULT 1",
                    )
                }
            }

        // Nickname choice before the first contribution. ApiKeyConfig.forceNicknameChoice (field 11)
        // => `community_configs.forceNicknameChoice` (INTEGER, NOT NULL DEFAULT 0), so existing rows keep
        // the prefilled field until the next config fetch hydrates the real value.
        private val MIGRATION_112_113 =
            object : Migration(112, 113) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `forceNicknameChoice` INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        // The community's own consent wordings (ApiKeyConfig.consentTexts, field 12). Seven additive
        // nullable TEXT columns embedded under the `consentTexts` prefix, matching
        // `CommunityConfigEntity.ConsentTextsEntity` (mode + one column per wording). Existing rows get
        // NULL everywhere, i.e. no texts (the SDK wordings), until the next config fetch.
        private val MIGRATION_113_114 =
            object : Migration(113, 114) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `community_configs` ADD COLUMN `consentTextsmode` TEXT")
                    db.execSQL("ALTER TABLE `community_configs` ADD COLUMN `consentTextsnotice` TEXT")
                    db.execSQL("ALTER TABLE `community_configs` ADD COLUMN `consentTextstermsLabel` TEXT")
                    db.execSQL("ALTER TABLE `community_configs` ADD COLUMN `consentTextsprivacyLabel` TEXT")
                    db.execSQL("ALTER TABLE `community_configs` ADD COLUMN `consentTextsrulesLabel` TEXT")
                    db.execSQL("ALTER TABLE `community_configs` ADD COLUMN `consentTextssingleLabel` TEXT")
                    db.execSQL("ALTER TABLE `community_configs` ADD COLUMN `consentTextsbelowText` TEXT")
                }
            }

        // Community default avatar: three nullable TEXT columns per profile, raw server strings parsed on read.
        // - MinimalProfile.defaultAvatar rides the embedded author of cached content, so it becomes
        //   `author_defaultAvatar…` on `posts` / `comments` / `replies`. Notifications store their authors as a
        //   serialized blob, so they need no column.
        // - PublicProfile.defaultAvatar / PrivateProfile.defaultAvatar => `public_profiles` /
        //   `current_user_profiles`, unprefixed.
        // Existing rows get NULL (computed initials); the content dates are reset below so the next feed refresh
        // fetches the cached content again.
        private val DEFAULT_AVATAR_COLUMNS =
            listOf("defaultAvatarBackgroundHex", "defaultAvatarInitialsHex", "defaultAvatarImageUrl")

        private val DEFAULT_AVATAR_IF_PICTURE_REMOVED_COLUMNS =
            listOf(
                "defaultAvatarIfPictureRemovedBackgroundHex",
                "defaultAvatarIfPictureRemovedInitialsHex",
                "defaultAvatarIfPictureRemovedImageUrl",
            )

        internal val MIGRATION_114_115 =
            object : Migration(114, 115) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    listOf(
                        "posts" to "author_",
                        "comments" to "author_",
                        "replies" to "author_",
                        "public_profiles" to "",
                        "current_user_profiles" to "",
                    ).forEach { (table, prefix) ->
                        DEFAULT_AVATAR_COLUMNS.forEach { column ->
                            db.execSQL("ALTER TABLE `$table` ADD COLUMN `$prefix$column` TEXT")
                        }
                    }
                    // PrivateProfile.defaultAvatarIfPictureRemoved: only the current user's profile has it.
                    DEFAULT_AVATAR_IF_PICTURE_REMOVED_COLUMNS.forEach { column ->
                        db.execSQL("ALTER TABLE `current_user_profiles` ADD COLUMN `$column` TEXT")
                    }

                    // Content cached by an SDK without default avatars: the server moved its `updatedAt` when the
                    // community chose its default avatars, possibly before this upgrade, and will not move it again.
                    // Resetting it once makes every cached post, comment and reply older than its feed entry, so
                    // the next feed page fetches it in full, with its author's default avatar (same reset as
                    // PostDao/CommentDao/ReplyDao.resetUpdatedAt).
                    listOf("posts", "comments", "replies").forEach { table ->
                        db.execSQL("UPDATE `$table` SET `updatedAt` = 0")
                    }
                }
            }

        // Bounded comment windows and MAX(position) use the same covering range index.
        // Existing feed positions and cached content are preserved.
        private val MIGRATION_115_116 =
            object : Migration(115, 116) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_feed_items_feedId_position` " +
                            "ON `feed_items` (`feedId`, `position`)",
                    )
                }
            }

        internal val MIGRATION_116_117 =
            object : Migration(116, 117) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `app_session_end_receipts` " +
                            "(`appSessionId` TEXT NOT NULL, PRIMARY KEY(`appSessionId`))",
                    )
                }
            }

        // Keep both optional tap behaviours disabled for cached configurations until fetched again.
        internal val MIGRATION_117_118 =
            object : Migration(117, 118) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `contentOptionspostImageTapOpensPost` INTEGER NOT NULL DEFAULT 0",
                    )
                    db.execSQL(
                        "ALTER TABLE `community_configs` " +
                            "ADD COLUMN `contentOptionspostGroupNameTapOpensGroup` INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        /** The access row keeps why the access is denied, and the details of a ban. */
        internal val MIGRATION_118_119 =
            object : Migration(118, 119) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    listOf(
                        "accessDeniedReason",
                        "banNickname",
                        "banPolicyName",
                        "banPolicyDescription",
                        "banContactEmail",
                    ).forEach { column -> db.execSQL("ALTER TABLE `user_configs` ADD COLUMN `$column` TEXT") }
                }
            }

        // A member's total points, for the host app's community data. Cached public profiles keep their
        // other columns and get the total at their next fetch.
        internal val MIGRATION_119_120 =
            object : Migration(119, 120) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `public_profiles` ADD COLUMN `gamificationScore` INTEGER")
                }
            }

        // The community languages whose SDK texts use the informal tone (ApiKeyConfig.informalLanguages,
        // internal ticket), as a JSON array. Existing rows get `[]`, i.e. formal everywhere as before, until the
        // next config fetch.
        internal val MIGRATION_120_121 =
            object : Migration(120, 121) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `community_configs` ADD COLUMN `informalLanguages` TEXT NOT NULL DEFAULT '[]'",
                    )
                }
            }
    }
}
