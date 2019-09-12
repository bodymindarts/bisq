package bisq.bsqtrading.domain.offer;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertTrue;



import bisq.bsqtrading.domain.payments.PaymentAccount;
import bisq.bsqtrading.domain.primitives.Currency;
import bisq.bsqtrading.domain.primitives.Id;

public class CreateOfferTest {
    @Test
    public void testThing() {
        Id<Mediator> mediatorId = Id.generateNewId();
        Id<PaymentAccount> accountId = Id.generateNewId();
        CreateOfferParams params = new CreateOfferParams(Direction.SELL, OfferPrice.marketOfferPrice(),10,10, Currency.USD,Currency.BTC, List.of(mediatorId), accountId);
        new CreateOffer(params).execute();
        assertTrue(true);
    }
}
