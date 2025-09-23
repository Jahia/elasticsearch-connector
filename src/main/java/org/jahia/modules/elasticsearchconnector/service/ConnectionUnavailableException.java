package org.jahia.modules.elasticsearchconnector.service;

public class ConnectionUnavailableException extends Exception {

    public ConnectionUnavailableException(String message) {
        super(message);
    }

    public ConnectionUnavailableException(Throwable cause) {
        super(cause);
    }

    public ConnectionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
