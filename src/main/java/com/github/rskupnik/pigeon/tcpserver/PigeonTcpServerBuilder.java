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

import com.github.rskupnik.parrot.Parrot;
import com.github.rskupnik.pigeon.commons.IncomingPacketHandleMode;
import com.github.rskupnik.pigeon.commons.PacketHandler;
import com.github.rskupnik.pigeon.commons.callback.ServerCallbackHandler;
import com.github.rskupnik.pigeon.commons.exceptions.PigeonException;
import com.github.rskupnik.pigeon.commons.server.PigeonServerBuilder;

public final class PigeonTcpServerBuilder implements PigeonServerBuilder {

    private final String PROPERTY_PORT = "port";
    private final String PROPERTY_RECEIVER_THREADS_NUMBER = "receiver_threads_number";
    private final String PROPERTY_PACKAGE_TO_SCAN = "package_to_scan";
    private final String PROPERTY_PACKET_HANDLE_MODE = "packet_handle_mode";

    private String propertiesFilename = TcpServerDefaults.PROPERTIES_FILENAME;

    private Integer port;
    private Integer receiverThreadsNumber;
    private IncomingPacketHandleMode incomingPacketHandleMode;
    private PacketHandler packetHandler;
    private ServerCallbackHandler serverCallbackHandler;
    private String packageToScan;

    public PigeonTcpServerBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    public PigeonTcpServerBuilder withReceiverThreadsNumber(int receiverThreadsNumber) {
        this.receiverThreadsNumber = receiverThreadsNumber;
        return this;
    }

    public PigeonTcpServerBuilder withIncomingPacketHandleMode(IncomingPacketHandleMode incomingPacketHandleMode) {
        this.incomingPacketHandleMode = incomingPacketHandleMode;
        return this;
    }

    public PigeonTcpServerBuilder withPacketHandler(PacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        return this;
    }

    public PigeonTcpServerBuilder withServerCallbackHandler(ServerCallbackHandler serverCallbackHandler) {
        this.serverCallbackHandler = serverCallbackHandler;
        return this;
    }

    public PigeonTcpServerBuilder withPackageToScan(String packageToScan) {
        this.packageToScan = packageToScan;
        return this;
    }

    public PigeonTcpServerBuilder withPropertiesFilename(String propertiesFilename) {
        this.propertiesFilename = propertiesFilename;
        return this;
    }

    public int getPort() {
        return port;
    }

    public int getReceiverThreadsNumber() {
        return receiverThreadsNumber;
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

    public String getPackageToScan() {
        return packageToScan;
    }

    public PigeonTcpServer build() throws PigeonException {
        load();
        validate();
        return new PigeonTcpServer(this);
    }

    private void validate() throws PigeonException {
        if (port == null)
            throw new PigeonException("Port cannot be null");

        if (receiverThreadsNumber == null)
            throw new PigeonException("Receiver threads number cannot be null");

        if (incomingPacketHandleMode == null)
            throw new PigeonException("Incoming packet handle mode cannot be null");

        if (incomingPacketHandleMode == IncomingPacketHandleMode.HANDLER && packetHandler == null)
            throw new PigeonException("Incoming packet handle mode is set to HANDLER but no handler was specified");
    }

    private void load() throws PigeonException {
        Parrot parrot = new Parrot(propertiesFilename);

        try {
            if (port == null)
                port = parrot.get(PROPERTY_PORT).isPresent() ? Integer.parseInt(parrot.get(PROPERTY_PORT).get()) : TcpServerDefaults.PORT;

            if (receiverThreadsNumber == null)
                receiverThreadsNumber = parrot.get(PROPERTY_RECEIVER_THREADS_NUMBER).isPresent() ? Integer.parseInt(parrot.get(PROPERTY_RECEIVER_THREADS_NUMBER).get()) : TcpServerDefaults.RECEIVER_THREADS_NUMBER;

            if (packageToScan == null)
                packageToScan = parrot.get(PROPERTY_PACKAGE_TO_SCAN).orElse(TcpServerDefaults.PACKAGE_TO_SCAN);

            if (incomingPacketHandleMode == null)
                incomingPacketHandleMode = parrot.get(PROPERTY_PACKET_HANDLE_MODE).isPresent() ? IncomingPacketHandleMode.fromString(parrot.get(PROPERTY_PACKET_HANDLE_MODE).get()) : TcpServerDefaults.PACKET_HANDLE_MODE;

        } catch (ClassCastException e) {
            throw new PigeonException(e.getMessage());
        }
    }
}
