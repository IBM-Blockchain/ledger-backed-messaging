package org.ampretia.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.ampretia.model.Trade;
import org.ampretia.model.TradeMessage;
import org.ampretia.transport.Transport;

import io.quarkus.scheduler.Scheduled;


@ApplicationScoped
public class TraderService {
	private static final Logger LOGGER = Logger.getLogger(TraderService.class.getName());
    // very simple solution to holding a list of the trades
    Map<String, Map<String, Trade>> dataStore;

    private ReentrantLock mutex = new ReentrantLock();

    @Inject
    Transport transport;

    @PostConstruct
    public void init() {
        dataStore = new HashMap<String, Map<String, Trade>>();

    }

    public TradeMessage submitTrade(String traderId, TradeMessage newOffer) {
        Trade t = new Trade(traderId, newOffer.tradeId);
        TradeMessage offer = t.createOffer().setPrice(newOffer.price).setQty(newOffer.qty).setDescription(newOffer.description);
        transport.sendTraderOffer(offer);

        try {
            mutex.lock();
            Map<String, Trade> listTrades = dataStore.get(traderId);
            if (listTrades == null) {
                listTrades = new HashMap<String, Trade>();
                dataStore.put(traderId, listTrades);
            }

            listTrades.put(t.tradeId, t);
            // dataStore.put(traderId, listTrades);
        } finally {
            mutex.unlock();
        }
        return offer;
    }

    public List<Trade> getTrades(String traderId) {

        try {
            mutex.lock();
            Map<String,Trade> tradeList = dataStore.get(traderId);
            if (tradeList==null){
                return new ArrayList();
            } else {
                return tradeList.values().stream().collect(Collectors.toList());
            }
  
        } finally {
            mutex.unlock();
        }
    }

    @Scheduled(every="10s") 
    public void updateTrades() {
        LOGGER.info("updateTrades running");
        try {
            mutex.lock();
            for (Map.Entry<String, Map<String, Trade>> entry : dataStore.entrySet()) {
                LOGGER.info("updateTrades looking at "+entry.getKey());
                
                for (Map.Entry<String, Trade> tradeEntry : entry.getValue().entrySet()) {
                    Trade t = tradeEntry.getValue();
                    LOGGER.info(entry.toString());
                    LOGGER.info(t.toString());
                    if (t.responseMsg==null){
                        t.setResponse(transport.getTraderResponse(tradeEntry.getKey()));
                        LOGGER.info("updateTrades looking at "+tradeEntry.getKey());
                    } else {
                        LOGGER.info("already got response");
                    }
                }

            }
        } finally {
            mutex.unlock();
        }
    }

    public Trade getTrade(String traderId, String tradeId) {

        try {
            mutex.lock();
            Trade t = null;
            Map<String, Trade> tradeList = dataStore.get(traderId);

            return tradeList.get(tradeId);
        } finally {
            mutex.unlock();
        }
    }
}
