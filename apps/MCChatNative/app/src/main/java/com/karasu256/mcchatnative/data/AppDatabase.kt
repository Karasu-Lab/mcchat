package com.karasu256.mcchatnative.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.karasu256.mcchatnative.model.ChatThread
import com.karasu256.mcchatnative.model.ConnectionInfo
import com.karasu256.mcchatnative.model.StoredMessage
import com.karasu256.mcchatnative.model.ThreadCategory
import com.karasu256.mcchatnative.model.World
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppDao {
    @Query("SELECT * FROM connections ORDER BY rowid")
    abstract fun connections(): Flow<List<ConnectionInfo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertConnection(connection: ConnectionInfo)

    @Query("UPDATE connections SET userId = :userId, userName = :userName WHERE id = :id")
    abstract suspend fun setConnectionUser(id: String, userId: String, userName: String)

    @Query("DELETE FROM connections WHERE id = :id")
    abstract suspend fun deleteConnection(id: String)

    @Query("SELECT * FROM threads ORDER BY rowid")
    abstract fun threads(): Flow<List<ChatThread>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertThreads(threads: List<ChatThread>)

    @Query("DELETE FROM threads WHERE clientId = :clientId")
    abstract suspend fun deleteThreadsByClient(clientId: String)

    @Query("UPDATE threads SET category = :category WHERE id = :id")
    abstract suspend fun setThreadCategory(id: String, category: ThreadCategory)

    @Query("SELECT * FROM worlds ORDER BY rowid")
    abstract fun worlds(): Flow<List<World>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertWorld(world: World)

    @Query("SELECT * FROM messages WHERE host = :host AND deletedAt IS NULL ORDER BY rowId")
    abstract suspend fun messages(host: String): List<StoredMessage>

    @Query("UPDATE messages SET deletedAt = :deletedAt WHERE host = :host AND deletedAt IS NULL")
    abstract suspend fun deleteMessages(host: String, deletedAt: Long)

    @Query("DELETE FROM messages WHERE host = :host AND deletedAt IS NULL")
    abstract suspend fun deleteLiveMessages(host: String)

    @Insert
    abstract suspend fun insertMessages(messages: List<StoredMessage>)

    @Query("SELECT MAX(deletedAt) FROM messages WHERE host = :host")
    abstract suspend fun clearedAt(host: String): Long?

    @Transaction
    open suspend fun replaceMessages(host: String, messages: List<StoredMessage>) {
        deleteLiveMessages(host)
        insertMessages(messages)
    }
}

@Database(
    entities = [
        ConnectionInfo::class,
        ChatThread::class,
        World::class,
        StoredMessage::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mcchat.db",
                ).build().also { instance = it }
            }
    }
}
