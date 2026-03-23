package ink.duo3.fogisland.shared.storage.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "catalog_entries",
    primaryKeys = ["catalogType", "catalogId", "threadId"],
    indices = [
        Index("threadId"),
        Index(value = ["catalogType", "catalogId", "page", "position"])
    ]
)
data class CatalogEntryEntity(
    val catalogType: String,
    val catalogId: Long,
    val threadId: Long,
    val page: Int,
    val position: Int,
    val refreshedAt: Long = 0L
)
