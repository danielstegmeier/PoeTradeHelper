package de.crass.poetradehelper.model;

public class CurrencyOffer {
    private String playerName;
    private String accountName;
    private CurrencyID sellID;
    private float sellAmount;
    private CurrencyID buyID;
    private float buyAmount;
    private int stock;
    private boolean isPlayerOffer = false;
    private String queryID;

    public CurrencyOffer(String username, String accountName, CurrencyID sellID, float sellValue, CurrencyID buyID, float buyValue, int stock) {
        this.playerName = username;
        this.accountName = accountName;
        this.sellID = sellID;
        this.sellAmount = sellValue;
        this.buyID = buyID;
        this.buyAmount = buyValue;
        this.stock = stock;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getAccountName() {
        return accountName;
    }

    public CurrencyID getSellID() {
        return sellID;
    }

    public float getSellAmount() {
        return sellAmount;
    }

    public CurrencyID getBuyID() {
        return buyID;
    }

    public float getBuyAmount() {
        return buyAmount;
    }

    public int getStock() {
        return stock;
    }

    @Override
    public String toString() {
        return "CurrencyOffer[" + getPlayerName() + ", buy " + getBuyAmount() + "x " + getBuyID() + ", sell " + getSellAmount() + "x " +getSellID()+"]";
    }

    public boolean isPlayerOffer() {
        return isPlayerOffer;
    }

    public void setPlayerOffer(boolean playerOffer) {
        isPlayerOffer = playerOffer;
    }

    public void setQueryId(String id){
        queryID = id;
    }

    public String getQueryID() {
        return queryID;
    }
}
