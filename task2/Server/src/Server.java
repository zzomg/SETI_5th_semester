import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Server extends Thread {
    private static final String delim = "\\"; // windows-style

    private final ServerSocket serverSocket;

    public Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        Socket clientSocket;

        // creating folder where to save files
        String currPath = Paths.get(".").toAbsolutePath().normalize().toString();
        String uploadsPath = currPath + delim + "uploads";
        System.out.println("Files will be stored in " + uploadsPath);
        if (!Files.exists(Paths.get(uploadsPath))) {
            new File(uploadsPath).mkdirs();
        }

        while (true) {
            try {
                clientSocket = this.serverSocket.accept();
                ClientThread clientThread = new ClientThread(clientSocket, uploadsPath);
                System.out.println("Accepted new client" + clientSocket);
                clientThread.start();
            } catch (IOException e) {
                System.out.println("Cannot accept new connection");
            }
        }
    }
}
