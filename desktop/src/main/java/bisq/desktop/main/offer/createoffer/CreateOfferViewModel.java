/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.offer.createoffer;

import bisq.desktop.Navigation;
import bisq.desktop.common.model.ViewModel;
import bisq.desktop.main.offer.MutableOfferViewModel;
import bisq.desktop.util.validation.AltcoinValidator;
import bisq.desktop.util.validation.BsqValidator;
import bisq.desktop.util.validation.BtcValidator;
import bisq.desktop.util.validation.FiatPriceValidator;
import bisq.desktop.util.validation.FiatVolumeValidator;
import bisq.desktop.util.validation.SecurityDepositValidator;

import bisq.core.btc.setup.WalletsSetup;
import bisq.core.provider.price.PriceFeedService;
import bisq.core.user.Preferences;
import bisq.core.util.coin.ImmutableCoinFormatter;
import bisq.core.util.coin.BsqFormatter;

import bisq.network.p2p.P2PService;

import com.google.inject.Inject;

class CreateOfferViewModel extends MutableOfferViewModel<CreateOfferDataModel> implements ViewModel {

    @Inject
    public CreateOfferViewModel(CreateOfferDataModel dataModel, FiatVolumeValidator fiatVolumeValidator, FiatPriceValidator fiatPriceValidator, AltcoinValidator altcoinValidator, BtcValidator btcValidator, BsqValidator bsqValidator, SecurityDepositValidator securityDepositValidator, P2PService p2PService, WalletsSetup walletsSetup, PriceFeedService priceFeedService, Navigation navigation, Preferences preferences, ImmutableCoinFormatter btcFormatter, BsqFormatter bsqFormatter) {
        super(dataModel, fiatVolumeValidator, fiatPriceValidator, altcoinValidator, btcValidator, bsqValidator, securityDepositValidator, p2PService, walletsSetup, priceFeedService, navigation, preferences, btcFormatter, bsqFormatter);
    }
}
