/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

import com.googlecode.junittoolbox.SuiteClasses;
import com.googlecode.junittoolbox.WildcardPatternSuite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import sirius.testtools.SiriusBaseSpecification;

@RunWith(WildcardPatternSuite.class)
@SuiteClasses({"**/*Test.class", "**/*Spec.class"})
public class SiriusKernelTestSuite {

    public static final String SUITE_NAME = "Sirius Kernel";

    @BeforeClass
    public static void setUp() {
        SiriusBaseSpecification.setupSiriusTestEnvironment(SUITE_NAME);
    }


    @AfterClass
    public static void tearDown() {
        SiriusBaseSpecification.cleanupSiriusTestEnvironment(SUITE_NAME);
    }


}
