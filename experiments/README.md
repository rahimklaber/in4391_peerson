Prerequisite:

- Docker

To start the application run in this folder the command:

`docker build -t peerson .`

`docker run -it peerson /bin/bash`

`current_ip=$(curl ifconfig.me)`

`sbt run current_ip`

Possible commands for the guardian:

- `login`
- `logout`
- `send-message`
- `add-to-wall`
- `request-wall`
- `exit`
