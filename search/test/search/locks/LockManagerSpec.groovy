/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package search.locks

import sirius.kernel.di.Injector
import sirius.kernel.health.HandledException
import sirius.search.Index
import sirius.search.IndexAccess
import sirius.search.OptimisticLockException
import sirius.search.locks.CriticalSection
import sirius.search.locks.Lock
import sirius.search.locks.LockInfo
import sirius.search.locks.LockManager
import sirius.testtools.SiriusBaseSpecification
import spock.lang.Shared

import java.util.concurrent.TimeUnit

class LockManagerSpec extends SiriusBaseSpecification {

    @Shared
    def Lock LOCK = Lock.named("L");
    def CriticalSection SECTION_TEST = LOCK.forSection("TEST");
    def CriticalSection SECTION_XXX = LOCK.forSection("XXX");

    def "lock succeeds on available lock"() {
        given:
        def lm = new LockManager();
        lm.index = Mock(IndexAccess);
        when:
        def result = lm.tryLock(SECTION_TEST, 1, TimeUnit.MILLISECONDS);
        then:
        1 * lm.index.find(LockInfo.class, "L") >> null;
        1 * lm.index.tryUpdate(_) >> new LockInfo();
        result == true
    }

    def "tryLock fails on already acquired lock"() {
        given:
        def lm = new LockManager();
        lm.index = Mock(IndexAccess);
        when:
        def result = lm.tryLock(SECTION_TEST, 1, TimeUnit.MILLISECONDS);
        then:
        _ * lm.index.find(LockInfo.class, "L") >> new LockInfo();
        0 * lm.index.tryUpdate(_);
        result == false
    }

    def "tryLock fails on contended lock"() {
        given:
        def lm = new LockManager();
        lm.index = Mock(IndexAccess);
        when:
        def result = lm.tryLock(SECTION_TEST, 1, TimeUnit.MILLISECONDS);
        then:
        _ * lm.index.find(LockInfo.class, "L") >> null;
        _ * lm.index.tryUpdate(_) >> { LockInfo l ->
            l.id == "L"
            l.currentOwnerSection == "TEST"
            throw new OptimisticLockException(null, l);
        }
        result == false
    }

    def "tryLock succeeds with in-memory index"() {
        given:
        def lm = Injector.context().getPart(LockManager.class);
        when:
        def result = lm.tryLock(SECTION_TEST, 1, TimeUnit.MILLISECONDS);
        if (result) {
            lm.unlock(SECTION_TEST);
        }
        then:
        result == true
    }

    def "tryLock fails on duplicate lock with in-memory index"() {
        given:
        def lm = Injector.context().getPart(LockManager.class);
        when:
        def result = lm.tryLock(SECTION_TEST, 1, TimeUnit.MILLISECONDS);
        def result1 = null;
        if (result) {
            try {
                result1 = lm.tryLock(SECTION_XXX, 1, TimeUnit.MILLISECONDS);
            } finally {
                lm.unlock(SECTION_TEST);
            }
        }
        then:
        result == true
        result1 == false
    }

    def "unlock fails with in-memory index when using wrong section"() {
        given:
        def lm = Injector.context().getPart(LockManager.class);
        when:
        def result = lm.tryLock(SECTION_TEST, 1, TimeUnit.MILLISECONDS);
        if (result) {
            try {
                lm.unlock(SECTION_XXX)
            } finally {
                lm.unlock(SECTION_TEST);
            }
        }
        then:
        thrown(HandledException)
    }

}
