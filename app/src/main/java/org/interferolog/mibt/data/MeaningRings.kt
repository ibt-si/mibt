// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

data class RingSavePlan(
    val insert: MeaningRingEntity? = null,
    val update: MeaningRingEntity? = null,
)

/** One row per cycle and ring index: the highest id is the latest save. */
fun latestRings(rings: List<MeaningRingEntity>): List<MeaningRingEntity> =
    rings
        .groupBy { it.cycleId to it.ring }
        .values
        .mapNotNull { rows -> rows.maxByOrNull { it.id } }
        .sortedWith(compareBy({ it.cycleId }, { it.ring }))

/**
 * Same cycle and ring index updates the latest row. A new index is inserted.
 * The plan does not allocate an id; the caller inserts with id 0.
 */
fun planRingSave(existing: List<MeaningRingEntity>, incoming: MeaningRingEntity): RingSavePlan {
    val current = latestRings(existing).firstOrNull { it.cycleId == incoming.cycleId && it.ring == incoming.ring }
    return if (current == null) {
        RingSavePlan(insert = incoming.copy(id = 0))
    } else {
        RingSavePlan(update = incoming.copy(id = current.id))
    }
}

suspend fun saveMeaningRing(
    existing: List<MeaningRingEntity>,
    insert: suspend (MeaningRingEntity) -> Unit,
    update: suspend (MeaningRingEntity) -> Unit,
    incoming: MeaningRingEntity,
) {
    val plan = planRingSave(existing, incoming)
    when {
        plan.update != null -> update(plan.update)
        plan.insert != null -> insert(plan.insert)
    }
}
