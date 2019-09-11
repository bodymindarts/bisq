package bisq.bsqtrading.domain.payments;

import java.util.UUID;

import lombok.Getter;

public class SepaAccount implements PaymentAccount{
    public static PaymentAccount createSepaAccount(String countryCode, String bankId) {
        return new SepaAccount(new PaymentAccountId(UUID.randomUUID().toString()), countryCode, bankId, PaymentMethod.SEPA);
    }

    @Getter
    private final PaymentAccountId id;
    @Getter
    private final String countryCode;
    @Getter
    private final String bankId;
    @Getter
    private final PaymentMethod paymentMethod;

    private SepaAccount(PaymentAccountId id, String countryCode, String bankId, PaymentMethod paymentMethod) {
        this.id = id;
        this.countryCode = countryCode;
        this.bankId = bankId;
        this.paymentMethod = paymentMethod;
    }
}
