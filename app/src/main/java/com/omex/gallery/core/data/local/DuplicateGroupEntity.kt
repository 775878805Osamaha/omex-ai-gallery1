package com.omex.gallery.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "duplicate_groups")
data class DuplicateGroupEntity(
    @PrimaryKey val groupId: String,
    val groupType: String, // EXACT, NEAR, VISUAL
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "duplicate_members",
    indices = [
        Index("groupId"),
        Index("mediaId")
    ]
)
data class DuplicateMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: String,
    val mediaId: Long,
    val similarityScore: Float
)
