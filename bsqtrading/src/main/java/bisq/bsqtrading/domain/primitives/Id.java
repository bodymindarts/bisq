package bisq.bsqtrading.domain.primitives;

import java.util.UUID;

public class Id<T> {

    public static <T> Id<T> generateNewId() {
        return new Id<T>(UUID.randomUUID().toString());
    }

    public static <T> Id<T> fromString(String uuid) {
        return new Id<T>(uuid);
    }

    private final String id;

    public Id(String id) {
        this.id = id;
    }

    public String toString() {
        return id;
    }
}
