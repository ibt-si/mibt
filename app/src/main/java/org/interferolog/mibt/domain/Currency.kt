// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.domain

import java.util.Currency
import java.util.Locale

/** Cryptocurrencies offered alongside the device's fiat currency list. */
enum class CryptoCurrency(val code: String, val fractionDigits: Int) {
    BTC("BTC", 8),
    ETH("ETH", 6),
    GRAM("GRAM", 9),
    XMR("XMR", 12),
    BNB("BNB", 8),
    SOL("SOL", 9),
    XRP("XRP", 6),
}

/**
 * A currency the journal can be denominated in. [fractionDigits] is the number of decimal
 * places kept in storage: amounts are integers in 1/10^[fractionDigits] units.
 * Fiat entries take their name, symbol and precision from [Currency], i.e. the system list.
 */
data class CurrencyChoice(
    val code: String,
    val label: String,
    val symbol: String,
    val fractionDigits: Int,
    val crypto: Boolean = false,
) {
    /** One whole unit, expressed in stored minor units. */
    val unit: Long get() = UNIT_CACHE.getOrElse(fractionDigits) { 1L }

    companion object {
        private val UNIT_CACHE = (0..12).map { n ->
            var value = 1L
            repeat(n) { value *= 10L }
            value
        }

        /** The book's currency, used for legacy rows and as a safe fallback. */
        val RUB = CurrencyChoice("RUB", "Российский рубль", "₽", 2)

        /** The crypto list, always available regardless of the device locale. */
        val cryptoChoices: List<CurrencyChoice> = CryptoCurrency.entries.map { coin ->
            val name = when (coin) {
                CryptoCurrency.BTC -> "Bitcoin"
                CryptoCurrency.ETH -> "Ethereum"
                CryptoCurrency.GRAM -> "Gram"
                CryptoCurrency.XMR -> "Monero"
                CryptoCurrency.BNB -> "BNB"
                CryptoCurrency.SOL -> "Solana"
                CryptoCurrency.XRP -> "Ripple"
            }
            val symbol = when (coin) {
                CryptoCurrency.BTC -> "₿"
                CryptoCurrency.ETH -> "Ξ"
                CryptoCurrency.GRAM -> "◈"
                CryptoCurrency.XMR -> "ɱ"
                else -> coin.code
            }
            CurrencyChoice(coin.code, "$name (${coin.code})", symbol, coin.fractionDigits, crypto = true)
        }

        /**
         * Codes used by at least one locale on this device. Locale data keeps retired national
         * currencies and fund/metal codes out, so the picker shows only currencies in current use.
         */
        private val localeCurrencyCodes: Set<String> by lazy {
            Locale.getAvailableLocales().mapNotNullTo(mutableSetOf()) { locale ->
                if (locale.country.isEmpty()) null else {
                    runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
                }
            }
        }

        /** Current fiat currencies from system locale data, sorted for display. */
        fun fiatChoices(locale: Locale = Locale.getDefault()): List<CurrencyChoice> =
            Currency.getAvailableCurrencies()
                .filter { it.currencyCode in localeCurrencyCodes && it.currencyCode !in setOf("XXX", "XTS") }
                .map { systemChoice(it, locale) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

        fun allChoices(locale: Locale = Locale.getDefault()): List<CurrencyChoice> =
            cryptoChoices + fiatChoices(locale)

        fun systemChoice(currency: Currency, locale: Locale = Locale.getDefault()): CurrencyChoice =
            CurrencyChoice(
                code = currency.currencyCode,
                label = currency.getDisplayName(locale),
                symbol = currency.getSymbol(locale),
                fractionDigits = currency.defaultFractionDigits,
            )

        /** Currency of the device locale, falling back to rubles when the locale has no country. */
        fun systemDefault(locale: Locale = Locale.getDefault()): CurrencyChoice = try {
            systemChoice(Currency.getInstance(locale), locale)
        } catch (_: IllegalArgumentException) {
            RUB
        } catch (_: java.util.MissingResourceException) {
            RUB
        }

        fun fromCode(code: String, locale: Locale = Locale.getDefault()): CurrencyChoice {
            cryptoChoices.firstOrNull { it.code == code }?.let { return it }
            return try {
                systemChoice(Currency.getInstance(code), locale)
            } catch (_: IllegalArgumentException) {
                RUB
            } catch (_: java.util.MissingResourceException) {
                RUB
            }
        }
    }
}
