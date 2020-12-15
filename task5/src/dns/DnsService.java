package dns;

import handlers.ConnectHandler;
import handlers.Handler;
import utility.CacheMap;
import sockets.SocketRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

import org.xbill.DNS.*;

import static handlers.SocketRequestHandler.handleError;

public class DnsService
{
    private static final int DNS_SERVER_PORT = 53;
    private static final byte HOST_UNREACHABLE_ERROR = 0x04;
    private static final int BUF_SIZE = 1024;
    private static final int CACHE_SIZE = 256;
    private int messageId = 0;

    private DatagramChannel socket;
    private InetSocketAddress dnsServerAddress;
    private Handler dnsResponseHandler;

    private Map<Integer, DnsMapValue> unresolvedNames = new HashMap<>();

    private CacheMap<String, String> dnsCache = new CacheMap<>(CACHE_SIZE);

    private static final DnsService dnsService = new DnsService();

    public static DnsService getInstance()
    {
        return DnsService.dnsService;
    }

    private DnsService()
    {
        String[] dnsServers = ResolverConfig.getCurrentConfig().servers();
        this.dnsServerAddress = new InetSocketAddress(dnsServers[0], DNS_SERVER_PORT);
    }

    public void setSocket(DatagramChannel socket)
    {
        this.socket = socket;
        initResponseHandler();
    }

    public void registerSelector(Selector selector) throws ClosedChannelException
    {
        socket.register(selector, SelectionKey.OP_READ, dnsResponseHandler);
    }

    public void resolveName(SocketRequest request, SelectionKey selectionKey) throws IOException
    {
        try
        {
            String name = request.getDomainName();
            String cachedAddress = dnsCache.get(name + ".");
            if (cachedAddress != null) {
                connectToTarget(cachedAddress, selectionKey, request.getTargetPort());
                return;
            }

            System.out.println("New domain name to resolve: " + request.getDomainName());
            DnsMapValue mapValue = new DnsMapValue(selectionKey, request.getTargetPort());
            Message query = getQuery(name);
            byte[] queryBytes = query.toWire();

            unresolvedNames.put(query.getHeader().getID(), mapValue);
            socket.send(ByteBuffer.wrap(queryBytes), dnsServerAddress);
        } catch (TextParseException exc){
            handleError(selectionKey, HOST_UNREACHABLE_ERROR);
            exc.printStackTrace();
        }
    }
    
    private void initResponseHandler()
    {
        dnsResponseHandler = new Handler(null)
        {
            @Override
            public void handle(SelectionKey selectionKey) throws IOException
            {
                ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);

                if(socket.receive(buf) == null) { return; }

                Message response = new Message(buf.flip());
                Record[] answers = response.getSectionArray(Section.ANSWER);

                int responseId = response.getHeader().getID();
                DnsMapValue unresolvedName = unresolvedNames.get(responseId);

                if (answers.length == 0)
                {
                    handleError(unresolvedName.getSelectionKey(), HOST_UNREACHABLE_ERROR);
                    return;
                }

                String hostname = response.getQuestion().getName().toString();
                System.out.println(hostname + " resolved");

                String address = answers[0].rdataToString();
                dnsCache.put(hostname, address);
                connectToTarget(address, unresolvedName.getSelectionKey(), unresolvedName.getTargetPort());
                unresolvedNames.remove(responseId);
            }
        };
    }

    private void connectToTarget(String address, SelectionKey selectionKey, int port) throws IOException
    {
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        ConnectHandler.connect(selectionKey, socketAddress);
    }

    private Message getQuery(String domainName) throws TextParseException
    {
        Header header = new Header(messageId++);
        header.setFlag(Flags.RD);
        header.setOpcode(0);

        Message message = new Message();
        message.setHeader(header);

        Record record = Record.newRecord(new Name(domainName + "."), Type.A, DClass.IN);
        message.addRecord(record, Section.QUESTION);

        return message;
    }
}
