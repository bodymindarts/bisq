/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.util;

import bisq.core.app.BisqEnvironment;
import bisq.core.offer.Offer;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.MonetaryFormat;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;

@Slf4j
@Singleton
public class BSFormatter {

    protected boolean useMilliBit;
    protected int scale = 3;

    // We don't support localized formatting. Format is always using "." as decimal mark and no grouping separator.
    // Input of "," as decimal mark (like in german locale) will be replaced with ".".
    // Input of a group separator (1,123,45) lead to an validation error.
    // Note: BtcFormat was intended to be used, but it lead to many problems (automatic format to mBit,
    // no way to remove grouping separator). It seems to be not optimal for user input formatting.
    protected MonetaryFormat coinFormat;
    private final FormattingUtils.CoinFormatter coinFormatter;

    //  protected String currencyCode = CurrencyUtil.getDefaultFiatCurrencyAsCode();


    @Inject
    public BSFormatter() {
        coinFormat = BisqEnvironment.getParameters().getMonetaryFormat();
        coinFormatter = new FormattingUtils.CoinFormatter(BisqEnvironment.getParameters().getMonetaryFormat());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // BTC
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String formatCoin(Coin coin) {
        return coinFormatter.formatCoin(coin, -1);
    }

    @NotNull
    public String formatCoin(Coin coin, int decimalPlaces) {
        return coinFormatter.formatCoin(coin, decimalPlaces, false, 0);
    }

    public String formatCoin(Coin coin, int decimalPlaces, boolean decimalAligned, int maxNumberOfDigits) {
        return coinFormatter.formatCoin(coin, decimalPlaces, decimalAligned, maxNumberOfDigits);
    }

    public String formatCoinWithCode(Coin coin) {
        return coinFormatter.formatCoin(coin);
    }

    public String formatCoinWithCode(long value) {
        return coinFormatter.formatCoinWithCode(value);
    }

    public Coin parseToCoin(String input) {
        return coinFormatter.parseToCoin(input);
    }

    /**
     * Converts to a coin with max. 4 decimal places. Last place gets rounded.
     * 0.01234 -> 0.0123
     * 0.01235 -> 0.0124
     *
     * @param input
     * @return
     */
    public Coin parseToCoinWith4Decimals(String input) {
        return coinFormatter.parseToCoinWith4Decimals(input);
    }

    public boolean hasBtcValidDecimals(String input) {
        return coinFormatter.hasBtcValidDecimals(input);
    }

    /**
     * Transform a coin with the properties defined in the format (used to reduce decimal places)
     *
     * @param coin The coin which should be transformed
     * @return The transformed coin
     */
    public Coin reduceTo4Decimals(Coin coin) {
        return coinFormatter.reduceTo4Decimals(coin);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Amount
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String formatAmount(Offer offer) {
        return coinFormatter.formatAmount(offer);
    }

    public String formatAmount(Offer offer, int decimalPlaces, boolean decimalAligned, int maxPlaces) {
        return coinFormatter.formatAmount(offer, decimalPlaces, decimalAligned, maxPlaces);
    }
}
