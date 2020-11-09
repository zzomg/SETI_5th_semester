package TreeChat;

import TreeChat.Master.InputMaster;
import TreeChat.Master.MessageBufferMaster;
import TreeChat.Master.PingMaster;
import TreeChat.Master.RecvMaster;
import TreeChat.Slave.RecvSlave;
import TreeChat.Slave.SendSlave;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Timer;

public class Main {
    public static void main(String[] args) throws SocketException {
        int argsLength = args.length;

        if (argsLength != 3 && argsLength != 5) {
            System.err.println("Error: wrong number of arguments. Requires: node_name, loss_percentage, port." +
                    "Optional: parent_address, parent_port");
            System.exit(-1);
        }

        String name = args[0];
        int loss_percentage = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[2]);

        TreeNode node;
        if (argsLength == 3) {
            node = new TreeNode(name, port, loss_percentage);
        } else {
            InetSocketAddress parent = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
            node = new TreeNode(name, port, loss_percentage, parent);
        }

        Timer timer = new Timer(true);

        new Thread(new InputMaster(node)).start();
        new Thread(new RecvMaster(node)).start();
        new Thread(new SendSlave(node)).start();
        new Thread(new RecvSlave(node)).start();

        timer.scheduleAtFixedRate(new PingMaster(node),7000,7000);
        timer.scheduleAtFixedRate(new MessageBufferMaster(node),10000,15000);

        System.out.println(name + " is alive");
    }
}