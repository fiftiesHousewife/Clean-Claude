#!/usr/bin/env python3
"""Print headline-number differences between two reworkCompare archives.

Reads two raw-comparison markdowns (archived under docs/sessions/) and
prints the Cost and Findings tables side by side, one variant per row.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

VARIANTS = ("vanilla", "mcp gradle only", "mcp + recipes", "harness + agent")
METRICS = (
    ("cost (USD)",     "cost",         "{:>7.2f}", float),
    ("duration (s)",   "wall (s)",     "{:>7.0f}", float),
    ("turns",          "turns",        "{:>7d}",   int),
    ("cache creation", "cache creation", "{:>8d}", int),
    ("cache hit rate", "cache hit rate", "{:>7}",  str),
)
FINDING_ROWS = (("baseline", int), ("fixed", int),
                ("introduced", int), ("final", int))


def _row(text: str, row_key: str, cast):
    pattern = rf"\|\s*{re.escape(row_key)}\s*\|([^\n]+)"
    match = re.search(pattern, text)
    if not match:
        return None
    cells = [c.strip() for c in match.group(1).split("|") if c.strip()]
    if len(cells) < 4:
        return None
    try:
        return [cast(c) for c in cells[:4]]
    except ValueError:
        return cells[:4]


def _load(path: Path):
    text = path.read_text()
    cost = {key: _row(text, row, cast) for row, key, _, cast in METRICS}
    findings = {row: _row(text, row, cast) for row, cast in FINDING_ROWS}
    return {"cost": cost, "findings": findings}


def _diff_row(label: str, fmt: str, prev, now):
    if prev is None or now is None:
        return f"  {label:<16} (missing in one archive)"
    cells = []
    for p, n in zip(prev, now):
        if isinstance(p, (int, float)) and isinstance(n, (int, float)):
            delta = n - p
            arrow = "↓" if delta < 0 else ("↑" if delta > 0 else "·")
            cells.append(f"{fmt.format(p)} → {fmt.format(n)} {arrow}")
        else:
            cells.append(f"{p} → {n}")
    return f"  {label:<16} | " + " | ".join(cells)


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: compare-runs.py <prev.md> <now.md>", file=sys.stderr)
        return 2
    prev = _load(Path(sys.argv[1]))
    now = _load(Path(sys.argv[2]))
    print(" " * 18 + "| " + " | ".join(f"{v:<16}" for v in VARIANTS))
    for row_key, _, fmt, _cast in METRICS:
        key = {"cost (USD)": "cost", "duration (s)": "wall (s)",
               "turns": "turns", "cache creation": "cache creation",
               "cache hit rate": "cache hit rate"}[row_key]
        print(_diff_row(row_key, fmt, prev["cost"].get(key), now["cost"].get(key)))
    for row_key, _ in FINDING_ROWS:
        print(_diff_row(row_key, "{:>7d}", prev["findings"].get(row_key),
                        now["findings"].get(row_key)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
