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

package bisq.desktop.main.disputes.trader;

import bisq.desktop.common.view.ActivatableView;
import bisq.desktop.common.view.FxmlView;
import bisq.desktop.components.AutoTooltipButton;
import bisq.desktop.components.AutoTooltipLabel;
import bisq.desktop.components.AutoTooltipTableColumn;
import bisq.desktop.components.HyperlinkWithIcon;
import bisq.desktop.components.InputTextField;
import bisq.desktop.main.Chat.Chat;
import bisq.desktop.main.overlays.popups.Popup;
import bisq.desktop.main.overlays.windows.ContractWindow;
import bisq.desktop.main.overlays.windows.DisputeSummaryWindow;
import bisq.desktop.main.overlays.windows.SendPrivateNotificationWindow;
import bisq.desktop.main.overlays.windows.TradeDetailsWindow;
import bisq.desktop.util.GUIUtil;

import bisq.core.account.witness.AccountAgeWitnessService;
import bisq.core.alert.PrivateNotificationManager;
import bisq.core.app.AppOptionKeys;
import bisq.core.arbitration.Dispute;
import bisq.core.arbitration.DisputeChatSession;
import bisq.core.arbitration.DisputeManager;
import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.Res;
import bisq.core.trade.Contract;
import bisq.core.trade.Trade;
import bisq.core.trade.TradeManager;
import bisq.core.util.BSFormatter;

import bisq.network.p2p.NodeAddress;
import bisq.network.p2p.P2PService;

import bisq.common.app.Version;
import bisq.common.crypto.KeyRing;
import bisq.common.crypto.PubKeyRing;
import bisq.common.util.Utilities;

import com.google.inject.name.Named;

import javax.inject.Inject;

import com.google.common.collect.Lists;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javafx.geometry.Insets;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;

import javafx.event.EventHandler;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import javafx.util.Callback;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;

// will be probably only used for arbitration communication, will be renamed and the icon changed
@FxmlView
public class TraderDisputeView extends ActivatableView<VBox, Void> {

    private final DisputeManager disputeManager;
    protected final KeyRing keyRing;
    private final TradeManager tradeManager;
    protected final BSFormatter formatter;
    private final DisputeSummaryWindow disputeSummaryWindow;
    private final PrivateNotificationManager privateNotificationManager;
    private final ContractWindow contractWindow;
    private final TradeDetailsWindow tradeDetailsWindow;
    private final P2PService p2PService;

    private final AccountAgeWitnessService accountAgeWitnessService;
    private final boolean useDevPrivilegeKeys;

    private TableView<Dispute> tableView;
    private SortedList<Dispute> sortedList;

    @Getter
    private Dispute selectedDispute;

    private Chat disputeChat;

    private ChangeListener<Boolean> selectedDisputeClosedPropertyListener;
    private Subscription selectedDisputeSubscription;
    private EventHandler<KeyEvent> keyEventEventHandler;
    private Scene scene;
    protected FilteredList<Dispute> filteredList;
    private InputTextField filterTextField;
    private ChangeListener<String> filterTextFieldListener;
    protected HBox filterBox;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, lifecycle
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public TraderDisputeView(DisputeManager disputeManager,
                             KeyRing keyRing,
                             TradeManager tradeManager,
                             BSFormatter formatter,
                             DisputeSummaryWindow disputeSummaryWindow,
                             PrivateNotificationManager privateNotificationManager,
                             ContractWindow contractWindow,
                             TradeDetailsWindow tradeDetailsWindow,
                             P2PService p2PService,
                             AccountAgeWitnessService accountAgeWitnessService,
                             @Named(AppOptionKeys.USE_DEV_PRIVILEGE_KEYS) boolean useDevPrivilegeKeys) {
        this.disputeManager = disputeManager;
        this.keyRing = keyRing;
        this.tradeManager = tradeManager;
        this.formatter = formatter;
        this.disputeSummaryWindow = disputeSummaryWindow;
        this.privateNotificationManager = privateNotificationManager;
        this.contractWindow = contractWindow;
        this.tradeDetailsWindow = tradeDetailsWindow;
        this.p2PService = p2PService;
        this.accountAgeWitnessService = accountAgeWitnessService;
        this.useDevPrivilegeKeys = useDevPrivilegeKeys;
    }

    @Override
    public void initialize() {
        Label label = new AutoTooltipLabel(Res.get("support.filter"));
        HBox.setMargin(label, new Insets(5, 0, 0, 0));
        filterTextField = new InputTextField();
        filterTextField.setText("open");
        filterTextFieldListener = (observable, oldValue, newValue) -> applyFilteredListPredicate(filterTextField.getText());

        filterBox = new HBox();
        filterBox.setSpacing(5);
        filterBox.getChildren().addAll(label, filterTextField);
        VBox.setVgrow(filterBox, Priority.NEVER);
        filterBox.setVisible(false);
        filterBox.setManaged(false);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.SOMETIMES);
        tableView.setMinHeight(150);

        root.getChildren().addAll(filterBox, tableView);

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Label placeholder = new AutoTooltipLabel(Res.get("support.noTickets"));
        placeholder.setWrapText(true);
        tableView.setPlaceholder(placeholder);
        tableView.getSelectionModel().clearSelection();

        tableView.getColumns().add(getSelectColumn());

        TableColumn<Dispute, Dispute> contractColumn = getContractColumn();
        tableView.getColumns().add(contractColumn);

        TableColumn<Dispute, Dispute> dateColumn = getDateColumn();
        tableView.getColumns().add(dateColumn);

        TableColumn<Dispute, Dispute> tradeIdColumn = getTradeIdColumn();
        tableView.getColumns().add(tradeIdColumn);

        TableColumn<Dispute, Dispute> buyerOnionAddressColumn = getBuyerOnionAddressColumn();
        tableView.getColumns().add(buyerOnionAddressColumn);

        TableColumn<Dispute, Dispute> sellerOnionAddressColumn = getSellerOnionAddressColumn();
        tableView.getColumns().add(sellerOnionAddressColumn);


        TableColumn<Dispute, Dispute> marketColumn = getMarketColumn();
        tableView.getColumns().add(marketColumn);

        TableColumn<Dispute, Dispute> roleColumn = getRoleColumn();
        tableView.getColumns().add(roleColumn);

        TableColumn<Dispute, Dispute> stateColumn = getStateColumn();
        tableView.getColumns().add(stateColumn);

        tradeIdColumn.setComparator(Comparator.comparing(Dispute::getTradeId));
        dateColumn.setComparator(Comparator.comparing(Dispute::getOpeningDate));
        buyerOnionAddressColumn.setComparator(Comparator.comparing(this::getBuyerOnionAddressColumnLabel));
        sellerOnionAddressColumn.setComparator(Comparator.comparing(this::getSellerOnionAddressColumnLabel));
        marketColumn.setComparator((o1, o2) -> formatter.getCurrencyPair(o1.getContract().getOfferPayload().getCurrencyCode()).compareTo(o2.getContract().getOfferPayload().getCurrencyCode()));

        dateColumn.setSortType(TableColumn.SortType.DESCENDING);
        tableView.getSortOrder().add(dateColumn);

        selectedDisputeClosedPropertyListener = (observable, oldValue, newValue) -> disputeChat.setInputBoxVisible(!newValue);

        keyEventEventHandler = event -> {
            if (Utilities.isAltOrCtrlPressed(KeyCode.L, event)) {
                Map<String, List<Dispute>> map = new HashMap<>();
                disputeManager.getDisputesAsObservableList().forEach(dispute -> {
                    String tradeId = dispute.getTradeId();
                    List<Dispute> list;
                    if (!map.containsKey(tradeId))
                        map.put(tradeId, new ArrayList<>());

                    list = map.get(tradeId);
                    list.add(dispute);
                });
                List<List<Dispute>> disputeGroups = new ArrayList<>();
                map.forEach((key, value) -> disputeGroups.add(value));
                disputeGroups.sort((o1, o2) -> !o1.isEmpty() && !o2.isEmpty() ? o1.get(0).getOpeningDate().compareTo(o2.get(0).getOpeningDate()) : 0);
                StringBuilder stringBuilder = new StringBuilder();

                // We don't translate that as it is not intended for the public
                stringBuilder.append("Summary of all disputes (No. of disputes: ").append(disputeGroups.size()).append(")\n\n");
                disputeGroups.forEach(disputeGroup -> {
                    Dispute dispute0 = disputeGroup.get(0);
                    stringBuilder.append("##########################################################################################/\n")
                            .append("## Trade ID: ")
                            .append(dispute0.getTradeId())
                            .append("\n")
                            .append("## Date: ")
                            .append(formatter.formatDateTime(dispute0.getOpeningDate()))
                            .append("\n")
                            .append("## Is support ticket: ")
                            .append(dispute0.isSupportTicket())
                            .append("\n");
                    if (dispute0.disputeResultProperty().get() != null && dispute0.disputeResultProperty().get().getReason() != null) {
                        stringBuilder.append("## Reason: ")
                                .append(dispute0.disputeResultProperty().get().getReason())
                                .append("\n");
                    }
                    stringBuilder.append("##########################################################################################/\n")
                            .append("\n");
                    disputeGroup.forEach(dispute -> {
                        stringBuilder
                                .append("*******************************************************************************************\n")
                                .append("** Trader's ID: ")
                                .append(dispute.getTraderId())
                                .append("\n*******************************************************************************************\n")
                                .append("\n");
                        dispute.getDisputeCommunicationMessages().forEach(m -> {
                            String role = m.isSenderIsTrader() ? ">> Trader's msg: " : "<< Arbitrator's msg: ";
                            stringBuilder.append(role)
                                    .append(m.getMessage())
                                    .append("\n");
                        });
                        stringBuilder.append("\n");
                    });
                    stringBuilder.append("\n");
                });
                String message = stringBuilder.toString();
                // We don't translate that as it is not intended for the public
                new Popup<>().headLine("All disputes (" + disputeGroups.size() + ")")
                        .information(message)
                        .width(1000)
                        .actionButtonText("Copy")
                        .onAction(() -> Utilities.copyToClipboard(message))
                        .show();
            } else if (Utilities.isAltOrCtrlPressed(KeyCode.U, event)) {
                // Hidden shortcut to re-open a dispute. Allow it also for traders not only arbitrator.
                if (selectedDispute != null) {
                    if (selectedDisputeClosedPropertyListener != null)
                        selectedDispute.isClosedProperty().removeListener(selectedDisputeClosedPropertyListener);
                    selectedDispute.setIsClosed(false);
                }
            } else if (Utilities.isAltOrCtrlPressed(KeyCode.R, event)) {
                if (selectedDispute != null) {
                    PubKeyRing pubKeyRing = selectedDispute.getTraderPubKeyRing();
                    NodeAddress nodeAddress;
                    if (pubKeyRing.equals(selectedDispute.getContract().getBuyerPubKeyRing()))
                        nodeAddress = selectedDispute.getContract().getBuyerNodeAddress();
                    else
                        nodeAddress = selectedDispute.getContract().getSellerNodeAddress();

                    new SendPrivateNotificationWindow(pubKeyRing, nodeAddress, useDevPrivilegeKeys)
                            .onAddAlertMessage(privateNotificationManager::sendPrivateNotificationMessageIfKeyIsValid)
                            .show();
                }
            }
        };

        disputeChat = new Chat(disputeManager.getChatManager(), formatter);
        disputeChat.initialize();
    }

    @Override
    protected void activate() {
        filterTextField.textProperty().addListener(filterTextFieldListener);
        disputeManager.cleanupDisputes();

        filteredList = new FilteredList<>(disputeManager.getDisputesAsObservableList());
        applyFilteredListPredicate(filterTextField.getText());

        sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);

        // sortedList.setComparator((o1, o2) -> o2.getOpeningDate().compareTo(o1.getOpeningDate()));
        selectedDisputeSubscription = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(), this::onSelectDispute);

        Dispute selectedItem = tableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null)
            tableView.getSelectionModel().select(selectedItem);

        if (disputeChat != null) {
            disputeChat.activate();
            disputeChat.scrollToBottom();
        }

        scene = root.getScene();
        if (scene != null)
            scene.addEventHandler(KeyEvent.KEY_RELEASED, keyEventEventHandler);

        // If doPrint=true we print out a html page which opens tabs with all deposit txs
        // (firefox needs about:config change to allow > 20 tabs)
        // Useful to check if there any funds in not finished trades (no payout tx done).
        // Last check 10.02.2017 found 8 trades and we contacted all traders as far as possible (email if available
        // otherwise in-app private notification)
        boolean doPrint = false;
        //noinspection ConstantConditions
        if (doPrint) {
            try {
                DateFormat formatter = new SimpleDateFormat("dd/MM/yy");
                //noinspection UnusedAssignment
                Date startDate = formatter.parse("10/02/17");
                startDate = new Date(0); // print all from start

                HashMap<String, Dispute> map = new HashMap<>();
                disputeManager.getDisputesAsObservableList().forEach(dispute -> map.put(dispute.getDepositTxId(), dispute));

                final Date finalStartDate = startDate;
                List<Dispute> disputes = new ArrayList<>(map.values());
                disputes.sort(Comparator.comparing(Dispute::getOpeningDate));
                List<List<Dispute>> subLists = Lists.partition(disputes, 1000);
                StringBuilder sb = new StringBuilder();
                // We don't translate that as it is not intended for the public
                subLists.forEach(list -> {
                    StringBuilder sb1 = new StringBuilder("\n<html><head><script type=\"text/javascript\">function load(){\n");
                    StringBuilder sb2 = new StringBuilder("\n}</script></head><body onload=\"load()\">\n");
                    list.forEach(dispute -> {
                        if (dispute.getOpeningDate().after(finalStartDate)) {
                            String txId = dispute.getDepositTxId();
                            sb1.append("window.open(\"https://blockchain.info/tx/").append(txId).append("\", '_blank');\n");

                            sb2.append("Dispute ID: ").append(dispute.getId()).
                                    append(" Tx ID: ").
                                    append("<a href=\"https://blockchain.info/tx/").append(txId).append("\">").
                                    append(txId).append("</a> ").
                                    append("Opening date: ").append(formatter.format(dispute.getOpeningDate())).append("<br/>\n");
                        }
                    });
                    sb2.append("</body></html>");
                    String res = sb1.toString() + sb2.toString();

                    sb.append(res).append("\n\n\n");
                });
                log.info(sb.toString());
            } catch (ParseException ignore) {
            }
        }
        GUIUtil.requestFocus(filterTextField);
    }

    @Override
    protected void deactivate() {
        filterTextField.textProperty().removeListener(filterTextFieldListener);
        sortedList.comparatorProperty().unbind();
        selectedDisputeSubscription.unsubscribe();
        removeListenersOnSelectDispute();

        if (scene != null)
            scene.removeEventHandler(KeyEvent.KEY_RELEASED, keyEventEventHandler);

        if (disputeChat != null)
            disputeChat.deactivate();
    }

    protected void applyFilteredListPredicate(String filterString) {
        // If in trader view we must not display arbitrators own disputes as trader (must not happen anyway)
        filteredList.setPredicate(dispute -> !dispute.getArbitratorPubKeyRing().equals(keyRing.getPubKeyRing()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // UI actions
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void onOpenContract(Dispute dispute) {
        contractWindow.show(dispute);
    }

    private void removeListenersOnSelectDispute() {
        if (selectedDispute != null) {
            if (selectedDisputeClosedPropertyListener != null)
                selectedDispute.isClosedProperty().removeListener(selectedDisputeClosedPropertyListener);
        }
    }

    private void addListenersOnSelectDispute() {
        if (selectedDispute != null)
            selectedDispute.isClosedProperty().addListener(selectedDisputeClosedPropertyListener);
    }

    private void onSelectDispute(Dispute dispute) {
        removeListenersOnSelectDispute();
        if (dispute == null) {
            if (root.getChildren().size() > 2)
                root.getChildren().remove(2);

            selectedDispute = null;
        } else if (selectedDispute != dispute) {
            this.selectedDispute = dispute;
            if (disputeChat != null) {
                Button closeDisputeButton = null;
                if (!dispute.isClosed() && !disputeManager.isTrader(dispute)) {
                    closeDisputeButton = new AutoTooltipButton(Res.get("support.closeTicket"));
                    closeDisputeButton.setOnAction(e -> onCloseDispute(getSelectedDispute()));
                }
                disputeChat.display(new DisputeChatSession(dispute, disputeManager), closeDisputeButton,
                        root.widthProperty()
                );
            }

            if (root.getChildren().size() > 2)
                root.getChildren().remove(2);
            root.getChildren().add(2, disputeChat);
        }

        addListenersOnSelectDispute();
    }

    private void onCloseDispute(Dispute dispute) {
        long protocolVersion = dispute.getContract().getOfferPayload().getProtocolVersion();
        if (protocolVersion == Version.TRADE_PROTOCOL_VERSION) {
            disputeSummaryWindow.onFinalizeDispute(() -> disputeChat.removeInputBox())
                    .show(dispute);
        } else {
            new Popup<>()
                    .warning(Res.get("support.wrongVersion", protocolVersion))
                    .show();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Table
    ///////////////////////////////////////////////////////////////////////////////////////////

    private TableColumn<Dispute, Dispute> getSelectColumn() {
        TableColumn<Dispute, Dispute> column = new AutoTooltipTableColumn<>(Res.get("shared.select"));
        column.setMinWidth(80);
        column.setMaxWidth(80);
        column.setSortable(false);
        column.getStyleClass().add("first-column");

        column.setCellValueFactory((addressListItem) ->
                new ReadOnlyObjectWrapper<>(addressListItem.getValue()));
        column.setCellFactory(
                new Callback<>() {

                    @Override
                    public TableCell<Dispute, Dispute> call(TableColumn<Dispute,
                            Dispute> column) {
                        return new TableCell<>() {

                            Button button;

                            @Override
                            public void updateItem(final Dispute item, boolean empty) {
                                super.updateItem(item, empty);

                                if (item != null && !empty) {
                                    if (button == null) {
                                        button = new AutoTooltipButton(Res.get("shared.select"));
                                        setGraphic(button);
                                    }
                                    button.setOnAction(e -> tableView.getSelectionModel().select(item));
                                } else {
                                    setGraphic(null);
                                    if (button != null) {
                                        button.setOnAction(null);
                                        button = null;
                                    }
                                }
                            }
                        };
                    }
                });
        return column;
    }

    private TableColumn<Dispute, Dispute> getContractColumn() {
        TableColumn<Dispute, Dispute> column = new AutoTooltipTableColumn<>(Res.get("shared.details")) {
            {
                setMinWidth(80);
                setSortable(false);
            }
        };
        column.setCellValueFactory((dispute) -> new ReadOnlyObjectWrapper<>(dispute.getValue()));
        column.setCellFactory(
                new Callback<>() {

                    @Override
                    public TableCell<Dispute, Dispute> call(TableColumn<Dispute, Dispute> column) {
                        return new TableCell<>() {
                            Button button;

                            @Override
                            public void updateItem(final Dispute item, boolean empty) {
                                super.updateItem(item, empty);

                                if (item != null && !empty) {
                                    if (button == null) {
                                        button = new AutoTooltipButton(Res.get("shared.details"));
                                        setGraphic(button);
                                    }
                                    button.setOnAction(e -> onOpenContract(item));
                                } else {
                                    setGraphic(null);
                                    if (button != null) {
                                        button.setOnAction(null);
                                        button = null;
                                    }
                                }
                            }
                        };
                    }
                });
        return column;
    }

    private TableColumn<Dispute, Dispute> getDateColumn() {
        TableColumn<Dispute, Dispute> column = new AutoTooltipTableColumn<>(Res.get("shared.date")) {
            {
                setMinWidth(180);
            }
        };
        column.setCellValueFactory((dispute) -> new ReadOnlyObjectWrapper<>(dispute.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<Dispute, Dispute> call(TableColumn<Dispute, Dispute> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final Dispute item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                    setText(formatter.formatDateTime(item.getOpeningDate()));
                                else
                                    setText("");
                            }
                        };
                    }
                });
        return column;
    }

    private TableColumn<Dispute, Dispute> getTradeIdColumn() {
        TableColumn<Dispute, Dispute> column = new AutoTooltipTableColumn<>(Res.get("shared.tradeId")) {
            {
                setMinWidth(110);
            }
        };
        column.setCellValueFactory((dispute) -> new ReadOnlyObjectWrapper<>(dispute.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<Dispute, Dispute> call(TableColumn<Dispute, Dispute> column) {
                        return new TableCell<>() {
                            private HyperlinkWithIcon field;

                            @Override
                            public void updateItem(final Dispute item, boolean empty) {
                                super.updateItem(item, empty);

                                if (item != null && !empty) {
                                    field = new HyperlinkWithIcon(item.getShortTradeId());
                                    Optional<Trade> tradeOptional = tradeManager.getTradeById(item.getTradeId());
                                    if (tradeOptional.isPresent()) {
                                        field.setMouseTransparent(false);
                                        field.setTooltip(new Tooltip(Res.get("tooltip.openPopupForDetails")));
                                        field.setOnAction(event -> tradeDetailsWindow.show(tradeOptional.get()));
                                    } else {
                                        field.setMouseTransparent(true);
                                    }
                                    setGraphic(field);
                                } else {
                                    setGraphic(null);
                                    if (field != null)
                                        field.setOnAction(null);
                                }
                            }
                        };
                    }
                });
        return column;
    }

    private TableColumn<Dispute, Dispute> getBuyerOnionAddressColumn() {
        TableColumn<Dispute, Dispute> column = new AutoTooltipTableColumn<>(Res.get("support.buyerAddress")) {
            {
                setMinWidth(190);
            }
        };
        column.setCellValueFactory((dispute) -> new ReadOnlyObjectWrapper<>(dispute.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<Dispute, Dispute> call(TableColumn<Dispute, Dispute> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final Dispute item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                    setText(getBuyerOnionAddressColumnLabel(item));
                                else
                                    setText("");
                            }
                        };
                    }
                });
        return column;
    }

    private TableColumn<Dispute, Dispute> getSellerOnionAddressColumn() {
        TableColumn<Dispute, Dispute> column = new AutoTooltipTableColumn<>(Res.get("support.sellerAddress")) {
            {
                setMinWidth(190);
            }
        };
        column.setCellValueFactory((dispute) -> new ReadOnlyObjectWrapper<>(dispute.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<Dispute, Dispute> call(TableColumn<Dispute, Dispute> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final Dispute item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                    setText(getSellerOnionAddressColumnLabel(item));
                                else
                                    setText("");
                            }
                        };
                    }
                });
        return column;
    }


    private String getBuyerOnionAddressColumnLabel(Dispute item) {
        Contract contract = item.getContract();
        if (contract != null) {
            NodeAddress buyerNodeAddress = contract.getBuyerNodeAddress();
            if (buyerNodeAddress != null) {
                String nrOfDisputes = disputeManager.getNrOfDisputes(true, contract);
                long accountAge = accountAgeWitnessService.getAccountAge(contract.getBuyerPaymentAccountPayload(), contract.getBuyerPubKeyRing());
                String age = formatter.formatAccountAge(accountAge);
                String postFix = CurrencyUtil.isFiatCurrency(item.getContract().getOfferPayload().getCurrencyCode()) ? " / " + age : "";
                return buyerNodeAddress.getHostNameWithoutPostFix() + " (" + nrOfDisputes + postFix + ")";
            } else
                return Res.get("shared.na");
        } else {
            return Res.get("shared.na");
        }
    }

    private String getSellerOnionAddressColumnLabel(Dispute item) {
        Contract contract = item.getContract();
        if (contract != null) {
            NodeAddress sellerNodeAddress = contract.getSellerNodeAddress();
            if (sellerNodeAddress != null) {
                String nrOfDisputes = disputeManager.getNrOfDisputes(false, contract);
                long accountAge = accountAgeWitnessService.getAccountAge(contract.getSellerPaymentAccountPayload(), contract.getSellerPubKeyRing());
                String age = formatter.formatAccountAge(accountAge);
                String postFix = CurrencyUtil.isFiatCurrency(item.getContract().getOfferPayload().getCurrencyCode()) ? " / " + age : "";
                return sellerNodeAddress.getHostNameWithoutPostFix() + " (" + nrOfDisputes + postFix + ")";
            } else
                return Res.get("shared.na");
        } else {
            return Res.get("shared.na");
        }
    }

    private TableColumn<Dispute, Dispute> getMarketColumn() {
        TableColumn<Dispute, Dispute> column = new AutoTooltipTableColumn<>(Res.get("shared.market")) {
            {
                setMinWidth(80);
            }
        };
        column.setCellValueFactory((dispute) -> new ReadOnlyObjectWrapper<>(dispute.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<Dispute, Dispute> call(TableColumn<Dispute, Dispute> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final Dispute item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                    setText(formatter.getCurrencyPair(item.getContract().getOfferPayload().getCurrencyCode()));
                                else
                                    setText("");
                            }
                        };
                    }
                });
        return column;
    }

    private TableColumn<Dispute, Dispute> getRoleColumn() {
        TableColumn<Dispute, Dispute> column = new AutoTooltipTableColumn<>(Res.get("support.role")) {
            {
                setMinWidth(130);
            }
        };
        column.setCellValueFactory((dispute) -> new ReadOnlyObjectWrapper<>(dispute.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<Dispute, Dispute> call(TableColumn<Dispute, Dispute> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final Dispute item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    if (item.isDisputeOpenerIsMaker())
                                        setText(item.isDisputeOpenerIsBuyer() ? Res.get("support.buyerOfferer") : Res.get("support.sellerOfferer"));
                                    else
                                        setText(item.isDisputeOpenerIsBuyer() ? Res.get("support.buyerTaker") : Res.get("support.sellerTaker"));
                                } else {
                                    setText("");
                                }
                            }
                        };
                    }
                });
        return column;
    }

    private TableColumn<Dispute, Dispute> getStateColumn() {
        TableColumn<Dispute, Dispute> column = new AutoTooltipTableColumn<>(Res.get("support.state")) {
            {
                setMinWidth(50);
            }
        };
        column.getStyleClass().add("last-column");
        column.setCellValueFactory((dispute) -> new ReadOnlyObjectWrapper<>(dispute.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<Dispute, Dispute> call(TableColumn<Dispute, Dispute> column) {
                        return new TableCell<>() {


                            ReadOnlyBooleanProperty closedProperty;
                            ChangeListener<Boolean> listener;

                            @Override
                            public void updateItem(final Dispute item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    listener = (observable, oldValue, newValue) -> {
                                        setText(newValue ? Res.get("support.closed") : Res.get("support.open"));
                                        if (getTableRow() != null)
                                            getTableRow().setOpacity(newValue ? 0.4 : 1);
                                    };
                                    closedProperty = item.isClosedProperty();
                                    closedProperty.addListener(listener);
                                    boolean isClosed = item.isClosed();
                                    setText(isClosed ? Res.get("support.closed") : Res.get("support.open"));
                                    if (getTableRow() != null)
                                        getTableRow().setOpacity(isClosed ? 0.4 : 1);
                                } else {
                                    if (closedProperty != null) {
                                        closedProperty.removeListener(listener);
                                        closedProperty = null;
                                    }
                                    setText("");
                                }
                            }
                        };
                    }
                });
        return column;
    }

}


