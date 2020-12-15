package handlers;

import utility.Connection;
import sockets.SocketResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ConnectHandler extends Handler
{
    private static final int ANY_PORT = 0;

    public ConnectHandler(Connection connection)
    {
        super(connection);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        Handler handler = (Handler) selectionKey.attachment();
        Connection connection = handler.getConnection();

        socketChannel.finishConnect();

        selectionKey.attach(new ForwardHandler(connection));
        selectionKey.interestOpsAnd(~SelectionKey.OP_CONNECT);
        selectionKey.interestOpsOr(SelectionKey.OP_READ);
    }

    public static SocketChannel initTargetSocket(Connection clientConnection, SelectionKey selectionKey,
                                                 InetSocketAddress targetAddress) throws IOException
    {
        SocketChannel targetSocket = SocketChannel.open();
        targetSocket.bind(new InetSocketAddress(ANY_PORT));
        targetSocket.configureBlocking(false);

        Connection targetConnection = new Connection(clientConnection.getObservableInputBuffer(),
                clientConnection.getObservableOutputBuffer());

        targetSocket.connect(targetAddress);
        ConnectHandler connectHandler = new ConnectHandler(targetConnection);

        clientConnection.setSockChannel(targetSocket);
        targetConnection.setSockChannel((SocketChannel) selectionKey.channel());

        SelectionKey key = targetSocket.register(selectionKey.selector(), SelectionKey.OP_CONNECT, connectHandler);
        targetConnection.registerBufferListener(() -> key.interestOpsOr(SelectionKey.OP_WRITE));

        return targetSocket;
    }

    public static void connect(SelectionKey clientKey, InetSocketAddress targetAddress) throws IOException {
        Handler handler = (Handler) clientKey.attachment();
        Connection clientConnection = handler.getConnection();
        SocketChannel targetSocketChannel = initTargetSocket(clientConnection, clientKey, targetAddress);

        resp2Buf(clientConnection, targetSocketChannel);
        clientKey.interestOpsOr(SelectionKey.OP_WRITE);
        clientKey.attach(new ForwardHandler(clientConnection));
        clientConnection.getOutputBuffer().clear();
    }

    private static void resp2Buf(Connection connection, SocketChannel socketChannel) throws IOException {
        InetSocketAddress socketAddress = (InetSocketAddress) socketChannel.getLocalAddress();

        SocketResponse response = new SocketResponse();
        byte[] address = InetAddress.getLocalHost().getAddress();

        response.setBoundIp4Addr(address);
        response.setBoundPort((short) socketAddress.getPort());

        ByteBuffer inputBuff = connection.getInputBuffer();
        inputBuff.put(response.toByteBuf());
    }
}
