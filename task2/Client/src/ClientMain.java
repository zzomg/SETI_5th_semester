public class ClientMain {
    public static void main(String[] args) {
        try {
            Client client1 = new Client("C:\\Users\\user\\Desktop\\y\\55043240.png", "localhost", 6666);
            Client client2 = new Client("C:\\Users\\user\\Downloads\\Shrek.2.[HEVC].[Wanterlude].mkv", "localhost", 6666);
            Client client3 = new Client("C:\\Users\\user\\Downloads\\41bfea80ccdc2b4a009cf63858cc5521.jpg", "localhost", 6666);

            Thread client1Thread = new Thread(client1);
            Thread client2Thread = new Thread(client2);
            Thread client3Thread = new Thread(client3);

            client1Thread.start();
            client2Thread.start();
            client3Thread.start();

            client1Thread.join();
            client2Thread.join();
            client3Thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
