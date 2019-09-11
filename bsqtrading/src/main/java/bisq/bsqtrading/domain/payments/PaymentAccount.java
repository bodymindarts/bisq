package bisq.bsqtrading.domain.payments;

public interface PaymentAccount {
    PaymentAccountId getId();
    String getCountryCode();
    String getBankId();
    String getPaymentMethod();
}

