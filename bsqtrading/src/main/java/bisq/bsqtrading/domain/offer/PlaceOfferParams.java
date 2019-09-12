package bisq.bsqtrading.domain.offer;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import bisq.bsqtrading.domain.payments.PaymentAccount;
import bisq.bsqtrading.domain.primitives.Currency;
import bisq.bsqtrading.domain.primitives.Id;

public class PlaceOfferParams {

    private final Direction direction;
    private final OfferPrice price;
    private final OfferAmountRange amount;
    private final Currency sendCurrency;
    private final Currency receiveCurrency;
    private final List<Id<Mediator>> acceptedMediators;
    private final Id<PaymentAccount> paymentAccountId;

    public PlaceOfferParams(Direction direction,
                            OfferPrice price,
                            OfferAmountRange amount,
                            Currency sendCurrency,
                            Currency receiveCurrency,
                            List<Id<Mediator>> acceptedMediators,
                            Id<PaymentAccount> paymentAccountId) {
        this.direction = direction;
        this.price = price;
        this.amount = amount;
        this.sendCurrency = sendCurrency;
        this.receiveCurrency = receiveCurrency;
        this.acceptedMediators = acceptedMediators;
        this.paymentAccountId = paymentAccountId;

        validate();
    }
    private void validate() {
        checkNotNull(direction, "Direction musst not be null");
        checkNotNull(price, "OfferPrice must not be null");
        checkNotNull(amount, "OfferAmountRange must not be null");
        checkNotNull(sendCurrency, "Currency must not be null");
        checkNotNull(receiveCurrency, "Currency must not be null");
        checkNotNull(acceptedMediators, "Accepted mediators must not be null");
        checkArgument(acceptedMediators.size() > 0, "Accepted mediators must contain at least 1 element");
        checkNotNull(paymentAccountId, "PaymentAccountId must not be null");
    }

}
