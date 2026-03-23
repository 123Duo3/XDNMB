package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ink.duo3.fogisland.shared.storage.db.entity.CatalogEntryEntity

@Dao
interface CatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<CatalogEntryEntity>)

    @Query(
        """
        DELETE FROM catalog_entries
        WHERE catalogType = :catalogType AND catalogId = :catalogId AND page = :page
        """
    )
    suspend fun deletePage(catalogType: String, catalogId: Long, page: Int)
}
