package bisq.bsqtrading.domain.offer;

public class OfferPrice {
    private static final long MARKET_OFFER_PRICE = -1;
    private static final OfferPrice marketOffer = new OfferPrice(MARKET_OFFER_PRICE);

    public static OfferPrice marketOfferPrice() {
      return marketOffer;
    }

    private final long price;

    private OfferPrice(long price) {
        this.price = price;
    }

    public boolean isMarketOffer() {
        return price == MARKET_OFFER_PRICE;
    }
}
