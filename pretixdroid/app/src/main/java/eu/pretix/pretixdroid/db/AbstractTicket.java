package eu.pretix.pretixdroid.db;


import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.Nullable;

@Entity(cacheable = false)
public abstract class AbstractTicket {

    @Generated
    @Key
    public Long id;

    @Column(unique = true)
    public String secret;

    @Column(name = "order_code")
    public String order;

    public String item;

    @Nullable
    public String attendee_name;

    @Nullable
    public String variation;

    public boolean redeemed;

    public boolean paid;
}
