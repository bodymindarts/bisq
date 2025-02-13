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

package bisq.desktop.main.portfolio.pendingtrades;

import bisq.desktop.common.view.ActivatableViewAndModel;
import bisq.desktop.common.view.FxmlView;
import bisq.desktop.components.AutoTooltipLabel;
import bisq.desktop.components.HyperlinkWithIcon;
import bisq.desktop.components.PeerInfoIcon;
import bisq.desktop.main.Chat.Chat;
import bisq.desktop.main.MainView;
import bisq.desktop.main.overlays.popups.Popup;
import bisq.desktop.main.overlays.windows.TradeDetailsWindow;
import bisq.desktop.util.FormBuilder;
import bisq.desktop.util.CssTheme;

import bisq.core.alert.PrivateNotificationManager;
import bisq.core.app.AppOptionKeys;
import bisq.core.arbitration.messages.DisputeCommunicationMessage;
import bisq.core.locale.Res;
import bisq.core.trade.Trade;
import bisq.core.trade.TradeChatSession;
import bisq.core.user.Preferences;
import bisq.core.util.BSFormatter;

import bisq.network.p2p.NodeAddress;

import bisq.common.UserThread;
import bisq.common.util.Utilities;

import com.google.inject.name.Named;

import javax.inject.Inject;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;

import com.jfoenix.controls.JFXBadge;

import javafx.fxml.FXML;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;

import javafx.event.EventHandler;

import javafx.collections.ListChangeListener;
import javafx.collections.transformation.SortedList;

import javafx.util.Callback;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@FxmlView
public class PendingTradesView extends ActivatableViewAndModel<VBox, PendingTradesViewModel> {

    private final TradeDetailsWindow tradeDetailsWindow;
    private final BSFormatter formatter;
    private final PrivateNotificationManager privateNotificationManager;
    private final boolean useDevPrivilegeKeys;
    private final Preferences preferences;
    @FXML
    TableView<PendingTradesListItem> tableView;
    @FXML
    TableColumn<PendingTradesListItem, PendingTradesListItem> priceColumn, volumeColumn, amountColumn, avatarColumn,
            marketColumn, roleColumn, paymentMethodColumn, tradeIdColumn, dateColumn, chatColumn;
    private SortedList<PendingTradesListItem> sortedList;
    private TradeSubView selectedSubView;
    private EventHandler<KeyEvent> keyEventEventHandler;
    private Scene scene;
    private Subscription selectedTableItemSubscription;
    private Subscription selectedItemSubscription;
    private Stage chatPopupStage;
    private ListChangeListener<PendingTradesListItem> tradesListChangeListener;
    private Map<String, Long> newChatMessagesByTradeMap = new HashMap<>();
    private String tradeIdOfOpenChat;
    private double chatPopupStageXPosition = -1;
    private double chatPopupStageYPosition = -1;
    private ChangeListener<Number> xPositionListener;
    private ChangeListener<Number> yPositionListener;

    private Map<String, Button> buttonByTrade = new HashMap<>();
    private Map<String, JFXBadge> badgeByTrade = new HashMap<>();
    private Map<String, ListChangeListener<DisputeCommunicationMessage>> listenerByTrade = new HashMap<>();
    private TradeChatSession.DisputeStateListener disputeStateListener;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, Initialisation
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public PendingTradesView(PendingTradesViewModel model,
                             TradeDetailsWindow tradeDetailsWindow,
                             BSFormatter formatter,
                             PrivateNotificationManager privateNotificationManager,
                             Preferences preferences,
                             @Named(AppOptionKeys.USE_DEV_PRIVILEGE_KEYS) boolean useDevPrivilegeKeys) {
        super(model);
        this.tradeDetailsWindow = tradeDetailsWindow;
        this.formatter = formatter;
        this.privateNotificationManager = privateNotificationManager;
        this.preferences = preferences;
        this.useDevPrivilegeKeys = useDevPrivilegeKeys;
    }

    @Override
    public void initialize() {
        priceColumn.setGraphic(new AutoTooltipLabel(Res.get("shared.price")));
        amountColumn.setGraphic(new AutoTooltipLabel(Res.get("shared.amountWithCur", Res.getBaseCurrencyCode())));
        volumeColumn.setGraphic(new AutoTooltipLabel(Res.get("shared.amount")));
        marketColumn.setGraphic(new AutoTooltipLabel(Res.get("shared.market")));
        roleColumn.setGraphic(new AutoTooltipLabel(Res.get("portfolio.pending.role")));
        dateColumn.setGraphic(new AutoTooltipLabel(Res.get("shared.dateTime")));
        tradeIdColumn.setGraphic(new AutoTooltipLabel(Res.get("shared.tradeId")));
        paymentMethodColumn.setGraphic(new AutoTooltipLabel(Res.get("shared.paymentMethod")));
        avatarColumn.setText("");
        chatColumn.setText("");

        setTradeIdColumnCellFactory();
        setDateColumnCellFactory();
        setAmountColumnCellFactory();
        setPriceColumnCellFactory();
        setVolumeColumnCellFactory();
        setPaymentMethodColumnCellFactory();
        setMarketColumnCellFactory();
        setRoleColumnCellFactory();
        setAvatarColumnCellFactory();
        setChatColumnCellFactory();

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new AutoTooltipLabel(Res.get("table.placeholder.noItems", Res.get("shared.openTrades"))));
        tableView.setMinHeight(100);

        tradeIdColumn.setComparator(Comparator.comparing(o -> o.getTrade().getId()));
        dateColumn.setComparator(Comparator.comparing(o -> o.getTrade().getDate()));
        volumeColumn.setComparator((o1, o2) -> {
            if (o1.getTrade().getTradeVolume() != null && o2.getTrade().getTradeVolume() != null)
                return o1.getTrade().getTradeVolume().compareTo(o2.getTrade().getTradeVolume());
            else
                return 0;
        });
        amountColumn.setComparator((o1, o2) -> {
            if (o1.getTrade().getTradeAmount() != null && o2.getTrade().getTradeAmount() != null)
                return o1.getTrade().getTradeAmount().compareTo(o2.getTrade().getTradeAmount());
            else
                return 0;
        });
        priceColumn.setComparator(Comparator.comparing(PendingTradesListItem::getPrice));
        paymentMethodColumn.setComparator(Comparator.comparing(o -> o.getTrade().getOffer() != null ?
                o.getTrade().getOffer().getPaymentMethod().getId() : null));
        avatarColumn.setComparator((o1, o2) -> {
            if (o1.getTrade().getTradingPeerNodeAddress() != null && o2.getTrade().getTradingPeerNodeAddress() != null)
                return o1.getTrade().getTradingPeerNodeAddress().getFullAddress().compareTo(o2.getTrade().getTradingPeerNodeAddress().getFullAddress());
            else
                return 0;
        });
        roleColumn.setComparator(Comparator.comparing(model::getMyRole));
        marketColumn.setComparator(Comparator.comparing(model::getMarketLabel));

        dateColumn.setSortType(TableColumn.SortType.DESCENDING);
        tableView.getSortOrder().add(dateColumn);


        // we use a hidden emergency shortcut to open support ticket
        keyEventEventHandler = keyEvent -> {
            if (Utilities.isAltOrCtrlPressed(KeyCode.O, keyEvent)) {
                Popup popup = new Popup<>();
                popup.headLine(Res.get("portfolio.pending.openSupportTicket.headline"))
                        .message(Res.get("portfolio.pending.openSupportTicket.msg"))
                        .actionButtonText(Res.get("portfolio.pending.openSupportTicket.headline"))
                        .onAction(model.dataModel::onOpenSupportTicket)
                        .closeButtonText(Res.get("shared.cancel"))
                        .onClose(popup::hide)
                        .show();
            } else if (Utilities.isAltPressed(KeyCode.Y, keyEvent)) {
                new Popup<>().warning(Res.get("portfolio.pending.removeFailedTrade"))
                        .onAction(model.dataModel::onMoveToFailedTrades)
                        .show();
            }
        };

        tradesListChangeListener = c -> updateNewChatMessagesByTradeMap();
    }

    @Override
    protected void activate() {
        sortedList = new SortedList<>(model.dataModel.list);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);

        scene = root.getScene();
        if (scene != null) {
            scene.addEventHandler(KeyEvent.KEY_RELEASED, keyEventEventHandler);

            //TODO: in what cases is it necessary to request focus?
            /*appFocusSubscription = EasyBind.subscribe(scene.getWindow().focusedProperty(), isFocused -> {
                if (isFocused && model.dataModel.selectedItemProperty.get() != null) {
                    // Focus selectedItem from model
                    int index = table.getItems().indexOf(model.dataModel.selectedItemProperty.get());
                    UserThread.execute(() -> {
                        //TODO app wide focus
                        //table.requestFocus();
                        //UserThread.execute(() -> table.getFocusModel().focus(index));
                    });
                }
            });*/
        }

        selectedItemSubscription = EasyBind.subscribe(model.dataModel.selectedItemProperty, selectedItem -> {
            if (selectedItem != null) {
                if (selectedSubView != null)
                    selectedSubView.deactivate();

                if (selectedItem.getTrade() != null) {
                    selectedSubView = model.dataModel.tradeManager.isBuyer(model.dataModel.getOffer()) ?
                            new BuyerSubView(model) : new SellerSubView(model);

                    selectedSubView.setMinHeight(440);
                    VBox.setVgrow(selectedSubView, Priority.ALWAYS);
                    if (root.getChildren().size() == 1)
                        root.getChildren().add(selectedSubView);
                    else if (root.getChildren().size() == 2)
                        root.getChildren().set(1, selectedSubView);
                }

                updateTableSelection();
            } else {
                removeSelectedSubView();
            }

            model.onSelectedItemChanged(selectedItem);

            if (selectedSubView != null && selectedItem != null)
                selectedSubView.activate();
        });

        selectedTableItemSubscription = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                selectedItem -> {
                    if (selectedItem != null && !selectedItem.equals(model.dataModel.selectedItemProperty.get()))
                        model.dataModel.onSelectItem(selectedItem);
                });

        updateTableSelection();

        model.dataModel.list.addListener(tradesListChangeListener);
        updateNewChatMessagesByTradeMap();
    }

    @Override
    protected void deactivate() {
        sortedList.comparatorProperty().unbind();
        selectedItemSubscription.unsubscribe();
        selectedTableItemSubscription.unsubscribe();

        removeSelectedSubView();

        model.dataModel.list.removeListener(tradesListChangeListener);

        if (scene != null)
            scene.removeEventHandler(KeyEvent.KEY_RELEASED, keyEventEventHandler);
    }

    private void removeSelectedSubView() {
        if (selectedSubView != null) {
            selectedSubView.deactivate();
            root.getChildren().remove(selectedSubView);
            selectedSubView = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Chat
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void updateNewChatMessagesByTradeMap() {
        model.dataModel.list.forEach(t -> {
            Trade trade = t.getTrade();
            newChatMessagesByTradeMap.put(trade.getId(),
                    trade.getCommunicationMessages().stream()
                            .filter(m -> !m.isWasDisplayed())
                            .filter(m -> !m.isSystemMessage())
                            .count());
        });
    }

    private void openChat(Trade trade) {
        if (chatPopupStage != null)
            chatPopupStage.close();

        if (trade.getCommunicationMessages().isEmpty()) {
            ((TradeChatSession) model.dataModel.tradeManager.getChatManager().getChatSession()).addSystemMsg(trade);
        }

        trade.getCommunicationMessages().forEach(m -> m.setWasDisplayed(true));
        trade.persist();
        tradeIdOfOpenChat = trade.getId();

        Chat tradeChat = new Chat(model.dataModel.tradeManager.getChatManager(), formatter);
        tradeChat.setAllowAttachments(false);
        tradeChat.setDisplayHeader(false);
        tradeChat.initialize();

        AnchorPane pane = new AnchorPane(tradeChat);
        pane.setPrefSize(760, 500);
        AnchorPane.setLeftAnchor(tradeChat, 10d);
        AnchorPane.setRightAnchor(tradeChat, 10d);
        AnchorPane.setTopAnchor(tradeChat, -20d);
        AnchorPane.setBottomAnchor(tradeChat, 10d);

        boolean isTaker = !model.dataModel.isMaker(trade.getOffer());
        boolean isBuyer = model.dataModel.isBuyer();
        TradeChatSession chatSession = new TradeChatSession(trade, isTaker, isBuyer,
                model.dataModel.tradeManager,
                model.dataModel.tradeManager.getChatManager());

        disputeStateListener = tradeId -> {
            if (trade.getId().equals(tradeId)) {
                chatPopupStage.hide();
            }
        };
        chatSession.addDisputeStateListener(disputeStateListener);

        tradeChat.display(chatSession, null, pane.widthProperty());

        tradeChat.activate();
        tradeChat.scrollToBottom();

        chatPopupStage = new Stage();
        chatPopupStage.setTitle(Res.get("tradeChat.chatWindowTitle", trade.getShortId()));
        StackPane owner = MainView.getRootContainer();
        Scene rootScene = owner.getScene();
        chatPopupStage.initOwner(rootScene.getWindow());
        chatPopupStage.initModality(Modality.NONE);
        chatPopupStage.initStyle(StageStyle.DECORATED);
        chatPopupStage.setOnHiding(event -> {
            tradeChat.deactivate();
            // at close we set all as displayed. While open we ignore updates of the numNewMsg in the list icon.
            trade.getCommunicationMessages().forEach(m -> m.setWasDisplayed(true));
            trade.persist();
            tradeIdOfOpenChat = null;

            if (xPositionListener != null) {
                chatPopupStage.xProperty().removeListener(xPositionListener);
            }
            if (yPositionListener != null) {
                chatPopupStage.xProperty().removeListener(yPositionListener);
            }
            if (disputeStateListener != null) {
                chatSession.removeDisputeStateListener(disputeStateListener);
            }
        });

        Scene scene = new Scene(pane);
        CssTheme.loadSceneStyles(scene, preferences.getCssTheme());
        scene.addEventHandler(KeyEvent.KEY_RELEASED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                ev.consume();
                chatPopupStage.hide();
            }
        });
        chatPopupStage.setScene(scene);

        chatPopupStage.setOpacity(0);
        chatPopupStage.show();

        xPositionListener = (observable, oldValue, newValue) -> chatPopupStageXPosition = (double) newValue;
        chatPopupStage.xProperty().addListener(xPositionListener);
        yPositionListener = (observable, oldValue, newValue) -> chatPopupStageYPosition = (double) newValue;
        chatPopupStage.yProperty().addListener(yPositionListener);

        if (chatPopupStageXPosition == -1) {
            Window rootSceneWindow = rootScene.getWindow();
            double titleBarHeight = rootSceneWindow.getHeight() - rootScene.getHeight();
            chatPopupStage.setX(Math.round(rootSceneWindow.getX() + (owner.getWidth() - chatPopupStage.getWidth() / 4 * 3)));
            chatPopupStage.setY(Math.round(rootSceneWindow.getY() + titleBarHeight + (owner.getHeight() - chatPopupStage.getHeight() / 4 * 3)));
        } else {
            chatPopupStage.setX(chatPopupStageXPosition);
            chatPopupStage.setY(chatPopupStageYPosition);
        }

        // Delay display to next render frame to avoid that the popup is first quickly displayed in default position
        // and after a short moment in the correct position
        UserThread.execute(() -> chatPopupStage.setOpacity(1));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void updateTableSelection() {
        PendingTradesListItem selectedItemFromModel = model.dataModel.selectedItemProperty.get();
        if (selectedItemFromModel != null) {
            // Select and focus selectedItem from model
            int index = tableView.getItems().indexOf(selectedItemFromModel);
            UserThread.execute(() -> {
                //TODO app wide focus
                tableView.getSelectionModel().select(index);
                //table.requestFocus();
                //UserThread.execute(() -> table.getFocusModel().focus(index));
            });
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // CellFactories
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void setTradeIdColumnCellFactory() {
        tradeIdColumn.getStyleClass().add("first-column");
        tradeIdColumn.setCellValueFactory((pendingTradesListItem) -> new ReadOnlyObjectWrapper<>(pendingTradesListItem.getValue()));
        tradeIdColumn.setCellFactory(
                new Callback<>() {

                    @Override
                    public TableCell<PendingTradesListItem, PendingTradesListItem> call(TableColumn<PendingTradesListItem,
                            PendingTradesListItem> column) {
                        return new TableCell<>() {
                            private HyperlinkWithIcon field;

                            @Override
                            public void updateItem(final PendingTradesListItem item, boolean empty) {
                                super.updateItem(item, empty);

                                if (item != null && !empty) {
                                    field = new HyperlinkWithIcon(item.getTrade().getShortId());
                                    field.setOnAction(event -> tradeDetailsWindow.show(item.getTrade()));
                                    field.setTooltip(new Tooltip(Res.get("tooltip.openPopupForDetails")));
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
    }

    private void setDateColumnCellFactory() {
        dateColumn.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        dateColumn.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<PendingTradesListItem, PendingTradesListItem> call(
                            TableColumn<PendingTradesListItem, PendingTradesListItem> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final PendingTradesListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    setGraphic(new AutoTooltipLabel(formatter.formatDateTime(item.getTrade().getDate())));
                                } else {
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                });
    }

    private void setAmountColumnCellFactory() {
        amountColumn.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        amountColumn.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<PendingTradesListItem, PendingTradesListItem> call(
                            TableColumn<PendingTradesListItem, PendingTradesListItem> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final PendingTradesListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                    setGraphic(new AutoTooltipLabel(formatter.formatCoin(item.getTrade().getTradeAmount())));
                                else
                                    setGraphic(null);
                            }
                        };
                    }
                });
    }

    private void setPriceColumnCellFactory() {
        priceColumn.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        priceColumn.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<PendingTradesListItem, PendingTradesListItem> call(
                            TableColumn<PendingTradesListItem, PendingTradesListItem> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final PendingTradesListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                    setGraphic(new AutoTooltipLabel(formatter.formatPrice(item.getPrice())));
                                else
                                    setGraphic(null);
                            }
                        };
                    }
                });
    }

    private void setVolumeColumnCellFactory() {
        volumeColumn.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        volumeColumn.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<PendingTradesListItem, PendingTradesListItem> call(
                            TableColumn<PendingTradesListItem, PendingTradesListItem> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final PendingTradesListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                    setGraphic(new AutoTooltipLabel(formatter.formatVolumeWithCode(item.getTrade().getTradeVolume())));
                                else
                                    setGraphic(null);
                            }
                        };
                    }
                });
    }

    private void setPaymentMethodColumnCellFactory() {
        paymentMethodColumn.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        paymentMethodColumn.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<PendingTradesListItem, PendingTradesListItem> call(
                            TableColumn<PendingTradesListItem, PendingTradesListItem> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final PendingTradesListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                    setGraphic(new AutoTooltipLabel(model.getPaymentMethod(item)));
                                else
                                    setGraphic(null);
                            }
                        };
                    }
                });
    }

    private void setMarketColumnCellFactory() {
        marketColumn.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        marketColumn.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<PendingTradesListItem, PendingTradesListItem> call(
                            TableColumn<PendingTradesListItem, PendingTradesListItem> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final PendingTradesListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                setGraphic(new AutoTooltipLabel(model.getMarketLabel(item)));
                            }
                        };
                    }
                });
    }

    private void setRoleColumnCellFactory() {
        roleColumn.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        roleColumn.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<PendingTradesListItem, PendingTradesListItem> call(
                            TableColumn<PendingTradesListItem, PendingTradesListItem> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final PendingTradesListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                    setGraphic(new AutoTooltipLabel(model.getMyRole(item)));
                                else
                                    setGraphic(null);
                            }
                        };
                    }
                });
    }

    @SuppressWarnings("UnusedReturnValue")
    private TableColumn<PendingTradesListItem, PendingTradesListItem> setAvatarColumnCellFactory() {
        avatarColumn.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        avatarColumn.getStyleClass().addAll("last-column", "avatar-column");
        avatarColumn.setCellFactory(
                new Callback<>() {

                    @Override
                    public TableCell<PendingTradesListItem, PendingTradesListItem> call(TableColumn<PendingTradesListItem, PendingTradesListItem> column) {
                        return new TableCell<>() {

                            @Override
                            public void updateItem(final PendingTradesListItem newItem, boolean empty) {
                                super.updateItem(newItem, empty);
                                if (!empty && newItem != null) {
                                    final Trade trade = newItem.getTrade();
                                    final NodeAddress tradingPeerNodeAddress = trade.getTradingPeerNodeAddress();
                                    int numPastTrades = model.getNumPastTrades(trade);
                                    String role = Res.get("peerInfoIcon.tooltip.tradePeer");
                                    Node peerInfoIcon = new PeerInfoIcon(tradingPeerNodeAddress,
                                            role,
                                            numPastTrades,
                                            privateNotificationManager,
                                            trade,
                                            preferences,
                                            model.accountAgeWitnessService,
                                            formatter,
                                            useDevPrivilegeKeys);
                                    setPadding(new Insets(1, 0, 0, 0));
                                    setGraphic(peerInfoIcon);
                                } else {
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                });
        return avatarColumn;
    }

    @SuppressWarnings("UnusedReturnValue")
    private TableColumn<PendingTradesListItem, PendingTradesListItem> setChatColumnCellFactory() {
        chatColumn.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        //TODO
        chatColumn.getStyleClass().addAll("last-column", "avatar-column");
        chatColumn.setSortable(false);
        chatColumn.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<PendingTradesListItem, PendingTradesListItem> call(TableColumn<PendingTradesListItem, PendingTradesListItem> column) {
                        return new TableCell<>() {

                            @Override
                            public void updateItem(final PendingTradesListItem newItem, boolean empty) {
                                super.updateItem(newItem, empty);

                                if (!empty && newItem != null) {
                                    Trade trade = newItem.getTrade();
                                    String id = trade.getId();

                                    // We use maps for each trade to avoid multiple listener registrations when
                                    // switching views. With current implementation we avoid that but we do not
                                    // remove listeners when a trade is removed (completed) but that has no consequences
                                    // as we will not receive any message anyway from a closed trade. Supporting it
                                    // more correctly would require more effort and managing listener deactivation at
                                    // screen switches (currently we get the update called if we have selected another
                                    // view.
                                    Button button;
                                    if (!buttonByTrade.containsKey(id)) {
                                        button = FormBuilder.getIconButton(MaterialDesignIcon.COMMENT_MULTIPLE_OUTLINE);
                                        buttonByTrade.put(id, button);
                                        button.setTooltip(new Tooltip(Res.get("tradeChat.openChat")));
                                    } else {
                                        button = buttonByTrade.get(id);
                                    }

                                    JFXBadge badge;
                                    if (!badgeByTrade.containsKey(id)) {
                                        badge = new JFXBadge(button);
                                        badgeByTrade.put(id, badge);
                                        badge.setPosition(Pos.TOP_RIGHT);
                                    } else {
                                        badge = badgeByTrade.get(id);
                                    }

                                    button.setOnAction(e -> {
                                        openChat(trade);
                                        update(trade, badge);
                                    });

                                    if (!listenerByTrade.containsKey(id)) {
                                        ListChangeListener<DisputeCommunicationMessage> listener = c -> update(trade, badge);
                                        listenerByTrade.put(id, listener);
                                        trade.getCommunicationMessages().addListener(listener);
                                    }

                                    update(trade, badge);

                                    setGraphic(badge);
                                } else {
                                    setGraphic(null);
                                }
                            }

                            private void update(Trade trade, JFXBadge badge) {
                                if (!trade.getId().equals(tradeIdOfOpenChat)) {
                                    updateNewChatMessagesByTradeMap();
                                    long num = newChatMessagesByTradeMap.get(trade.getId());
                                    if (num > 0) {
                                        badge.setText(String.valueOf(num));
                                        badge.setEnabled(true);
                                    } else {
                                        badge.setText("");
                                        badge.setEnabled(false);
                                    }
                                } else {
                                    badge.setText("");
                                    badge.setEnabled(false);
                                }
                                badge.refreshBadge();
                            }
                        };
                    }
                });
        return chatColumn;
    }
}

