import argparse
import array
import re
import sys
import wave
from dataclasses import dataclass

BITS_PER_FRAME = 80
SYNC_WORD = (0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1)
USER_BITS_OFFSETS = (4, 12, 20, 28, 36, 44, 52, 60)
HEADER = "#User bits  Timecode   |    Pos. (samples)"
TIMECODE = re.compile(r"^(\d{1,2}):(\d{1,2}):(\d{1,2})[:;.](\d{1,2})$")


@dataclass(frozen=True)
class Timecode:
    hours: int
    minutes: int
    seconds: int
    frame: int

    @staticmethod
    def from_index(index, fps):
        frame = index % fps
        total_seconds = index // fps
        return Timecode((total_seconds // 3600) % 24, (total_seconds // 60) % 60, total_seconds % 60, frame)

    def index(self, fps):
        return ((self.hours * 60 + self.minutes) * 60 + self.seconds) * fps + self.frame

    def __str__(self):
        return f"{self.hours:02d}:{self.minutes:02d}:{self.seconds:02d}:{self.frame:02d}"


def parse_timecode(text):
    match = TIMECODE.match(text.strip())
    if match is None:
        raise ValueError(f"'{text}' is not a timecode like 00:00:00:00")
    return Timecode(*(int(part) for part in match.groups()))


def frame_bits(timecode, user_bits=0):
    bits = [0] * BITS_PER_FRAME

    def put(offset, count, value):
        for bit in range(count):
            bits[offset + bit] = (value >> bit) & 1

    put(0, 4, timecode.frame % 10)
    put(8, 2, timecode.frame // 10)
    put(16, 4, timecode.seconds % 10)
    put(24, 3, timecode.seconds // 10)
    put(32, 4, timecode.minutes % 10)
    put(40, 3, timecode.minutes // 10)
    put(48, 4, timecode.hours % 10)
    put(56, 2, timecode.hours // 10)
    for nibble, offset in enumerate(USER_BITS_OFFSETS):
        put(offset, 4, (user_bits >> (4 * nibble)) & 0xF)
    bits[64:80] = SYNC_WORD
    if bits.count(0) % 2 == 1:
        bits[27] = 1
    return bits


def decode_frame_bits(bits):
    def get(offset, count):
        return sum(bits[offset + bit] << bit for bit in range(count))

    frame_units, frame_tens = get(0, 4), get(8, 2)
    seconds_units, seconds_tens = get(16, 4), get(24, 3)
    minutes_units, minutes_tens = get(32, 4), get(40, 3)
    hours_units, hours_tens = get(48, 4), get(56, 2)
    if max(frame_units, seconds_units, minutes_units, hours_units) > 9 or seconds_tens > 5 or minutes_tens > 5:
        return None
    user_bits = sum(get(offset, 4) << (4 * nibble) for nibble, offset in enumerate(USER_BITS_OFFSETS))
    timecode = Timecode(
        hours_tens * 10 + hours_units,
        minutes_tens * 10 + minutes_units,
        seconds_tens * 10 + seconds_units,
        frame_tens * 10 + frame_units,
    )
    if timecode.hours > 23:
        return None
    return timecode, user_bits


def encode(start, frames, fps, sample_rate, amplitude):
    half_bit = sample_rate / (fps * BITS_PER_FRAME * 2)
    levels = []
    level = amplitude
    for index in range(start.index(fps), start.index(fps) + frames):
        for bit in frame_bits(Timecode.from_index(index, fps)):
            level = -level
            levels.append(level)
            if bit:
                level = -level
            levels.append(level)
    samples = array.array("h")
    position = 0
    for number, value in enumerate(levels, start=1):
        end = round(number * half_bit)
        samples.extend([value] * (end - position))
        position = end
    return samples


def write_wav(path, samples, sample_rate):
    if sys.byteorder == "big":
        samples = array.array("h", samples)
        samples.byteswap()
    with wave.open(str(path), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(sample_rate)
        output.writeframes(samples.tobytes())


def read_wav(path):
    with wave.open(str(path), "rb") as source:
        channels = source.getnchannels()
        width = source.getsampwidth()
        sample_rate = source.getframerate()
        data = source.readframes(source.getnframes())
    if width == 2:
        samples = array.array("h")
        samples.frombytes(data)
        if sys.byteorder == "big":
            samples.byteswap()
    elif width == 1:
        samples = array.array("h", ((byte - 128) << 8 for byte in data))
    elif width in (3, 4):
        samples = array.array(
            "h",
            (int.from_bytes(data[offset + width - 2 : offset + width], "little", signed=True)
             for offset in range(0, len(data), width)),
        )
    else:
        raise ValueError(f"unsupported sample width {width}")
    if channels > 1:
        samples = samples[::channels]
    return samples, sample_rate


def zero_crossings(samples):
    if len(samples) == 0:
        return []
    peak = sorted(abs(sample) for sample in samples[:: max(1, len(samples) // 20000)])
    threshold = max(64, 0.15 * peak[int(0.99 * (len(peak) - 1))])
    crossings = []
    state = 0
    last_up = last_down = 0.0
    previous = samples[0]
    for index in range(1, len(samples)):
        sample = samples[index]
        if previous < 0 <= sample:
            last_up = index - 1 + (-previous) / (sample - previous)
        elif previous >= 0 > sample:
            last_down = index - 1 + previous / (previous - sample)
        if state <= 0 and sample > threshold:
            if state < 0:
                crossings.append(last_up)
            state = 1
        elif state >= 0 and sample < -threshold:
            if state > 0:
                crossings.append(last_down)
            state = -1
        previous = sample
    return crossings


class BitClock:
    def __init__(self, crossings, nominal):
        self.crossings = crossings
        self.nominal = nominal
        self.period = nominal
        self.index = 0
        self.hits = []

    def acquire(self, after):
        crossings = self.crossings
        while self.index + 1 < len(crossings):
            start, end = crossings[self.index], crossings[self.index + 1]
            self.index += 1
            if start >= after and 0.8 * self.nominal <= end - start <= 1.25 * self.nominal:
                self.period = self.nominal
                self.hits = []
                return start
        return None

    def next(self, boundary):
        predicted = boundary + self.period
        window = 0.35 * self.period
        crossings = self.crossings
        while self.index < len(crossings) and crossings[self.index] < predicted - window:
            self.index += 1
        best = None
        index = self.index
        while index < len(crossings) and crossings[index] <= predicted + window:
            if best is None or abs(crossings[index] - predicted) < abs(best - predicted):
                best = crossings[index]
            index += 1
        if best is None:
            self.hits.append(0)
            following = predicted
        else:
            self.hits.append(1)
            error = best - predicted
            following = predicted + 0.6 * error
            self.period = min(max(self.period + 0.02 * error, 0.8 * self.nominal), 1.25 * self.nominal)
        del self.hits[:-24]
        return following

    def locked(self):
        return len(self.hits) < 24 or sum(self.hits) >= 18


def decode(samples, sample_rate, fps):
    if len(samples) < 2:
        return []
    mean = sum(samples) / len(samples)
    nominal = sample_rate / (fps * BITS_PER_FRAME)
    clock = BitClock(zero_crossings(samples), nominal)
    frames = []

    def integral(start, end):
        first, last = max(0, round(start)), min(len(samples), round(end))
        if last <= first:
            return 0.0
        return sum(samples[first:last]) - mean * (last - first)

    def halves(start, end):
        margin = 0.1 * (end - start)
        middle = (start + end) / 2
        return integral(start + margin, middle - margin), integral(middle + margin, end - margin)

    boundary = clock.acquire(0.0)
    while boundary is not None:
        bits = []
        starts = []
        end = clock.next(boundary)
        first, second = halves(boundary, end)
        previous_second = None
        while end + clock.period <= len(samples) and clock.locked():
            following = clock.next(end)
            next_first, next_second = halves(end, following)
            first_level = first - previous_second if previous_second is not None else first
            second_level = second - next_first
            bits.append(1 if first_level * second_level < 0 else 0)
            starts.append(boundary)
            if len(bits) >= BITS_PER_FRAME and tuple(bits[-16:]) == SYNC_WORD:
                decoded = decode_frame_bits(bits[-BITS_PER_FRAME:])
                if decoded is not None:
                    frames.append((decoded[0], decoded[1], round(starts[-BITS_PER_FRAME]), round(end) - 1))
                del bits[:-16]
                del starts[:-16]
            elif len(bits) > 4 * BITS_PER_FRAME:
                del bits[:-BITS_PER_FRAME]
                del starts[:-BITS_PER_FRAME]
            previous_second = second
            boundary, end = end, following
            first, second = next_first, next_second
        boundary = clock.acquire(end)
    return frames


def dump_lines(frames, fps):
    lines = [HEADER]
    previous = None
    for timecode, user_bits, start, end in frames:
        index = timecode.index(fps)
        if previous is not None and index != (previous + 1) % (24 * 3600 * fps):
            lines.append("#DISCONTINUITY")
        previous = index
        lines.append(f"{user_bits:08x}   {timecode} | {start:8d} {end:8d}")
    return lines


def parse_duration(text, fps):
    if TIMECODE.match(text.strip()):
        return parse_timecode(text).index(fps)
    return round(float(text) / 1000 * fps)


def ltcgen_main(argv=None):
    parser = argparse.ArgumentParser(prog="ltcgen", description="Write SMPTE linear timecode audio to a WAV file.")
    parser.add_argument("-f", "--fps", type=int, default=25)
    parser.add_argument("-t", "--timecode", default="00:00:00:00")
    parser.add_argument("-l", "--duration", default="00:01:00:00",
                        help="HH:MM:SS:FF or milliseconds (default one minute)")
    parser.add_argument("-s", "--samplerate", type=int, default=48000)
    parser.add_argument("-g", "--volume", type=float, default=-18.0, help="level in dBFS")
    parser.add_argument("output")
    args = parser.parse_args(argv)
    amplitude = round(32767 * 10 ** (args.volume / 20))
    samples = encode(parse_timecode(args.timecode), parse_duration(args.duration, args.fps), args.fps,
                     args.samplerate, amplitude)
    write_wav(args.output, samples, args.samplerate)
    return 0


def ltcdump_main(argv=None):
    parser = argparse.ArgumentParser(prog="ltcdump", description="Print the SMPTE linear timecode in a WAV file.")
    parser.add_argument("-f", "--fps", type=int, default=25)
    parser.add_argument("input")
    args = parser.parse_args(argv)
    samples, sample_rate = read_wav(args.input)
    print("\n".join(dump_lines(decode(samples, sample_rate, args.fps), args.fps)))
    return 0
