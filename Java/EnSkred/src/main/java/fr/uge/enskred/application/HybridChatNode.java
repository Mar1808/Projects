package fr.uge.enskred.application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class HybridChatNode {
    private static final int BUFFER_SIZE = 1024;
    private static final Logger logger = Logger.getLogger(HybridChatNode.class.getName());
    
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private final String username;
    
    public HybridChatNode(String username, int port) throws IOException {
        this.username = username;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }
    
    public void start() {
        new Thread(this::runServer).start();
        new Thread(this::runClient).start();
    }
    
    private void runServer() {
        try {
            while (true) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) acceptConnection();
                    if (key.isReadable()) readMessage(key);
                }
            }
        } catch (IOException e) {
            logger.severe("Server error: " + e.getMessage());
        }
    }
    
    private void acceptConnection() throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        if (client != null) {
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            logger.info("New client connected: " + client.getRemoteAddress());
        }
    }
    
    private void readMessage(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int bytesRead = channel.read(buffer);
        if (bytesRead == -1) {
            channel.close();
            return;
        }
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String message = new String(bytes);
        System.out.println("Received: " + message);
    }
    
    private void runClient() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter message: ");
                String message = scanner.nextLine();
                messageQueue.add(username + ": " + message);
                System.out.print("Enter server IP to send to: ");
                String ip = scanner.nextLine();
                System.out.print("Enter server port: ");
                int port = Integer.parseInt(scanner.nextLine());
                sendMessage(ip, port);
            }
        }
    }
    
    private void sendMessage(String ip, int port) {
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(ip, port))) {
            socketChannel.configureBlocking(true);
            while (!messageQueue.isEmpty()) {
                ByteBuffer buffer = ByteBuffer.wrap(messageQueue.poll().getBytes());
                socketChannel.write(buffer);
            }
        } catch (IOException e) {
            logger.warning("Failed to send message: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java HybridChatNode <username> <port>");
            return;
        }
        String username = args[0];
        int port = Integer.parseInt(args[1]);
        new HybridChatNode(username, port).start();
    }
}
