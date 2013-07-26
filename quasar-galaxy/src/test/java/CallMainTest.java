/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import co.paralleluniverse.galaxy.example.testing.Peer1;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author eitan
 */
@Ignore
public class CallMainTest {
    public CallMainTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //

    @Test
    public void hello() throws Exception {
        Peer1.main(new String[0]);
    }
}
