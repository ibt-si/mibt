// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val completedAt: Long? = null,
    val routeKind: String,
    val weekOrder: String,
    val status: String,
    val fullCycleCompleted: Boolean,
    val vacation: Boolean = false,
    @ColumnInfo(defaultValue = "'RUB'") val currency: String = "RUB",
)

@Entity(
    tableName = "phase_tests",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class PhaseTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val createdAt: Long,
    val moneyCoversMonth: Boolean,
    val noOverdue: Boolean,
    val answers: String,
    val durationMillis: Long,
)

@Entity(
    tableName = "week_attempts",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class WeekAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val weekNumber: Int,
    val attempt: Int,
    val status: String,
    val markerConfirmed: Boolean = false,
    val startedAt: Long,
    val completedAt: Long? = null,
)

@Entity(
    tableName = "calibration_answers",
    foreignKeys = [ForeignKey(entity = WeekAttemptEntity::class, parentColumns = ["id"], childColumns = ["attemptId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("attemptId")],
)
data class CalibrationAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attemptId: Long,
    val questionIndex: Int,
    val text: String,
)

@Entity(
    tableName = "phase_diary",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class PhaseDiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val atMillis: Long,
    val amountKopecks: Long?,
    val context: String,
    val bodyMarker: String,
    val impulse: String,
    val fact: String,
)

@Entity(
    tableName = "well_entries",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class WellEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val atMillis: Long,
    val situation: String,
    val amountKopecks: Long?,
    val question: String,
    val spasm: String,
    val pauseEffect: String,
)

@Entity(
    tableName = "spends",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class SpendEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val atMillis: Long,
    val node: String,
    val amountKopecks: Long,
    val note: String,
    val cleanSheet: Boolean,
)

@Entity(
    tableName = "mirror_bills",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class MirrorBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val spendEntryId: Long?,
    val amountKopecks: Long,
    val openedAt: Long,
    val settledAt: Long? = null,
)

@Entity(
    tableName = "income_sources",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class IncomeSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val name: String,
    val netIncomeKopecks: Long,
    val compensatoryNoiseKopecks: Long,
    val contractHours: Double,
    val frictionHours: Double,
    val incomeShare: Double,
    val hoursShare: Double,
    val frictionScore: Int,
    @ColumnInfo(defaultValue = "''") val decision: String = "",
)

@Entity(
    tableName = "price_rehearsals",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class PriceRehearsalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val filePath: String,
    val silenceHeld: Boolean,
    val outcome: String,
    val createdAt: Long,
)

@Entity(
    tableName = "work_items",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class WorkItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val name: String,
    val resultShare: Double,
    val hours: Double,
    val hoursShare: Double,
    val frictionScore: Int,
    val mark: String,
    @ColumnInfo(defaultValue = "''") val decision: String = "",
)

@Entity(
    tableName = "hour_logs",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class HourLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val day: String,
    val hours: Double,
    val note: String,
)

@Entity(tableName = "family_plans")
data class FamilyPlanEntity(
    @PrimaryKey val cycleId: Long,
    val income1: Long = 0,
    val income2: Long = 0,
    val foundation: Long = 0,
    val targetFund: Long = 0,
    val personal1: Long = 0,
    val personal2: Long = 0,
    val threshold: Long = 0,
    val phrase: String = "Я сейчас в сжатии",
    val saturdayNote: String = "",
)

@Entity(tableName = "rest_mass")
data class RestMassEntity(
    @PrimaryKey val cycleId: Long,
    val support: Long = 0,
    val tool: Long = 0,
    val obligations: Long = 0,
    val balance: Long = 0,
    val profile: String = "HIRE",
    val firstCutPercent: Double = 5.0,
    val accountOpened: Boolean = false,
    val strategy: String = "AVALANCHE",
)

@Entity(
    tableName = "debts",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val name: String,
    val balance: Long,
    val ratePercent: Double,
    val minPayment: Long,
    val extra: Long,
)

@Entity(tableName = "crisis_maps")
data class CrisisMapEntity(
    @PrimaryKey val cycleId: Long,
    val accessible: Long = 0,
    val staffMinimum: Long = 0,
    val emergencyMinimum: Long = 0,
    val cutList: String = "",
    val supportRing: String = "",
    val offers: String = "",
    val drillDone: Boolean = false,
)

@Entity(
    tableName = "quarantines",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class QuarantineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val kind: String,
    val startedAt: Long,
    val endsAt: Long,
    val reason: String,
)

@Entity(
    tableName = "tranches",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class TrancheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val atMillis: Long,
    val amount: Long,
    val runwayMonths: Double,
)

@Entity(
    tableName = "fan_branches",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class FanBranchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val echelon: String,
    val name: String,
    val hoursPerWeek: Double,
    val horizonDays: Int?,
    val reviewAt: Long?,
    val signals: String,
    val closed: Boolean,
)

@Entity(
    tableName = "fan_channels",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class FanChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val echelon: String,
    val slot: Int,
    val failurePoint: String,
)

@Entity(tableName = "passports")
data class InvestmentPassportEntity(
    @PrimaryKey val cycleId: Long,
    val massStatus: String = "",
    val contributionRule: String = "",
    val horizonRule: String = "",
    val allocation: String = "",
    val riskLimit: String = "",
    val stormRule: String = "",
    val hygiene: String = "",
    val accountBalance: Long = 0,
    val monthlyContribution: Long = 0,
    val annualRate: Double = 0.10,
    val years: Int = 20,
    val reinvestPercent: Double = 50.0,
    val supportPercent: Double = 30.0,
    val crystalPercent: Double = 20.0,
    val drawdownOk: Boolean = false,
)

@Entity(
    tableName = "cluster_people",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class ClusterPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val name: String,
    val role: String,
    val transparency: Int,
    val word: Int,
    val coherence: Int,
    val action: String,
)

@Entity(tableName = "position_audits")
data class PositionAuditEntity(
    @PrimaryKey val cycleId: Long,
    val craft: String = "",
    val surplus: String = "",
    val people: String = "",
    val horizon: String = "",
    val timeFeel: String = "",
    val answer: String = "",
)

@Entity(
    tableName = "meaning_rings",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class MeaningRingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val ring: Int,
    val what: String,
    val growthShare: Double,
    val hoursPerWeek: Double,
    val firstStone: String,
)

@Entity(
    tableName = "maintenance_logs",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class MaintenanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val rhythm: String,
    val atMillis: Long,
    val note: String,
)

@Entity(
    tableName = "marker_checks",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cycleId")],
)
data class MarkerCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val marker: Int,
    val marked: Boolean,
    val atMillis: Long,
)

@Entity(tableName = "reminder_plans")
data class ReminderPlanEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = true,
    val minute1: Int = 9 * 60,
    val minute2: Int = 14 * 60,
    val minute3: Int = 20 * 60,
)

/** App-wide preference for the currency of newly created cycles. */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val currencyCode: String,
)
