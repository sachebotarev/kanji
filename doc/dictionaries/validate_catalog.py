"""Validate the packaged catalog; optional full comparison with its source snapshot."""
import argparse
import csv
import gzip
import json
from pathlib import Path
import unicodedata
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
HEADER = [
    "id", "Кандзи", "Названия на русском", "Верхнее чтение", "Нижнее чтение",
    "Количество черт", "Ключ", "Входит в Кёику кандзи", "Входит в Дзёё кандзи",
    "Входит в Синмэйё кандзи",
]


def read_csv(path):
    raw = path.read_bytes()
    assert not raw.startswith(b"\xef\xbb\xbf"), f"Unexpected BOM: {path}"
    assert b"\r" not in raw, f"Expected LF: {path}"
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.reader(stream))


def validate(source=None, apk=None):
    catalog = ROOT / "data/catalog"
    radicals = read_csv(catalog / "radicals.csv")
    assert radicals[0] == ["Номер", "Ключ", "Название", "Количество черт"]
    assert len(radicals) == 215
    assert all(len(row) == 4 for row in radicals)
    keys = [r[1] for r in radicals[1:]]
    for number, key, name, strokes in radicals[1:]:
        assert 1 <= int(number) <= 214
        assert key == unicodedata.normalize("NFKC", chr(0x2F00 + int(number) - 1))
        assert name.strip() and strokes.isdigit() and 1 <= int(strokes) <= 17
    assert {int(r[0]) for r in radicals[1:]} == set(range(1, 215))
    assert keys == [unicodedata.normalize("NFKC", chr(0x2F00 + i)) for i in range(214)]
    records = read_csv(catalog / "kanji.csv")
    assert records[0] == HEADER
    encoded_rows = records[1:]
    assert all(len(row) == 10 for row in encoded_rows)
    assert len({row[0] for row in encoded_rows}) == len(encoded_rows)
    for row in encoded_rows:
        assert len(row[1]) == 1
        assert row[0] == str(ord(row[1]))
        assert 0 <= int(row[0]) <= 0x10FFFF and not 0xD800 <= int(row[0]) <= 0xDFFF
    rows = [row[1:] for row in encoded_rows]
    assert len(rows) == 3001
    assert all(len(row) == 9 for row in rows)
    assert len({r[0] for r in rows}) == len(rows)
    by_character = {r[0]: r for r in rows}
    assert by_character["々"] == ["々", "знак повтора", "", "", "3", "", "false", "false", "false"]
    for row in rows:
        assert len(row[0]) == 1, row
        assert row[1].strip(), row
        if row[0] == "々":
            continue  # Iteration mark: no on/kun readings, radical or list membership.
        assert row[2] or row[3], row
        assert row[4].isdigit() and 0 < int(row[4]) < 100, row
        assert row[5] in {r[0] for r in radicals[1:]}, row
        assert all(v in {"true", "false"} for v in row[6:]), row
        assert row[6] != "true" or row[7] == "true", row
        assert (row[7] == "true") != (row[8] == "true"), row
        for value in row[1:4]:
            if value:
                parts = value.split("|")
                assert all(part and part.strip() == part for part in parts), row
                assert len(parts) == len(set(parts)), row
    assert [sum(r[i] == "true" for r in rows) for i in (6, 7, 8)] == [1026, 2136, 864]
    assert by_character["勒"][8] == "true"
    assert by_character["𠮟"][7] == "true"
    assert "神" in by_character and "神" in by_character
    assert by_character["低"][1] == "низкий"
    assert by_character["謁"][1] == "аудиенция"

    if source:
        raw = source.read_bytes()
        root = ET.fromstring(gzip.decompress(raw) if raw[:2] == b"\x1f\x8b" else raw)
        chars = {c.findtext("literal"): c for c in root.findall("character")}
        expected = {ch for ch, c in chars.items() if c.findtext("misc/grade") in
                    {"1", "2", "3", "4", "5", "6", "8", "9", "10"}} | {"勒"}
        assert set(by_character) == expected | {"々"}, "Character-set mismatch"
        fallbacks = {"歷": "歴", "欄": "欄", "廊": "廊", "類": "類"}
        for ch, row in by_character.items():
            if ch == "々":
                continue  # User-added symbol outside the KANJIDIC2 selection.
            c = chars[ch]
            grade = int(c.findtext("misc/grade") or (9 if ch == "勒" else 0))
            assert row[6:] == [str(grade <= 6).lower(), str(grade <= 8).lower(), str(grade >= 9).lower()], ch
            assert row[4] == c.findtext("misc/stroke_count"), ch
            radical_number = int(c.findtext('radical/rad_value[@rad_type="classical"]'))
            assert row[5] == str(radical_number), ch
            reading_source = chars[fallbacks.get(ch, ch)]
            for index, kind in ((2, "ja_on"), (3, "ja_kun")):
                values = [r.text for r in reading_source.findall(f'.//reading[@r_type="{kind}"]')]
                assert row[index] == "|".join(values), (ch, kind)
    report = json.loads((ROOT / "doc/dictionaries/kanji-verification.json").read_text())
    assert {r["kanji"] for r in report["records"]} == set(by_character) - {"々"}
    assert report["summary"]["total"] == 3000
    for record in report["records"]:
        assert record["status"] and record["yarxi_url"]
    if apk:
        with zipfile.ZipFile(apk) as archive:
            for name in ("radicals.csv", "kanji.csv", "NOTICE.txt"):
                assert archive.read("assets/" + name) == (catalog / name).read_bytes(), name
    print("OK: 214 radicals; 3000 kanji + iteration mark 々; 1026/2136/864; schema, readings and radical references validated"
          + ("; source snapshot matched" if source else "")
          + ("; APK bytes matched" if apk else ""))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--kanjidic", type=Path)
    parser.add_argument("--apk", type=Path)
    args = parser.parse_args()
    validate(args.kanjidic, args.apk)
