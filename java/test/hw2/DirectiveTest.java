/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hw2;

import cs653.Directive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author rmanders
 */
public class DirectiveTest {

    public static String f1 = "BLAH aksjhkasjhd UJBDG";
    public static String w1 = "WAITING:";
    public static String w2 = "WAITING: ARG1 ARG2";
    public static String r1 = "REQUIRE:";
    public static String r2 = "REQUIRE: ARG1 BLAH BLAH";
    public static String e1 = "COMMAND_ERROR:";
    public static String e2 = "COMMAND_ERROR THIS IS A TEST";
    public static String c1 = "COMMENT:";
    public static String c2 = "COMMENT: This here is a comment";
    public static String s1 = "RESULT:";
    public static String s2 = "RESULT: ARG1";
    public static String s3 = "RESULT: ARG2 these are the results";
    public static String p1 = "PARTICIPANT_PASSWORD_CHECKSUM:";
    public static String p2 = "PARTICIPANT_PASSWORD_CHECKSUM: PASSWORDCHECKSUM";
    public static String p3 = "PARTICIPANT_PASSWORD_CHECKSUM: PASSWORD CHECKSUM";


    public DirectiveTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getInstance method, of class Directive.
     */
    @Test
    public void testWAITING() {
        System.out.println("Test Waiting");

        Directive dir1 = Directive.getInstance(w1);
        Directive dir2 = Directive.getInstance(w2);
        assertNull(Directive.getInstance(f1));
        assertTrue(testDirective(dir1, Directive.WAITING, null, null));
        assertTrue(testDirective(dir2, Directive.WAITING, null, null));
    }

    @Test
    public void testREQUIRE() {
        System.out.println("Test Require");
        Directive dir1 = Directive.getInstance(r1);
        Directive dir2 = Directive.getInstance(r2);
        assertNull(dir1);
        assertTrue(testDirective(dir2, Directive.REQUIRE, "ARG1", null));
    }

    @Test
    public void testCOMMAND_ERROR() {
        System.out.println("====Test Command_error");
        Directive dir1 = Directive.getInstance(e1);
        Directive dir2 = Directive.getInstance(e2);
        System.out.println(dir1);
        System.out.println(dir2);
        assertTrue(testDirective(dir1, Directive.COMMAND_ERROR, null, null));
        assertTrue(testDirective(dir2, Directive.COMMAND_ERROR,
                null, "THIS IS A TEST"));
    }

    @Test
    public void testCOMMENT() {
        System.out.println("Test Comment");
        Directive dir1 = Directive.getInstance(c1);
        Directive dir2 = Directive.getInstance(c2);
        assertTrue(testDirective(dir1, Directive.COMMENT, null, null));
        System.out.println(dir2.toString());
        assertTrue(testDirective(dir2, Directive.COMMENT,
                null, "This here is a comment"));
    }

    @Test
    public void testRESULT() {
        System.out.println("Test Result");
        Directive dir1 = Directive.getInstance(s1);
        Directive dir2 = Directive.getInstance(s2);
        Directive dir3 = Directive.getInstance(s3);
        assertNull(dir1);
        assertTrue(testDirective(dir2, Directive.RESULT, "ARG1", null));
        System.out.println(dir3.toString());
        assertTrue(testDirective(dir3, Directive.RESULT,
                "ARG2", "these are the results"));
    }

    @Test
    public void testPPC() {
        System.out.println("Test Participant Password Checksum");
        Directive dir1 = Directive.getInstance(p1);
        Directive dir2 = Directive.getInstance(p2);
        Directive dir3 = Directive.getInstance(p3);
        assertNull(dir1);
        assertTrue(testDirective(dir2, Directive.PARTICIPANT_PASSWORD_CHECKSUM,
                "PASSWORDCHECKSUM", null));
        //System.out.println(dir3.toString());
        assertTrue(testDirective(dir3, Directive.PARTICIPANT_PASSWORD_CHECKSUM,
                "PASSWORD", null));
    }

    public boolean testDirective( final Directive dir, int expDir,
            String expArg, String expPayload)
    {
        if( null == dir )
        {
            System.out.println("FAIL: Directive instance is NULL");
            return false;
        }

        if( dir.getDirective() != expDir) { return false; }
        if( null == expArg )
        {
            if( null != dir.getArg() ) { return false; }
        }
        else
        {
            if( null == dir.getArg() ||
                    !dir.getArg().equals(expArg) ) { return false; }
        }
        if( null == expPayload )
        {
            if( null != dir.getPayload() ) { return false; }
        }
        else
        {
            if( null == dir.getPayload() ||
                    !dir.getPayload().equals(expPayload) ) { return false; }
        }
        return true;
    }
}