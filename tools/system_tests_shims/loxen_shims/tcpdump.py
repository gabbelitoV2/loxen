import sys


def main():
    print("tcpdump: packet capture is not available on Windows, run the stability test without --network-capture",
          file=sys.stderr)
    return 1
