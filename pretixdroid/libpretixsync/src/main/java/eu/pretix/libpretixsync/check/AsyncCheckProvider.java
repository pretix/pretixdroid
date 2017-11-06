package eu.pretix.libpretixsync.check;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.pretix.libpretixsync.DummySentryImplementation;
import eu.pretix.libpretixsync.SentryInterface;
import eu.pretix.libpretixsync.api.DefaultHttpClientFactory;
import eu.pretix.libpretixsync.api.HttpClientFactory;
import eu.pretix.libpretixsync.api.PretixApi;
import eu.pretix.libpretixsync.config.ConfigStore;
import eu.pretix.libpretixsync.db.QueuedCheckIn;
import eu.pretix.libpretixsync.db.Ticket;
import io.requery.BlockingEntityStore;
import io.requery.Persistable;

public class AsyncCheckProvider implements TicketCheckProvider {
    private PretixApi api;
    private ConfigStore config;
    private BlockingEntityStore<Persistable> dataStore;
    private SentryInterface sentry;

    public AsyncCheckProvider(ConfigStore config, BlockingEntityStore<Persistable> dataStore, HttpClientFactory httpClientFactory) {
        this.config = config;
        this.api = PretixApi.fromConfig(config, httpClientFactory);
        this.dataStore = dataStore;
        this.sentry = new DummySentryImplementation();
    }

    public AsyncCheckProvider(ConfigStore config, BlockingEntityStore<Persistable> dataStore) {
        this(config, dataStore, new DefaultHttpClientFactory());
    }

    public SentryInterface getSentry() {
        return sentry;
    }

    public void setSentry(SentryInterface sentry) {
        this.sentry = sentry;
        this.api.setSentry(sentry);
    }

    @Override
    public CheckResult check(String ticketid) {
        sentry.addBreadcrumb("provider.check", "offline check started");

        List<Ticket> tickets = dataStore.select(Ticket.class)
                .where(Ticket.SECRET.eq(ticketid))
                .get().toList();

        if (tickets.size() == 0) {
            return new CheckResult(CheckResult.Type.INVALID);
        }

        Ticket ticket = tickets.get(0);

        CheckResult res = new CheckResult(CheckResult.Type.ERROR);

        long queuedCheckIns = dataStore.count(QueuedCheckIn.class)
                .where(QueuedCheckIn.SECRET.eq(ticketid))
                .get().value();

        if (!ticket.isPaid()) {
            res.setType(CheckResult.Type.UNPAID);
        } else if (ticket.isRedeemed() || queuedCheckIns > 0) {
            res.setType(CheckResult.Type.USED);
        } else {
            res.setType(CheckResult.Type.VALID);
            ticket.setRedeemed(true);
            dataStore.update(ticket);

            QueuedCheckIn qci = new QueuedCheckIn();
            qci.generateNonce();
            qci.setSecret(ticketid);
            qci.setDatetime(new Date());
            dataStore.insert(qci);
        }

        res.setTicket(ticket.getItem());
        res.setVariation(ticket.getVariation());
        res.setAttendee_name(ticket.getAttendee_name());
        res.setOrderCode(ticket.getOrder());
        res.setRequireAttention(ticket.isRequire_attention());

        return res;
    }

    @Override
    public List<SearchResult> search(String query) throws CheckException {
        sentry.addBreadcrumb("provider.search", "offline search started");

        List<SearchResult> results = new ArrayList<>();

        if (query.length() < 4) {
            return results;
        }


        List<Ticket> tickets;
        if (config.getAllowSearch()) {
            tickets = dataStore.select(Ticket.class)
                    .where(
                            Ticket.SECRET.like(query + "%")
                                    .or(Ticket.ATTENDEE_NAME.like("%" + query + "%"))
                                    .or(Ticket.ORDER.like(query + "%"))
                    )
                    .limit(25)
                    .get().toList();
        } else {
            tickets = dataStore.select(Ticket.class)
                    .where(
                            Ticket.SECRET.like(query + "%")
                    )
                    .limit(25)
                    .get().toList();
        }

        for (Ticket ticket : tickets) {
            SearchResult sr = new SearchResult();
            sr.setTicket(ticket.getItem());
            sr.setVariation(ticket.getVariation());
            sr.setAttendee_name(ticket.getAttendee_name());
            sr.setOrderCode(ticket.getOrder());
            sr.setSecret(ticket.getSecret());
            sr.setRedeemed(ticket.isRedeemed());
            sr.setPaid(ticket.isPaid());
            sr.setRequireAttention(ticket.isRequire_attention());
            results.add(sr);
        }

        return results;
    }

    @Override
    public StatusResult status() throws CheckException {
        sentry.addBreadcrumb("provider.status", "offline status started");
        if (config.getLastStatusData() == null) {
            throw new CheckException("No current data available.");
        }
        StatusResult statusResult;
        try {
            statusResult = OnlineCheckProvider.parseStatusResponse(new JSONObject(config.getLastStatusData()));
        } catch (JSONException e) {
            e.printStackTrace();
            throw new CheckException("Invalid status data available.");
        }

        if (dataStore.count(Ticket.class).where(Ticket.ITEM_ID.eq((long) 0)).get().value() > 0) {
            throw new CheckException("Incompatible with your current pretix version.");
        }
        int total_all = 0;
        int checkins_all = 0;
        for (StatusResultItem resultItem : statusResult.getItems()) {
            int total = 0;
            int checkins = 0;
            if (resultItem.getVariations().size() > 0) {
                for (StatusResultItemVariation itemVariation : resultItem.getVariations()) {
                    itemVariation.setTotal(
                            dataStore.count(Ticket.class).where(
                                    Ticket.ITEM_ID.eq(resultItem.getId())
                                            .and(Ticket.VARIATION_ID.eq(itemVariation.getId()))
                                            .and(Ticket.PAID.eq(true))
                            ).get().value()
                    );
                    itemVariation.setCheckins(
                            dataStore.count(Ticket.class).where(
                                    Ticket.ITEM_ID.eq(resultItem.getId())
                                            .and(Ticket.VARIATION_ID.eq(itemVariation.getId()))
                                            .and(Ticket.REDEEMED.eq(true))
                                            .and(Ticket.PAID.eq(true))
                            ).get().value()
                    );
                    total += itemVariation.getTotal();
                    checkins += itemVariation.getCheckins();
                }
            } else {
                total = dataStore.count(Ticket.class).where(
                        Ticket.ITEM_ID.eq(resultItem.getId())
                                .and(Ticket.PAID.eq(true))
                ).get().value();
                checkins = dataStore.count(Ticket.class).where(
                        Ticket.ITEM_ID.eq(resultItem.getId())
                                .and(Ticket.REDEEMED.eq(true))
                                .and(Ticket.PAID.eq(true))
                ).get().value();
            }
            resultItem.setTotal(total);
            resultItem.setCheckins(checkins);
            total_all += total;
            checkins_all += checkins;
        }
        statusResult.setAlreadyScanned(checkins_all);
        statusResult.setTotalTickets(total_all);

        return statusResult;
    }
}
