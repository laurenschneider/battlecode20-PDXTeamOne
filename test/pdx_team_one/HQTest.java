package pdx_team_one;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.*;

public class HQTest {

    private RobotController rcMock = mock(RobotController.class);
    HQ hqtest = new HQ(rcMock);

    @Test
    public void buildMinersShouldBuild1() throws GameActionException {
        HQ hqspy = Mockito.spy(hqtest);

        Mockito.doReturn(true).when(hqspy).tryBuild(RobotType.MINER, Direction.EAST);

        int actual = hqspy.buildMiners();
        assertEquals(1, actual);
    }

    @Test
    public void takeTurnShouldSendLocation() throws GameActionException {
        HQ hqspy = Mockito.spy(hqtest);
        Mockito.doReturn(true).when(hqspy).sendLocation();
        Mockito.doReturn(1).when(hqspy).buildMiners();
        Mockito.doReturn(true).when(hqspy).defense();

        hqspy.takeTurn();
        assertEquals(true, hqtest.locationSent);
    }

}
