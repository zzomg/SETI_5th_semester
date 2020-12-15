package handlers;

import utility.Connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public abstract class Handler
{
    private static final int BUF_LEN = 65536;

    private Connection connection;

    public Handler(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() { return connection; }

    abstract public void handle(SelectionKey selectionKey) throws IOException;

    public int read(SelectionKey selectionKey) throws IOException {
        Handler handler = (Handler) selectionKey.attachment();
        SocketChannel socket = (SocketChannel) selectionKey.channel();
        Connection connection = handler.getConnection();
        ByteBuffer outputBuffer = connection.getOutputBuffer();

        if(!readyToRead(outputBuffer, connection)) {
            return 0;
        }

        int readCount = socket.read(outputBuffer);

        if(readCount <= 0) {
            connection.shutdown();
            selectionKey.interestOps(0);
            checkConnectionClose(socket);
        }

        return readCount;
    }

    public int write(SelectionKey selectionKey) throws IOException {
        ByteBuffer inputBuffer = connection.getInputBuffer();
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        connection.setBuf();
        socketChannel.write(inputBuffer);

        int remaining = inputBuffer.remaining();

        if(remaining == 0) {
            selectionKey.interestOps(SelectionKey.OP_READ);
            checkConn(socketChannel, inputBuffer);
        }
        else {
            connection.setStartPos();
        }
        return remaining;
    }

    private boolean readyToRead(ByteBuffer buffer, Connection connection) {
        return buffer.position() < BUF_LEN / 2 || connection.isBufShutDown();
    }

    private void checkConnectionClose(SocketChannel socketChannel) throws IOException {
        if(connection.canClose()) {
            System.out.println("Socket closed: " + socketChannel.getRemoteAddress());
            socketChannel.close();
            connection.closeSockChannel();
        }
    }

    private void checkConn(SocketChannel socketChannel, ByteBuffer buffer) throws IOException {
        if(connection.isBufShutDown()) {
            socketChannel.shutdownOutput();
            return;
        }
        buffer.clear();
        connection.resetStartPos();
    }
}
