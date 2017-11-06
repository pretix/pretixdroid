package eu.pretix.libpretixsync.db;


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

    public Long item_id;

    @Nullable
    public String attendee_name;

    @Nullable
    public String variation;

    @Nullable
    public Long variation_id;

    public boolean redeemed;

    @Column(value = "0")
    public boolean require_attention;

    public boolean paid;
}
