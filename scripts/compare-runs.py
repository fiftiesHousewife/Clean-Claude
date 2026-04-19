#!/usr/bin/env python3
"""Print headline-number differences between two reworkCompare archives.

Reads two raw-comparison markdowns (archived under docs/sessions/) and
prints the Cost and Findings tables side by side, one variant per row.
Copes with the archives having different column counts (useful when a
new variant like RECIPES_ONLY lands between runs).
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

METRICS = (
    ("cost (USD)",     "cost",         "{:>7.2f}", float),
    ("duration (s)",   "wall (s)",     "{:>7.0f}", float),
    ("turns",          "turns",        "{:>7d}",   int),
    ("cache creation", "cache creation", "{:>8d}", int),
    ("cache hit rate", "cache hit rate", "{:>7}",  str),
)
FINDING_ROWS = (("baseline", int), ("fixed", int),
                ("introduced", int), ("final", int))


def _variants(text: str):
    """Return the variant column headers (in order) from the first ## Cost table."""
    cost_index = text.find("## Cost")
    if cost_index < 0:
        return []
    header = re.search(r"\|\s*\|([^\n]+)", text[cost_index:])
    if not header:
        return []
    return [c.strip() for c in header.group(1).split("|") if c.strip()]


def _row(text: str, row_key: str, cast, width: int):
    pattern = rf"\|\s*{re.escape(row_key)}\s*\|([^\n]+)"
    match = re.search(pattern, text)
    if not match:
        return None
    cells = [c.strip() for c in match.group(1).split("|") if c.strip()]
    if not cells:
        return None
    cells = cells[:width]
    out = []
    for cell in cells:
        try:
            out.append(cast(cell))
        except ValueError:
            out.append(cell)
    return out


def _load(path: Path):
    text = path.read_text()
    variants = _variants(text)
    width = len(variants)
    cost = {key: _row(text, row, cast, width) for row, key, _, cast in METRICS}
    findings = {row: _row(text, row, cast, width) for row, cast in FINDING_ROWS}
    return {"cost": cost, "findings": findings, "variants": variants}


def _diff_row(label: str, fmt: str, prev_vals, now_vals,
              prev_variants, now_variants):
    if prev_vals is None or now_vals is None:
        return f"  {label:<16} (missing in one archive)"
    cells = []
    for variant in now_variants:
        n = now_vals[now_variants.index(variant)] if variant in now_variants else None
        p = prev_vals[prev_variants.index(variant)] if variant in prev_variants else None
        if p is None:
            cells.append(f"{'NEW':>7} → {fmt.format(n) if isinstance(n, (int, float)) else n}")
        elif isinstance(p, (int, float)) and isinstance(n, (int, float)):
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
    print(" " * 18 + "| " + " | ".join(f"{v:<16}" for v in now["variants"]))
    for row_key, cost_key, fmt, _cast in METRICS:
        print(_diff_row(row_key, fmt,
                        prev["cost"].get(cost_key), now["cost"].get(cost_key),
                        prev["variants"], now["variants"]))
    for row_key, _ in FINDING_ROWS:
        print(_diff_row(row_key, "{:>7d}",
                        prev["findings"].get(row_key), now["findings"].get(row_key),
                        prev["variants"], now["variants"]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
