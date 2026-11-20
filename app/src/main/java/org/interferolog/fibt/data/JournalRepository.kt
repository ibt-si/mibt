package org.interferolog.fibt.data

import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.interferolog.fibt.domain.Letter
import org.interferolog.fibt.domain.PhaseAnswers
import org.interferolog.fibt.domain.PlannedRoute
import org.interferolog.fibt.domain.NoiseBooking
import org.interferolog.fibt.domain.bookNoise
import org.interferolog.fibt.domain.planRoute

data class JournalUi(
    val cycle: CycleEntity? = null,
    val test: PhaseTestEntity? = null,
    val attempts: List<WeekAttemptEntity> = emptyList(),
    val calibration: List<CalibrationAnswerEntity> = emptyList(),
    val diary: List<PhaseDiaryEntryEntity> = emptyList(),
    val wells: List<WellEntryEntity> = emptyList(),
    val spends: List<SpendEntryEntity> = emptyList(),
    val bills: List<MirrorBillEntity> = emptyList(),
    val sources: List<IncomeSourceEntity> = emptyList(),
    val rehearsals: List<PriceRehearsalEntity> = emptyList(),
    val work: List<WorkItemEntity> = emptyList(),
    val hours: List<HourLogEntity> = emptyList(),
    val family: FamilyPlanEntity? = null,
    val rest: RestMassEntity? = null,
    val debts: List<DebtEntity> = emptyList(),
    val crisis: CrisisMapEntity? = null,
    val quarantines: List<QuarantineEntity> = emptyList(),
    val tranches: List<TrancheEntity> = emptyList(),
    val branches: List<FanBranchEntity> = emptyList(),
    val channels: List<FanChannelEntity> = emptyList(),
    val passport: InvestmentPassportEntity? = null,
    val people: List<ClusterPersonEntity> = emptyList(),
    val position: PositionAuditEntity? = null,
    val rings: List<MeaningRingEntity> = emptyList(),
    val maintenance: List<MaintenanceLogEntity> = emptyList(),
    val markers: List<MarkerCheckEntity> = emptyList(),
    val reminders: ReminderPlanEntity? = null,
    val fullCycleAlreadyCompleted: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalRepository(private val db: FibtDatabase) {
    private val dao = db.journal()

    fun observe(): Flow<JournalUi> = combine(
        dao.observeActiveCycle(),
        dao.observeReminder(),
    ) { cycle, reminders -> cycle to reminders }
        .flatMapLatest { (cycle, reminders) ->
            if (cycle == null) {
                flowOf(JournalUi(reminders = reminders))
            } else {
                observeCycle(cycle, reminders)
            }
        }

    private fun observeCycle(cycle: CycleEntity, reminders: ReminderPlanEntity?): Flow<JournalUi> {
        val id = cycle.id
        val head = combine(
            dao.observeTest(id),
            dao.observeAttempts(id),
            dao.observeDiary(id),
            dao.observeWells(id),
            dao.observeSpends(id),
        ) { test, attempts, diary, wells, spends ->
            listOf(test, attempts, diary, wells, spends)
        }
        val money = combine(
            dao.observeBills(id),
            dao.observeSources(id),
            dao.observeRehearsals(id),
            dao.observeWork(id),
            dao.observeHours(id),
        ) { bills, sources, rehearsals, work, hours ->
            listOf(bills, sources, rehearsals, work, hours)
        }
        val buffer = combine(
            dao.observeFamily(id),
            dao.observeRest(id),
            dao.observeDebts(id),
            dao.observeCrisis(id),
            dao.observeQuarantines(id),
        ) { family, rest, debts, crisis, quarantines ->
            listOf(family, rest, debts, crisis, quarantines)
        }
        val later = combine(
            dao.observeTranches(id),
            dao.observeBranches(id),
            dao.observeChannels(id),
            dao.observePassport(id),
            dao.observePeople(id),
        ) { tranches, branches, channels, passport, people ->
            listOf(tranches, branches, channels, passport, people)
        }
        val tail = combine(
            dao.observePosition(id),
            dao.observeRings(id),
            dao.observeMaintenance(id),
            dao.observeMarkers(id),
        ) { position, rings, maintenance, markers ->
            listOf(position, rings, maintenance, markers)
        }
        return combine(head, money, buffer, later, tail) { headPart, moneyPart, bufferPart, laterPart, tailPart ->
            val (test, attempts, diary, wells, spends) = headPart
            val (bills, sources, rehearsals, work, hours) = moneyPart
            val (family, rest, debts, crisis, quarantines) = bufferPart
            val (tranches, branches, channels, passport, people) = laterPart
            val (position, rings, maintenance, markers) = tailPart
            @Suppress("UNCHECKED_CAST")
            JournalUi(
                cycle = cycle,
                test = test as PhaseTestEntity?,
                attempts = attempts as List<WeekAttemptEntity>,
                diary = diary as List<PhaseDiaryEntryEntity>,
                wells = wells as List<WellEntryEntity>,
                spends = spends as List<SpendEntryEntity>,
                bills = bills as List<MirrorBillEntity>,
                sources = sources as List<IncomeSourceEntity>,
                rehearsals = rehearsals as List<PriceRehearsalEntity>,
                work = work as List<WorkItemEntity>,
                hours = hours as List<HourLogEntity>,
                family = family as FamilyPlanEntity?,
                rest = rest as RestMassEntity?,
                debts = debts as List<DebtEntity>,
                crisis = crisis as CrisisMapEntity?,
                quarantines = quarantines as List<QuarantineEntity>,
                tranches = tranches as List<TrancheEntity>,
                branches = branches as List<FanBranchEntity>,
                channels = channels as List<FanChannelEntity>,
                passport = passport as InvestmentPassportEntity?,
                people = people as List<ClusterPersonEntity>,
                position = position as PositionAuditEntity?,
                rings = rings as List<MeaningRingEntity>,
                maintenance = maintenance as List<MaintenanceLogEntity>,
                markers = markers as List<MarkerCheckEntity>,
                reminders = reminders,
            )
        }.flatMapLatest { partial ->
            val ids = partial.attempts.map { it.id }
            val calibration = if (ids.isEmpty()) flowOf(emptyList()) else dao.observeCalibration(ids)
            calibration.combine(flowOf(partial)) { answers, base ->
                base.copy(calibration = answers)
            }
        }
    }

    suspend fun fullCycleAlreadyCompleted(): Boolean = dao.hasCompletedFullCycle()

    suspend fun startCycle(answers: PhaseAnswers, durationMillis: Long, now: Long): PlannedRoute {
        val route = planRoute(answers, dao.hasCompletedFullCycle())
        val fullBefore = dao.hasCompletedFullCycle()
        val cycleId = dao.insertCycle(
            CycleEntity(
                startedAt = now,
                routeKind = route.kind.name,
                weekOrder = route.weeks.joinToString(","),
                status = "ACTIVE",
                fullCycleCompleted = fullBefore,
            ),
        )
        dao.insertTest(
            PhaseTestEntity(
                cycleId = cycleId,
                createdAt = now,
                moneyCoversMonth = answers.moneyCoversMonth,
                noOverdue = answers.noOverdue,
                answers = answers.items.joinToString(",") { it.name },
                durationMillis = durationMillis,
            ),
        )
        dao.insertAttempt(
            WeekAttemptEntity(
                cycleId = cycleId,
                weekNumber = route.weeks.first(),
                attempt = 1,
                status = "ACTIVE",
                startedAt = now,
            ),
        )
        return route
    }

    suspend fun saveCalibration(attemptId: Long, index: Int, text: String) {
        val existing = dao.calibration(attemptId).firstOrNull { it.questionIndex == index }
        if (existing == null) {
            dao.insertCalibration(CalibrationAnswerEntity(attemptId = attemptId, questionIndex = index, text = text))
        } else {
            dao.updateCalibration(existing.id, text)
        }
    }

    suspend fun confirmMarker(attemptId: Long) = dao.confirmMarker(attemptId)

    suspend fun repeatWeek(attempt: WeekAttemptEntity, now: Long) {
        db.withTransaction {
            dao.markRepeated(attempt.id)
            dao.insertAttempt(
                WeekAttemptEntity(
                    cycleId = attempt.cycleId,
                    weekNumber = attempt.weekNumber,
                    attempt = attempt.attempt + 1,
                    status = "ACTIVE",
                    startedAt = now,
                ),
            )
        }
    }

    suspend fun advance(cycle: CycleEntity, attempt: WeekAttemptEntity, now: Long): Boolean {
        val answers = dao.calibration(attempt.id)
        val written = (0..2).all { index -> answers.any { it.questionIndex == index && it.text.isNotBlank() } }
        if (!attempt.markerConfirmed || !written) return false
        db.withTransaction {
            dao.completeAttempt(attempt.id, now)
            val order = cycle.weekOrder.split(",").map { it.toInt() }
            val next = order.getOrNull(order.indexOf(attempt.weekNumber) + 1)
            if (next == null) {
                val full = cycle.routeKind == "BASIC" || cycle.routeKind == "EMERGENCY" || cycle.fullCycleCompleted
                dao.completeCycle(cycle.id, now, full)
            } else {
                dao.insertAttempt(
                    WeekAttemptEntity(
                        cycleId = cycle.id,
                        weekNumber = next,
                        attempt = 1,
                        status = "ACTIVE",
                        startedAt = now,
                    ),
                )
            }
        }
        return true
    }

    suspend fun saveDiary(entry: PhaseDiaryEntryEntity) = dao.insertDiary(entry)

    suspend fun saveWell(entry: WellEntryEntity) = dao.insertWell(entry)

    suspend fun saveSpend(
        cycleId: Long,
        atMillis: Long,
        node: String,
        amount: Long,
        note: String,
        cleanSheet: Boolean,
        canFundMirror: Boolean,
    ): Boolean {
        val booking = if (node == "U4") bookNoise(cleanSheet, canFundMirror) else NoiseBooking(true, false)
        if (!booking.savePurchase) return false
        db.withTransaction {
            val spendId = dao.insertSpend(
                SpendEntryEntity(
                    cycleId = cycleId,
                    atMillis = atMillis,
                    node = node,
                    amountKopecks = amount,
                    note = note,
                    cleanSheet = cleanSheet,
                ),
            )
            if (booking.openMirror) {
                dao.insertBill(
                    MirrorBillEntity(
                        cycleId = cycleId,
                        spendEntryId = spendId,
                        amountKopecks = amount,
                        openedAt = atMillis,
                    ),
                )
            }
        }
        return true
    }

    suspend fun settleBill(bill: MirrorBillEntity, now: Long) {
        db.withTransaction {
            dao.settleBill(bill.id, now)
            dao.insertSpend(
                SpendEntryEntity(
                    cycleId = bill.cycleId,
                    atMillis = now,
                    node = "U3",
                    amountKopecks = bill.amountKopecks,
                    note = "Перевод двойного тарифа",
                    cleanSheet = false,
                ),
            )
        }
    }

    suspend fun saveSource(source: IncomeSourceEntity) = dao.insertSource(source)
    suspend fun deleteSource(id: Long) = dao.deleteSource(id)
    suspend fun saveRehearsal(entry: PriceRehearsalEntity) = dao.insertRehearsal(entry)
    suspend fun saveWork(item: WorkItemEntity) = dao.insertWork(item)
    suspend fun saveHour(entry: HourLogEntity) = dao.insertHour(entry)
    suspend fun saveFamily(plan: FamilyPlanEntity) = dao.upsertFamily(plan)
    suspend fun saveRest(rest: RestMassEntity) = dao.upsertRest(rest)
    suspend fun saveDebt(debt: DebtEntity) = dao.insertDebt(debt)
    suspend fun deleteDebt(id: Long) = dao.deleteDebt(id)

    suspend fun withdraw(cycleId: Long, amount: Long, now: Long): String? {
        val active = dao.quarantines(cycleId).any { it.kind == "H72" && it.endsAt > now }
        if (active) return "Карантин ещё идёт. Изъятие из массы покоя подождёт."
        val rest = dao.rest(cycleId) ?: return "Сначала запишите массу покоя."
        if (amount > rest.balance) return "В массе покоя меньше этой суммы."
        dao.upsertRest(rest.copy(balance = rest.balance - amount))
        return null
    }

    suspend fun applyFirstCut(cycleId: Long, income: Long) {
        val rest = dao.rest(cycleId) ?: RestMassEntity(cycleId = cycleId)
        val cut = (income.toDouble() * rest.firstCutPercent / 100.0).toLong()
        dao.upsertRest(rest.copy(balance = rest.balance + cut, accountOpened = true))
    }

    suspend fun saveCrisis(map: CrisisMapEntity) = dao.upsertCrisis(map)

    suspend fun startQuarantine(entry: QuarantineEntity) = dao.insertQuarantine(entry)

    suspend fun saveTranche(entry: TrancheEntity) = dao.insertTranche(entry)

    suspend fun saveBranch(entry: FanBranchEntity) = dao.insertBranch(entry)

    suspend fun closeBranch(id: Long, now: Long): Boolean {
        val branch = dao.branch(id) ?: return false
        val review = branch.reviewAt
        if (review != null && now < review) return false
        dao.closeBranch(id)
        return true
    }

    suspend fun saveChannel(entry: FanChannelEntity) = dao.insertChannel(entry)
    suspend fun savePassport(passport: InvestmentPassportEntity) = dao.upsertPassport(passport)
    suspend fun savePerson(person: ClusterPersonEntity) = dao.insertPerson(person)
    suspend fun savePosition(audit: PositionAuditEntity) = dao.upsertPosition(audit)
    suspend fun saveRing(ring: MeaningRingEntity) = dao.insertRing(ring)
    suspend fun logMaintenance(entry: MaintenanceLogEntity) = dao.insertMaintenance(entry)
    suspend fun setMarker(entry: MarkerCheckEntity) = dao.insertMarker(entry)
    suspend fun saveReminder(plan: ReminderPlanEntity) = dao.upsertReminder(plan)
    suspend fun reminder(): ReminderPlanEntity? = dao.reminder()
    suspend fun setVacation(cycleId: Long, vacation: Boolean) = dao.setVacation(cycleId, vacation)

    suspend fun snapshot(cycleId: Long): TelemetrySnapshot {
        val cycle = dao.activeCycle()?.takeIf { it.id == cycleId } ?: dao.latestCycle()?.takeIf { it.id == cycleId }
        return TelemetrySnapshot(
            cycle = cycle,
            test = dao.latestTest(cycleId),
            attempts = dao.attempts(cycleId),
            calibration = dao.attempts(cycleId).flatMap { dao.calibration(it.id) },
            diary = dao.diary(cycleId),
            wells = dao.wells(cycleId),
            spends = dao.spends(cycleId),
            bills = dao.bills(cycleId),
            sources = dao.sources(cycleId),
            rehearsals = dao.rehearsals(cycleId),
            work = dao.work(cycleId),
            hours = dao.hours(cycleId),
            family = dao.family(cycleId),
            rest = dao.rest(cycleId),
            debts = dao.debts(cycleId),
            crisis = dao.crisis(cycleId),
            quarantines = dao.quarantines(cycleId),
            tranches = dao.tranches(cycleId),
            branches = dao.branches(cycleId),
            channels = dao.channels(cycleId),
            passport = dao.passport(cycleId),
            people = dao.people(cycleId),
            position = dao.position(cycleId),
            rings = dao.rings(cycleId),
            maintenance = dao.maintenance(cycleId),
            markers = dao.markers(cycleId),
        )
    }

    suspend fun exportText(cycleId: Long): String = exportTelemetry(snapshot(cycleId))
}

fun parseLetters(raw: String): List<Letter> = raw.split(",").map { Letter.valueOf(it) }
