package TreeChat;

import java.net.InetSocketAddress;
import java.net.SocketException;

public class Main  {
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

        TreeChat app;

        if (argsLength == 3) {
            app = new TreeChat(new TreeNode(name, port, loss_percentage));
        } else {
            String parent_hostname = args[3];
            int parent_port = Integer.parseInt(args[4]);
            InetSocketAddress parent = new InetSocketAddress(parent_hostname, parent_port);
            app = new TreeChat(new TreeNode(name, port, loss_percentage, parent));
        }

        app.run();
    }
}