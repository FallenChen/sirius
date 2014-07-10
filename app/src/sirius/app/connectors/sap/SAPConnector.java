/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.connectors.sap;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.util.Properties;

/**
 * Created by aha on 25.06.14.
 */
public class SAPConnector {

    public static Log LOG = Log.get("sap");

    private static volatile boolean bridgeInstalled;

    static class DestinationProvider implements DestinationDataProvider {

        @Override
        public Properties getDestinationProperties(String destName) {
            Extension ext = Extensions.getExtension("connector.sap", destName);
            if (ext.isDefault()) {
                throw Exceptions.handle().withSystemErrorMessage("Unknown SAP connection: %s", destName).to(LOG).handle();
            }
            Properties connectProperties = new Properties();
            connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST,
                    ext.get("host").asString());
            connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, ext.get("sys-nr").asString());
            connectProperties
                    .setProperty(DestinationDataProvider.JCO_CLIENT, ext.get("client").asString());
            connectProperties.setProperty(DestinationDataProvider.JCO_USER,
                    ext.get("user").asString());
            connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD,
                    ext.get("password").asString());
            connectProperties.setProperty(DestinationDataProvider.JCO_LANG, ext.get("lang").asString());
            connectProperties.setProperty(
                    DestinationDataProvider.JCO_POOL_CAPACITY, ext.get("capacity").asString());
            connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT,
                    ext.get("peak-limit").asString());

            return connectProperties;
        }

        @Override
        public void setDestinationDataEventListener(DestinationDataEventListener el) {
        }

        @Override
        public boolean supportsEvents() {
            return false;
        }
    }

    public static JCoDestination getDestination(String name) {
        if (!bridgeInstalled) {
            bridgeInstalled = true;
            Environment
                    .registerDestinationDataProvider(new DestinationProvider());
        }
        try {
            return JCoDestinationManager
                    .getDestination(name);
        } catch (Throwable e) {
            throw Exceptions.handle().to(LOG).withSystemErrorMessage("Error establishing SAP connection to %s: %s (%s)", name).error(e).handle();
        }
    }
}
