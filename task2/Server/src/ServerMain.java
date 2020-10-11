public class ServerMain {
    public static void main(String[] args) {
        try {
            Server server = new Server(6666);
            Thread serverThread = new Thread(server);
            serverThread.start();
            serverThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
