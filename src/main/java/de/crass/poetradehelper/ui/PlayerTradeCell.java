package de.crass.poetradehelper.ui;

import de.crass.poetradehelper.model.CurrencyDeal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.IOException;

import static de.crass.poetradehelper.ui.UIManager.prettyFloat;

/**
 * Created by mcrass on 19.07.2018.
 */
public class PlayerTradeCell extends javafx.scene.control.ListCell<CurrencyDeal> {

    private static final float WARNING_DIFF_THRESHOLD = 10;

    private FXMLLoader mLLoader;

    @FXML
    private AnchorPane root;

    @FXML
    private Text playerSell;

    @FXML
    private Text marketSell;

    @FXML
    private Text diff;

    @FXML
    private ImageView currencyIcon;

    @FXML
    private Text marketBuy;

    @FXML
    private Text diffValue;

    @FXML
    private Text playerBuy;

    @FXML
    private ImageView buyTendency;

    @FXML
    private ImageView sellTendency;

    @Override
    protected void updateItem(CurrencyDeal deal, boolean empty) {
        super.updateItem(deal, empty);

        if (empty || deal == null) {

            setText(null);
            setGraphic(null);
            setOnContextMenuRequested(null);

        } else {
            if (mLLoader == null) {
                mLLoader = new FXMLLoader(getClass().getResource("player_cell.fxml"));
                mLLoader.setController(this);

                try {
                    mLLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

//            root.setBorder(new Border(new BorderStroke(Color.BLACK,
//                    BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));

            UIManager.setImage(deal.getSecondaryCurrencyID().getID() + ".png", currencyIcon);

            float buy = deal.getBuyAmount();
            float sell = deal.getSellAmount();

            float pBuy = deal.getPlayerBuyAmount();
            float pSell = deal.getPlayerSellAmount();

            float currencyValue = deal.getcValue();

            float diffF = 0;

            float diffV = 0;
            if (pBuy != 0 && pSell != 0) {
                diffF = pBuy - pSell;
                diffV = diffF * currencyValue;
            }

            float playerBuyStock = deal.getPlayerBuyStock();
            // buystock is for primary, but buy and sell values are for secondary
            // deals are normalized to primary buy/sell == 1, therefore
            // stock for primary is always > primaryPrice, otherwise it would not even show up.
            boolean playerHasBuyStock = true;
//            if (playerBuyStock > 0 && playerBuyStock < 1) {
//                playerBuy.setFill(Color.GRAY);
//                playerHasBuyStock = false;
//            } else {
                playerBuy.setFill(marketBuy.getFill());

            float playerSellStock = deal.getPlayerSellStock();
            boolean playerHasSellStock = true;
            if (playerSellStock > 0 && playerSellStock < pSell) {
                playerSell.setFill(Color.GRAY);
                playerHasSellStock = false;
            } else {
                playerSell.setFill(marketBuy.getFill());
            }

            // Set icons
            if (buy > 0 && pBuy > 0) {
                if (pBuy > 0 && pBuy >= buy) {
                    UIManager.setImage(pBuy == buy ? "neut.png" : "nok.png", buyTendency);
                } else {
                    UIManager.setImage("ok.png", buyTendency);
                }
                buyTendency.setCache(true);
                buyTendency.setCacheHint(CacheHint.SPEED);
                if(!buyTendency.isVisible()) {
                    buyTendency.setVisible(true);
                }

                handleIconEffects(buyTendency, playerHasBuyStock, pBuy, buy, currencyValue);
            } else {
                buyTendency.setCache(false);
                if(buyTendency.isVisible()) {
                    buyTendency.setVisible(false);
                }
                buyTendency.setEffect(null);
            }

            if (sell > 0 && pSell > 0) {
                if (pSell > 0 && pSell <= sell) {
                    UIManager.setImage(pSell == sell ? "neut.png" : "nok.png", sellTendency);
                } else {
                    UIManager.setImage("ok.png", sellTendency);
                }
                sellTendency.setCache(true);
                sellTendency.setCacheHint(CacheHint.SPEED);
                if(!sellTendency.isVisible()) {
                    sellTendency.setVisible(true);
                }

                handleIconEffects(sellTendency, playerHasSellStock, pSell, sell, currencyValue);
            } else {
                sellTendency.setCache(false);
                if(sellTendency.isVisible()) {
                    sellTendency.setVisible(false);
                }
                sellTendency.setEffect(null);
            }

            marketBuy.setText(prettyFloat(buy, true, false));
            marketSell.setText(prettyFloat(sell, true, false));
            diff.setText(prettyFloat((diffF)));

            playerBuy.setText(prettyFloat(pBuy, true, false));
            playerSell.setText(prettyFloat(pSell, true, false));

            diffValue.setText(prettyFloat((diffV)) + "c");

            setGraphic(root);

            setContextMenu(new DealContextMenu(deal));

            setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                    if (mouseEvent.getClickCount() == 2) {
                       UIManager.getInstance().openOffers(deal.getSecondaryCurrencyID());
                    }
                }
            });
        }
    }

    private void handleIconEffects(ImageView view, boolean playerHasStock, float playerValue, float marketValue, float
            cValue) {

        // Check if player offer is too far from the market offers
        if (Math.abs(playerValue - marketValue) * cValue > WARNING_DIFF_THRESHOLD) {
            view.setEffect(getColorEffect(Color.RED));
        } else if (!playerHasStock) {
            view.setEffect(getColorEffect(Color.GRAY));
        } else {
            view.setEffect(null);
        }
    }

    @Override
    public void updateSelected(boolean selected) {
//        super.updateSelected(selected);
    }

    private static Effect getColorEffect(Color color) {
        Lighting lighting = new Lighting();
        lighting.setDiffuseConstant(1.0);
        lighting.setSpecularConstant(0.0);
        lighting.setSurfaceScale(0.0);
        lighting.setLight(new Light.Distant(45, 45, color));

        return lighting;
    }
}
