package bisq.bsqtrading.domain.primitives;

public class CountryCode {
    private final String code;

    public CountryCode(String code) {
        this.code = code;
    }

    public String toString() {
        return code;
    }
}
