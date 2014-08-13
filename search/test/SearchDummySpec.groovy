import spock.lang.Specification

/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Created by mgu on 13.08.14.
 */
class SearchDummySpec extends Specification {

    def """DummySpec:
|            This class' only purpose is to keep SiriusSearchTestSuite happy
|            as long as no actual Tests exist for the search module.
|            Please delete as soon as search contains at least one real test. """ () {
        given:
        def useless = ""
        when:
        def hasNoMeaning = useless
        then:
        useless == hasNoMeaning

    }
}
