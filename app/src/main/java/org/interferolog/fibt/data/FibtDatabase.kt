package org.interferolog.fibt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CycleEntity::class,
        PhaseTestEntity::class,
        WeekAttemptEntity::class,
        CalibrationAnswerEntity::class,
        PhaseDiaryEntryEntity::class,
        WellEntryEntity::class,
        SpendEntryEntity::class,
        MirrorBillEntity::class,
        IncomeSourceEntity::class,
        PriceRehearsalEntity::class,
        WorkItemEntity::class,
        HourLogEntity::class,
        FamilyPlanEntity::class,
        RestMassEntity::class,
        DebtEntity::class,
        CrisisMapEntity::class,
        QuarantineEntity::class,
        TrancheEntity::class,
        FanBranchEntity::class,
        FanChannelEntity::class,
        InvestmentPassportEntity::class,
        ClusterPersonEntity::class,
        PositionAuditEntity::class,
        MeaningRingEntity::class,
        MaintenanceLogEntity::class,
        MarkerCheckEntity::class,
        ReminderPlanEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class FibtDatabase : RoomDatabase() {
    abstract fun journal(): JournalDao

    companion object {
        @Volatile private var instance: FibtDatabase? = null

        fun get(context: Context): FibtDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FibtDatabase::class.java,
                    "fibt.db",
                ).build().also { instance = it }
            }
        }
    }
}
