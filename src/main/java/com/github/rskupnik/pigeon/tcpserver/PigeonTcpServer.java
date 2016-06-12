/*
    Copyright 2016 Radosław Skupnik

    This file is part of pigeon-tcp-server.

    Pigeon-tcp-server is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Pigeon-tcp-server is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with pigeon-tcp-server; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package com.github.rskupnik.pigeon.tcpserver;

import com.github.rskupnik.pigeon.commons.*;
import com.github.rskupnik.pigeon.commons.annotations.AnnotationsScanner;
import com.github.rskupnik.pigeon.commons.callback.ServerCallbackHandler;
import com.github.rskupnik.pigeon.commons.exceptions.PigeonException;
import com.github.rskupnik.pigeon.commons.exceptions.PigeonServerException;
import com.github.rskupnik.pigeon.commons.glue.designpatterns.observer.Message;
import com.github.rskupnik.pigeon.commons.glue.designpatterns.observer.Observable;
import com.github.rskupnik.pigeon.commons.server.PigeonServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public final class PigeonTcpServer extends Thread implements PigeonServer {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(PigeonTcpServer.class);

    private final int port;
    private final int receiverThreadsNumber;
    private final String packageToScan;

    private final ServerSocket serverSocket;
    private final Map<UUID, Connection> connections = new HashMap<UUID, Connection>();
    private final ExecutorService executorService;
    private final IncomingPacketHandleMode incomingPacketHandleMode;
    private final IncomingPacketQueue incomingPacketQueue;
    private final PacketHandler packetHandler;
    private final ServerCallbackHandler serverCallbackHandler;

    private boolean exit;

    public PigeonTcpServer(PigeonTcpServerBuilder builder) throws PigeonServerException {
        this.port = builder.getPort();
        this.receiverThreadsNumber = builder.getReceiverThreadsNumber();
        this.incomingPacketHandleMode = builder.getIncomingPacketHandleMode();
        this.serverCallbackHandler = builder.getServerCallbackHandler();
        this.packageToScan = builder.getPackageToScan();

        if (incomingPacketHandleMode == IncomingPacketHandleMode.QUEUE) {
            packetHandler = null;
            incomingPacketQueue = new IncomingPacketQueue();
        } else {    // HANDLER mode is the default mode
            packetHandler = builder.getPacketHandler();
            incomingPacketQueue = null;
        }

        AnnotationsScanner.getInstance().scan(packageToScan);

        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new PigeonServerException(e.getMessage(), e);
        }

        if (receiverThreadsNumber <= 0) {
            executorService = Executors.newCachedThreadPool();
        } else {
            executorService = Executors.newFixedThreadPool(receiverThreadsNumber);
        }
    }

    @Override
    public void run() {
        if (serverCallbackHandler != null)
            serverCallbackHandler.onStarted();

        try {
            while (!exit) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                } catch (SocketTimeoutException e) {
                    if (exit)
                        break;
                }

                if (clientSocket == null)
                    continue;

                // Determine the number of free threads in case of THREADPOOLED mode and decline connection if there are no threads available
                if (fixedNumberOfThreads()) {
                    ThreadPoolExecutor tpc = (ThreadPoolExecutor) executorService;
                    if (tpc.getActiveCount() >= receiverThreadsNumber) {
                        clientSocket.getOutputStream().write(0);    // Indicate to the client that connection was refused
                        log.info("Connection from IP " + clientSocket.getInetAddress().getHostAddress() + " was declined due to not enough threads to handle it.");
                        continue;
                    }
                }

                UUID uuid = UUID.randomUUID();
                Connection connection = new Connection(uuid, clientSocket);
                if (connection.isOk()) {    // Connection is not considered ok when there is an IOException in the constructor
                    connection.attach(this);
                    connections.put(uuid, connection);
                    log.info(String.format("Accepted a new connection [%s] from IP: %s", uuid, clientSocket.getInetAddress().getHostAddress()));

                    executorService.execute(connection);

                    clientSocket.getOutputStream().write(1);    // Indicate to the client the connection was accepted

                    if (serverCallbackHandler != null)
                        serverCallbackHandler.onNewConnection(connection);
                }
            }
        } catch (IOException e) {
            if (!(e instanceof SocketException))
                log.error(e.getMessage(), e);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void update(Observable observable, Message message, Object payload) {
        switch (message) {
            case DISCONNECTED:
                UUID connectionUuid = (UUID) payload;
                Connection connection = connections.get(connectionUuid);
                if (connection != null) {
                    connections.remove(connectionUuid);
                    log.debug("Removed connection [" + connectionUuid + "] from [" + (connection.getHost() != null ? connection.getHost() : "unknown") + "]");
                    log.debug("Remaining connections: " + connections.entrySet().size());
                    connection.disconnect();
                }
                break;
            case RECEIVED_PACKET:
                Packet packet = (Packet) payload;
                switch (incomingPacketHandleMode) {
                    case QUEUE:
                        incomingPacketQueue.push(packet);
                        break;
                    default:
                    case HANDLER:
                        packetHandler.handle(packet);
                        break;
                }
                break;
        }
    }

    public void send(Packet packet, Connection connection) throws PigeonException {
        connection.send(packet);
    }

    public void send(Packet packet, List<Connection> connections) throws PigeonException {
        for (Connection connection : connections) {
            send(packet, connection);
        }
    }

    public void shutdown() {
        try {
            for (Connection connection : connections.values()) {
                connection.disconnect();
            }
            serverSocket.close();
            executorService.shutdown();
            exit = true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }



    public IncomingPacketQueue getIncomingPacketQueue() {
        return incomingPacketQueue;
    }

    public int getPort() {
        return port;
    }

    public int getReceiverThreadsNumber() {
        return receiverThreadsNumber;
    }

    public String getPackageToScan() {
        return packageToScan;
    }

    public IncomingPacketHandleMode getIncomingPacketHandleMode() {
        return incomingPacketHandleMode;
    }

    public PacketHandler getPacketHandler() {
        return packetHandler;
    }

    public ServerCallbackHandler getServerCallbackHandler() {
        return serverCallbackHandler;
    }

    private boolean fixedNumberOfThreads() {
        return receiverThreadsNumber > 0;
    }
}
