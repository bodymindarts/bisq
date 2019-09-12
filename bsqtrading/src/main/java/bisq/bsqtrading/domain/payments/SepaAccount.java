package bisq.bsqtrading.domain.payments;

import lombok.Getter;

import bisq.bsqtrading.domain.primitives.CountryCode;
import bisq.bsqtrading.domain.primitives.Id;

public class SepaAccount implements PaymentAccount{
    public static PaymentAccount createSepaAccount(CountryCode countryCode, BankId bankId) {
        return new SepaAccount(Id.generateNewId(), countryCode, bankId, PaymentMethod.SEPA);
    }

    @Getter
    private final Id<PaymentAccount> id;
    @Getter
    private final CountryCode countryCode;
    @Getter
    private final BankId bankId;
    @Getter
    private final PaymentMethod paymentMethod;

    private SepaAccount(Id<PaymentAccount> id, CountryCode countryCode, BankId bankId, PaymentMethod paymentMethod) {
        this.id = id;
        this.countryCode = countryCode;
        this.bankId = bankId;
        this.paymentMethod = paymentMethod;
    }
}
