import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.*;
import java.net.Socket;

public class Client extends Thread {
    private static final int BUF_SZ = 1024;

    private final String path;
    private final Socket socket;
    
    private static final String success = "success";

    public Client(String path, String serverAddr, int serverPort) throws IOException {
        this.path = path;
        this.socket = new Socket(serverAddr, serverPort);
    }

    @Override
    public void run() {
        try (this.socket;
             DataInputStream in = new DataInputStream(this.socket.getInputStream());
             DataOutputStream out = new DataOutputStream(this.socket.getOutputStream())
        ) {
            File file = new File(this.path);
            long fileSize = file.length();

            // sending file size + name
            out.writeLong(fileSize);
            out.writeUTF(file.getName());

            // sending hash sum
            HashCode hash = Files.asByteSource(file).hash(Hashing.crc32());
            out.writeUTF(hash.toString());
            out.flush();

            // sending file
            int nSend;
            byte[] buf = new byte[BUF_SZ];
            FileInputStream fis = new FileInputStream(file);

            long totalSend = 0;
            while (fileSize - totalSend > 0) {
                if (fileSize - totalSend < BUF_SZ) {
                    nSend = fis.read(buf, 0, (int) (fileSize - totalSend));
                } else {
                    nSend = fis.read(buf, 0, BUF_SZ);
                }

                totalSend += nSend;

                out.write(buf, 0, nSend);
                out.flush();
            }

            // getting return code
            String ret = in.readUTF();
            if (ret.equals(success)) {
                System.out.println("SUCCESS: File sent");
            } else {
                System.out.println("FAIL: Couldn't send file");
            }

            in.close();
            out.close();
            this.socket.close();

        } catch (IOException e) {
            System.out.println("I/O error occurred: " + this.socket);
            e.printStackTrace();
        }
    }
}
