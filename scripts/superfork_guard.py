#!/usr/bin/env python3
"""Compatibility entrypoint. Canonical validator lives in scripts/superfork/validate_project_state.py."""
from pathlib import Path
import runpy

target = Path(__file__).resolve().parent / "superfork" / "validate_project_state.py"
runpy.run_path(str(target), run_name="__main__")
