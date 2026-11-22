// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.content

data class ReferenceTable(
    val title: String?,
    val header: List<String>,
    val rows: List<List<String>>,
)

/**
 * One journal form. [number] is the journal index («1», «2а»).
 * [title] is the full heading «Бланк N. …», copied from the flight log.
 */
data class BlankCanon(
    val number: String,
    val title: String,
    val referenceTables: List<ReferenceTable>,
    val columns: List<String>,
    val examples: List<List<String>>,
    val entriesTitle: String,
    val notes: List<String> = emptyList(),
    val rowLabels: List<String> = emptyList(),
    val totalLabels: List<String> = emptyList(),
) {
    companion object {
        val phaseDiary = BlankCanon(
            number = "1",
            title = "Бланк 1. Дневник фазы",
            referenceTables = listOf(
                ReferenceTable(
                    title = "Сверочная таблица телесных маркеров",
                    header = listOf(
                        "Параметр",
                        "Импульс из пустоты",
                        "Действие из наполненности",
                    ),
                    rows = listOf(
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
                ),
            ),
            columns = listOf(
                "Дата и время",
                "Контекст и сумма",
                "Телесный маркер за 3 секунды до оплаты",
                "Метка: П (пустота) / Н (наполненность)",
                "Факт без оценки",
            ),
            entriesTitle = "Дневник",
            examples = listOf(
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
        )

        val pointC = BlankCanon(
            number = "2а",
            title = "Бланк 2а. Колодец или Точка C",
            referenceTables = listOf(
                ReferenceTable(
                    title = null,
                    header = listOf(
                        "Параметр",
                        "Колодец нехватки",
                        "Точка C",
                    ),
                    rows = listOf(
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
                ),
            ),
            columns = listOf(
                "Дата и время",
                "Ситуация и сумма",
                "Вопрос в начале решения: колодец / Точка C",
                "Спазм: анестезия / паралич / демпинг / отсечка масштаба",
                "Что изменила пауза",
            ),
            entriesTitle = "Журнал спазмов",
            examples = listOf(
                listOf(
                    "Пример",
                    "Ремонт подвески, счёт 45 000 ₽",
                    "Колодец",
                    "Паралич",
                    "После двух секунд в стопах — вопрос о гарантии и сроках вместо спора",
                ),
            ),
        )

        val holter = BlankCanon(
            number = "2",
            title = "Бланк 2. Холтер четырёх узлов",
            referenceTables = emptyList(),
            columns = listOf(
                "Дата",
                "Доход; Обз отдельно",
                "U₁ Опора",
                "U₂ Инструмент",
                "U₃ Рост",
                "U₄ Шум",
                "Двойной тариф",
                "η",
            ),
            examples = emptyList(),
            entriesTitle = "",
            notes = listOf(
                "Правило пропусков: если вечер пропущен, примерная сумма вписывается в U₄, и запись спокойно продолжается с сегодняшнего вечера. Холтер из-за пропуска не бросается.",
            ),
            rowLabels = listOf(
                "День 1",
                "День 2",
                "День 3",
                "День 4",
                "День 5",
                "День 6",
                "День 7",
                "Итого за неделю",
            ),
        )

        val sources = BlankCanon(
            number = "3",
            title = "Бланк 3. Индекс сопротивления источников",
            referenceTables = emptyList(),
            columns = listOf(
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
            examples = emptyList(),
            entriesTitle = "",
            notes = listOf(
                "Балл трения Rᵢ ставится по телу: вспомнить последний звонок от этого источника. Если хочется отложить телефон и не брать — балл не ниже семи.",
            ),
            rowLabels = listOf("Итого"),
            totalLabels = listOf("Итого", "100%", "Sср =", "Строк с Fᵢ ≥ 2:"),
        )

        val work = BlankCanon(
            number = "3н",
            title = "Бланк 3н. Дела участка",
            referenceTables = listOf(
                ReferenceTable(
                    title = "Квадранты",
                    header = listOf("", "Rᵢ ≤ 5", "Rᵢ ≥ 6"),
                    rows = listOf(
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
                ),
            ),
            columns = listOf(
                "Дело",
                "Dᵢ, % результата",
                "Часы в месяц и hᵢ, %",
                "Rᵢ, 1–10",
                "Dᵢ / hᵢ",
                "Fᵢ = (Rᵢ × hᵢ) / (5 × Dᵢ)",
                "Пометка",
                "Квадрант и решение",
            ),
            examples = emptyList(),
            entriesTitle = "",
            notes = listOf(
                "Контрольное правило: дело с Fᵢ ≥ 2 считается делом квадранта IV, даже если по ощущениям оно спокойное.",
            ),
            rowLabels = listOf("Итого"),
            totalLabels = listOf("Итого", "100%"),
        )

        val all: List<BlankCanon> = listOf(phaseDiary, pointC, holter, sources, work)
    }
}
