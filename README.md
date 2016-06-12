# What is it?

Pigeon is a small library for packet transmission for Java. This module handles the server side of a TCP connection.

# How to use it?

You build a PigeonTcpServer like so:

```
PigeonTcpClient client = Pigeon.newClient()
    .withHost("localhost)
    .withPacketHandler(new PacketHandler() {
        @Override
        public void handle(Packet packet) {

        }
    })
    .build();
    
client.connect();
```

And at this point Pigeon will attempt a connection to localhost at the default port. Provided in the example are minimal required parameters for Pigeon to work - host and packet handler. The rest will be set to defaults.

Here are all the possible configuration options:

```
Pigeon.newClient()
    .withHost("localhost")                                          // Defaults to null which will cause an error
    .withPort(9191)                                                 // Defaults to 9191
    .withIncomingPacketHandleMode(IncomingPacketHandleMode.HANDLER) // Defaults to HANDLER - see below for more options
    .withPacketHandler(new PacketHandler() {                        // Required when incoming packet mode is HANDLER
        @Override
        public void handle(Packet packet) {

        }
    })
    .withClientCallbackHandler(clientCallbackHandler)               // Not required, no default
    .withPackageToScan("com.github.rskupnik")                       // Not required, defaults to null which will cause the entire classpath to be scanned
    .withPropertiesFilename("my-properties.properties")             // Points the properties file to load, by default searched for pigeon-tcp-client.properties
    .build();
```

Most of these can be provided in a properties file. By default, PigeonTcpClient will look for a pigeon-tcp-client.properties file
either on the classpath or in the project's root folder. You can point to another properties file, however, using the
`withProperties()` builder option - Pigeon will then look for that filename on the classpath or in the project's root folder.

Here's how a configuration file looks like:

```
host=localhost
port=9192
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

// Assume we have a properly initiated PigeonTcpClient under the variable 'client' here
client.send(packet);
```

### Receiving and handling packets

When receiving packets, Pigeon identifies them using the id value provided in the `@PigeonPacket` annotation, so it's very important
to not have overlapping ids.

Once a packet is received, there are two modes for handling it - these can be set using the configuration file or
the `.withIncomingPacketHandleMode()` builder method.

The default mode - HANDLER - will make a callback to the handler function you must provide when initializing the client:

```
Pigeon.newClient()
    .withHost("localhost)
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
PigeonTcpClient client = Pigeon.newClient()
    .withHost("localhost)
    .withIncomingPacketHandleMode(IncomingPacketHandleMode.QUEUE)
    .build();
    
client.connect();
    
// Do this periodically in a separate thread
List<Packet> packets = client.getIncomingPacketQueue().popAll();
for (Packet packet : packets) {
  if (packet.getId() == 1) {
    TestPacket testPacket = (TestPacket) packet;
    // Handle the packet...
  }
}
```

## Callbacks

You can provide Pigeon with a callback handler if you want to receive callbacks when specific things happen.

```
class TestClientCallbackHandler implements ClientCallbackHandler {

    @Override
    public void onConnected() {

    }
}

PigeonTcpClient client = Pigeon.newClient()
    .withHost("localhost)
    .withIncomingPacketHandleMode(IncomingPacketHandleMode.QUEUE)
    .withClientCallbackHandler(new TestClientCallbackHandler())
    .build();
```

Currently, PigeonTcpClient only provides callback for the `onConnected` event.

### Rationale

I've found myself often in need of a simple library for handling packet communication in Java. Available libraries
where usually too complex and rewriting the same custom code in each hobby project was tedious. I've created Pigeon to 
be able to implement packet communication quickly and be able to define new packets easily - with annotations and 
no wiring required.

Pigeon does not aim to be the most performant, the most configurable or the safest packet handling library out there. It rather aims
to be simple, easy to use and pick up. Therefore, it is mostly suitable for small hobby projects.

### Future

* Handle more data types than just integer and string
* Add support for SSL encryption?
* Perhaps other incoming packet handle modes?
* More callback functions
