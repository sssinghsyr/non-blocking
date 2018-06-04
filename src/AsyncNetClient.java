import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AsyncNetClient {
  static BufferedReader userInputReader = null;
  static List<SocketChannel> channels = new ArrayList<SocketChannel>();

  public static boolean processReadySet(Set<SelectionKey> readySet) throws Exception {
    Iterator<SelectionKey> iterator = readySet.iterator();
    while (iterator.hasNext()) {
      SelectionKey key = (SelectionKey) iterator.next();
      iterator.remove();
      if (key.isConnectable()) {
        boolean connected = processConnect(key);
        if (!connected) {
          return true;
        }
      }
      if (key.isReadable()) {
        String msg = processRead(key);
        //System.out.println("[Server]: " + msg);
      }
      if (key.isWritable()) {
    	String msg = "Hello";
        SocketChannel sChannel = (SocketChannel) key.channel();
        //System.out.println("SO_SNDBUF :"+SocketOptions.SO_SNDBUF);
        msg += key.attachment();
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        while(buffer.hasRemaining())
        	sChannel.write(buffer);
        buffer.clear();
      }
    }
    return false;
  }
  public static boolean processConnect(SelectionKey key) throws Exception{
    SocketChannel channel = (SocketChannel) key.channel();
    while (channel.isConnectionPending()) {
      channel.finishConnect();
    }
    return true;
  }
  public static String processRead(SelectionKey key) throws Exception {
    SocketChannel sChannel = (SocketChannel) key.channel();
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    sChannel.read(buffer);
    buffer.flip();
    Charset charset = Charset.forName("UTF-8");
    CharsetDecoder decoder = charset.newDecoder();
    CharBuffer charBuffer = decoder.decode(buffer);
    buffer.clear();
    String msg = charBuffer.toString();
    return msg;
  }
  public static void main(String[] args) throws Exception {
    InetAddress serverIPAddress = InetAddress.getByName("localhost");
    int port = 19000;
    InetSocketAddress serverAddress = new InetSocketAddress(
        serverIPAddress, port);
    Selector selector = Selector.open();
    for(int i=0; i<1000; i++) {
    	SocketChannel lChannel = SocketChannel.open();
    	lChannel.configureBlocking(false);
    	lChannel.connect(serverAddress);
	    int operations = SelectionKey.OP_CONNECT | SelectionKey.OP_READ
	        | SelectionKey.OP_WRITE;
	    SelectionKey selectionKey = lChannel.register(selector, operations);
	    selectionKey.attach(i);
	    channels.add(lChannel);
    }

    userInputReader = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      if (selector.select() > 0) {
        boolean doneStatus = processReadySet(selector.selectedKeys());
        if (doneStatus) {
          break;
        }
      }
    }
    for(SocketChannel channel: channels)
    	channel.close();
  }
}