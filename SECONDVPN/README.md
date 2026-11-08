# Second VPN

## About
Establish VPN connections using the SSH2 protocol through HTTPS, an HTTP proxy, or an HTTPS proxy.

## Description

Basic application workflow:

1. The user enters a payload (which is essentially the structure of an arbitrary request), selects the connection method (HTTPS or HTTP), and provides the server that will handle the SSH connection.
2. The app formats the request and attempts to establish a connection through the specified proxy.
3. If successful, SSH authentication begins.
4. Once authenticated, the app starts a local SOCKS server and launches the Android VPNService to use it.
5. All device traffic is tunneled through the local SOCKS server, which is connected to the remote SSH server.

## Installation
To build the project, follow these steps:

1. Clone the repository.
2. Import the project into Android Studio.
