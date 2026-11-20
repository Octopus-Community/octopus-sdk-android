package com.octopuscommunity.sdk.di

import com.octopuscommunity.sdk.data.local.database.OctopusDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.dsl.onClose

internal val databaseModule get() =
    module {
        single { OctopusDatabase.build(androidContext()) } onClose { it?.close() }
        single { get<OctopusDatabase>().commentDao() }
        single { get<OctopusDatabase>().communityConfigDao() }
        single { get<OctopusDatabase>().feedDao() }
        single { get<OctopusDatabase>().notificationDao() }
        single { get<OctopusDatabase>().postDao() }
        single { get<OctopusDatabase>().profileDao() }
        single { get<OctopusDatabase>().replyDao() }
        single { get<OctopusDatabase>().sectionDao() }
        single { get<OctopusDatabase>().topicDao() }
        single { get<OctopusDatabase>().trackerEventDao() }
        single { get<OctopusDatabase>().userConfigDao() }
    }
