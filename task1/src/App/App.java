package App;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class App implements AutoCloseable
{
    private final MulticastSocket socket;
    private final InetSocketAddress mcastAddr;

    private Map<String, Long> alive = new HashMap<>();

    private final int SOCK_TIMEOUT = 2000;
    private final int UPDATE_TIMEOUT = 3000;

    private long lastSend;
    private long lastRecv;

    public App(String addr, int port) throws IOException {
        this.mcastAddr = new InetSocketAddress(addr, port);
        this.socket = new MulticastSocket(port);
        this.socket.setSoTimeout(this.SOCK_TIMEOUT);
        this.socket.joinGroup(InetAddress.getByName(addr));
    }

    private void send(String msg) throws IOException {
        DatagramPacket dgram = new DatagramPacket(msg.getBytes(), msg.length(), this.mcastAddr);
        this.socket.send(dgram);
        this.lastSend = System.currentTimeMillis();
    }

    private String receive() {
        byte[] buf = new byte[1000];
        DatagramPacket dgram = new DatagramPacket(buf, buf.length);
        try {
            this.socket.receive(dgram);
            this.lastRecv = System.currentTimeMillis();
        } catch (IOException e) {
            return null;
        }
        return dgram.getAddress().toString();
    }

    private void updateAlive(String recv) {
        LinkedList<String> dead = new LinkedList<>();

        if (recv != null) {
            this.alive.put(recv, this.lastRecv);
        }

        for (Map.Entry<String, Long> entry : this.alive.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue() > UPDATE_TIMEOUT) {
                dead.add(entry.getKey());
            }
        }
        for (String val : dead) {
            this.alive.remove(val);
        }
    }

    private void printAlive() {
        System.out.println(String.format("Alive: %d", this.alive.size()));
        for (Map.Entry<String, Long> entry : this.alive.entrySet()) {
            System.out.println("\t" + entry.getKey());
        }
    }

    public void run() throws IOException {
        this.lastSend = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - this.lastSend > SOCK_TIMEOUT) {
                send("check alive");
            }
            String recv = receive();

            updateAlive(recv);
            printAlive();
        }
    }

    @Override
    public void close() {
        this.socket.close();
    }
}
