package eu.pretix.pretixdroid.check;


import android.net.wifi.WifiConfiguration;

import java.util.List;

public interface TicketCheckProvider {

    class CheckResult {
        public enum Type {
            INVALID, VALID, USED, ERROR, UNPAID
        }

        private Type type;
        private String ticket;
        private String variation;
        private String attendee_name;
        private String message;
        private String order_code;

        public CheckResult(Type type, String message) {
            this.type = type;
            this.message = message;
        }

        public CheckResult(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public String getTicket() {
            return ticket;
        }

        public void setTicket(String ticket) {
            this.ticket = ticket;
        }

        public String getVariation() {
            return variation;
        }

        public void setVariation(String variation) {
            this.variation = variation;
        }

        public String getAttendee_name() {
            return attendee_name;
        }

        public void setAttendee_name(String attendee_name) {
            this.attendee_name = attendee_name;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getOrderCode() {
            return order_code;
        }

        public void setOrderCode(String order_code) {
            this.order_code = order_code;
        }
    }

    class SearchResult {

        private String secret;
        private String ticket;
        private String variation;
        private String attendee_name;
        private String order_code;
        private boolean paid;
        private boolean redeemed;

        public SearchResult() {
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public boolean isPaid() {
            return paid;
        }

        public void setPaid(boolean paid) {
            this.paid = paid;
        }

        public boolean isRedeemed() {
            return redeemed;
        }

        public void setRedeemed(boolean redeemed) {
            this.redeemed = redeemed;
        }

        public String getTicket() {
            return ticket;
        }

        public void setTicket(String ticket) {
            this.ticket = ticket;
        }

        public String getVariation() {
            return variation;
        }

        public void setVariation(String variation) {
            this.variation = variation;
        }

        public String getAttendee_name() {
            return attendee_name;
        }

        public void setAttendee_name(String attendee_name) {
            this.attendee_name = attendee_name;
        }

        public String getOrderCode() {
            return order_code;
        }

        public void setOrderCode(String order_code) {
            this.order_code = order_code;
        }
    }

    class StatusResultItemVariation {
        private String name;
        private int checkins;
        private int total;

        public StatusResultItemVariation(String name, int total, int checkins) {
            this.name = name;
            this.checkins = checkins;
            this.total = total;
        }

        public String getName() {
            return name;
        }

        public int getCheckins() {
            return checkins;
        }

        public int getTotal() {
            return total;
        }
    }

    class StatusResultItem {
        private String name;
        private int checkins;
        private int total;
        private List<StatusResultItemVariation> variations;

        public StatusResultItem(String name, int total, int checkins, List<StatusResultItemVariation> variations) {
            this.name = name;
            this.checkins = checkins;
            this.total = total;
            this.variations = variations;
        }

        public String getName() {
            return name;
        }

        public int getCheckins() {
            return checkins;
        }

        public int getTotal() {
            return total;
        }

        public List<StatusResultItemVariation> getVariations() {
            return variations;
        }
    }

    class StatusResult {
        private String eventName;
        private int totalTickets;
        private int alreadyScanned;
        private List<StatusResultItem> items;

        public StatusResult(String eventName, int totalTickets, int alreadyScanned, List<StatusResultItem> items) {
            this.eventName = eventName;
            this.totalTickets = totalTickets;
            this.alreadyScanned = alreadyScanned;
            this.items = items;
        }

        public String getEventName() {
            return eventName;
        }

        public int getTotalTickets() {
            return totalTickets;
        }

        public int getAlreadyScanned() {
            return alreadyScanned;
        }

        public List<StatusResultItem> getItems() {
            return items;
        }
    }

    CheckResult check(String ticketid);

    List<SearchResult> search(String query) throws CheckException;

    StatusResult status() throws CheckException;
}
