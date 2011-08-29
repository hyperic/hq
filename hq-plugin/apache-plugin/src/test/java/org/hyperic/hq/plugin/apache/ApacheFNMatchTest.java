package org.hyperic.hq.plugin.apache;

import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author administrator
 */
public class ApacheFNMatchTest {

    private static boolean FAILS = false;
    private static boolean SUCCEEDS = true;
    TestCase tests[] = {
        new TestCase("", "test", FAILS),
        new TestCase("", "*", FAILS),
        new TestCase("test", "*", FAILS),
        new TestCase("test", "test", SUCCEEDS),
        new TestCase("te\\*t", "test", FAILS),
        new TestCase("te\\?t", "test", FAILS),
        new TestCase("tesT", "test", FAILS),
        new TestCase("test", "Test", FAILS),
        new TestCase("tEst", "teSt", FAILS),
        new TestCase("?est", "test", SUCCEEDS),
        new TestCase("te?t", "test", SUCCEEDS),
        new TestCase("tes?", "test", SUCCEEDS),
        new TestCase("test?", "test", FAILS),
        new TestCase("*", "", SUCCEEDS),
        new TestCase("*", "test", SUCCEEDS),
        new TestCase("*test", "test", SUCCEEDS),
        new TestCase("*est", "test", SUCCEEDS),
        new TestCase("*st", "test", SUCCEEDS),
        new TestCase("t*t", "test", SUCCEEDS),
        new TestCase("te*t", "test", SUCCEEDS),
        new TestCase("te*st", "test", SUCCEEDS),
        new TestCase("te*", "test", SUCCEEDS),
        new TestCase("tes*", "test", SUCCEEDS),
        new TestCase("test*", "test", SUCCEEDS),
        new TestCase(".[\\-\\t]", ".t", SUCCEEDS),
        new TestCase("test*?*[a-z]*", "testgoop", SUCCEEDS),
        new TestCase("te[^x]t", "test", SUCCEEDS),
        new TestCase("te[^abc]t", "test", SUCCEEDS),
        new TestCase("te[^x]t", "test", SUCCEEDS),
        new TestCase("te[!x]t", "test", SUCCEEDS),
        new TestCase("te[^x]t", "text", FAILS),
        new TestCase("te[^\\x]t", "text", FAILS),
        new TestCase("te[^x\\", "text", FAILS),
        new TestCase("te[/]t", "text", FAILS),
        new TestCase("te[r-t]t", "test", SUCCEEDS),
        new TestCase("te[R-T]t", "tent", FAILS),
        new TestCase("tes[]t]", "test", SUCCEEDS),
        new TestCase("tes[t-]", "test", SUCCEEDS),
        new TestCase("tes[t-]]", "test]", SUCCEEDS),
        new TestCase("tes[t-]]", "test", FAILS),
        new TestCase("tes[u-]", "test", FAILS),
        new TestCase("tes[t-]", "tes[t-]", FAILS),
        new TestCase("/", "", FAILS),
        new TestCase("", "/", FAILS),
        new TestCase("/test", "test", FAILS),
        new TestCase("test", "/test", FAILS),
        new TestCase("test/", "test", FAILS),
        new TestCase("test", "test/", FAILS),
        new TestCase("/*/test/", "/test", FAILS),
        new TestCase("/*/test/", "/test/test/", SUCCEEDS),
        new TestCase("test/this", "test/", FAILS),
        new TestCase("test/", "test/this", FAILS),
        new TestCase("test*/this", "test/this", SUCCEEDS),
        new TestCase("test*/this", "test/that", FAILS),
        new TestCase("test/*this", "test/this", SUCCEEDS),
        new TestCase(".*", ".this", SUCCEEDS),
        new TestCase("*", ".this", FAILS),
        new TestCase("?this", ".this", FAILS),
        new TestCase("[.]this", ".this", FAILS),
        new TestCase("test/this", "test/this", SUCCEEDS),
        new TestCase("test?this", "test/this", SUCCEEDS),
        new TestCase("test*this", "test/this", SUCCEEDS),
        new TestCase("test[/]this", "test/this", SUCCEEDS),
        new TestCase("test/.*", "test/.this", SUCCEEDS),
        new TestCase("test/*", "test/.this", FAILS),
        new TestCase("test/?this", "test/.this", FAILS),
        new TestCase("test/[.]this", "test/.this", FAILS)
    };

    @Test
    public void fnmatch() {
        for (TestCase test : tests) {
            boolean res = ApacheConf.fnmatch(test.pattern, test.filename);
            assertEquals(test.toString(), test.resultExpected, res);
        }
    }

    @Test
    public void config() throws Exception {
        ApacheConf apacheConf = new ApacheConf(new File("target/test-classes/httpd.conf"));
        assertSame(apacheConf.getVHosts().size(),5);
    }
    
    private class TestCase {

        protected String pattern, filename;
        protected boolean resultExpected;

        public TestCase(String pattern, String filename, boolean resultExpected) {
            this.pattern = pattern;
            this.filename = filename;
            this.resultExpected = resultExpected;
        }

        @Override
        public String toString() {
            return "TestCase{pattern=" + pattern + ", filename=" + filename + ", resultExpected=" + resultExpected + '}';
        }
    }
}