package pdx_team_one;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import org.junit.Test;
import org.mockito.Mockito;

public class RefineryTest {
    RobotController rcMock = Mockito.mock(RobotController.class);
    Refinery testRef = new Refinery(rcMock);

    /*
    @Test
    public void takeTurnTest() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        Mockito.doReturn(1).when(rcMock).getTeamSoup();
        Mockito.doReturn(loc).when(rcMock).getLocation();
        Mockito.doReturn(2).when(rcMock).sensePollution(loc);
        testRef.takeTurn();
    }*/
}
