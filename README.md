# PeerSon - P2P Online social network

## IN4391 Distributed Systems Group 3

### Authors:

- Klabér, Rahim
- Morstøl, Hallvard Molin
- Qiu, Jason
- Samardžić, Mariana

## About PeerSoN

PeerSoN is a distributed Online Social Network (OSN). It is using the all-peers decentralized system architecture. PeerSoN is doing this by combining peer-to-peer (P2P) infrastructure, encryption and a distributed hash table (DHT).

## Requirements

These are the versions of tools we used. We have not tested if the system works on earlier or later versions.

- Open JDK: 17.0.2
- Scala version: 2.13.8
- SBT: 1.6.2

## Running the project

### Running with Intellij

In IntelliJ, the `src/main/resources` folder needs to be marked as a resource folder. To do this, right-click
the folder and at the bottom there should be an option called "Mark directory as" and then "Resources root" should be selected.

### Running the project

Add your ip address in the ./src/main/resources/application.
conf file in the canonical.hostname.

(This can be found by running `curl ifconfig.me`)

Depending on your system build/compile:

- `sbt compile` or build from inside your IDE.

After building the project.
Run the main object in ./src/main/scala/main.scala.

- `sbt run` or run from inside your IDE.

You can then run some possible commands to send to the guardian, which works as the interface for the application:

- `login`
  - Guardian asks for mail and location to log user in. User gets any messages he received while offline.
- `logout`
  - Guardian logs user out by asking for mail and location.
- `send-message`
  - Guardian asks for sender, receiver and text to send and sends the message.
- `add-to-wall`
  - Guardian asks for sender, receiver and text to send and adds the text to the users wall.
- `request-wall`
  - Guardian asks for the user requesting, the file name and sends the newest version of the wall.
- `exit`
  - Exit the interface
