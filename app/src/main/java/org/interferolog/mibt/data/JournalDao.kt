// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM cycles WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveCycle(): Flow<CycleEntity?>

    @Query("SELECT * FROM cycles WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeCycle(): CycleEntity?

    @Query("SELECT * FROM cycles ORDER BY startedAt DESC LIMIT 1")
    suspend fun latestCycle(): CycleEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM cycles WHERE fullCycleCompleted = 1)")
    suspend fun hasCompletedFullCycle(): Boolean

    @Insert
    suspend fun insertCycle(cycle: CycleEntity): Long

    @Query("UPDATE cycles SET status = 'COMPLETED', completedAt = :at, fullCycleCompleted = :full WHERE id = :id")
    suspend fun completeCycle(id: Long, at: Long, full: Boolean)

    @Query("UPDATE cycles SET vacation = :vacation WHERE id = :id")
    suspend fun setVacation(id: Long, vacation: Boolean)

    @Insert
    suspend fun insertTest(test: PhaseTestEntity)

    @Query("SELECT * FROM phase_tests WHERE cycleId = :cycleId ORDER BY createdAt DESC LIMIT 1")
    fun observeTest(cycleId: Long): Flow<PhaseTestEntity?>

    @Query("SELECT * FROM phase_tests WHERE cycleId = :cycleId ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestTest(cycleId: Long): PhaseTestEntity?

    @Insert
    suspend fun insertAttempt(attempt: WeekAttemptEntity): Long

    @Query("SELECT * FROM week_attempts WHERE cycleId = :cycleId ORDER BY weekNumber, attempt")
    fun observeAttempts(cycleId: Long): Flow<List<WeekAttemptEntity>>

    @Query("SELECT * FROM week_attempts WHERE cycleId = :cycleId")
    suspend fun attempts(cycleId: Long): List<WeekAttemptEntity>

    @Query("UPDATE week_attempts SET status = 'REPEATED' WHERE id = :id")
    suspend fun markRepeated(id: Long)

    @Query("UPDATE week_attempts SET markerConfirmed = 1 WHERE id = :id")
    suspend fun confirmMarker(id: Long)

    @Query("UPDATE week_attempts SET status = 'COMPLETED', completedAt = :at WHERE id = :id")
    suspend fun completeAttempt(id: Long, at: Long)

    @Insert
    suspend fun insertCalibration(answer: CalibrationAnswerEntity): Long

    @Query("UPDATE calibration_answers SET text = :text WHERE id = :id")
    suspend fun updateCalibration(id: Long, text: String)

    @Query("SELECT * FROM calibration_answers WHERE attemptId = :attemptId")
    suspend fun calibration(attemptId: Long): List<CalibrationAnswerEntity>

    @Query("SELECT * FROM calibration_answers WHERE attemptId IN (:attemptIds)")
    fun observeCalibration(attemptIds: List<Long>): Flow<List<CalibrationAnswerEntity>>

    @Insert
    suspend fun insertDiary(entry: PhaseDiaryEntryEntity)

    @Query("DELETE FROM phase_diary WHERE id = :id")
    suspend fun deleteDiary(id: Long)

    @Query("SELECT * FROM phase_diary WHERE cycleId = :cycleId ORDER BY atMillis DESC")
    fun observeDiary(cycleId: Long): Flow<List<PhaseDiaryEntryEntity>>

    @Query("SELECT * FROM phase_diary WHERE cycleId = :cycleId ORDER BY atMillis")
    suspend fun diary(cycleId: Long): List<PhaseDiaryEntryEntity>

    @Insert
    suspend fun insertWell(entry: WellEntryEntity)

    @Query("DELETE FROM well_entries WHERE id = :id")
    suspend fun deleteWell(id: Long)

    @Query("SELECT * FROM well_entries WHERE cycleId = :cycleId ORDER BY atMillis DESC")
    fun observeWells(cycleId: Long): Flow<List<WellEntryEntity>>

    @Query("SELECT * FROM well_entries WHERE cycleId = :cycleId ORDER BY atMillis")
    suspend fun wells(cycleId: Long): List<WellEntryEntity>

    @Insert
    suspend fun insertSpend(entry: SpendEntryEntity): Long

    @Query("DELETE FROM spends WHERE id = :id")
    suspend fun deleteSpend(id: Long)

    @Query("DELETE FROM mirror_bills WHERE spendEntryId = :spendId")
    suspend fun deleteBillsForSpend(spendId: Long)

    @Query("SELECT * FROM spends WHERE cycleId = :cycleId ORDER BY atMillis DESC")
    fun observeSpends(cycleId: Long): Flow<List<SpendEntryEntity>>

    @Query("SELECT * FROM spends WHERE cycleId = :cycleId ORDER BY atMillis")
    suspend fun spends(cycleId: Long): List<SpendEntryEntity>

    @Insert
    suspend fun insertBill(bill: MirrorBillEntity): Long

    @Query("SELECT * FROM mirror_bills WHERE cycleId = :cycleId ORDER BY openedAt DESC")
    fun observeBills(cycleId: Long): Flow<List<MirrorBillEntity>>

    @Query("SELECT * FROM mirror_bills WHERE cycleId = :cycleId")
    suspend fun bills(cycleId: Long): List<MirrorBillEntity>

    @Query("UPDATE mirror_bills SET settledAt = :at WHERE id = :id")
    suspend fun settleBill(id: Long, at: Long)

    @Insert
    suspend fun insertSource(source: IncomeSourceEntity)

    @Update
    suspend fun updateSource(source: IncomeSourceEntity)

    @Query("DELETE FROM income_sources WHERE id = :id")
    suspend fun deleteSource(id: Long)

    @Query("SELECT * FROM income_sources WHERE cycleId = :cycleId ORDER BY id")
    fun observeSources(cycleId: Long): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE cycleId = :cycleId")
    suspend fun sources(cycleId: Long): List<IncomeSourceEntity>

    @Insert
    suspend fun insertRehearsal(entry: PriceRehearsalEntity)

    @Query("SELECT * FROM price_rehearsals WHERE cycleId = :cycleId ORDER BY createdAt DESC")
    fun observeRehearsals(cycleId: Long): Flow<List<PriceRehearsalEntity>>

    @Query("SELECT * FROM price_rehearsals WHERE cycleId = :cycleId")
    suspend fun rehearsals(cycleId: Long): List<PriceRehearsalEntity>

    @Insert
    suspend fun insertWork(item: WorkItemEntity)

    @Update
    suspend fun updateWork(item: WorkItemEntity)

    @Query("DELETE FROM work_items WHERE id = :id")
    suspend fun deleteWork(id: Long)

    @Query("SELECT * FROM work_items WHERE cycleId = :cycleId ORDER BY id")
    fun observeWork(cycleId: Long): Flow<List<WorkItemEntity>>

    @Query("SELECT * FROM work_items WHERE cycleId = :cycleId")
    suspend fun work(cycleId: Long): List<WorkItemEntity>

    @Insert
    suspend fun insertHour(entry: HourLogEntity)

    @Query("SELECT * FROM hour_logs WHERE cycleId = :cycleId ORDER BY day")
    fun observeHours(cycleId: Long): Flow<List<HourLogEntity>>

    @Query("SELECT * FROM hour_logs WHERE cycleId = :cycleId")
    suspend fun hours(cycleId: Long): List<HourLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFamily(plan: FamilyPlanEntity)

    @Query("SELECT * FROM family_plans WHERE cycleId = :cycleId")
    fun observeFamily(cycleId: Long): Flow<FamilyPlanEntity?>

    @Query("SELECT * FROM family_plans WHERE cycleId = :cycleId")
    suspend fun family(cycleId: Long): FamilyPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRest(rest: RestMassEntity)

    @Query("SELECT * FROM rest_mass WHERE cycleId = :cycleId")
    fun observeRest(cycleId: Long): Flow<RestMassEntity?>

    @Query("SELECT * FROM rest_mass WHERE cycleId = :cycleId")
    suspend fun rest(cycleId: Long): RestMassEntity?

    @Insert
    suspend fun insertDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebt(id: Long)

    @Query("SELECT * FROM debts WHERE cycleId = :cycleId ORDER BY id")
    fun observeDebts(cycleId: Long): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE cycleId = :cycleId")
    suspend fun debts(cycleId: Long): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrisis(map: CrisisMapEntity)

    @Query("SELECT * FROM crisis_maps WHERE cycleId = :cycleId")
    fun observeCrisis(cycleId: Long): Flow<CrisisMapEntity?>

    @Query("SELECT * FROM crisis_maps WHERE cycleId = :cycleId")
    suspend fun crisis(cycleId: Long): CrisisMapEntity?

    @Insert
    suspend fun insertQuarantine(entry: QuarantineEntity)

    @Query("SELECT * FROM quarantines WHERE cycleId = :cycleId ORDER BY startedAt DESC")
    fun observeQuarantines(cycleId: Long): Flow<List<QuarantineEntity>>

    @Query("SELECT * FROM quarantines WHERE cycleId = :cycleId")
    suspend fun quarantines(cycleId: Long): List<QuarantineEntity>

    @Insert
    suspend fun insertTranche(entry: TrancheEntity)

    @Query("SELECT * FROM tranches WHERE cycleId = :cycleId ORDER BY atMillis DESC")
    fun observeTranches(cycleId: Long): Flow<List<TrancheEntity>>

    @Query("SELECT * FROM tranches WHERE cycleId = :cycleId")
    suspend fun tranches(cycleId: Long): List<TrancheEntity>

    @Insert
    suspend fun insertBranch(entry: FanBranchEntity): Long

    @Query("UPDATE fan_branches SET closed = 1 WHERE id = :id")
    suspend fun closeBranch(id: Long)

    @Query("SELECT * FROM fan_branches WHERE cycleId = :cycleId ORDER BY id")
    fun observeBranches(cycleId: Long): Flow<List<FanBranchEntity>>

    @Query("SELECT * FROM fan_branches WHERE id = :id")
    suspend fun branch(id: Long): FanBranchEntity?

    @Query("SELECT * FROM fan_branches WHERE cycleId = :cycleId")
    suspend fun branches(cycleId: Long): List<FanBranchEntity>

    @Insert
    suspend fun insertChannel(entry: FanChannelEntity)

    @Query("SELECT * FROM fan_channels WHERE cycleId = :cycleId ORDER BY echelon, slot")
    fun observeChannels(cycleId: Long): Flow<List<FanChannelEntity>>

    @Query("SELECT * FROM fan_channels WHERE cycleId = :cycleId")
    suspend fun channels(cycleId: Long): List<FanChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPassport(passport: InvestmentPassportEntity)

    @Query("SELECT * FROM passports WHERE cycleId = :cycleId")
    fun observePassport(cycleId: Long): Flow<InvestmentPassportEntity?>

    @Query("SELECT * FROM passports WHERE cycleId = :cycleId")
    suspend fun passport(cycleId: Long): InvestmentPassportEntity?

    @Insert
    suspend fun insertPerson(person: ClusterPersonEntity)

    @Query("SELECT * FROM cluster_people WHERE cycleId = :cycleId ORDER BY id")
    fun observePeople(cycleId: Long): Flow<List<ClusterPersonEntity>>

    @Query("SELECT * FROM cluster_people WHERE cycleId = :cycleId")
    suspend fun people(cycleId: Long): List<ClusterPersonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosition(audit: PositionAuditEntity)

    @Query("SELECT * FROM position_audits WHERE cycleId = :cycleId")
    fun observePosition(cycleId: Long): Flow<PositionAuditEntity?>

    @Query("SELECT * FROM position_audits WHERE cycleId = :cycleId")
    suspend fun position(cycleId: Long): PositionAuditEntity?

    @Insert
    suspend fun insertRing(ring: MeaningRingEntity)

    @Update
    suspend fun updateRing(ring: MeaningRingEntity)

    @Query("SELECT * FROM meaning_rings WHERE cycleId = :cycleId ORDER BY ring")
    fun observeRings(cycleId: Long): Flow<List<MeaningRingEntity>>

    @Query("SELECT * FROM meaning_rings WHERE cycleId = :cycleId")
    suspend fun rings(cycleId: Long): List<MeaningRingEntity>

    @Insert
    suspend fun insertMaintenance(entry: MaintenanceLogEntity)

    @Query("SELECT * FROM maintenance_logs WHERE cycleId = :cycleId ORDER BY atMillis DESC")
    fun observeMaintenance(cycleId: Long): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_logs WHERE cycleId = :cycleId")
    suspend fun maintenance(cycleId: Long): List<MaintenanceLogEntity>

    @Insert
    suspend fun insertMarker(entry: MarkerCheckEntity)

    @Query("SELECT * FROM marker_checks WHERE cycleId = :cycleId ORDER BY atMillis DESC")
    fun observeMarkers(cycleId: Long): Flow<List<MarkerCheckEntity>>

    @Query("SELECT * FROM marker_checks WHERE cycleId = :cycleId")
    suspend fun markers(cycleId: Long): List<MarkerCheckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(plan: ReminderPlanEntity)

    @Query("SELECT * FROM reminder_plans WHERE id = 1")
    fun observeReminder(): Flow<ReminderPlanEntity?>

    @Query("SELECT * FROM reminder_plans WHERE id = 1")
    suspend fun reminder(): ReminderPlanEntity?
}
