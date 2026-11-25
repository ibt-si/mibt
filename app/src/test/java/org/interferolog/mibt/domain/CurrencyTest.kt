// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CurrencyTest {
    @Test
    fun rubleWrappersKeepTheBookFormat() {
        assertEquals("4700 ₽", formatRubles(4_700_00))
        assertEquals("150,50 ₽", formatRubles(150_50))
        assertEquals("−10 ₽", formatRubles(-10_00))
        assertEquals(4_700_00L, parseRubles("4 700 ₽"))
        assertEquals(150_50L, parseRubles("150,50"))
    }

    @Test
    fun cryptoUsesItsOwnPrecision() {
        val btc = CurrencyChoice.fromCode("BTC")
        assertEquals(8, btc.fractionDigits)
        assertEquals("0,50000000 ₿", formatMoney(50_000_000L, btc))
        assertEquals(50_000_000L, parseMoney("0,5 BTC", btc))

        val eth = CurrencyChoice.fromCode("ETH")
        assertEquals(6, eth.fractionDigits)
        assertEquals("0,250000 Ξ", formatMoney(250_000L, eth))

        val gram = CurrencyChoice.fromCode("GRAM")
        assertEquals(9, gram.fractionDigits)
        assertEquals("1,500000000 ◈", formatMoney(1_500_000_000L, gram))
        assertEquals(1_500_000_000L, parseMoney("1,5 ◈", gram))

        val xmr = CurrencyChoice.fromCode("XMR")
        assertEquals(12, xmr.fractionDigits)
        val tiny = 1_000_000_000_001L
        assertEquals("1,000000000001 ɱ", formatMoney(tiny, xmr))
        assertEquals(tiny, parseMoney("1,000000000001 XMR", xmr))
    }

    @Test
    fun fiatPrecisionComesFromTheSystem() {
        val jpy = CurrencyChoice.fromCode("JPY", Locale.US)
        assertEquals(0, jpy.fractionDigits)
        assertTrue(formatMoney(1_500L, jpy).startsWith("1500 "))

        val rub = CurrencyChoice.fromCode("RUB", Locale.forLanguageTag("ru-RU"))
        assertEquals(2, rub.fractionDigits)
        assertEquals("₽", rub.symbol)
        assertEquals(12_34L, parseMoney("12,34 ₽", rub))
    }

    @Test
    fun systemDefaultFollowsTheLocaleWithARubleFallback() {
        assertEquals("RUB", CurrencyChoice.systemDefault(Locale.forLanguageTag("ru-RU")).code)
        assertEquals("USD", CurrencyChoice.systemDefault(Locale.US).code)
        assertEquals("EUR", CurrencyChoice.systemDefault(Locale.GERMANY).code)
        assertEquals("RUB", CurrencyChoice.systemDefault(Locale.forLanguageTag("en")).code)
        assertEquals("RUB", CurrencyChoice.fromCode("NOPE").code)
    }

    @Test
    fun pickerListsCryptoAndSystemCurrencies() {
        val all = CurrencyChoice.allChoices(Locale.US)
        assertTrue(all.map { it.code }.containsAll(listOf("BTC", "ETH", "GRAM", "XMR", "BNB", "SOL", "XRP")))
        assertTrue(all.map { it.code }.containsAll(listOf("RUB", "USD", "EUR")))
        assertFalse(all.map { it.code }.contains("USDT"))
        assertFalse(all.map { it.code }.contains("XXX"))
        assertEquals(7, CurrencyChoice.cryptoChoices.size)
    }
}
