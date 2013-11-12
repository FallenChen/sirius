/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides a in-memory store for logs and exceptions.
 * <p>
 * This will be inherently limited in size but should always contain
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
@Register(classes = {MemoryBasedHealthMonitor.class, LogTap.class, ExceptionHandler.class})
public class MemoryBasedHealthMonitor implements ExceptionHandler, LogTap {

    private List<Incident> incidents = Collections.synchronizedList(new ArrayList<Incident>());
    private List<LogMessage> messages = Collections.synchronizedList(new ArrayList<LogMessage>());

    @ConfigValue("health.memory.max-errors")
    private int maxErrors;
    @ConfigValue("health.memory.max-logs")
    private int maxMsg;

    @Override
    public void handle(Incident incident) throws Exception {
        synchronized (incidents) {
            Iterator<Incident> iter = incidents.iterator();
            while (iter.hasNext()) {
                if (Strings.areEqual(iter.next().getLocation(), incident.getLocation())) {
                    iter.remove();
                }
            }
            incidents.add(0, incident);
            while (incidents.size() > maxErrors) {
                incidents.remove(incidents.size() - 1);
            }
        }
    }

    @Override
    public void handleLogMessage(LogMessage msg) {
        synchronized (messages) {
            messages.add(0, msg);
            while (messages.size() > maxMsg) {
                messages.remove(messages.size() - 1);
            }
        }
    }

    /**
     * Contains all recorded incidents.
     *
     * @return all recorded incidents
     */
    public List<Incident> getIncidents() {
        return incidents;
    }

    /**
     * Contains all recorded log messages.
     *
     * @return all recorded messages
     */
    public List<LogMessage> getMessages() {
        return messages;
    }

}
