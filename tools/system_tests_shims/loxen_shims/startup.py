import os
import re
import subprocess

FONT_VARIABLE = "LOXEN_DRAWTEXT_FONT"
DRAWTEXT = re.compile(r"(^|[,;\[\]])drawtext=(?![^,;]*fontfile=)")


def with_font(argument, font):
    escaped = font.replace("\\", "/").replace(":", "\\:")
    return DRAWTEXT.sub(lambda match: f"{match.group(1)}drawtext=fontfile='{escaped}':", argument)


def ffmpeg_arguments(arguments, font):
    if not arguments or not os.path.basename(str(arguments[0])).lower().startswith(("ffmpeg", "ffplay")):
        return arguments
    return [arguments[0], *(with_font(argument, font) if isinstance(argument, str) else argument
                            for argument in arguments[1:])]


def install():
    font = os.environ.get(FONT_VARIABLE)
    if os.name != "nt" or not font:
        return
    original = subprocess.Popen.__init__

    def __init__(self, args, *rest, **options):
        if isinstance(args, (list, tuple)):
            args = ffmpeg_arguments(list(args), font)
        original(self, args, *rest, **options)

    subprocess.Popen.__init__ = __init__


install()
