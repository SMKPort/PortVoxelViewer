#!/usr/bin/env python3
"""Nexus Aterna RP - Minecraft Java server-side agent (Python 3.10+, stdlib).
Run on the Minecraft server host. Uses local RCON and outbound HTTPS only.
It never accepts inbound network connections. Credentials via environment.
"""
import json
import os
import re
import socket
import struct
import sys
import time
import urllib.error
import urllib.request

URL = os.environ.get("NEXUS_RP_URL", "https://nexus-aterna-rp.onrender.com").rstrip("/")
KEY = os.environ.get("NEXUS_RP_AGENT_KEY", "").strip()
RCON_HOST = os.environ.get("RCON_HOST", "127.0.0.1")
RCON_PORT = int(os.environ.get("RCON_PORT", "25575"))
RCON_PASSWORD = os.environ.get("RCON_PASSWORD", "")
PLAYER = os.environ.get("NEXUS_RP_PLAYER", "").strip()
POLL = min(20.0, max(2.0, float(os.environ.get("NEXUS_RP_INTERVAL", "3"))))
health_last = None
effects_last = None

def request(method, route, body=None):
    raw = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf8")
    headers = {"X-Nexus-Agent": KEY, "User-Agent": "NexusAternaRPAagent/1.2"}
    if raw is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(URL + route, data=raw, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=10) as response:
        return json.loads(response.read(65536).decode("utf8"))

def packet(request_id, packet_type, body):
    payload = struct.pack("<ii", request_id, packet_type) + body.encode("utf8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload

def recv_packet(sock):
    length = recv_exact(sock, 4)
    total = struct.unpack("<i", length)[0]
    if total < 10 or total > 1048576:
        raise ValueError("Resposta RCON com comprimento inválido")
    data = recv_exact(sock, total)
    return struct.unpack("<ii", data[:8]), data[8:-2].decode("utf8", "replace")

def recv_exact(sock, n):
    buf = b""
    while len(buf) < n:
        part = sock.recv(n-len(buf))
        if not part:
            raise ConnectionError("RCON fechou a conexão")
        buf += part
    return buf

def rcon(command):
    if not RCON_PASSWORD:
        raise RuntimeError("Defina RCON_PASSWORD no ambiente do agente")
    with socket.create_connection((RCON_HOST, RCON_PORT), timeout=8) as sock:
        sock.settimeout(8)
        sock.sendall(packet(1101, 3, RCON_PASSWORD))
        authorized = False
        for _ in range(3):
            (rid, typ), _ = recv_packet(sock)
            if rid == -1:
                raise PermissionError("Senha RCON rejeitada")
            if rid == 1101 and typ == 2:
                authorized = True
                break
        if not authorized:
            raise RuntimeError("RCON não confirmou autenticação")
        sock.sendall(packet(1102, 2, command))
        for _ in range(8):
            (rid, typ), output = recv_packet(sock)
            if rid == 1102:
                return output[:1200]
        raise RuntimeError("RCON não respondeu ao comando")

def emit(kind, amount=0, health=None, note=""):
    if not PLAYER:
        return
    body = {
        "type": kind, "player": PLAYER, "amount": amount,
        "health": health, "note": note[:150]
    }
    request("POST", "/api/bridge/event", body)

def poll_health():
    global health_last, effects_last
    if not PLAYER:
        return
    # Player name is only used in the fixed, read-only /data command.
    out = rcon("data get entity " + PLAYER + " Health")
    m = re.search(r":\s*(-?\d+(?:\.\d+)?)f?\s*$", out, re.IGNORECASE)
    if m is not None:
        hp = max(0.0, min(40.0, float(m.group(1))))
        if health_last is not None:
            diff = hp - health_last
            if health_last > 0 and hp <= 0:
                emit("death", health=0, note="Jogador morreu")
            elif health_last <= 0 and hp > 0:
                emit("respawn", health=hp, note="Jogador renasceu")
            elif diff < -0.25:
                emit("damage", -diff, hp)
            elif diff > 0.25:
                emit("heal", diff, hp)
        else:
            emit("health", health=hp, note="Primeira leitura")
        health_last = hp
    try:
        raw = rcon("data get entity " + PLAYER + " active_effects")
        if "has the following entity data" in raw:
            marker = raw.split(":", 1)[-1].strip()
            if effects_last is not None and marker != effects_last:
                emit("effect", note=marker[:140])
            effects_last = marker
    except Exception:
        pass

def cycle():
    jobs = request("GET", "/api/bridge/jobs").get("jobs", [])
    for job in jobs:
        job_id, cmd = str(job.get("id", "")), str(job.get("command", ""))
        if not cmd.startswith(("list", "tellraw ", "function ")):
            response = {"jobId": job_id, "ok": False, "error": "Comando fora da lista permitida"}
        else:
            try:
                response = {"jobId": job_id, "ok": True, "output": rcon(cmd)}
            except Exception as e:
                response = {"jobId": job_id, "ok": False, "error": str(e)[:200]}
        request("POST", "/api/bridge/ack", response)
    poll_health()

def main():
    if not URL.startswith("https://"):
        sys.exit("NEXUS_RP_URL precisa utilizar HTTPS")
    if len(KEY) < 24:
        sys.exit("Defina NEXUS_RP_AGENT_KEY (minimo 24 caracteres)")
    if not PLAYER or not re.fullmatch(r"[A-Za-z0-9_]{3,16}", PLAYER):
        sys.exit("Defina NEXUS_RP_PLAYER com o nick valido do Minecraft")
    if RCON_HOST not in ("localhost", "127.0.0.1", "::1"):
        print("Aviso: RCON_HOST nao e loopback. Prefira agente no mesmo host do Minecraft.")
    print("NEXUS ATERNA RP AGENT - monitorando " + PLAYER + " a cada " + str(POLL) + "s")
    while True:
        try:
            cycle()
        except urllib.error.HTTPError as e:
            print("HTTPS: HTTP " + str(e.code))
        except Exception as e:
            print("Falha temporaria no agente: " + str(e)[:180])
        time.sleep(POLL)

if __name__ == "__main__":
    main()
