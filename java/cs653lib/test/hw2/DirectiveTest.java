/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hw2;

import cs653.Directive;
import cs653.DirectiveType;
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
    public static String r3 = "REQUIRE: ARG1 ";
    public static String e1 = "COMMAND_ERROR:";
    public static String e2 = "COMMAND_ERROR: THIS IS A TEST";
    public static String c1 = "COMMENT:";
    public static String c2 = "COMMENT: This here is a comment";
    public static String s1 = "RESULT:";
    public static String s2 = "RESULT: IDENT";
    public static String s3 = "RESULT: PASSWORD these are the results";
    public static String s4 = "RESULT:  SUBSET_A  1 3 4 5 6 8 9";
    public static String p1 = "PARTICIPANT_PASSWORD_CHECKSUM:";
    public static String p2 = "PARTICIPANT_PASSWORD_CHECKSUM: 05fA67b05D";
    public static String p3 = "PARTICIPANT_PASSWORD_CHECKSUM: PASSWORD CHECKSUM";
    public static String x1 = "TRANSFER:";
    public static String x2 = "TRANSFER: kjhd 23 SDW kjhd";
    public static String x3 = "TRANSFER: PLAYER_1 xyz FROM player_3";
    public static String x4 = "TRANSFER: PLAYER_1 100 FROM player_3";

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
        System.out.println("==== Test Waiting ====");

        String args[] = new String[0];

        Directive dir1 = Directive.getInstance(w1);
        Directive dir2 = Directive.getInstance(w2);

        assertTrue(testDirective(dir1, DirectiveType.WAITING, 0, args));
        assertNull(Directive.getInstance(w2));
    }

    @Test
    public void testREQUIRE() {
        System.out.println("==== Test Require ====");

        String args[] = {"ARG1"};

        Directive dir1 = Directive.getInstance(r1);
        Directive dir2 = Directive.getInstance(r2);
        Directive dir3 = Directive.getInstance(r3);
        assertNull(dir1);
        assertNull(dir2);
        assertTrue(testDirective(dir3, DirectiveType.REQUIRE, 1, args));
    }

    @Test
    public void testCOMMAND_ERROR() {
        System.out.println("==== Test Command_error ====");

        String args1[] = new String[0];
        String args2[] = {"THIS IS A TEST"};

        Directive dir1 = Directive.getInstance(e1);
        Directive dir2 = Directive.getInstance(e2);
        //System.out.println(dir1);
        //System.out.println(dir2);
        assertTrue(testDirective(dir1, DirectiveType.COMMAND_ERROR, 0, args1));
        assertTrue(testDirective(dir2, DirectiveType.COMMAND_ERROR, 1, args2));
    }

    @Test
    public void testCOMMENT() {
        System.out.println("==== Test Comment ====");

        String args1[] = new String[0];
        String args2[] = {"This here is a comment"};
        Directive dir1 = Directive.getInstance(c1);
        Directive dir2 = Directive.getInstance(c2);
        assertTrue(testDirective(dir1, DirectiveType.COMMENT, 0, args1));
        System.out.println(dir2.toString());
        assertTrue(testDirective(dir2, DirectiveType.COMMENT, 1, args2));
    }

    @Test
    public void testRESULT() {
        System.out.println("==== Test Result ====");

        String args1[] = new String[0];
        String args2[] = {"IDENT"};
        String args3[] = {"PASSWORD","these are the results"};
        String args4[] = {"SUBSET_A","1 3 4 5 6 8 9"};

        Directive dir1 = Directive.getInstance(s1);
        Directive dir2 = Directive.getInstance(s2);
        Directive dir3 = Directive.getInstance(s3);
        Directive dir4 = Directive.getInstance(s4);
        assertNull(dir1);
        assertTrue(testDirective(dir2, DirectiveType.RESULT, 1, args2));
        //System.out.println(dir3.toString());
        assertTrue(testDirective(dir3, DirectiveType.RESULT, 2, args3));
        assertTrue(testDirective(dir4, DirectiveType.RESULT, 2, args4));

        Integer test[] = dir4.getArgIntArray(1);
        for( Integer I : test ) {
            System.out.println(I);
            }

    }

    @Test
    public void testPPC() {
        System.out.println("==== Test Participant Password Checksum ====");

        String args1[] = new String[0];
        String args2[] = {"05fA67b05D"};
        String args3[] = {"PASSWORD"};

        Directive dir1 = Directive.getInstance(p1);
        Directive dir2 = Directive.getInstance(p2);
        Directive dir3 = Directive.getInstance(p3);
        assertNull(dir1);
        assertTrue(testDirective(dir2, 
                DirectiveType.PARTICIPANT_PASSWORD_CHECKSUM,
                1, args2));
        assertNull(dir3);

    }

    @Test
    public void testTransfer() {
        System.out.println("==== Test Transfer ====");

        String args4[] = {"PLAYER_1", "100", "FROM", "player_3"};

        Directive dir1 = Directive.getInstance(x1);
        Directive dir2 = Directive.getInstance(x2);
        Directive dir3 = Directive.getInstance(x3);
        Directive dir4 = Directive.getInstance(x4);
        assertNull(dir1);
        assertNull(dir2);
        assertNull(dir3);
        assertTrue(testDirective(dir4,DirectiveType.TRANSFER, 4, args4));

    }

    public boolean testDirective( final Directive dir, DirectiveType expDir,
            int expArgCount, String[] expArgs)
    {
        if( null == dir )
        {
            System.out.println("FAIL: Directive instance is NULL");
            return false;
        }

        if( dir.getDirectiveType() != expDir) { return false; }

        if( expArgCount != dir.getArgCount() )
        {
            System.out.println("FAIL: Directive argument count doesn't match expected");
            System.out.println("\tExpected: " + expArgCount);
            System.out.println("\tActual: " + dir.getArgCount());
            return false;
        }

        try {
            for( int i=0; i<expArgs.length; i++ ) {
                if( !expArgs[i].equals(dir.getArg(i))) {
                    System.out.println("FAIL: Directive arguments don't match expected");
                    System.out.println("\tOn Arg Number: " + i);
                    System.out.println("\tExpected: " + expArgs[i]);
                    System.out.println("\tActual: " + dir.getArg(i));
                    return false;
                }
            }
        } catch (Exception ex) {
            System.out.println("FAIL: Directive arguments don't match expected");
            System.out.println("\tExpected: " + expArgCount);
            System.out.println("\tActual: " + dir.getArgCount());
            System.out.println("\tException Occurred: " + ex);
            return false;
        }

        return true;
    }
}