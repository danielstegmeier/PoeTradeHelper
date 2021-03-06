package de.crass.poetradehelper.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.Main;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.web.HttpManager;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public class PoeNinjaParser {
    private static final String currencyURL = "https://poe.ninja/api/data/currencyoverview?type=Currency";
    private final ObjectMapper objectMapper;
    private String cacheFileName = "_data.dat";

    private boolean useOfflineCache = true;
    private long updateDelay = 3 * 60 * 60 * 1000; // 3 hours

    private PoeNinjaListener listener;

    // Currency - (CurrencyID<>Chaos Value)
    private HashMap<CurrencyID, Float> currentRates = new HashMap<>();

    PoeNinjaParser() {
        objectMapper = new ObjectMapper();
        loadDefaultFile();
    }

    boolean fetchRates(String league, boolean forceUpdate) {
        if (forceUpdate || !loadFromCache(league)) {

            // Fetch online
            LogManager.getInstance().log(getClass(), "Fetching new currency values for league " + league + " from poe.ninja.");
            JSONObject json;
            try {
                json = HttpManager.getInstance().getJson(currencyURL, "&league=" + league);

                if (json == null || json.length() == 0) {
                    return false;
                }

                writeToCache(league, json.toString());

                parseJson(json, false);
                LogManager.getInstance().log(getClass(), "Fetching currency values finished.");
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "Fetching rates from PoeNinja failed. No internet connection?");
                e.printStackTrace();
                return false;
            } catch (JSONException j) {
                LogManager.getInstance().log(getClass(), currencyURL + "&league=" + league + " returned no valid JSON! API may have changed.");
                j.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private void writeToCache(String league, String data) {
        File cacheFile = new File(league + cacheFileName);
        try {
            objectMapper.writeValue(cacheFile, data);
        } catch (Exception e) {
            LogManager.getInstance().log(getClass(), "Writing ninja cache failed!\n" + e);
        }
    }

    public void loadDefaultFile() {
        URL url = Main.class.getResource("default_data.dat");
        try {
            parseJson(objectMapper.readValue(url, JSONObject.class), true);
        } catch (Exception e) {
            LogManager.getInstance().log(getClass(), "Error loading default values!");
        }
    }

    private boolean loadFromCache(String league) {
        File cacheFile = new File(league + cacheFileName);
        // If there is a file  and it is recent
        if ((useOfflineCache && cacheFile.exists() &&
                cacheFile.lastModified() + updateDelay > System.currentTimeMillis())) {
            // If no current rates have been loaded before, load from cache, otherwise current data is up to date
            if (currentRates == null || currentRates.isEmpty()) {
                //TypeReference<HashMap<CurrencyID, Float>> typeRef = new TypeReference<HashMap<CurrencyID, Float>>() {};
                try {
                    //LogManager.getInstance().log(getClass(), "Loading poe.ninja values for league " + league + " from cache.");
                    parseJson(objectMapper.readValue(cacheFile, JSONObject.class), false);
                } catch (Exception e) {
                    LogManager.getInstance().log(getClass(), "Error loading poe.ninja values from cache.");
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void parseJson(JSONObject json, boolean onlyCurrencyDetails) {
        JSONArray idArray = json.getJSONArray("currencyDetails");
        for (Object currencyDetailsObject : idArray) {
            if (currencyDetailsObject instanceof JSONObject) {
                JSONObject currencyDetails = (JSONObject) currencyDetailsObject;
                if (currencyDetails.get("tradeId") != null && currencyDetails.getInt("poeTradeId") > 0 && !currencyDetails.get("tradeId").equals(JSONObject.NULL)) {
                    new CurrencyID(currencyDetails).store();
                }
            }
        }

        if (onlyCurrencyDetails) {
            return;
        }

        JSONArray array = json.getJSONArray("lines");
        for (Object currencyObject : array) {
            if (currencyObject instanceof JSONObject) {
                JSONObject currency = (JSONObject) currencyObject;
                String currencyName = currency.getString("currencyTypeName");
                CurrencyID id = CurrencyID.getByDisplayName(currencyName);
                if (id != null) {
                    float chaosValue = currency.getFloat("chaosEquivalent");
                    currentRates.put(id, chaosValue);
                }
            }
        }
        currentRates.put(CurrencyID.CHAOS, 1f);

        if (listener != null) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    listener.onRatesFetched();
                }
            });
        }
    }

    HashMap<CurrencyID, Float> getCurrentRates() {
        if (currentRates == null || currentRates.isEmpty()) {
            fetchRates(PropertyManager.getInstance().getCurrentLeague(), false);
        }
        return currentRates;
    }

    Float getCurrentCValueFor(CurrencyID id) {
        if (id.equals(CurrencyID.CHAOS)) {
            return 1f;
        }
        Float value = getCurrentRates().get(id);

        return value == null ? 0 : value;
    }

    Float getCurrentValue(CurrencyID what, CurrencyID inWhat) {
        Float whatValue = getCurrentCValueFor(what);
        Float inWhatValue = getCurrentCValueFor(inWhat);

        if (inWhatValue == null || inWhatValue == 0) {
            return 0f;
        }

        return whatValue / inWhatValue;
    }

    void registerListener(PoeNinjaListener ninjaListener) {
        listener = ninjaListener;
    }

    void reset() {
        currentRates.clear();
    }

    public boolean requiresMultipleTransactions(CurrencyID secondaryCurrency) {
        if (PropertyManager.getInstance().getFilterMultipleTransactionDeals()) {
            float tradeAmount = getCurrentValue(PropertyManager.getInstance().getPrimaryCurrency(), secondaryCurrency);
            int threshold = secondaryCurrency.getStackSize() * 5 * 12;

            return tradeAmount > threshold;
        }
        return false;
    }

    public interface PoeNinjaListener {
        void onRatesFetched();
    }
}
