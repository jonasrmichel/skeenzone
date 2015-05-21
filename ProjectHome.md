### What is SkeenZone? ###
**SkeenZone** is a lightweight and extensible Java middleware that enables development of distributed mobile applications.

At it's core, SkeenZone implements Skeen's algorithm for total ordering of multicast messages. The SkeenZone middleware enables an application to establish network connections with other devices in a peer-to-peer (p2p) fashion and to send/receive typed messages. Connections may be grouped into independent and potentially overlapping _Sessions_. All messages pertaining to a _Session_ are guaranteed to be totally ordered between the devices connected in that _Session_.

### What does SkeenZone do? ###
Java applications, including Android applications, may use the SkeenZone middleware to:
  * Establish and maintain groups of p2p TCP connections (called _Sessions_) with other devices
  * Send and receive typed messages of arbitrary content
  * Guarantee a total ordering of multicast messages within groups (_Sessions_) of connected devices

### How do I use SkeenZone within my application? ###
Coming soon...

### SkeenZone in action! ###
We implemented the SkeenZone middleware in a distributed Android chat application called **ChatHoc**. The ChatHoc application (apk) and source are available for download.