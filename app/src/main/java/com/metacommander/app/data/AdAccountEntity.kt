package com.metacommander.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ad_accounts")
data class AdAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: Int,
    val currency: String,
    val spend: String,
    val limit: String,
    val cachedAtMillis: Long
)
@Dao interface AdAccountDao {
    @Query("SELECT * FROM ad_accounts ORDER BY name") fun observeAll(): Flow<List<AdAccountEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(accounts: List<AdAccountEntity>)
    @Query("DELETE FROM ad_accounts") suspend fun clear()
}
@Database(entities = [AdAccountEntity::class], version = 1, exportSchema = false)
abstract class AdsDatabase : RoomDatabase() { abstract fun adAccountDao(): AdAccountDao }
