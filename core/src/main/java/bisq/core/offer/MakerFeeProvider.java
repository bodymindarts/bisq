package bisq.core.offer;

import bisq.core.btc.wallet.BsqWalletService;
import bisq.core.user.Preferences;

import org.bitcoinj.core.Coin;

public class MakerFeeProvider {
    public Coin getMakerFee(BsqWalletService bsqWalletService, Preferences preferences, Coin amount) {
        return OfferUtil.getMakerFee(bsqWalletService, preferences, amount);
    }
}
