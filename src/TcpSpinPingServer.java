/*
 * Heavily inspired by http://code.google.com/p/core-java-performance-examples/source/browse/trunk/src/test/java/com/google/code/java/core/socket/PingTest.java
 * And therefore maintaining original licence:
 * -------------------------------------------
 * Copyright (c) 2011.  Peter Lawrey
 *
 * "THE BEER-WARE LICENSE" (Revision 128)
 * As long as you retain this notice you can do whatever you want with this stuff.
 * If we meet some day, and you think this stuff is worth it, you can buy me a beer in return
 * There is no warranty.
 * -------------------------------------------
 * Further mutated by Nitsan Wakart.
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import util.Helper;

public class TcpSpinPingServer {
    private static final int PAGE_SIZE = 4096;

    public static void main(String[] args) throws IOException, InterruptedException {
        String nic = args.length > 0 ? args[0] : "0.0.0.0";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 12345;
        System.out.println("Listening on interface : " + nic + ":" + port);
        final ByteBuffer buffy = ByteBuffer.allocateDirect(PAGE_SIZE).order(ByteOrder.nativeOrder());
        final ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(nic, port));
        SocketChannel accepted = null;
        try {
            accepted = serverSocket.accept();
            accepted.socket().setTcpNoDelay(true);
            accepted.configureBlocking(false);
            serverSocket.close();
            while (!Thread.interrupted()) {
                if (pong(buffy, accepted)) return;
            }
        } finally {
            if (accepted != null) {
                try {
                    accepted.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static boolean pong(ByteBuffer buffy, SocketChannel accepted) throws IOException {
        int read;
        buffy.clear();
        while ((read = accepted.read(buffy)) == 0) {
            Helper.yield();
        }

        if (read == -1)
            return true;
        buffy.flip();
        do {
            accepted.write(buffy);
        } while (buffy.hasRemaining());
        return false;
    }
}
