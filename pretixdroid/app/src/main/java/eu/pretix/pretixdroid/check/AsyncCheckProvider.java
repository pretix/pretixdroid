package eu.pretix.pretixdroid.check;

import android.content.Context;

import com.joshdholtz.sentry.Sentry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.db.QueuedCheckIn;
import eu.pretix.pretixdroid.db.Ticket;
import eu.pretix.pretixdroid.net.api.PretixApi;
import io.requery.BlockingEntityStore;
import io.requery.Persistable;

public class AsyncCheckProvider implements TicketCheckProvider {
    private Context ctx;
    private PretixApi api;
    private AppConfig config;
    private BlockingEntityStore<Persistable> dataStore;

    public AsyncCheckProvider(Context ctx) {
        this.ctx = ctx;

        this.config = new AppConfig(ctx);
        this.api = PretixApi.fromConfig(config);
        dataStore = ((PretixDroid) ctx.getApplicationContext()).getData();
    }

    @Override
    public CheckResult check(String ticketid) {
        Sentry.addBreadcrumb("provider.check", "offline check started");

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
        return res;
    }

    @Override
    public List<SearchResult> search(String query) throws CheckException {
        Sentry.addBreadcrumb("provider.search", "offline search started");

        List<SearchResult> results = new ArrayList<>();

        if (query.length() < 4) {
            return results;
        }

        List<Ticket> tickets = dataStore.select(Ticket.class)
                .where(
                        Ticket.SECRET.like(query + "%")
                                .or(Ticket.ATTENDEE_NAME.like("%" + query + "%"))
                                .or(Ticket.ORDER.like(query + "%"))
                )
                .limit(25)
                .get().toList();

        for (Ticket ticket : tickets) {
            SearchResult sr = new SearchResult();
            sr.setTicket(ticket.getItem());
            sr.setVariation(ticket.getVariation());
            sr.setAttendee_name(ticket.getAttendee_name());
            sr.setOrderCode(ticket.getOrder());
            sr.setSecret(ticket.getSecret());
            sr.setRedeemed(ticket.isRedeemed());
            sr.setPaid(ticket.isPaid());
            results.add(sr);
        }
        return results;
    }
}
