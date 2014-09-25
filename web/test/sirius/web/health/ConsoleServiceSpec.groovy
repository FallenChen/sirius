/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */



package sirius.web.health

import io.netty.handler.codec.http.HttpResponseStatus
import sirius.kernel.async.CallContext
import sirius.web.http.TestRequest
import sirius.web.http.TestResponse
import sirius.web.security.UserContext
import sirius.web.security.UserInfo
import spock.lang.Specification

class ConsoleServiceSpec extends Specification {

    def "/service/xml/console returns XML for help"() {
        when:
        UserContext.get().setCurrentUser(UserInfo.GOD_LIKE);
        def result = TestRequest.GET("/service/xml/console").executeAndBlock();
        then:
        result.getStatus() == HttpResponseStatus.OK;
    }

}