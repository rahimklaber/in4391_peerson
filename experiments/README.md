Prerequisite:

- Docker

To start the application run in this folder the command:

Find your ip with: `curl ifconfig.me`

The first node created will have the bootstrap ip with: `curl ifconfig.me`

`docker build -t peerson .\ --build-arg buildtime_bootstrap=your_ip --build-arg buildtime_bootstrap=bootstrap_ip`

`docker run -it peerson /bin/bash`

Check that the addresses are set correctly `echo host:$HOST bootstrap:$BOOTSTRAP`

`sbt run`

Possible commands for the guardian:

- `login`
- `logout`
- `send-message`
- `add-to-wall`
- `request-wall`
- `exit`
