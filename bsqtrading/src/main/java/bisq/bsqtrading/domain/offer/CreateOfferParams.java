package bisq.bsqtrading.domain.offer;

import java.util.List;

import bisq.bsqtrading.domain.payments.PaymentAccount;
import bisq.bsqtrading.domain.primitives.Currency;
import bisq.bsqtrading.domain.primitives.Id;

public class CreateOfferParams {
    public CreateOfferParams(Direction direction,
                        OfferPrice price,
                        long amount,
                        long minAmount,
                        Currency sendCurrency,
                        Currency receiveCurrency,
                        List<Id<Mediator>> acceptedMediators,
                        Id<PaymentAccount> paymentAccountId) {

    }
}
