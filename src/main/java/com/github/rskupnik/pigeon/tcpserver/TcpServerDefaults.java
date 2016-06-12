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

import com.github.rskupnik.pigeon.commons.IncomingPacketHandleMode;

public interface TcpServerDefaults {
    String PROPERTIES_FILENAME = "pigeon-tcp-server.properties";
    int PORT = 9191;
    int RECEIVER_THREADS_NUMBER = 0;    // This equals to inifinite
    String PACKAGE_TO_SCAN = null;
    IncomingPacketHandleMode PACKET_HANDLE_MODE = IncomingPacketHandleMode.HANDLER;
}
