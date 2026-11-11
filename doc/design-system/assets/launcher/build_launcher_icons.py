"""Rebuild and install the selected cat launcher resources into the app."""
from pathlib import Path
import shutil
import subprocess
import sys
import argparse

ROOT = Path(__file__).resolve().parents[4]


def build():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--variant", choices=("cat", "jade", "dark"), default="dark")
    variant = parser.parse_args().variant
    subprocess.run([sys.executable, str(ROOT / "doc/design-system/assets/launcher/package_launcher_variant.py"), variant], check=True)
    source = ROOT / f"doc/design-system/assets/launcher/generated/{variant}/res"
    for asset in source.rglob("*"):
        if asset.is_file():
            target = ROOT / "app/src/main/res" / asset.relative_to(source)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(asset, target)


if __name__ == "__main__":
    build()
