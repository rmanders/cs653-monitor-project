/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

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
public class ConfigDataTest {

    public ConfigDataTest() {
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
     * Test of save method, of class ConfigData.
     */
    @Test
    public void testMain() {
        System.out.println("save");
        String filename = "";
        ConfigData instance = new ConfigData();
        //filename = instance.getFilename();

        //instance.addProperty("password", "------");
        //instance.addProperty("ident", "test2");
        //instance.addProperty("cookie", "9872JHGJG7891JHG");
        //instance.save("c:\\test.cfg");
        instance.load("c:\\test.cfg");
        System.out.println(instance);
    }
}