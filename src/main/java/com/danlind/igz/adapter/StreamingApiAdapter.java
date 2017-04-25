package com.danlind.igz.adapter;

import com.danlind.igz.Zorro;
import com.danlind.igz.domain.AccountDetails;
import com.danlind.igz.domain.PriceDetails;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.domain.types.Volume;
import com.danlind.igz.handler.AssetHandler;
import com.danlind.igz.ig.api.client.StreamingAPI;
import com.danlind.igz.ig.api.client.rest.AuthenticationResponseAndConversationContext;
import com.danlind.igz.ig.api.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Objects;


/**
 * Created by danlin on 2017-04-07.
 */
@Component
public class StreamingApiAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(AssetHandler.class);
    private static final String OFFER = "OFR";
    private static final String BID = "BID";
    private static final String MINUTE = "1MINUTE";
    private static final String CONS_END = "CONS_END";
    private static final String LAST_TRADED_VOLUME = "LTV";
    private static final String ONE = "1";
    private final ArrayList<HandyTableListenerAdapter> listeners = new ArrayList<>();

    //Parameter injection here to avoid circular dependencies

    @Autowired
    private StreamingAPI streamingAPI;

    public Observable<PriceDetails> getTickObservable(Epic epic) {
        LOG.info("Subscribing to Lightstreamer tick updates for market: {} ", epic.getName());
        return Observable.create(event -> {
            try {
                listeners.add(streamingAPI.subscribeForChartTicks(epic.getName(), new HandyTableListenerAdapter() {
                    @Override
                    public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                        if (Objects.nonNull(updateInfo.getNewValue(BID))) {
                            event.onNext(new PriceDetails(new Epic(updateInfo.getItemName().split(":")[1]),
                                    Double.parseDouble(updateInfo.getNewValue(BID)),
                                    Double.parseDouble(updateInfo.getNewValue(OFFER))));
                        }
                    }

                    @Override
                    public void onUnsubscrAll() {
                        event.onComplete();
                    }
                }));
            } catch (Exception e) {
                event.onError(e);
            }
        });
    }

    public Observable<Volume> getVolumeObservable(Epic epic) {
        LOG.info("Subscribing to Lightstreamer volume updates for market: {} ", epic.getName());

        return Observable.create(event -> {
            try {
                listeners.add(streamingAPI.subscribeForChartCandles(epic.getName(), MINUTE, new HandyTableListenerAdapter() {
                    @Override
                    public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                        if (updateInfo.getNewValue(CONS_END).equals(ONE)) {
                            event.onNext(new Volume(Integer.parseInt(updateInfo.getNewValue(LAST_TRADED_VOLUME))));
                        }
                    }

                    @Override
                    public void onUnsubscrAll() {
                        event.onComplete();
                    }
                }));
            } catch (Exception e) {
                event.onError(e);
            }
        });
    }

    public Observable<AccountDetails> getAccountObservable(String accountId) {
        LOG.info("Subscribing to Lightstreamer account updates for account: {} ", accountId);
        return Observable.create(event -> {
            try {
                LOG.info("Adding listener");
                listeners.add(streamingAPI.subscribeForAccountBalanceInfo(accountId, new HandyTableListenerAdapter() {
                    @Override
                    public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                        event.onNext(new AccountDetails(
                                Double.parseDouble(updateInfo.getNewValue(0)),
                                Double.parseDouble(updateInfo.getNewValue(1)),
                                Double.parseDouble(updateInfo.getNewValue(2))));
                    }

                    @Override
                    public void onUnsubscrAll() {
                        event.onComplete();
                    }
                }));
            } catch (Exception e) {
                event.onError(e);
            }
        });
    }

    public Observable<Long> getHeartbeatObservable() {
        return Observable.create(event -> {
            try {
                listeners.add(streamingAPI.subscribe(new HandyTableListenerAdapter() {
                    @Override
                    public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                        event.onNext(Long.parseLong(updateInfo.getNewValue(1)) * 1000);
                    }

                    @Override
                    public void onUnsubscrAll() {
                        event.onComplete();
                    }
                }, new String[]{"TRADE:HB.U.HEARTBEAT.IP"}, "MERGE", new String[]{"HEARTBEAT"}));
            } catch (Exception e) {
                event.onError(e);
            }
        });
    }

    public void connect(AuthenticationResponseAndConversationContext authenticationContext) throws Exception {
        streamingAPI.connect(authenticationContext.getAccountId(), authenticationContext.getConversationContext(), authenticationContext.getLightstreamerEndpoint());
    }

    public void disconnect() {
        unsubscribeAllLightstreamerListeners();
        streamingAPI.disconnect();
    }

    private void unsubscribeAllLightstreamerListeners() {
        for (HandyTableListenerAdapter listener : listeners) {
            try {
                streamingAPI.unsubscribe(listener.getSubscribedTableKey());
            } catch (Exception e) {
                LOG.error("Failed to unsubscribe Lightstreamer listener", e);
            }
        }
        listeners.clear();
    }


}
