package bisq.bsqtrading.domain.offer;

public class OfferPrice {
    private static final OfferPrice marketOffer = new OfferPrice(0);

    public static OfferPrice marketOfferPrice() {
      return marketOffer;
    }

    private final long price;

    public OfferPrice(long price) {
        this.price = price;
    }

    public boolean isMarketOffer() {
        return price == 0;
    }
}
