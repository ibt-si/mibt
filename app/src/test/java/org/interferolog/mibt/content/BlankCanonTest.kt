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
        assertEquals(listOf("1", "2а"), blanks.map { it.number })
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
            assertTrue(blank.examples.isNotEmpty())
            blank.examples.forEach { row ->
                assertEquals(blank.columns.size, row.size)
            }
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

    private fun texts(blank: BlankCanon): List<String> = buildList {
        add(blank.title)
        addAll(blank.columns)
        blank.referenceTables.forEach { table ->
            table.title?.let(::add)
            addAll(table.header)
            table.rows.forEach { addAll(it) }
        }
        blank.examples.forEach { addAll(it) }
    }
}
