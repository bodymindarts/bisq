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
import bisq.core.locale.Res;
import bisq.core.monetary.Altcoin;
import bisq.core.monetary.Volume;
import bisq.core.offer.Offer;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Monetary;
import org.bitcoinj.utils.Fiat;
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

    protected final MonetaryFormat fiatVolumeFormat = new MonetaryFormat().shift(0).minDecimals(2).repeatOptionalDecimals(0, 0);



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

    public String formatCoin(long value, MonetaryFormat coinFormat) {
        return formatCoin(Coin.valueOf(value), -1, false, 0, coinFormat);
    }

    public String formatCoin(Coin coin, int decimalPlaces, boolean decimalAligned, int maxNumberOfDigits) {
        return formatCoin(coin, decimalPlaces, decimalAligned, maxNumberOfDigits, coinFormat);
    }

    public String formatCoin(Coin coin, int decimalPlaces, boolean decimalAligned, int maxNumberOfDigits, MonetaryFormat coinFormat) {
        String formattedCoin = "";

        if (coin != null) {
            try {
                if (decimalPlaces < 0 || decimalPlaces > 4) {
                    formattedCoin = coinFormat.noCode().format(coin).toString();
                } else {
                    formattedCoin = coinFormat.noCode().minDecimals(decimalPlaces).repeatOptionalDecimals(1, decimalPlaces).format(coin).toString();
                }
            } catch (Throwable t) {
                log.warn("Exception at formatBtc: " + t.toString());
            }
        }

        if (decimalAligned) {
            formattedCoin = FormattingUtils.fillUpPlacesWithEmptyStrings(formattedCoin, maxNumberOfDigits);
        }

        return formattedCoin;
    }

    public String formatCoinWithCode(Coin coin) {
        return formatCoinWithCode(coin, coinFormat);
    }

    public String formatCoinWithCode(long value) {
        return formatCoinWithCode(Coin.valueOf(value), coinFormat);
    }

    public String formatCoinWithCode(long value, MonetaryFormat coinFormat) {
        return formatCoinWithCode(Coin.valueOf(value), coinFormat);
    }

    public String formatCoinWithCode(Coin coin, MonetaryFormat coinFormat) {
        if (coin != null) {
            try {
                // we don't use the code feature from coinFormat as it does automatic switching between mBTC and BTC and
                // pre and post fixing
                return coinFormat.postfixCode().format(coin).toString();
            } catch (Throwable t) {
                log.warn("Exception at formatBtcWithCode: " + t.toString());
                return "";
            }
        } else {
            return "";
        }
    }

    public Coin parseToCoin(String input) {
        return parseToCoin(input, coinFormat);
    }

    public Coin parseToCoin(String input, MonetaryFormat coinFormat) {
        if (input != null && input.length() > 0) {
            try {
                return coinFormat.parse(ParsingUtils.cleanDoubleInput(input));
            } catch (Throwable t) {
                log.warn("Exception at parseToBtc: " + t.toString());
                return Coin.ZERO;
            }
        } else {
            return Coin.ZERO;
        }
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
    // FIAT
    ///////////////////////////////////////////////////////////////////////////////////////////

    protected Fiat parseToFiat(String input, String currencyCode) {
        if (input != null && input.length() > 0) {
            try {
                return Fiat.parseFiat(currencyCode, ParsingUtils.cleanDoubleInput(input));
            } catch (Exception e) {
                log.warn("Exception at parseToFiat: " + e.toString());
                return Fiat.valueOf(currencyCode, 0);
            }

        } else {
            return Fiat.valueOf(currencyCode, 0);
        }
    }

    /**
     * Converts to a fiat with max. 2 decimal places. Last place gets rounded.
     * 0.234 -> 0.23
     * 0.235 -> 0.24
     *
     * @param input
     * @return
     */

    public Fiat parseToFiatWithPrecision(String input, String currencyCode) {
        if (input != null && input.length() > 0) {
            try {
                return parseToFiat(new BigDecimal(ParsingUtils.cleanDoubleInput(input)).setScale(2, BigDecimal.ROUND_HALF_UP).toString(),
                        currencyCode);
            } catch (Throwable t) {
                log.warn("Exception at parseToFiatWithPrecision: " + t.toString());
                return Fiat.valueOf(currencyCode, 0);
            }

        }
        return Fiat.valueOf(currencyCode, 0);
    }

    public boolean isFiatAlteredWhenPrecisionApplied(String input, String currencyCode) {
        return parseToFiat(input, currencyCode).equals(parseToFiatWithPrecision(input, currencyCode));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Volume
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String formatVolume(Offer offer, Boolean decimalAligned, int maxNumberOfDigits) {
        return formatVolume(offer, decimalAligned, maxNumberOfDigits, true);
    }

    public String formatVolume(Offer offer, Boolean decimalAligned, int maxNumberOfDigits, boolean showRange) {
        String formattedVolume = offer.isRange() && showRange ? formatVolume(offer.getMinVolume()) + FormattingUtils.RANGE_SEPARATOR + formatVolume(offer.getVolume()) : formatVolume(offer.getVolume());

        if (decimalAligned) {
            formattedVolume = FormattingUtils.fillUpPlacesWithEmptyStrings(formattedVolume, maxNumberOfDigits);
        }
        return formattedVolume;
    }

    public String formatVolume(Volume volume) {
        return formatVolume(volume, fiatVolumeFormat, false);
    }

    public String formatVolumeWithCode(Volume volume) {
        return formatVolume(volume, fiatVolumeFormat, true);
    }

    public String formatVolume(Volume volume, MonetaryFormat fiatVolumeFormat, boolean appendCurrencyCode) {
        if (volume != null) {
            Monetary monetary = volume.getMonetary();
            if (monetary instanceof Fiat)
                return FormattingUtils.formatFiat((Fiat) monetary, fiatVolumeFormat, appendCurrencyCode);
            else
                return FormattingUtils.formatAltcoinVolume((Altcoin) monetary, appendCurrencyCode);
        } else {
            return "";
        }
    }

    public String formatVolumeLabel(String currencyCode) {
        return formatVolumeLabel(currencyCode, "");
    }

    public String formatVolumeLabel(String currencyCode, String postFix) {
        return Res.get("formatter.formatVolumeLabel",
                currencyCode, postFix);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Amount
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String formatAmount(Offer offer) {
        return formatAmount(offer, false);
    }

    public String formatAmount(Offer offer, boolean decimalAligned) {
        String formattedAmount = offer.isRange() ? formatCoin(offer.getMinAmount()) + FormattingUtils.RANGE_SEPARATOR + formatCoin(offer.getAmount()) : formatCoin(offer.getAmount());
        if (decimalAligned) {
            formattedAmount = FormattingUtils.fillUpPlacesWithEmptyStrings(formattedAmount, 15);
        }
        return formattedAmount;
    }

    public String formatAmount(Offer offer, int decimalPlaces, boolean decimalAligned, int maxPlaces) {
        String formattedAmount = offer.isRange() ? formatCoin(offer.getMinAmount(), decimalPlaces) + FormattingUtils.RANGE_SEPARATOR + formatCoin(offer.getAmount(), decimalPlaces) : formatCoin(offer.getAmount(), decimalPlaces);

        if (decimalAligned) {
            formattedAmount = FormattingUtils.fillUpPlacesWithEmptyStrings(formattedAmount, maxPlaces);
        }
        return formattedAmount;
    }
}
