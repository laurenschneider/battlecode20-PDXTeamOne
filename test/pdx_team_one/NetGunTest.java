package pdx_team_one;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

public class NetGunTest {

    RobotController rcMock = mock(RobotController.class);
    NetGun testGun = new NetGun(rcMock);

    @Test
    public void testTakeTurn() throws GameActionException {
        RobotInfo rinfoMock= mock(RobotInfo.class);
        RobotInfo[] arr = new RobotInfo[1];
        arr[0] = rinfoMock;
        Mockito.doReturn(arr).when(rcMock).senseNearbyRobots();


        testGun.takeTurn();
    }
}
