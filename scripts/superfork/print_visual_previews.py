#!/usr/bin/env python3
"""Print small JPEG previews of captured screenshots into the CI log.

Review aid only: reviewers (and agents whose network cannot reach artifact storage) can decode
the `NETFLIX_PREVIEW <name> <base64>` lines. The full-resolution PNGs in the artifact remain the
evidence; nothing here judges a screenshot.
"""

from __future__ import annotations

import base64
import io
import sys
from pathlib import Path


def main() -> int:
    directory = Path(sys.argv[1])
    width = int(sys.argv[2]) if len(sys.argv) > 2 else 640
    try:
        from PIL import Image
    except ImportError:
        print("Pillow unavailable; skipping log previews.")
        return 0
    for path in sorted(directory.glob("*.png")):
        with Image.open(path) as image:
            image = image.convert("RGB")
            image.thumbnail((width, width))
            buffer = io.BytesIO()
            image.save(buffer, "JPEG", quality=70, optimize=True)
        print(f"NETFLIX_PREVIEW {path.name} {base64.b64encode(buffer.getvalue()).decode('ascii')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
