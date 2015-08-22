package eu.pretix.pretixdroid.api;

import java.io.IOException;

public class ApiException extends IOException {

    public ApiException(String msg) {
        super(msg);
    }

}
