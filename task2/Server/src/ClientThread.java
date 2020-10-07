import com.google.common.base.Stopwatch;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.log4j.BasicConfigurator;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * 1. getting file size
 * 2. getting file name
 * 3. getting checksum
 * 4. getting file
 */

public class ClientThread extends Thread {
    private static final int BUF_SZ = 1024;
    private static final int PRINT_INFO_CD = 3; // seconds
    private static final long FILE_MAX_SZ = 1099511627776L; // 1 TB

    private static final String delim = "\\"; // windows-style

    private static final String sizeMismatch = "sz_fail";
    private static final String checksumMismatch = "hash_fail";
    private static final String success = "success";

    private final Socket socket;
    private final String path;

    public ClientThread(Socket sock, String path) {
        this.socket = sock;
        this.path = path;
        BasicConfigurator.configure();
    }

    @Override
    public void run() {
        try (this.socket;
             DataInputStream in = new DataInputStream(this.socket.getInputStream());
             DataOutputStream out = new DataOutputStream(this.socket.getOutputStream())
        )
        {
            // getting file size + name
            long fileSize = in.readLong();

            if (fileSize > FILE_MAX_SZ) {
                System.out.println("File is too large");
                freeResources(in, out, this.socket);
                return;
            }

            String fileName = in.readUTF();

            // receiving hash sum
            String recvHash = in.readUTF();

            // receiving file
            File file = new File(this.path + delim + fileName);
            FileOutputStream fos = new FileOutputStream(file);

            byte[] buf = new byte[BUF_SZ];

            int nRecv;
            double nRecvMB;
            long totalRecv = 0;
            double totalRecvMB;
            long bytesLeft;

            double speed;
            double avgSpeed;

            double elapsed;
            double totalElapsed;

            Stopwatch printInfoTimer = Stopwatch.createStarted();
            Stopwatch globalTime = Stopwatch.createStarted();
            Stopwatch readingTime = Stopwatch.createStarted();

            System.out.println(String.format("Client: %s ----- Downloading started, file %s", this.socket, fileName));

            while (totalRecv < fileSize) {
                bytesLeft = fileSize - totalRecv;
                if (bytesLeft < BUF_SZ) {
                    nRecv = in.read(buf, 0, (int)bytesLeft);
                } else {
                    nRecv = in.read(buf, 0, BUF_SZ);
                }

                nRecvMB = (double)nRecv / 10e6;
                elapsed = readingTime.elapsed(TimeUnit.NANOSECONDS) / 10e9;

                speed = nRecvMB / elapsed; // instant speed

                totalRecv += nRecv;

                if (printInfoTimer.elapsed(TimeUnit.SECONDS) >= PRINT_INFO_CD) {
                    totalRecvMB = (double)totalRecv / 10e6;
                    totalElapsed = globalTime.elapsed(TimeUnit.NANOSECONDS) / 10e9;
                    avgSpeed = totalRecvMB / totalElapsed;
                    System.out.println(String.format("Client: %s ----- Speed: %f MBytes/sec ----- Avg speed: %f MBytes/sec",
                            this.socket, speed, avgSpeed));
                    printInfoTimer.reset(); printInfoTimer.start();
                }

                fos.write(buf, 0, nRecv);
                readingTime.reset(); readingTime.start();
            }
            fos.close();

            totalElapsed = globalTime.elapsed(TimeUnit.NANOSECONDS) / 10e9;
            globalTime.stop();
            readingTime.stop();
            printInfoTimer.stop();

            if (totalElapsed <= PRINT_INFO_CD) {
                totalRecvMB = (double)totalRecv / 10e6;
                avgSpeed = totalRecvMB / totalElapsed;
                // printing out inst. speed doesn't make sense here b/c connection was really short
                System.out.println(String.format("Client: %s ----- Avg speed: %f bytes/sec", this.socket, avgSpeed));
            }

            if (totalRecv != fileSize) {
                out.writeUTF(sizeMismatch);
                System.out.println(String.format("Failed during obtaining file %s: size mismatch. " +
                        "Got %d, expected %d", fileName, totalRecv, fileSize));
                if(!file.delete()) {
                    System.out.println("Cannot delete file" + fileName);
                }
                freeResources(in, out, this.socket);
                return;
            }

            // checking hash sum
            HashCode realHash = Files.asByteSource(file).hash(Hashing.crc32());

            if (!recvHash.equals(realHash.toString())) {
                out.writeUTF(checksumMismatch);
                System.out.println(String.format("Failed after obtaining file %s: hash sum mismatch.", fileName));
                if(!file.delete()) {
                    System.out.println("Cannot delete file");
                }
                freeResources(in, out, this.socket);
                return;
            }

            System.out.println(String.format("Client: %s ----- Success: File %s downloaded", this.socket, fileName));

            out.writeUTF(success);
            freeResources(in, out, this.socket);

        } catch (IOException e) {
            System.out.println("I/O error occurred: " + this.socket);
            e.printStackTrace();
        }
    }

    private void freeResources(InputStream in, OutputStream out, Socket sock) throws IOException {
        in.close();
        out.close();
        sock.close();
    }
}
