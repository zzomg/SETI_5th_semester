package handlers;

import utility.Connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptHandler extends Handler
{
    private static final int BUF_LEN = 65536;

    private ServerSocketChannel serverSocketChannel;

    public AcceptHandler(ServerSocketChannel serverSocketChannel) {
        super(null);
        this.serverSocketChannel = serverSocketChannel;
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        Connection connection = new Connection(BUF_LEN);
        SocketConnectHandler connectHandler = new SocketConnectHandler(connection);

        SelectionKey key = socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ, connectHandler);
        connection.registerBufferListener(() -> key.interestOpsOr(SelectionKey.OP_WRITE));

        System.out.println("New connection: " + socketChannel.getRemoteAddress());
    }
}
