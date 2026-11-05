package dev.kumbuka.app.data.repository

import dev.kumbuka.app.data.local.dao.UnitDao
import dev.kumbuka.app.domain.model.Unit as UnitModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UnitRepository(private val unitDao: UnitDao) {
    fun observeAll(): Flow<List<UnitModel>> =
        unitDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): UnitModel? = unitDao.getById(id)?.toDomain()

    suspend fun getByPackId(packId: String): UnitModel? = unitDao.getByPackId(packId)?.toDomain()

    suspend fun upsert(unit: UnitModel) = unitDao.upsert(unit.toEntity())

    suspend fun delete(unit: UnitModel) = unitDao.delete(unit.toEntity())
}
