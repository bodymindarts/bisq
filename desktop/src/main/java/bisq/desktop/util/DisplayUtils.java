package bisq.desktop.util;

import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.GlobalSettings;
import bisq.core.locale.Res;
import bisq.core.offer.OfferPayload;
import bisq.core.util.FormattingUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.text.DateFormat;

import java.util.Date;

public class DisplayUtils {
    public static String getDirectionWithCode(OfferPayload.Direction direction, String currencyCode) {
        if (CurrencyUtil.isFiatCurrency(currencyCode))
            return (direction == OfferPayload.Direction.BUY) ? Res.get("shared.buyCurrency", Res.getBaseCurrencyCode()) : Res.get("shared.sellCurrency", Res.getBaseCurrencyCode());
        else
            return (direction == OfferPayload.Direction.SELL) ? Res.get("shared.buyCurrency", currencyCode) : Res.get("shared.sellCurrency", currencyCode);
    }

    public static String formatDateTime(Date date) {
        return FormattingUtils.formatDateTime(date, true);
    }

    public static String formatDateTimeSpan(Date dateFrom, Date dateTo) {
        if (dateFrom != null && dateTo != null) {
            DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, GlobalSettings.getLocale());
            DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.DEFAULT, GlobalSettings.getLocale());
            return dateFormatter.format(dateFrom) + " " + timeFormatter.format(dateFrom) + FormattingUtils.RANGE_SEPARATOR + timeFormatter.format(dateTo);
        } else {
            return "";
        }
    }

    public static String formatTime(Date date) {
        if (date != null) {
            DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.DEFAULT, GlobalSettings.getLocale());
            return timeFormatter.format(date);
        } else {
            return "";
        }
    }

    public static String formatDate(Date date) {
        if (date != null) {
            DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, GlobalSettings.getLocale());
            return dateFormatter.format(date);
        } else {
            return "";
        }
    }

    public static String formatDurationAsWords(long durationMillis) {
        return FormattingUtils.formatDurationAsWords(durationMillis, false, true);
    }

    public static String formatAccountAge(long durationMillis) {
        durationMillis = Math.max(0, durationMillis);
        String day = Res.get("time.day").toLowerCase();
        String days = Res.get("time.days");
        String format = "d\' " + days + "\'";
        return StringUtils.replaceOnce(DurationFormatUtils.formatDuration(durationMillis, format), "1 " + days, "1 " + day);
    }

    public static String booleanToYesNo(boolean value) {
        return value ? Res.get("shared.yes") : Res.get("shared.no");
    }

    public static String getDirectionBothSides(OfferPayload.Direction direction, String currencyCode) {
        if (CurrencyUtil.isFiatCurrency(currencyCode)) {
            currencyCode = Res.getBaseCurrencyCode();
            return direction == OfferPayload.Direction.BUY ?
                    Res.get("formatter.makerTaker", currencyCode, Res.get("shared.buyer"), currencyCode, Res.get("shared.seller")) :
                    Res.get("formatter.makerTaker", currencyCode, Res.get("shared.seller"), currencyCode, Res.get("shared.buyer"));
        } else {
            return direction == OfferPayload.Direction.SELL ?
                    Res.get("formatter.makerTaker", currencyCode, Res.get("shared.buyer"), currencyCode, Res.get("shared.seller")) :
                    Res.get("formatter.makerTaker", currencyCode, Res.get("shared.seller"), currencyCode, Res.get("shared.buyer"));
        }
    }
}
