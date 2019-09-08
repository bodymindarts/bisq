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

    //  protected String currencyCode = CurrencyUtil.getDefaultFiatCurrencyAsCode();


    @Inject
    public BSFormatter() {
        coinFormat = BisqEnvironment.getParameters().getMonetaryFormat();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // BTC
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String formatCoin(Coin coin) {
        return formatCoin(coin, -1);
    }

    @NotNull
    public String formatCoin(Coin coin, int decimalPlaces) {
        return formatCoin(coin, decimalPlaces, false, 0);
    }

    public String formatCoin(Coin coin, int decimalPlaces, boolean decimalAligned, int maxNumberOfDigits) {
        return FormattingUtils.formatCoin(coin, decimalPlaces, decimalAligned, maxNumberOfDigits, coinFormat);
    }

    public String formatCoinWithCode(Coin coin) {
        return FormattingUtils.formatCoinWithCode(coin, coinFormat);
    }

    public String formatCoinWithCode(long value) {
        return FormattingUtils.formatCoinWithCode(Coin.valueOf(value), coinFormat);
    }

    public Coin parseToCoin(String input) {
        return ParsingUtils.parseToCoin(input, coinFormat);
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
        try {
            return Coin.valueOf(new BigDecimal(parseToCoin(ParsingUtils.cleanDoubleInput(input)).value).setScale(-scale - 1,
                    BigDecimal.ROUND_HALF_UP).setScale(scale + 1, BigDecimal.ROUND_HALF_UP).toBigInteger().longValue());
        } catch (Throwable t) {
            if (input != null && input.length() > 0)
                log.warn("Exception at parseToCoinWith4Decimals: " + t.toString());
            return Coin.ZERO;
        }
    }

    public boolean hasBtcValidDecimals(String input) {
        return parseToCoin(input).equals(parseToCoinWith4Decimals(input));
    }

    /**
     * Transform a coin with the properties defined in the format (used to reduce decimal places)
     *
     * @param coin The coin which should be transformed
     * @return The transformed coin
     */
    public Coin reduceTo4Decimals(Coin coin) {
        return parseToCoin(formatCoin(coin));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Amount
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String formatAmount(Offer offer) {
        return offer.isRange() ? formatCoin(offer.getMinAmount()) + FormattingUtils.RANGE_SEPARATOR + formatCoin(offer.getAmount()) : formatCoin(offer.getAmount());
    }

    public String formatAmount(Offer offer, int decimalPlaces, boolean decimalAligned, int maxPlaces) {
        String formattedAmount = offer.isRange() ? formatCoin(offer.getMinAmount(), decimalPlaces) + FormattingUtils.RANGE_SEPARATOR + formatCoin(offer.getAmount(), decimalPlaces) : formatCoin(offer.getAmount(), decimalPlaces);

        if (decimalAligned) {
            formattedAmount = FormattingUtils.fillUpPlacesWithEmptyStrings(formattedAmount, maxPlaces);
        }
        return formattedAmount;
    }
}
