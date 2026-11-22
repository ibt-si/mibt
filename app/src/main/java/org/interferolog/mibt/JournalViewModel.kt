// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.interferolog.mibt.data.ClusterPersonEntity
import org.interferolog.mibt.data.CrisisMapEntity
import org.interferolog.mibt.data.DebtEntity
import org.interferolog.mibt.data.FamilyPlanEntity
import org.interferolog.mibt.data.FanBranchEntity
import org.interferolog.mibt.data.FanChannelEntity
import org.interferolog.mibt.data.MibtDatabase
import org.interferolog.mibt.data.HourLogEntity
import org.interferolog.mibt.data.IncomeSourceEntity
import org.interferolog.mibt.data.InvestmentPassportEntity
import org.interferolog.mibt.data.JournalRepository
import org.interferolog.mibt.data.JournalUi
import org.interferolog.mibt.data.MaintenanceLogEntity
import org.interferolog.mibt.data.MarkerCheckEntity
import org.interferolog.mibt.data.MeaningRingEntity
import org.interferolog.mibt.data.MirrorBillEntity
import org.interferolog.mibt.data.PhaseDiaryEntryEntity
import org.interferolog.mibt.data.PositionAuditEntity
import org.interferolog.mibt.data.PriceRehearsalEntity
import org.interferolog.mibt.data.QuarantineEntity
import org.interferolog.mibt.data.ReminderPlanEntity
import org.interferolog.mibt.data.RestMassEntity
import org.interferolog.mibt.data.TrancheEntity
import org.interferolog.mibt.data.WeekAttemptEntity
import org.interferolog.mibt.data.WellEntryEntity
import org.interferolog.mibt.data.WorkItemEntity
import org.interferolog.mibt.domain.Letter
import org.interferolog.mibt.domain.PhaseAnswers
import org.interferolog.mibt.reminder.ReminderScheduler

class JournalViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = JournalRepository(MibtDatabase.get(app))

    val ui = repo.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUi())

    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    private val _priorFullCycle = MutableStateFlow(false)
    val priorFullCycle = _priorFullCycle.asStateFlow()

    init {
        viewModelScope.launch { _priorFullCycle.value = repo.fullCycleAlreadyCompleted() }
    }

    fun clearNotice() {
        _notice.value = null
    }

    fun submitIntake(money: Boolean, noOverdue: Boolean, letters: List<Letter>, durationMillis: Long) {
        viewModelScope.launch {
            repo.startCycle(
                PhaseAnswers(money, noOverdue, letters),
                durationMillis,
                System.currentTimeMillis(),
            )
            _priorFullCycle.value = repo.fullCycleAlreadyCompleted()
            _notice.value = "Маршрут записан. Дальше — шаги текущей недели, затем бланк и расчёт."
        }
    }

    fun saveCalibration(attemptId: Long, index: Int, text: String) {
        viewModelScope.launch { repo.saveCalibration(attemptId, index, text) }
    }

    fun confirmMarker(attemptId: Long) {
        viewModelScope.launch { repo.confirmMarker(attemptId) }
    }

    fun repeatWeek(attempt: WeekAttemptEntity) {
        viewModelScope.launch { repo.repeatWeek(attempt, System.currentTimeMillis()) }
    }

    fun advance(attempt: WeekAttemptEntity) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch {
            val moved = repo.advance(cycle, attempt, System.currentTimeMillis())
            if (!moved) _notice.value = "Сначала три письменных ответа и подтверждение маркера."
        }
    }

    fun saveDiary(amount: Long?, context: String, body: String, impulse: String, fact: String) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch {
            repo.saveDiary(
                PhaseDiaryEntryEntity(
                    cycleId = cycle.id,
                    atMillis = System.currentTimeMillis(),
                    amountKopecks = amount,
                    context = context,
                    bodyMarker = body,
                    impulse = impulse,
                    fact = fact,
                ),
            )
        }
    }

    fun saveWell(situation: String, amount: Long?, question: String, spasm: String, effect: String) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch {
            repo.saveWell(
                WellEntryEntity(
                    cycleId = cycle.id,
                    atMillis = System.currentTimeMillis(),
                    situation = situation,
                    amountKopecks = amount,
                    question = question,
                    spasm = spasm,
                    pauseEffect = effect,
                ),
            )
        }
    }

    fun saveSpend(node: String, amount: Long, note: String, cleanSheet: Boolean, canFundMirror: Boolean) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch {
            val saved = repo.saveSpend(cycle.id, System.currentTimeMillis(), node, amount, note, cleanSheet, canFundMirror)
            if (!saved) _notice.value = "Покупки не будет: на двойную цену сейчас нет."
        }
    }

    fun settleBill(bill: MirrorBillEntity) {
        viewModelScope.launch { repo.settleBill(bill, System.currentTimeMillis()) }
    }

    fun saveSource(source: IncomeSourceEntity) {
        viewModelScope.launch { repo.saveSource(source) }
    }

    fun deleteSource(id: Long) {
        viewModelScope.launch { repo.deleteSource(id) }
    }

    fun saveRehearsal(path: String, silenceHeld: Boolean, outcome: String) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch {
            repo.saveRehearsal(
                PriceRehearsalEntity(
                    cycleId = cycle.id,
                    filePath = path,
                    silenceHeld = silenceHeld,
                    outcome = outcome,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun saveWork(item: WorkItemEntity) {
        viewModelScope.launch { repo.saveWork(item) }
    }

    fun saveHour(day: String, hours: Double, note: String) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch { repo.saveHour(HourLogEntity(cycleId = cycle.id, day = day, hours = hours, note = note)) }
    }

    fun saveFamily(plan: FamilyPlanEntity) {
        viewModelScope.launch { repo.saveFamily(plan) }
    }

    fun saveRest(rest: RestMassEntity) {
        viewModelScope.launch { repo.saveRest(rest) }
    }

    fun saveDebt(debt: DebtEntity) {
        viewModelScope.launch { repo.saveDebt(debt) }
    }

    fun deleteDebt(id: Long) {
        viewModelScope.launch { repo.deleteDebt(id) }
    }

    fun applyFirstCut(income: Long) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch { repo.applyFirstCut(cycle.id, income) }
    }

    fun withdraw(amount: Long) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch {
            val blocked = repo.withdraw(cycle.id, amount, System.currentTimeMillis())
            if (blocked != null) _notice.value = blocked
        }
    }

    fun saveCrisis(map: CrisisMapEntity) {
        viewModelScope.launch { repo.saveCrisis(map) }
    }

    fun startQuarantine(kind: String, hours: Int, reason: String) {
        val cycle = ui.value.cycle ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repo.startQuarantine(
                QuarantineEntity(
                    cycleId = cycle.id,
                    kind = kind,
                    startedAt = now,
                    endsAt = now + hours * 3_600_000L,
                    reason = reason,
                ),
            )
        }
    }

    fun saveTranche(amount: Long, runway: Double) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch {
            repo.saveTranche(
                TrancheEntity(
                    cycleId = cycle.id,
                    atMillis = System.currentTimeMillis(),
                    amount = amount,
                    runwayMonths = runway,
                ),
            )
        }
    }

    fun saveBranch(branch: FanBranchEntity) {
        viewModelScope.launch { repo.saveBranch(branch) }
    }

    fun closeBranch(id: Long) {
        viewModelScope.launch {
            val closed = repo.closeBranch(id, System.currentTimeMillis())
            if (!closed) _notice.value = "Горизонт тишины ещё не дошёл до даты ревизии."
        }
    }

    fun saveChannel(channel: FanChannelEntity) {
        viewModelScope.launch { repo.saveChannel(channel) }
    }

    fun savePassport(passport: InvestmentPassportEntity) {
        viewModelScope.launch { repo.savePassport(passport) }
    }

    fun savePerson(person: ClusterPersonEntity) {
        viewModelScope.launch { repo.savePerson(person) }
    }

    fun savePosition(audit: PositionAuditEntity) {
        viewModelScope.launch { repo.savePosition(audit) }
    }

    fun saveRing(ring: MeaningRingEntity) {
        viewModelScope.launch { repo.saveRing(ring) }
    }

    fun logMaintenance(rhythm: String, note: String) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch {
            repo.logMaintenance(
                MaintenanceLogEntity(
                    cycleId = cycle.id,
                    rhythm = rhythm,
                    atMillis = System.currentTimeMillis(),
                    note = note,
                ),
            )
        }
    }

    fun setMarker(marker: Int, marked: Boolean) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch {
            repo.setMarker(
                MarkerCheckEntity(
                    cycleId = cycle.id,
                    marker = marker,
                    marked = marked,
                    atMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun saveReminders(plan: ReminderPlanEntity) {
        viewModelScope.launch {
            repo.saveReminder(plan)
            ReminderScheduler.reschedule(getApplication())
        }
    }

    fun setVacation(vacation: Boolean) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch { repo.setVacation(cycle.id, vacation) }
    }

    fun exportTo(uri: Uri) {
        val cycle = ui.value.cycle ?: return
        viewModelScope.launch {
            val text = repo.exportText(cycle.id)
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
            }
        }
    }
}

fun JournalUi.activeAttempt(week: Int? = null): WeekAttemptEntity? {
    val cycle = cycle ?: return null
    val order = cycle.weekOrder.split(",").mapNotNull { it.toIntOrNull() }
    val completed = attempts.filter { it.status == "COMPLETED" }.map { it.weekNumber }.toSet()
    val current = week ?: order.firstOrNull { it !in completed } ?: return null
    return attempts.filter { it.weekNumber == current && it.status == "ACTIVE" }.maxByOrNull { it.attempt }
}

fun JournalUi.currentWeekNumber(): Int? {
    val cycle = cycle ?: return null
    val order = cycle.weekOrder.split(",").mapNotNull { it.toIntOrNull() }
    val completed = attempts.filter { it.status == "COMPLETED" }.map { it.weekNumber }.toSet()
    return order.firstOrNull { it !in completed }
}
