import re
import sys

INTERNET = re.compile(r"^-i(TCP|UDP)?(?::(\d+))?$", re.IGNORECASE)
HEADER = "COMMAND     PID USER   FD   TYPE DEVICE SIZE/OFF NODE NAME"


def parse(argv):
    pid = None
    protocol = None
    port = None
    arguments = iter(argv)
    for argument in arguments:
        if argument == "-p":
            pid = int(next(arguments))
        elif argument.startswith("-p") and argument[2:].isdigit():
            pid = int(argument[2:])
        elif INTERNET.match(argument):
            match = INTERNET.match(argument)
            protocol = (match.group(1) or "inet").lower()
            port = int(match.group(2)) if match.group(2) else None
        elif argument.startswith("-") and set(argument[1:]) <= set("nPa"):
            continue
        else:
            raise SystemExit(f"lsof: only -nP -a -p PID -i[TCP|UDP][:PORT] are supported, not '{argument}'")
    return pid, protocol, port


def connections(pid, protocol):
    import psutil

    process = psutil.Process(pid)
    method = getattr(process, "net_connections", None) or process.connections
    return process.name(), method(kind=protocol or "inet")


def main(argv=None):
    pid, protocol, port = parse(sys.argv[1:] if argv is None else argv)
    if pid is None:
        raise SystemExit("lsof: -p PID is required")
    try:
        name, found = connections(pid, protocol)
    except Exception:
        return 1
    rows = []
    for connection in found:
        if not connection.laddr or (port is not None and connection.laddr.port != port):
            continue
        kind = "UDP" if connection.type == 2 else "TCP"
        address = f"{connection.laddr.ip}:{connection.laddr.port}"
        if connection.raddr:
            address += f"->{connection.raddr.ip}:{connection.raddr.port}"
        if kind == "TCP" and connection.status:
            address += f" ({connection.status})"
        rows.append(f"{name[:9]:<9} {pid:>5} user {connection.fd:>4}u IPv4 0t0 {kind} {address}")
    if not rows:
        return 1
    print(HEADER)
    print("\n".join(rows))
    return 0
