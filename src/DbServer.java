import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class DbServer {
  public static void main(String[] args) throws Exception {
    InetAddress hostIPAddress = InetAddress.getByName("localhost");
    int port = 19000;
    Selector selector = Selector.open();
    ServerSocketChannel ssChannel = ServerSocketChannel.open();
    ssChannel.configureBlocking(false);
    ssChannel.socket().bind(new InetSocketAddress(hostIPAddress, port));
    ssChannel.register(selector, SelectionKey.OP_ACCEPT);
    while (true) {
      if (selector.select() <= 0) {
        continue;
      }
      processReadySet(selector.selectedKeys());
    }
  }
  public static void processReadySet(Set<SelectionKey> readySet) throws Exception {
    Iterator<SelectionKey> iterator = readySet.iterator();
    while (iterator.hasNext()) {
      SelectionKey key = (SelectionKey) iterator.next();
      iterator.remove();
      if (key.isAcceptable()) {
        ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
        SocketChannel sChannel = (SocketChannel) ssChannel.accept();
        sChannel.configureBlocking(false);
        sChannel.register(key.selector(), SelectionKey.OP_READ);
      }
      if (key.isReadable()) {
        String msg = processRead(key);
        if (msg.length() > 0) {
          //msg += "echoed back from Server";
          SocketChannel sChannel = (SocketChannel) key.channel();
          ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
          //System.out.println(Charset.defaultCharset().decode(buffer));
          while(buffer.hasRemaining()) {
        	  sChannel.write(buffer);
          }
          buffer.clear();
        }
      }
    }
  }
  public static String processRead(SelectionKey key) throws Exception {
    SocketChannel sChannel = (SocketChannel) key.channel();
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    try {
    	int bytesCount = sChannel.read(buffer);
    	if (bytesCount > 0) {
    	      buffer.flip();
    	      return new String(buffer.array());
    	}
    	return "NoMessage";
    }catch(IOException ex) {
    	System.out.println("Connection closed by the server");
    }
    return "";
  }
}