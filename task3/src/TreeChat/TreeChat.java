package TreeChat;

import TreeChat.Master.InputMaster;
import TreeChat.Master.MessageBufferMaster;
import TreeChat.Master.PingMaster;
import TreeChat.Master.RecvMaster;
import TreeChat.Slave.RecvSlave;
import TreeChat.Slave.SendSlave;

import java.io.Closeable;
import java.util.Timer;

public class TreeChat implements Closeable {

    private Thread inputMasterThread;
    private Thread recvMasterThread;
    private Thread sendSlaveThread;
    private Thread recvSlaveThread;

    TreeNode node;
    Timer timer;

    public TreeChat(TreeNode node) {
        this.node = node;
    }

    public void run() {
        timer = new Timer(true);

        inputMasterThread = new Thread(new InputMaster(node));
        inputMasterThread.start();
        recvMasterThread = new Thread(new RecvMaster(node));
        recvMasterThread.start();
        sendSlaveThread = new Thread(new SendSlave(node));
        sendSlaveThread.start();
        recvSlaveThread = new Thread(new RecvSlave(node));
        recvSlaveThread.start();

        timer.scheduleAtFixedRate(new PingMaster(node),7000,7000);
        timer.scheduleAtFixedRate(new MessageBufferMaster(node),10000,15000);

        System.out.println(node.getName() + " is alive");
    }

    @Override
    public void close() {
        timer.cancel();

        recvSlaveThread.interrupt();
        recvMasterThread.interrupt();
        sendSlaveThread.interrupt();
        inputMasterThread.interrupt();

        node.getSocket().close();
    }
}
