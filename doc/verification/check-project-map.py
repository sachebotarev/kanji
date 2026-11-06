#!/usr/bin/env python3
"""Check stable map IDs, registry coverage, parent IDs and local Markdown links.

This is structural validation, not proof that every visual state was reviewed.
"""
import json
import re
from collections import Counter
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[2]
MAP = ROOT / "doc/project-map.md"
text = MAP.read_text()
screen_pattern = r"(?:HOME|BOOKS|REFS|DICT|RECIPE|SHARED|SELECT|PRACTICE|SETTINGS|LEGACY)(?:\.(?:\d+|D\d+))*"
widget_pattern = screen_pattern + r"(?:\.W\d+)+"
headings = re.findall(r"^#{3,6} (" + screen_pattern + r"|UI-[A-Z0-9-]+) —", text, re.M)
retired_section = re.search(r"^## Выведенные ID\s*\n(.*?)(?=^## |\Z)", text, re.M | re.S)
retired_ids = set(
    re.findall(r"\*\*`([^`]+)`", retired_section.group(1) if retired_section else "")
)
# List markers under «Выведенные ID» are history, not live widgets.
list_ids = re.findall(r"^\s*- \*\*`([^`]+)` —", text, re.M)
widgets = [value for value in list_ids if value not in retired_ids]
ids = headings + widgets
errors = []
for value, count in Counter(ids).items():
    if count > 1:
        errors.append(f"Duplicate ID: {value}")
known = set(ids) | retired_ids
for widget in widgets:
    if not re.fullmatch(widget_pattern, widget):
        errors.append(f"Invalid widget ID: {widget}")
    parent = widget.rsplit(".", 1)[0]
    if parent not in known:
        errors.append(f"Unknown parent: {widget} -> {parent}")
for retired in sorted(retired_ids):
    if re.fullmatch(widget_pattern, retired):
        parent = retired.rsplit(".", 1)[0]
        if parent not in known and parent not in retired_ids:
            # Parent may itself be a retired screen/widget chain; allow either.
            if not re.fullmatch(screen_pattern, parent) and not re.fullmatch(widget_pattern, parent):
                errors.append(f"Unknown parent: {retired} -> {parent}")
registry = json.loads((ROOT / "doc/design-system/theme/screens.json").read_text())
allowed_status = {"approved", "unapproved", "retired"}
for entry in registry["screens"]:
    if entry.get("status") not in allowed_status:
        errors.append(f"Unknown screen status: {entry['id']} -> {entry.get('status')}")
expected_active = {entry["id"] for entry in registry["screens"] if entry.get("status") != "retired"}
expected_retired = {entry["id"] for entry in registry["screens"] if entry.get("status") == "retired"}
all_screens = {value for value in headings if re.fullmatch(screen_pattern, value)}
alias_rows = json.loads((ROOT / "doc/design-system/theme/screen-id-aliases.json").read_text())["aliases"]
extra_screens = {row["id"] for row in alias_rows if row["old_id"].startswith(("DLG-", "EXT-"))}
if len({row["old_id"] for row in alias_rows}) != len(alias_rows):
    errors.append("Duplicate old IDs in correspondence table")
for row in alias_rows:
    if row["id"] not in all_screens and row["id"] not in retired_ids:
        errors.append(f"Alias target missing from map: {row['old_id']} -> {row['id']}")
for entry in registry["screens"]:
    if {row["old_id"] for row in alias_rows if row["id"] == entry["id"]} != set(entry.get("legacy_ids", [])):
        errors.append(f"Alias table differs from registry: {entry['id']}")
screens = all_screens - extra_screens
for entry in registry["screens"]:
    parent = entry.get("parent_id")
    if parent and parent not in all_screens and parent not in retired_ids:
        errors.append(f"Unknown screen parent: {entry['id']} -> {parent}")
aliases = [alias for entry in registry["screens"] for alias in entry.get("legacy_ids", [])]
if len(aliases) != len(set(aliases)) or set(aliases) & (expected_active | expected_retired):
    errors.append("Duplicate or reused legacy screen aliases")
for value in sorted(expected_active - screens):
    errors.append(f"Screen missing from map: {value}")
for value in sorted(expected_retired - retired_ids):
    errors.append(f"Retired screen missing from map history: {value}")
for value in sorted(screens - expected_active):
    errors.append(
        f"Screen absent from design registry: {value}; register additional screens in the alias/extra-screen table"
    )
for component in re.findall(r"Общий: `(UI-[A-Z0-9-]+)`", text):
    if component not in known:
        errors.append(f"Unknown shared component: {component}")
for target in re.findall(r"\]\(([^)]+)\)", text):
    if target.startswith(("https://", "http://", "mailto:", "#")):
        continue
    target = unquote(target.split("#", 1)[0])
    if not (MAP.parent / target).exists():
        errors.append(f"Broken local link: {target}")
if errors:
    raise SystemExit("\n".join(errors))
print(
    f"OK: {len(screens)} registered screens, {len(expected_retired)} retired, "
    f"{len(widgets)} widgets, unique IDs and local links"
)
