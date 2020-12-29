package handlers;

import utility.Connection;
import sockets.SocketConnectRequest;
import sockets.SocketConnectResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import static sockets.SocketParser.*;

public class SocketConnectHandler extends SocketHandler
{
    private static final byte NO_AUTH = 0x00;
    private static final int SOCKS_VERSION = 0x05;
    private static final byte NO_METHOD = (byte) 0xFF;

    public SocketConnectHandler(Connection connection)
    {
        super(connection);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException
    {
        Connection connection = getConnection();
        ByteBuffer outputBuffer = connection.getOutputBuffer();
        read(selectionKey);
        SocketConnectRequest connectRequest = parseConnect(outputBuffer);

        if(connectRequest == null) { return; }

        SocketConnectResponse connectResponse = new SocketConnectResponse();

        if(!checkRequest(connectRequest)) {
            connectResponse.setMethod(NO_METHOD);
        }

        ByteBuffer inputBuffer = connection.getInputBuffer();
        inputBuffer.put(connectResponse.toByteArr());

        selectionKey.interestOpsOr(SelectionKey.OP_WRITE);
        selectionKey.attach(new SocketRequestHandler(connection));
        connection.getOutputBuffer().clear();
    }

    private boolean checkRequest(SocketConnectRequest connectRequest) {
        return connectRequest.getVersion() == SOCKS_VERSION && checkMethods(connectRequest.getMethods());
    }

    private static boolean checkMethods(byte[] methods) {
        for (var method : methods) {
            if(method == NO_AUTH) {
                return true;
            }
        }
        return false;
    }
}
