"""Rebuild and install the selected cat launcher resources into the app."""
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]


def build():
    subprocess.run([sys.executable, str(ROOT / "doc/design/package_launcher_variant.py"), "cat"], check=True)
    source = ROOT / "doc/design/launcher-variants/cat/res"
    for asset in source.rglob("*"):
        if asset.is_file():
            target = ROOT / "app/src/main/res" / asset.relative_to(source)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(asset, target)
    shutil.copyfile(ROOT / "doc/design/launcher-variants/cat/preview.png", ROOT / "doc/design/launcher-icon-preview.png")


if __name__ == "__main__":
    build()
