package bisq.bsqtrading.domain.offer;

import static com.google.common.base.Preconditions.checkArgument;

public class OfferAmountRange {
    private final long amount;
    private final long minAmount;

    public OfferAmountRange(long amount, long minAmount) {
        this.amount = amount;
        this.minAmount = minAmount;
        validate();
    }

    private void validate() {
        checkArgument(amount > 0, "Offer amount must be > 0");
        checkArgument(minAmount > 0, "Minimum offer amount must be > 0");
        checkArgument(minAmount < amount , "Minimum offer must be less than amount");
    }
}
