package dev.kumbuka.app.data.repository

import dev.kumbuka.app.data.local.entity.AssessmentMarkEntity
import dev.kumbuka.app.data.local.entity.DeadlineEntity
import dev.kumbuka.app.data.local.entity.SessionEntity
import dev.kumbuka.app.data.local.entity.TopicEntity
import dev.kumbuka.app.data.local.entity.UnitEntity
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel

fun UnitEntity.toDomain(): UnitModel = UnitModel(
    id = id,
    code = code,
    title = title,
    packId = packId,
    packVersion = packVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun UnitModel.toEntity(): UnitEntity = UnitEntity(
    id = id,
    code = code,
    title = title,
    packId = packId,
    packVersion = packVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun TopicEntity.toDomain(): Topic = Topic(
    id = id,
    unitId = unitId,
    title = title,
    objective = objective,
    retrievalPrompt = retrievalPrompt,
    examWeight = examWeight,
    orderIndex = orderIndex,
    resourcePointers = resourcePointers,
    titleEditedLocally = titleEditedLocally,
    objectiveEditedLocally = objectiveEditedLocally,
    weightEditedLocally = weightEditedLocally,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Topic.toEntity(): TopicEntity = TopicEntity(
    id = id,
    unitId = unitId,
    title = title,
    objective = objective,
    retrievalPrompt = retrievalPrompt,
    examWeight = examWeight,
    orderIndex = orderIndex,
    resourcePointers = resourcePointers,
    titleEditedLocally = titleEditedLocally,
    objectiveEditedLocally = objectiveEditedLocally,
    weightEditedLocally = weightEditedLocally,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SessionEntity.toDomain(): Session = Session(
    id = id,
    topicId = topicId,
    startedAt = startedAt,
    endedAt = endedAt,
    plannedMinutes = plannedMinutes,
    actualSeconds = actualSeconds,
    confidenceBefore = confidenceBefore,
    confidenceAfter = confidenceAfter,
    wasDeferred = wasDeferred,
    updatedAt = updatedAt,
)

fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id,
    topicId = topicId,
    startedAt = startedAt,
    endedAt = endedAt,
    plannedMinutes = plannedMinutes,
    actualSeconds = actualSeconds,
    confidenceBefore = confidenceBefore,
    confidenceAfter = confidenceAfter,
    wasDeferred = wasDeferred,
    updatedAt = updatedAt,
)

fun DeadlineEntity.toDomain(topicIds: List<String>): Deadline = Deadline(
    id = id,
    unitId = unitId,
    title = title,
    date = date,
    kind = kind,
    topicIds = topicIds,
    updatedAt = updatedAt,
)

fun Deadline.toEntity(): DeadlineEntity = DeadlineEntity(
    id = id,
    unitId = unitId,
    title = title,
    date = date,
    kind = kind,
    updatedAt = updatedAt,
)

fun AssessmentMarkEntity.toDomain(topicIds: List<String>): AssessmentMark = AssessmentMark(
    id = id,
    unitId = unitId,
    score = score,
    outOf = outOf,
    kind = kind,
    date = date,
    topicIds = topicIds,
    updatedAt = updatedAt,
)

fun AssessmentMark.toEntity(): AssessmentMarkEntity = AssessmentMarkEntity(
    id = id,
    unitId = unitId,
    score = score,
    outOf = outOf,
    kind = kind,
    date = date,
    updatedAt = updatedAt,
)
