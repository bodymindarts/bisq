package bisq.bsqtrading.domain.payments;

import java.util.UUID;

import lombok.Getter;



import bisq.bsqtrading.domain.primitives.CountryCode;

public class SepaAccount implements PaymentAccount{
    public static PaymentAccount createSepaAccount(CountryCode countryCode, BankId bankId) {
        return new SepaAccount(new PaymentAccountId(UUID.randomUUID().toString()), countryCode, bankId, PaymentMethod.SEPA);
    }

    @Getter
    private final PaymentAccountId id;
    @Getter
    private final CountryCode countryCode;
    @Getter
    private final BankId bankId;
    @Getter
    private final PaymentMethod paymentMethod;

    private SepaAccount(PaymentAccountId id, CountryCode countryCode, BankId bankId, PaymentMethod paymentMethod) {
        this.id = id;
        this.countryCode = countryCode;
        this.bankId = bankId;
        this.paymentMethod = paymentMethod;
    }
}
