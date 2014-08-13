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
import sirius.kernel.Sirius;

/**
 * Created by mgu on 08.08.14.
 */
@RunWith(WildcardPatternSuite.class)
@SuiteClasses({"**/*Test.class", "**/*Spec.class"})
public class SiriusKernelTestSuite {

    @BeforeClass
    public static void setUp() {
        System.out.println("Sirius Kernel Test Suite started");
        Sirius.initializeTestEnvironment();
    }


    @AfterClass
    public static void tearDown() {
        System.out.println("Sirius Kernel Test Suite finished");
    }


}
