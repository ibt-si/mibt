// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlankCanonTest {
    @Test
    fun structuralInvariants() {
        val blanks = BlankCanon.all
        assertEquals(listOf("1", "2а", "2", "3", "3н"), blanks.map { it.number })
        assertEquals(0x0430, blanks.first { it.number.startsWith("2") }.number[1].code)
        blanks.forEach { blank ->
            assertTrue(blank.title.matches(Regex("^Бланк \\S+\\. .+")))
            assertTrue(blank.title.startsWith("Бланк ${blank.number}."))
            texts(blank).forEach { cell ->
                assertFalse(cell, cell.contains("\\."))
                assertFalse(cell, cell.contains("\\="))
                assertFalse(cell, cell.contains("**"))
            }
            blank.referenceTables.forEach { table ->
                assertTrue(table.header.isNotEmpty())
                table.rows.forEach { row ->
                    assertEquals(table.header.size, row.size)
                }
            }
            if (blank.entriesTitle.isNotEmpty()) {
                assertFalse(blank.entriesTitle.endsWith(":"))
            }
            blank.examples.forEach { row ->
                assertEquals(blank.columns.size, row.size)
            }
        }
        assertTrue(BlankCanon.phaseDiary.examples.isNotEmpty())
        assertTrue(BlankCanon.pointC.examples.isNotEmpty())
        listOf(BlankCanon.holter, BlankCanon.sources, BlankCanon.work).forEach {
            assertTrue(it.examples.isEmpty())
        }
        val marker = blanks.first { it.number == "1" }.referenceTables.single()
        assertEquals(
            listOf(
                "Дыхание",
                "Тонус и опора",
                "Ощущение времени",
                "Ожидание",
                "Послевкусие через 24 часа",
            ),
            marker.rows.map { it.first() },
        )
    }

    @Test
    fun phaseDiaryMatchesJournalHeadingAndTables() {
        val blank = BlankCanon.phaseDiary
        assertEquals("Бланк 1. Дневник фазы", blank.title)
        assertEquals("Дневник", blank.entriesTitle)
        assertEquals(
            listOf(
                "Дата и время",
                "Контекст и сумма",
                "Телесный маркер за 3 секунды до оплаты",
                "Метка: П (пустота) / Н (наполненность)",
                "Факт без оценки",
            ),
            blank.columns,
        )
        val table = blank.referenceTables.single()
        assertEquals("Сверочная таблица телесных маркеров", table.title)
        assertEquals(
            listOf("Параметр", "Импульс из пустоты", "Действие из наполненности"),
            table.header,
        )
        assertEquals(
            listOf(
                listOf(
                    "Дыхание",
                    "Поверхностное, частое, задержка вдоха перед кнопкой",
                    "Ровное, глубокое, выдох длиннее вдоха",
                ),
                listOf(
                    "Тонус и опора",
                    "Внимание в голове и в экране, плечи подняты, стопы почти не чувствуют пола",
                    "Вес отчётливо в стопах, плечи опущены",
                ),
                listOf(
                    "Ощущение времени",
                    "Срочность: «надо брать прямо сейчас, потом буду жалеть»",
                    "Спокойная протяжённость: решение может подождать",
                ),
                listOf(
                    "Ожидание",
                    "Короткий «укол радости» от обладания",
                    "Спокойная польза от вещи или услуги",
                ),
                listOf(
                    "Послевкусие через 24 часа",
                    "Стыд, желание спрятать чек",
                    "Ровное удовлетворение",
                ),
            ),
            table.rows,
        )
        assertEquals(
            listOf(
                listOf(
                    "Пример: пн, 23:10",
                    "Корзина маркетплейса в кровати, 4 700 ₽",
                    "Усталость, телефон в руке на автомате",
                    "П",
                    "Корзина не оплачена, приложение закрыто до утра. Утром куплена одна позиция за 900 ₽",
                ),
                listOf(
                    "Пример: пт, 08:05",
                    "Платёж по кредиту, 18 000 ₽",
                    "Ровное дыхание, вес в стопах",
                    "Н",
                    "Обязательство внесено спокойно",
                ),
            ),
            blank.examples,
        )
    }

    @Test
    fun pointCMatchesJournalHeadingAndTables() {
        val blank = BlankCanon.pointC
        assertEquals("Бланк 2а. Колодец или Точка C", blank.title)
        assertEquals("Журнал спазмов", blank.entriesTitle)
        assertEquals(0x43, blank.title.last().code)
        assertEquals(
            listOf(
                "Дата и время",
                "Ситуация и сумма",
                "Вопрос в начале решения: колодец / Точка C",
                "Спазм: анестезия / паралич / демпинг / отсечка масштаба",
                "Что изменила пауза",
            ),
            blank.columns,
        )
        val table = blank.referenceTables.single()
        assertNull(table.title)
        assertEquals(listOf("Параметр", "Колодец нехватки", "Точка C"), table.header)
        assertEquals(
            listOf(
                listOf(
                    "Базовый вопрос",
                    "«Как бы мне здесь не потерять, не продешевить, успеть урвать?»",
                    "«Что здесь соответствует тому масштабу и качеству, которыми я уже являюсь?»",
                ),
                listOf(
                    "Маркер в теле",
                    "Дыхание в верхней части груди, зажатые плечи и челюсть, быстрая речь",
                    "Плотная опора в стопах, ровное дыхание, спокойный взгляд",
                ),
                listOf(
                    "Время",
                    "Спешка «надо решать сейчас» или паралич откладывания",
                    "Право на двухсекундную паузу и суточный перерыв",
                ),
                listOf(
                    "Видимый выбор",
                    "Одна колея: «у меня нет выбора»",
                    "Несколько вариантов на столе",
                ),
                listOf(
                    "Реакция на ошибку",
                    "Внутренний суд и новый срыв",
                    "Спокойная запись факта и следующий шаг",
                ),
            ),
            table.rows,
        )
        assertEquals(
            listOf(
                listOf(
                    "Пример",
                    "Ремонт подвески, счёт 45 000 ₽",
                    "Колодец",
                    "Паралич",
                    "После двух секунд в стопах — вопрос о гарантии и сроках вместо спора",
                ),
            ),
            blank.examples,
        )
        assertTrue(blank.examples.single().last().contains("—"))
    }

    @Test
    fun holterMatchesJournalHeadingColumnsAndSkipRule() {
        val blank = BlankCanon.holter
        assertEquals("Бланк 2. Холтер четырёх узлов", blank.title)
        assertEquals(0x0451, blank.title[blank.title.indexOf('ё')].code)
        assertEquals(
            listOf(
                "Дата",
                "Доход; Обз отдельно",
                "U₁ Опора",
                "U₂ Инструмент",
                "U₃ Рост",
                "U₄ Шум",
                "Двойной тариф",
                "η",
            ),
            blank.columns,
        )
        assertEquals(0x2081, blank.columns[2][1].code)
        assertEquals(0x03B7, blank.columns.last().single().code)
        assertEquals(
            listOf("День 1", "День 2", "День 3", "День 4", "День 5", "День 6", "День 7", "Итого за неделю"),
            blank.rowLabels,
        )
        assertEquals(
            "Правило пропусков: если вечер пропущен, примерная сумма вписывается в U₄, и запись спокойно продолжается с сегодняшнего вечера. Холтер из-за пропуска не бросается.",
            blank.notes.single(),
        )
    }

    @Test
    fun sourcesMatchJournalHeadingColumnsAndFrictionNote() {
        val blank = BlankCanon.sources
        assertEquals("Бланк 3. Индекс сопротивления источников", blank.title)
        assertEquals(
            listOf(
                "№",
                "Источник",
                "Чистый доход за месяц",
                "Dᵢ, % дохода",
                "Часы и hᵢ, % часов",
                "Sᵢ, ₽/ч",
                "Rᵢ, 1–10",
                "Fᵢ",
                "Зона и решение",
            ),
            blank.columns,
        )
        assertEquals(0x2116, blank.columns.first().single().code)
        assertEquals(0x1D62, blank.columns[3][1].code)
        assertEquals(0x2013, '–'.code)
        assertTrue(blank.columns[6].contains('–'))
        assertEquals(listOf("Итого"), blank.rowLabels)
        assertEquals(listOf("Итого", "100%", "Sср =", "Строк с Fᵢ ≥ 2:"), blank.totalLabels)
        assertEquals(0x0053, blank.totalLabels[2][0].code)
        assertEquals(0x0441, blank.totalLabels[2][1].code)
        assertEquals(
            "Балл трения Rᵢ ставится по телу: вспомнить последний звонок от этого источника. Если хочется отложить телефон и не брать — балл не ниже семи.",
            blank.notes.single(),
        )
        assertEquals(0x2014, blank.notes.single().first { it == '—' }.code)
    }

    @Test
    fun workMatchesJournalHeadingQuadrantsAndControlRule() {
        val blank = BlankCanon.work
        assertEquals("Бланк 3н. Дела участка", blank.title)
        assertEquals(
            listOf(
                "Дело",
                "Dᵢ, % результата",
                "Часы в месяц и hᵢ, %",
                "Rᵢ, 1–10",
                "Dᵢ / hᵢ",
                "Fᵢ = (Rᵢ × hᵢ) / (5 × Dᵢ)",
                "Пометка",
                "Квадрант и решение",
            ),
            blank.columns,
        )
        assertEquals(0x00D7, blank.columns[5].first { it == '×' }.code)
        val table = blank.referenceTables.single()
        assertEquals("Квадранты", table.title)
        assertEquals(listOf("", "Rᵢ ≤ 5", "Rᵢ ≥ 6"), table.header)
        assertEquals(0x2264, table.header[1].first { it == '≤' }.code)
        assertEquals(0x2265, table.header[2].first { it == '≥' }.code)
        assertEquals(
            listOf(
                listOf(
                    "Dᵢ / hᵢ ≥ 1",
                    "I. Ядро: оберегать и добавлять часы",
                    "II. Точка роста: снимать трение регламентом",
                ),
                listOf(
                    "Dᵢ / hᵢ < 1",
                    "III. Обязательная гигиена: держать в рамках по времени",
                    "IV. Трение: отдать инструменту, передать или отсечь",
                ),
            ),
            table.rows,
        )
        assertEquals('<', table.rows[1][0].first { it == '<' })
        assertEquals(listOf("Итого", "100%"), blank.totalLabels)
        assertEquals(
            "Контрольное правило: дело с Fᵢ ≥ 2 считается делом квадранта IV, даже если по ощущениям оно спокойное.",
            blank.notes.single(),
        )
    }

    private fun texts(blank: BlankCanon): List<String> = buildList {
        add(blank.title)
        add(blank.entriesTitle)
        addAll(blank.columns)
        addAll(blank.notes)
        addAll(blank.rowLabels)
        addAll(blank.totalLabels)
        blank.referenceTables.forEach { table ->
            table.title?.let(::add)
            addAll(table.header)
            table.rows.forEach { addAll(it) }
        }
        blank.examples.forEach { addAll(it) }
    }
}
