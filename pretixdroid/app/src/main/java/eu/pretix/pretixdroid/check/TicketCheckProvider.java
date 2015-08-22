package eu.pretix.pretixdroid.check;


public interface TicketCheckProvider {

    class CheckResult {
        public enum Type {
            INVALID, VALID, USED, ERROR
        }

        private Type type;
        private String ticket;
        private String variation;
        private String attendee_name;
        private String message;

        public CheckResult(Type type, String message) {
            this.type = type;
            this.message = message;
        }

        public CheckResult(Type type) {
            this.type = type;
            this.ticket = ticket;
            this.variation = variation;
            this.attendee_name = attendee_name;
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
    }

    CheckResult check(String ticketid);
}
