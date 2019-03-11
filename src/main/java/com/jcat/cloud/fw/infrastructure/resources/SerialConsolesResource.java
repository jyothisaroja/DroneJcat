package com.jcat.cloud.fw.infrastructure.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Resource describing Serial consoles
 *
 * @author esauali 2013-11-18 Initial version
 *
 */
public class SerialConsolesResource implements ConfigurationData {
    /**
     * Serial Console resource
     *
     * @author esauali 2013-11-18 Initial version
     *
     */
    public static class SerialConsole {
        private static final String PROPERTY_IP = "ip";
        private static final String PROPERTY_PORT = "port";
        private static final String PROPERTY_TARGET = "target";
        @JsonProperty(PROPERTY_IP)
        private String mIp;
        @JsonProperty(PROPERTY_PORT)
        private int mPort;

        /**
         * In EBS, format should be: 2-1, where 2-shelf number, 1 - slot number.
         * Will have different format for different underlying hardware
         */
        @JsonProperty(PROPERTY_TARGET)
        private String mTarget;

        /**
         * @return the ip
         */
        public String getIp() {
            return mIp;
        }

        /**
         * @return the port
         */
        public int getPort() {
            return mPort;
        }

        /**
         * @return the target
         */
        public String getTarget() {
            return mTarget;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Objects.toStringHelper(this).omitNullValues().add(PROPERTY_IP, mIp).add(PROPERTY_PORT, mPort)
                    .add(PROPERTY_TARGET, mTarget).toString();
        }
    }

    private static final String PROPERTY_CONSOLES = "consoles";
    private static final String PROPERTY_CONSOLE_SERVER_HOST = "host";
    public static final String RESOURCE_ID = "SerialConsoles";

    /**
     * {@link #getConsoles()}
     */
    private List<SerialConsole> mConsoles;

    /**
     * {@link #getServerHost()}
     */
    private String mConsoleServerHost;

    /**
     * Constructor for XMLAdapter
     *
     * @param xmlConfiguration
     */
    public SerialConsolesResource(ResourceElement xmlConfiguration) {
        this(xmlConfiguration.getProperty(PROPERTY_CONSOLES), xmlConfiguration
                .getProperty(PROPERTY_CONSOLE_SERVER_HOST));
    }

    /**
     * Constructor for TASS
     *
     * @param consolesJson
     */
    public SerialConsolesResource(@JsonProperty(PROPERTY_CONSOLES) String consolesJson,
            @JsonProperty(PROPERTY_CONSOLE_SERVER_HOST) String consoleServerHost) {
        Preconditions.checkArgument(!(consolesJson == null || consolesJson.isEmpty()));
        Preconditions.checkArgument(!(consoleServerHost == null || consoleServerHost.isEmpty()));
        mConsoles = parseConsoles(consolesJson);
        mConsoleServerHost = consoleServerHost;
    }

    /**
     * Helper method to parse JSON string
     *
     * @param consolesJson
     * @return
     */
    private List<SerialConsole> parseConsoles(String consolesJson) {
        try {
            return new ObjectMapper().readValue(consolesJson, new TypeReference<ArrayList<SerialConsole>>() {
            });
        } catch (IOException e) {
            throw new IllegalArgumentException("Consoles configuration is corrupted:" + consolesJson, e);
        }
    }

    /**
     * Find console resource for specified shelf and slot
     *
     * @param shelf
     * @param slot
     * @return null if not found
     */
    public SerialConsole getConsole(int shelf, int slot) {
        String target = shelf + "-" + slot;
        for (SerialConsole serialConsole : mConsoles) {
            if (serialConsole.getTarget().equals(target)) {
                return serialConsole;
            }
        }
        return null;
    }

    /**
     * Get console server host name
     *
     * @return
     */
    public String getServerHost() {
        return mConsoleServerHost;
    }

    /**
     * Get list of {@link SerialConsolesResource}
     *
     * @return
     */
    public List<SerialConsole> get() {
        return mConsoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ConfigurationData> getType() {
        return this.getClass();
    }

}
