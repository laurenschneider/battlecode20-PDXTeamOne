package pdx_team_one;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class HQTest {

    RobotController rc;
    HQ hqtest = new HQ(rc);

    @Test
    public void buildMinersShouldBuild5() {
        try {
            hqtest.buildMiners();
        } catch (GameActionException err) {
            System.out.println("Game action exception");
        }
        assertEquals(5, hqtest.numMiners);
    }

}
