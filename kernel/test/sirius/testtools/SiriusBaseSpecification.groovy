/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.testtools

import sirius.kernel.Sirius
import sirius.kernel.async.CallContext
import sirius.kernel.nls.NLS
import spock.lang.Specification

/**
 * Base class for all specs that require the Sirius framework to be setup.
 * Extending specifications can be run from their respective Suites or stand alone without any additional configuration.
 */
class SiriusBaseSpecification extends Specification {

    /**
     * indicates whether this class initially started the framework (and thus is responsible for shutting it down again).
     */
    static Class initialFrameworkStarter = null;

    /**
     * handles all initial Sirius setup.
     * @param suiteName the name of the Suite to be run
     */
    static def setupSiriusTestEnvironment(String suiteName) {
        System.out.println("$suiteName Test Suite started")
        Sirius.initializeTestEnvironment()
        NLS.setDefaultLanguage("de")
    }

    /**
     * handles all operations necessary after the tests are done.
     * @param suiteName the name of the suite which requests the shutdown
     */
    static def cleanupSiriusTestEnvironment(String suiteName){
        Sirius.stop()
        NLS.getTranslationEngine().reportMissingTranslations()
        System.out.println("$suiteName Test Suite finished")
    }

    /**
     * Will be executed once before every spec class.
     * If Sirius is not yet started, this will take care of that and mark itself as the {@link #initialFrameworkStarter}
     */
    def setupSpec() {
        if (!Sirius.startedAsTest){
            initialFrameworkStarter = getClass()
            setupSiriusTestEnvironment(getClass().simpleName)
        }
    }

    /**
     * Will be executed once before every spec method
     */
    def setup() {
      CallContext.initialize()
    }

    /**
     * Will be called once after every spec class.
     * if this spec is the {@link #initialFrameworkStarter}, this will handle shutdown.
     */
    def cleanupSpec() {
        if (initialFrameworkStarter == getClass()){
            cleanupSiriusTestEnvironment(getClass().simpleName)
        }
    }

}
