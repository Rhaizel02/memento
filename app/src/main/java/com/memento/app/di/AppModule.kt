package com.memento.app.di

import android.content.Context
import androidx.room.Room
import com.memento.app.data.local.dao.ConsumptionDao
import com.memento.app.data.local.dao.MediaDao
import com.memento.app.data.local.dao.RememberDao
import com.memento.app.data.local.dao.RecommendationDao
import com.memento.app.data.local.dao.BackupDao
import com.memento.app.data.local.database.MementoDatabase
import com.memento.app.data.local.database.MIGRATION_1_2
import com.memento.app.data.local.database.MIGRATION_2_3
import com.memento.app.data.local.database.MIGRATION_3_4
import com.memento.app.data.local.database.MIGRATION_4_5
import com.memento.app.data.local.database.MIGRATION_5_6
import com.memento.app.data.local.database.HARDENING_DATABASE_CALLBACK
import com.memento.app.data.local.dao.AiInsightDao
import com.memento.app.data.local.dao.TimelineDao
import com.memento.app.data.repository.RoomMediaRepository
import com.memento.app.data.repository.RoomRememberRepository
import com.memento.app.data.repository.RemoteMetadataRepository
import com.memento.app.data.repository.RoomRecommendationRepository
import com.memento.app.data.repository.RoomBackupRepository
import com.memento.app.data.remote.api.OpenLibraryApi
import com.memento.app.data.remote.api.RawgApi
import com.memento.app.data.remote.api.TmdbApi
import com.memento.app.domain.repository.MediaRepository
import com.memento.app.domain.repository.MetadataRepository
import com.memento.app.domain.repository.RememberRepository
import com.memento.app.domain.repository.RecommendationRepository
import com.memento.app.domain.repository.BackupRepository
import com.memento.app.ai.AiProcessor
import com.memento.app.ai.MlKitAiProcessor
import com.memento.app.data.repository.RoomAiInsightRepository
import com.memento.app.data.repository.RoomCulturalTimelineRepository
import com.memento.app.domain.repository.AiInsightRepository
import com.memento.app.domain.repository.CulturalTimelineRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.memento.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MementoDatabase =
        Room.databaseBuilder(context, MementoDatabase::class.java, "memento.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .addCallback(HARDENING_DATABASE_CALLBACK)
            .build()

    @Provides fun provideMediaDao(database: MementoDatabase): MediaDao = database.mediaDao()
    @Provides fun provideConsumptionDao(database: MementoDatabase): ConsumptionDao = database.consumptionDao()
    @Provides fun provideRememberDao(database: MementoDatabase): RememberDao = database.rememberDao()
    @Provides fun provideRecommendationDao(database: MementoDatabase): RecommendationDao = database.recommendationDao()
    @Provides fun provideBackupDao(database: MementoDatabase): BackupDao = database.backupDao()
    @Provides fun provideAiInsightDao(database: MementoDatabase): AiInsightDao = database.aiInsightDao()
    @Provides fun provideTimelineDao(database: MementoDatabase): TimelineDao = database.timelineDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Provides
    @Singleton
    fun provideTmdbApi(): TmdbApi = retrofit("https://api.themoviedb.org/").create(TmdbApi::class.java)

    @Provides
    @Singleton
    fun provideRawgApi(): RawgApi = retrofit("https://api.rawg.io/").create(RawgApi::class.java)

    @Provides
    @Singleton
    fun provideOpenLibraryApi(): OpenLibraryApi {
        val contact = BuildConfig.OPEN_LIBRARY_CONTACT.trim()
        val userAgent = if (contact.isEmpty()) "Memento/0.1 (Android)" else "Memento/0.1 ($contact)"
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
        }.build()
        return retrofit("https://openlibrary.org/", client).create(OpenLibraryApi::class.java)
    }

    private fun retrofit(baseUrl: String, client: OkHttpClient = OkHttpClient()): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCulturalTimelineRepository(implementation: RoomCulturalTimelineRepository): CulturalTimelineRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(implementation: RoomMediaRepository): MediaRepository

    @Binds
    @Singleton
    abstract fun bindRememberRepository(implementation: RoomRememberRepository): RememberRepository

    @Binds
    @Singleton
    abstract fun bindMetadataRepository(implementation: RemoteMetadataRepository): MetadataRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(implementation: RoomRecommendationRepository): RecommendationRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(implementation: RoomBackupRepository): BackupRepository

    @Binds
    @Singleton
    abstract fun bindAiProcessor(implementation: MlKitAiProcessor): AiProcessor

    @Binds
    @Singleton
    abstract fun bindAiInsightRepository(implementation: RoomAiInsightRepository): AiInsightRepository
}
