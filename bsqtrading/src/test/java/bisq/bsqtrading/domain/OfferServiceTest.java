package bisq.bsqtrading.domain;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertTrue;



import bisq.bsqtrading.domain.OfferService;
import bisq.bsqtrading.domain.offer.Direction;
import bisq.bsqtrading.domain.offer.Mediator;
import bisq.bsqtrading.domain.offer.OfferAmountRange;
import bisq.bsqtrading.domain.offer.OfferPrice;
import bisq.bsqtrading.domain.offer.PlaceOfferParams;
import bisq.bsqtrading.domain.payments.PaymentAccount;
import bisq.bsqtrading.domain.primitives.Currency;
import bisq.bsqtrading.domain.primitives.Id;

public class OfferServiceTest {
    @Test
    public void testThing() {
        Id<Mediator> mediatorId = Id.generateNewId();
        Id<PaymentAccount> accountId = Id.generateNewId();
        OfferAmountRange amount = new OfferAmountRange(10, 5);
        PlaceOfferParams params = new PlaceOfferParams(Direction.SELL, OfferPrice.marketOfferPrice(), amount, Currency.USD,Currency.BTC, List.of(mediatorId), accountId);
        new OfferService().placeOffer(params);
        assertTrue(true);
    }
}
