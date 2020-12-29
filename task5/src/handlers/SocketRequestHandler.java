package handlers;

import sockets.SocketRequest;
import utility.Connection;
import sockets.SocketResponse;
import dns.DnsService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import static handlers.ConnectHandler.connect;
import static sockets.SocketParser.parseRequest;

public class SocketRequestHandler extends SocketHandler
{
    private static final byte DOMAIN_NAME_TYPE = 0x03;

    public SocketRequestHandler(Connection connection)
    {
        super(connection);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException
    {
        ByteBuffer outputBuffer = getConnection().getOutputBuffer();
        read(selectionKey);
        SocketRequest request = parseRequest(outputBuffer);

        if (request == null) { return; }

        byte parseError = request.getParseError();

        if (parseError != 0) {
            handleError(selectionKey, parseError);
            return;
        }

        if (request.getAddressType() == DOMAIN_NAME_TYPE) {
            DnsService dnsService = DnsService.getInstance();
            dnsService.resolveName(request,selectionKey);
            return;
        }

        connect(selectionKey, request.getAddress());
    }

    public static void handleError(SelectionKey selectionKey, byte error) {
        Handler handler = (Handler) selectionKey.attachment();
        Connection connection = handler.getConnection();

        errResp2Buf(selectionKey, connection, error);
        selectionKey.attach(new SocketErrorHandler(connection));
    }

    public static void errResp2Buf(SelectionKey selectionKey, Connection connection, byte error) {
        SocketResponse response = new SocketResponse();
        response.setReply(error);

        ByteBuffer inputBuff = connection.getInputBuffer();
        inputBuff.put(response.toSimpleByteBuf());
        connection.getOutputBuffer().clear();
        selectionKey.interestOpsOr(SelectionKey.OP_WRITE);
    }
}
