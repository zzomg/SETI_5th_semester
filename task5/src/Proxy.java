import handlers.AcceptHandler;
import handlers.Handler;
import dns.DnsService;
import utility.Connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

public class Proxy
{
    private final int port;

    public Proxy(int port) {
        this.port = port;
    }

    public void start()
    {
        try(Selector selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            DatagramChannel datagramSocket = DatagramChannel.open())
        {
            datagramSocket.configureBlocking(false);

            DnsService dnsService = DnsService.getInstance();
            dnsService.setSocket(datagramSocket);
            dnsService.registerSelector(selector);

            initServerSocketChannel(serverSocketChannel, selector);
            select(selector);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initServerSocketChannel(ServerSocketChannel serverSocketChannel,
                                        Selector selector) throws IOException
    {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, new AcceptHandler(serverSocketChannel));
    }

    private void select(Selector selector) throws IOException
    {
        while (true)
        {
            selector.select();
            var readyKeys = selector.selectedKeys();
            var iterator = readyKeys.iterator();
            while (iterator.hasNext())
            {
                var readyKey = iterator.next();
                try
                {
                    iterator.remove();
                    if(readyKey.isValid())
                    {
                        handleSelectionKey(readyKey);
                    }
                } catch (IOException exception) {
                    closeConnection(readyKey);
                } catch (CancelledKeyException exc) {
                    exc.printStackTrace();
                }
            }
        }
    }

    private void closeConnection(SelectionKey selectionKey) throws IOException
    {
        Handler handler = (Handler) selectionKey.attachment();
        Connection connection = handler.getConnection();
        SocketChannel sock = (SocketChannel) selectionKey.channel();

        try {
            System.out.println("Socket closed: " + sock.getRemoteAddress());
            sock.close();
            connection.closeSockChannel();
        } catch (ClosedChannelException cce) {
            System.out.println(cce.getLocalizedMessage());
        }
    }

    private void handleSelectionKey(SelectionKey selectionKey) throws IOException
    {
        Handler handler = (Handler) selectionKey.attachment();

        if (selectionKey.isWritable()) {
            handler.write(selectionKey);
        }

        if (selectionKey.isValid() && selectionKey.readyOps() != SelectionKey.OP_WRITE) {
            handler.handle(selectionKey);
        }
    }
}
