package pdx_team_one;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class VaporatorTest {

    RobotController rcMock = mock(RobotController.class);
    Vaporator testVaporator = new Vaporator(rcMock);

    @Test
    public void basicTest() throws GameActionException {
        testVaporator.takeTurn();
    }
}
