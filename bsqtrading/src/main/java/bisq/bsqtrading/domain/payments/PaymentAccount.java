package bisq.bsqtrading.domain.payments;

import bisq.bsqtrading.domain.primitives.CountryCode;
import bisq.bsqtrading.domain.primitives.Id;

public interface PaymentAccount {
    Id<PaymentAccount> getId();
    PaymentMethod getPaymentMethod();
    CountryCode getCountryCode();
    BankId getBankId();
}

