/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.locks;

import javax.annotation.Nonnull;

/**
 * Encapsulates the name of a lock to permit the creation of named constants.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/11
 */
public class Lock {
    private final String name;

    private Lock(String name) {
        this.name = name;
    }

    /**
     * Creates a new lock containing the given name and critical section.
     * <p>
     * The lock name is used to globally acquire a lock. The section is mainly used for debugging or
     * trouble shooting purposes to know why and where a lock is held.
     * </p>
     *
     * @param name the name of the lock (unique key used to identify the lock)
     * @return a new lock instance
     */
    public static Lock named(@Nonnull String name) {
        return new Lock(name);
    }

    /**
     * Returns the name of the lock
     *
     * @return the name of the lock
     */
    public String getName() {
        return name;
    }

    /**
     * Creates a new critical section which acquired this lock.
     *
     * @param section the name of the section
     * @return a new section for the given name, acquiring the given lock
     */
    public CriticalSection forSection(@Nonnull String section) {
        return new CriticalSection(this, section);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Lock lock = (Lock) o;

        if (!name.equals(lock.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
