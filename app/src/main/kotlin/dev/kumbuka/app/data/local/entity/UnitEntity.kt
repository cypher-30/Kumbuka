package dev.kumbuka.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val id: String,
    val code: String,
    val title: String,
    val packId: String?,
    val packVersion: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)
