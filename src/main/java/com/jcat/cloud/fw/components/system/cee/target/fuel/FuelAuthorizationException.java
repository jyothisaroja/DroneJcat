package com.jcat.cloud.fw.components.system.cee.target.fuel;

/**
 * Exception thrown when there are problems due to authorization when interacting with Fuel
 */
public class FuelAuthorizationException extends Exception {
    private static final long serialVersionUID = -999091462570702896L;

    public FuelAuthorizationException(String message) {
        super(message);
    }

    public FuelAuthorizationException() {
        super();
    }
}
