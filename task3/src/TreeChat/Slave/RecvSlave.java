package TreeChat.Slave;

import TreeChat.Message.MessageType;
import TreeChat.Packet;
import TreeChat.TreeNode;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class RecvSlave implements Runnable {
    private final DatagramSocket socket;
    private final TreeNode node;
    private final int REC_TTL = 1;

    public RecvSlave(TreeNode node) {
        this.socket = node.getSocket();
        this.node = node;
    }

    @Override
    public void run() {
        while (true) {
            ConcurrentLinkedQueue<Packet> recvPackets = node.getRecvPackets();
            try {
                Packet packet = Packet.getPacket(socket, REC_TTL);
                if(packet.getMessage().getMessageType() == MessageType.CHAT_MESSAGE){
                    int flag =  ThreadLocalRandom.current().nextInt(0, 100);
                    if(flag > node.getLossPercentage()) {
                        System.out.println(flag);
                        recvPackets.add(packet);
                    }
                    else {
                        System.out.println("Packet was lost.");
                    }
                } else {
                    recvPackets.add(packet);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}