"""Rebuild and install the selected cat launcher resources into the app."""
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[4]


def build():
    subprocess.run([sys.executable, str(ROOT / "doc/design-system/assets/launcher/package_launcher_variant.py"), "cat"], check=True)
    source = ROOT / "doc/design-system/assets/launcher/generated/cat/res"
    for asset in source.rglob("*"):
        if asset.is_file():
            target = ROOT / "app/src/main/res" / asset.relative_to(source)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(asset, target)


if __name__ == "__main__":
    build()
