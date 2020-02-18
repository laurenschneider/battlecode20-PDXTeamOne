package pdx_team_one;

import battlecode.common.*;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;

public class DeliveryDroneTest {

    private RobotController rcMock = mock(RobotController.class);
    DeliveryDrone testDrone = new DeliveryDrone(rcMock);

    @Test
    public void parseBlockchainWrongTeam() throws GameActionException {
        int cost = 1;
        int [] message = new int [4];
        message[0] = 0;
        message[1] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;

        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Mockito.doReturn(t).when(rcMock).getBlock(1);

        int res = testDrone.parseBlockchain();
        assertEquals(0, res);
    }

    @Test
    public void parseBlockchainOurTeam() throws GameActionException {

        int cost = 1;
        int [] message = new int [5];
        message[0] = 2222;
        message[1] = 0; message[2] = 0; message[3] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;

        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Mockito.doReturn(t).when(rcMock).getBlock(1);

        int res = testDrone.parseBlockchain();
        assertEquals(res, 1);
    }

    @Test
    public void parseBlockchainTestReturn2() throws GameActionException {

        int cost = 1;
        int [] message = new int [5];
        message[0] = 2222;
        message[1] = 3; message[2] = 0; message[3] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;

        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Mockito.doReturn(t).when(rcMock).getBlock(1);
        Mockito.doReturn(t).when(rcMock).getBlock(2);

        int res = testDrone.parseBlockchain();
        assertEquals(2, res);
    }

    @Test
    public void parseBlockchainTestReturn3() throws GameActionException {

        int cost = 1;
        int [] message = new int [5];
        message[0] = 2222;
        message[1] = 5; message[2] = 0; message[3] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;

        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Mockito.doReturn(t).when(rcMock).getBlock(1);
        Mockito.doReturn(t).when(rcMock).getBlock(2);

        int res = testDrone.parseBlockchain();
        assertEquals(3, res);
    }
}
