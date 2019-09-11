package bisq.bsqtrading.domain.offer;

import java.util.List;

import bisq.bsqtrading.domain.payments.PaymentAccountId;
import bisq.bsqtrading.domain.primitives.Currency;

public class CreateOfferParams {
    public CreateOfferParams(Direction direction,
                        OfferPrice price,
                        long amount,
                        long minAmount,
                        Currency sendCurrency,
                        Currency receiveCurrency,
                        List<Mediator.Id> acceptedMediators,
                        PaymentAccountId paymentAccountId) {

    }
}
