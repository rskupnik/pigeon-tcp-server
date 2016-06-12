# What is it?

Pigeon is a small library for packet transmission for Java. This module handles the server side of a TCP connection.

# How to use it?

You build a PigeonTcpServer like so:

```
PigeonTcpServer server = Pigeon.newServer()
    .withPacketHandler(new PacketHandler() {
        @Override
        public void handle(Packet packet) {

        }
    })
    .build();
    
server.start();
```

At this point Pigeon will establish a server and listen for incoming connections. Provided in the example are minimal required parameters for Pigeon to work - only the packet handler. The rest will be set to defaults.

Here are all the possible configuration options:

```
Pigeon.newServer()
    .withPort(9191)                                                 // Defaults to 9191
    .withIncomingPacketHandleMode(IncomingPacketHandleMode.HANDLER) // Defaults to HANDLER - see below for more options
    .withPacketHandler(new PacketHandler() {                        // Required when incoming packet mode is HANDLER
        @Override
        public void handle(Packet packet) {

        }
    })
    .withServerCallbackHandler(clientCallbackHandler)               // Not required, no default
    .withPackageToScan("com.github.rskupnik")                       // Not required, defaults to null which will cause the entire classpath to be scanned
    .withPropertiesFilename("my-properties.properties")             // Points the properties file to load, by default searched for pigeon-tcp-client.properties
    .withReceiverThreadsNumber(0)                                   // Number of threads that will handle new connections, 0 = infinite, defaults to 0
    .build();
```

Most of these can be provided in a properties file. By default, PigeonTcpServer will look for a pigeon-tcp-server.properties file
either on the classpath or in the project's root folder. You can point to another properties file, however, using the
`withProperties()` builder option - Pigeon will then look for that filename on the classpath or in the project's root folder.

Here's how a configuration file looks like:

```
port=9192
receiver_threads_number=1
package_to_scan=com.github.rskupnik.pigeon.tcpserver
packet_handle_mode=handler
```

The precedence is as follows:

`manual_input -> configuration_file -> defaults`

Which means that Pigeon will first look for configuration values provided manually (by using the `.withXXX()` builder methods) and, if not found, fall back to configuration file and then to default values in case the file is not present as well.

## Handling packets

Pigeon makes use of annotations to describe packets for sending and receiving.
A valid packet class must conform to these rules:
* Be public
* Extend the `Packet` class
* Be annotated with `@PigeonPacket` (with id provided)
* Have proper getter and setter methods for any variables that should be a part of the packet (annotated with `@PacketDataField`)

```
@PigeonPacket(id = 1)
public class TestPacket extends Packet {

    @PacketDataField
    private int testData;

    public int getTestData() {
        return testData;
    }

    public void setTestData(int testData) {
        this.testData = testData;
    }
}
```

These packet classes should reside in a single package and Pigeon should be told which package to scan for them (using either
the configuration file or the `withPackageToScan()` builder method). Pigeon will still work if not provided with the package name
to scan but will initialize much, much slower because it will have to scan the entire classpath.

### Sending packets

Once the packet has been created and found by Pigeon, it can be easily sent using the `.send()` method on the client.

```
TestPacket packet = new TestPacket();
packet.setTestData(8);

// Assume we have a properly initiated PigeonTcpServer under the variable 'server' here and a proper connection under 'connection'
server.send(packet, connection);    // Will send a packet to a single connection
server.send(packet, Arrays.asList(connection)); // Will send a packet to a list of connections
```

### Receiving and handling packets

When receiving packets, Pigeon identifies them using the id value provided in the `@PigeonPacket` annotation, so it's very important
to not have overlapping ids.

Once a packet is received, there are two modes for handling it - these can be set using the configuration file or
the `.withIncomingPacketHandleMode()` builder method.

The default mode - HANDLER - will make a callback to the handler function you must provide when initializing the client:

```
Pigeon.newServer()
    .withPacketHandler(new PacketHandler() {
        @Override
        public void handle(Packet packet) {
          if (packet.getId() == 1) {
            TestPacket testPacket = (TestPacket) packet;
            // Handle the packet...
          }
        }
    })
    .build();
```

The other mode - QUEUE - will put the packet into a queue once it is received. At this point Pigeon stops caring about it,
it's your task to poll the queue periodically and handle the packets.

```
PigeonTcpServer server = Pigeon.newServer()
    .withIncomingPacketHandleMode(IncomingPacketHandleMode.QUEUE)
    .build();
    
server.start();
    
// Do this periodically in a separate thread
List<Packet> packets = server.getIncomingPacketQueue().popAll();
for (Packet packet : packets) {
  if (packet.getId() == 1) {
    TestPacket testPacket = (TestPacket) packet;
    // Handle the packet...
  }
}
```

### Connection handling threads

The `receiverThreadsNumber` configuration property controls how many threads will be used to handle the connections.
If set to 0 (default), there will be no limit - each new connection will receive a separate thread to handle it.
If set to a value > 0, a thread pool will be established with limited capacity and only as many connections will
be allowed as many threads are available to handle them. Therefore, if you set this value to 1, Pigeon will only
handle a single connection and reject any further connections until the thread is free.

## Callbacks

You can provide Pigeon with a callback handler if you want to receive callbacks when specific things happen.

```
class TestServerCallbackHandler implements ServerCallbackHandler {

    @Override
    public void onStarted() {

    }

    @Override
    public void onNewConnection(Connection connection) {
        connections.add(connection);    // You might want to keep track of your connections
    }
}

PigeonTcpServer server = Pigeon.newServer()
    .withIncomingPacketHandleMode(IncomingPacketHandleMode.QUEUE)
    .withClientCallbackHandler(new TestServerCallbackHandler())
    .build();
```

Currently, PigeonTcpServer only provides callback for the `onStarted` and `onNewConnection` events.

# Rationale

I've found myself often in need of a simple library for handling packet communication in Java. Available libraries
where usually too complex and rewriting the same custom code in each hobby project was tedious. I've created Pigeon to 
be able to implement packet communication quickly and be able to define new packets easily - with annotations and 
no wiring required.

Pigeon does not aim to be the most performant, the most configurable or the safest packet handling library out there. It rather aims
to be simple, easy to use and pick up. Therefore, it is mostly suitable for small hobby projects.

# Future

* Handle more data types than just integer and string
* Add support for SSL encryption?
* Perhaps other incoming packet handle modes?
* More callback functions
