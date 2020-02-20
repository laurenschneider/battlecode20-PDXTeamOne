package pdx_team_one;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

public class RobotPlayerTest {

	private RobotController rcMock = mock(RobotController.class);
	RobotPlayer rpTest = new RobotPlayer();

	@Test
	public void testRunHQ() throws GameActionException {

//		Mockito.doReturn(RobotType.HQ).when(rcMock).getType();
//		Mockito.doReturn(null).when(rcMock).getLocation();
//
//		rpTest.run(rcMock);
	}

}
