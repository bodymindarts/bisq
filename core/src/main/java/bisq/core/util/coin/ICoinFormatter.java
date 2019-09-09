package bisq.core.util.coin;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.MonetaryFormat;

public interface ICoinFormatter {
    MonetaryFormat getMonetaryFormat();

    String formatCoin(Coin coin);

    String formatCoinWithCode(Coin coin);
}
