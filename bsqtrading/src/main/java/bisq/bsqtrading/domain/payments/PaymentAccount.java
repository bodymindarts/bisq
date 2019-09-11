package bisq.bsqtrading.domain.payments;

import bisq.bsqtrading.domain.primitives.CountryCode;

public interface PaymentAccount {
    PaymentAccountId getId();
    PaymentMethod getPaymentMethod();
    CountryCode getCountryCode();
    BankId getBankId();
}

