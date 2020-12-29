package handlers;

import utility.Connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public abstract class SocketHandler extends Handler
{
    public SocketHandler(Connection connection)
    {
        super(connection);
    }

    @Override
    public int read(SelectionKey selectionKey) throws IOException {
        int readCount = super.read(selectionKey);

        if(readCount < 0) {
            throw new IOException("Socket closed during SOCKS5 handshake");
        }
        return readCount;
    }
}
