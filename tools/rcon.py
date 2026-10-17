#!/usr/bin/env python3
"""Send commands to the dev server over RCON (run/server.properties: enable-rcon, rcon.password).

    python3 -I tools/rcon.py "summon sexmod:jenny 0 100 0" "list"
"""
import socket
import struct
import sys

HOST, PORT, PASSWORD = "127.0.0.1", 25575, "devtest"


def packet(req_id, kind, body):
    data = struct.pack("<ii", req_id, kind) + body.encode() + b"\x00\x00"
    return struct.pack("<i", len(data)) + data


def read(sock):
    size = struct.unpack("<i", sock.recv(4))[0]
    data = b""
    while len(data) < size:
        data += sock.recv(size - len(data))
    req_id, _ = struct.unpack("<ii", data[:8])
    return req_id, data[8:-2].decode(errors="replace")


with socket.create_connection((HOST, PORT), timeout=10) as s:
    s.sendall(packet(1, 3, PASSWORD))
    if read(s)[0] == -1:
        sys.exit("rcon auth failed")
    for i, cmd in enumerate(sys.argv[1:], start=2):
        s.sendall(packet(i, 2, cmd))
        print(f"> {cmd}\n{read(s)[1]}")
