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
import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.GlobalSettings;
import bisq.core.locale.Res;
import bisq.core.monetary.Altcoin;
import bisq.core.monetary.Price;
import bisq.core.offer.OfferPayload;

import bisq.network.p2p.NodeAddress;

import bisq.common.util.MathUtils;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Monetary;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.text.DateFormat;
import java.text.DecimalFormat;

import java.math.BigDecimal;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;

@Slf4j
@Singleton
public class BSFormatter {

    // We don't support localized formatting. Format is always using "." as decimal mark and no grouping separator.
    // Input of "," as decimal mark (like in german locale) will be replaced with ".".
    // Input of a group separator (1,123,45) lead to an validation error.
    // Note: BtcFormat was intended to be used, but it lead to many problems (automatic format to mBit,
    // no way to remove grouping separator). It seems to be not optimal for user input formatting.
    @Getter
    protected MonetaryFormat monetaryFormat;

    //  protected String currencyCode = CurrencyUtil.getDefaultFiatCurrencyAsCode();


    @Inject
    public BSFormatter() {
        monetaryFormat = BisqEnvironment.getParameters().getMonetaryFormat();
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
        return FormattingUtils.formatCoin(coin, decimalPlaces, decimalAligned, maxNumberOfDigits, monetaryFormat);
    }

    public String formatCoinWithCode(Coin coin) {
        return FormattingUtils.formatCoinWithCode(coin, monetaryFormat);
    }

    public String formatCoinWithCode(long value) {
        return FormattingUtils.formatCoinWithCode(Coin.valueOf(value), monetaryFormat);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // FIAT
    ///////////////////////////////////////////////////////////////////////////////////////////

    private static Fiat parseToFiat(String input, String currencyCode) {
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

    public static String getDirectionWithCodeDetailed(OfferPayload.Direction direction, String currencyCode) {
        if (CurrencyUtil.isFiatCurrency(currencyCode))
            return (direction == OfferPayload.Direction.BUY) ? Res.get("shared.buyingBTCWith", currencyCode) : Res.get("shared.sellingBTCFor", currencyCode);
        else
            return (direction == OfferPayload.Direction.SELL) ? Res.get("shared.buyingCurrency", currencyCode) : Res.get("shared.sellingCurrency", currencyCode);
    }

    public static String arbitratorAddressesToString(List<NodeAddress> nodeAddresses) {
        return nodeAddresses.stream().map(NodeAddress::getFullAddress).collect(Collectors.joining(", "));
    }

    public static String getRole(boolean isBuyerMakerAndSellerTaker, boolean isMaker, String currencyCode) {
        if (CurrencyUtil.isFiatCurrency(currencyCode)) {
            String baseCurrencyCode = Res.getBaseCurrencyCode();
            if (isBuyerMakerAndSellerTaker)
                return isMaker ?
                        Res.get("formatter.asMaker", baseCurrencyCode, Res.get("shared.buyer")) :
                        Res.get("formatter.asTaker", baseCurrencyCode, Res.get("shared.seller"));
            else
                return isMaker ?
                        Res.get("formatter.asMaker", baseCurrencyCode, Res.get("shared.seller")) :
                        Res.get("formatter.asTaker", baseCurrencyCode, Res.get("shared.buyer"));
        } else {
            if (isBuyerMakerAndSellerTaker)
                return isMaker ?
                        Res.get("formatter.asMaker", currencyCode, Res.get("shared.seller")) :
                        Res.get("formatter.asTaker", currencyCode, Res.get("shared.buyer"));
            else
                return isMaker ?
                        Res.get("formatter.asMaker", currencyCode, Res.get("shared.buyer")) :
                        Res.get("formatter.asTaker", currencyCode, Res.get("shared.seller"));
        }

    }

    public static String getCurrencyPair(String currencyCode) {
        if (CurrencyUtil.isFiatCurrency(currencyCode))
            return Res.getBaseCurrencyCode() + "/" + currencyCode;
        else
            return currencyCode + "/" + Res.getBaseCurrencyCode();
    }

    public static String getCounterCurrency(String currencyCode) {
        if (CurrencyUtil.isFiatCurrency(currencyCode))
            return currencyCode;
        else
            return Res.getBaseCurrencyCode();
    }

    public static String getPriceWithCurrencyCode(String currencyCode) {
        return getPriceWithCurrencyCode(currencyCode, "shared.priceInCurForCur");
    }

    public static String getPriceWithCurrencyCode(String currencyCode, String translationKey) {
        if (CurrencyUtil.isCryptoCurrency(currencyCode))
            return Res.get(translationKey, Res.getBaseCurrencyCode(), currencyCode);
        else
            return Res.get(translationKey, currencyCode, Res.getBaseCurrencyCode());
    }
}
