package bisq.bsqtrading.domain.offer;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertTrue;



import bisq.bsqtrading.domain.payments.PaymentAccountId;
import bisq.bsqtrading.domain.primitives.Currency;

public class CreateOfferTest {
    @Test
    public void testThing() {
        Mediator.Id mediatorId = new Mediator.Id("bla");
        PaymentAccountId accountId = new PaymentAccountId("account");
        CreateOfferParams params = new CreateOfferParams(Direction.SELL, OfferPrice.marketOfferPrice(),10,10, Currency.USD,Currency.BTC, List.of(mediatorId), accountId);
        new CreateOffer(params).execute();
        assertTrue(true);
    }
}
