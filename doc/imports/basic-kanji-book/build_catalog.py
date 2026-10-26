"""Build the reviewed BKB import CSV from retained local source snapshots."""

import csv
import hashlib
import io
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
DOC = ROOT / "doc/imports/basic-kanji-book"
LOCAL = ROOT / "doc/dictionaries/local/basic-kanji-book"
OUTPUT = ROOT / "data/catalog/basic-kanji-book.csv"
FIELDS = [
    "Слово кандзи", "Чтение слова", "Изучаемый кандзи",
    "Номер кандзи в Basic Kanji Book", "Глава в Basic Kanji Book",
    "Чтение по кандзи", "Перевод на русский",
]

# Specific reviewed corrections, not general normalization rules.
CORRECTIONS = {
    ("遠い", "とい"): ("遠い", "とおい"),
    ("指す", "ゆす"): ("指す", "さす"),
    ("お手洗い", "おてらい"): ("お手洗い", "おてあらい"),
    ("小さ", "ちいさ"): ("小さい", "ちいさい"),
    ("大き", "おおきな"): ("大きな", "おおきな"),
    ("大切", "たいせつな"): ("大切", "たいせつ"),
    ("困難", "こんなんな"): ("困難", "こんなん"),
    ("便利", "べんりな"): ("便利", "べんり"),
    ("結婚 する", "けっこんする"): ("結婚する", "けっこんする"),
    ("２対３", "２たい３"): ("２対３", "にたいさん"),
}

# Additions explicitly identified by the publisher's revision lists.
# target number / written form / full reading / reading parts / editorial Russian.
ADDITIONS = """3\t土木\tどぼく\tど|ぼく\tгражданское строительство
6\t田\tた\tた\tрисовое поле — отдельное слово 田
9\t水車\tすいしゃ\tすい|しゃ\tводяное колесо, водяная мельница
12\t水車\tすいしゃ\tすい|しゃ\tводяное колесо, водяная мельница
98\t毎朝\tまいあさ\tまい|あさ\tкаждое утро
101\t毎晩\tまいばん\tまい|ばん\tкаждый вечер
130\t大雪\tおおゆき\tおお|ゆき\tсильный снегопад
136\t五度\tごど\tご|ど\tпять градусов
154\t山寺\tやまでら\tやま|でら\tгорный буддийский храм
161\t長兄\tちょうけい\tちょう|けい\tсамый старший брат
191\t禁止する\tきんしする\tきん|しする\tзапрещать
224\t県知事\tけんちじ\tけん|ち|じ\tгубернатор префектуры
228\t訓練する\tくんれんする\tくん|れんする\tтренировать, обучать практическим навыкам
234\t留学生\tりゅうがくせい\tりゅう|がく|せい\tиностранный студент
238\t回答する\tかいとうする\tかい|とうする\tотвечать на вопрос или запрос
239\t合宿\tがっしゅく\tがっ|しゅく\tсовместные учебные или тренировочные сборы с проживанием
240\t政治家\tせいじか\tせい|じ|か\tполитик
241\t治療する\tちりょうする\tち|りょうする\tлечить, проводить лечение
247\t化粧品\tけしょうひん\tけ|しょう|ひん\tкосметические средства
282\t予習する\tよしゅうする\tよ|しゅうする\tготовиться к предстоящему уроку
289\t真夏\tまなつ\tま|なつ\tразгар лета
293\t熱中する\tねっちゅうする\tねっ|ちゅうする\tувлекаться, полностью погружаться в занятие
294\t寒波\tかんぱ\tかん|ぱ\tволна холода
297\t温度\tおんど\tおん|ど\tтемпература
299\t雨天\tうてん\tう|てん\tдождливая погода
300\t仕上げる\tしあげる\tし|あげる\tзаканчивать работу, доводить до готовности
301\t物事\tものごと\tもの|ごと\tдела, вещи и явления
310\t授業\tじゅぎょう\tじゅ|ぎょう\tзанятие, урок
312\t良好\tりょうこう\tりょう|こう\tхороший, благоприятный — о состоянии
326\t面\tめん\tめん\tповерхность; сторона — чтение めん
332\t受信する\tじゅしんする\tじゅ|しんする\tпринимать сигнал или сообщение
335\t念入り\tねんいり\tねん|いり\tтщательный, основательный
337\t右折する\tうせつする\tう|せつする\tповорачивать направо
337\t左折する\tさせつする\tさ|せつする\tповорачивать налево
341\t水深\tすいしん\tすい|しん\tглубина воды
345\t決意する\tけついする\tけつ|いする\tтвёрдо решать, принимать решение
351\t外相\tがいしょう\tがい|しょう\tминистр иностранных дел
354\t連絡先\tれんらくさき\tれん|らく|さき\tконтактные данные
362\t飛行機\tひこうき\tひ|こう|き\tсамолёт
364\t郵便局\tゆうびんきょく\tゆう|びん|きょく\tпочтовое отделение
373\t市営バス\tしえいばす\tし|えいばす\tмуниципальный автобус
387\t辞退する\tじたいする\tじ|たいする\tотказываться от предложения или участия
388\t雑木林\tぞうきばやし\tぞう|き|ばやし\tсмешанная роща
397\t先々週\tせんせんしゅう\tせん|せん|しゅう\tпозапрошлая неделя
404\t泣き声\tなきごえ\tなき|ごえ\tплач, звук плача
415\t脱水\tだっすい\tだっ|すい\tобезвоживание; удаление воды
426\t弱気\tよわき\tよわ|き\tнеуверенность, робость
429\t航空書簡\tこうくうしょかん\tこう|くう|しょ|かん\tавиаписьмо на специальном бланке, аэрограмма
430\t単数形\tたんすうけい\tたん|すう|けい\tформа единственного числа
436\t設定する\tせっていする\tせっ|ていする\tзадавать настройки, устанавливать параметры
440\t手放す\tてばなす\tて|ばなす\tвыпускать из рук; расставаться с вещью
446\t平野\tへいや\tへい|や\tравнина
448\t風邪\tかぜ\t\tпростуда
451\t老化する\tろうかする\tろう|かする\tстареть, подвергаться старению
457\t民主主義\tみんしゅしゅぎ\tみん|しゅ|しゅ|ぎ\tдемократия
461\t大卒\tだいそつ\tだい|そつ\tокончание университета; выпускник — сокращённое слово
465\t必修科目\tひっしゅうかもく\tひっ|しゅう|か|もく\tобязательный учебный предмет
473\t半減する\tはんげんする\tはん|げんする\tуменьшаться вдвое
479\t以降\tいこう\tい|こう\tначиная с указанного времени, далее
485\t賛否\tさんぴ\tさん|ぴ\tсогласие и несогласие; доводы за и против
485\t賛成する\tさんせいする\tさん|せいする\tсоглашаться, поддерживать предложение
485\t賛同する\tさんどうする\tさん|どうする\tодобрять, присоединяться к мнению
490\t初雪\tはつゆき\tはつ|ゆき\tпервый снег
490\t初夏\tしょか\tしょ|か\tначало лета
496\t公的\tこうてき\tこう|てき\tофициальный, публичный
493\t無線\tむせん\tむ|せん\tрадиосвязь, беспроводная связь
499\t制限\tせいげん\tせい|げん\tограничение"""


def reading_parts(items):
    parts, prefix = [], ""
    for item in items:
        if "rt" in item:
            parts.append(prefix + item["rt"])
            prefix = ""
        elif parts:
            parts[-1] += item["ruby"]
        else:
            prefix += item["ruby"]
    return "|".join(parts) if not prefix else ""


def main():
    words = json.loads((LOCAL / "enriched.json").read_text())
    translations = json.loads((DOC / "editorial-translations.json").read_text())
    tr = {(x["note_id"], x["original_word"], x["original_reading"]): x for x in translations}
    extra_parts = {(x["word"], x["reading"]): x["parts"]
                   for x in json.loads((DOC / "reading-parts.json").read_text())}
    index = json.loads((LOCAL / "kanji-index.json").read_text())
    for item in index:
        if item["number"] == 360:
            item["kanji"] = "到"
    by_number = {x["number"]: x for x in index}
    by_symbol = {x["kanji"]: x for x in index}
    with (ROOT / "data/catalog/tobira-kanji.csv").open(encoding="utf-8-sig", newline="") as f:
        tobira = {(r["Слово кандзи"], r["Чтение слова"]): r for r in csv.DictReader(f, delimiter="|")}

    resolutions = {(x["word"], x["reading"]): x["translation"] for x in
                   json.loads((DOC / "translation-resolution.json").read_text())["translations"]}
    rows, provenance, changes, held = {}, {}, [], []

    def add(word, reading, russian, parts, target, origin):
        russian = resolutions.get((word, reading), russian)
        chapter, volume = target["chapter"], target["volume"]
        key = (word, reading, target["kanji"], target["number"], chapter)
        row = [word, reading, target["kanji"], str(target["number"]),
               f"Basic Kanji Book {'I' if volume == 1 else 'II'}, глава {chapter}", parts, russian]
        if parts and parts.replace("|", "") != reading:
            raise ValueError((word, reading, parts))
        if key not in rows:
            rows[key] = row
            provenance[key] = []
        provenance[key].append(origin)

    for item in words:
        original = (item["word"], item["reading"])
        translation = tr[(item["note_id"], *original)]
        word, reading = CORRECTIONS.get(original, original)
        russian = translation["translation"]
        parts = item["parts"]
        if original in CORRECTIONS:
            changes.append({"note_id": item["note_id"], "before": original, "after": [word, reading]})
            parts = ""
        if word == "大きな":
            russian = "большой — форма 大きな перед существительным"
        if (word, reading) in tobira:
            parts = tobira[(word, reading)]["Чтение по кандзи"]
        if not parts and "furigana" in item and original == (word, reading):
            candidate = reading_parts(item["furigana"])
            if candidate.replace("|", "") == reading:
                parts = candidate
        if not parts:
            parts = extra_parts.get((word, reading), "")
        if not parts:
            parts = {"指す": "さす", "小さい": "ちいさい", "大きな": "おおきな",
                     "遠い": "とおい", "大切": "たい|せつ", "困難": "こん|なん",
                     "便利": "べん|り", "お手洗い": "おて|あらい",
                     "２対３": "に|たい|さん"}.get(word, "")
        targets = [x for x in index if x["chapter"] in item["chapters"] and x["kanji"] in word]
        if not targets and word in {"位", "全く"}:
            targets = [by_symbol[word[0]]]
            changes.append({"note_id": item["note_id"], "word": word,
                            "chapter_before": item["chapters"], "chapter_after": targets[0]["chapter"],
                            "reason": "Chapter corrected to the chapter teaching the written kanji."})
        if not targets:
            held.append({"note_id": item["note_id"], "word": word, "reading": reading,
                         "translation": russian, "chapters": item["chapters"],
                         "reason": "No literal target kanji taught in the tagged chapter; do not invent a link."})
            continue
        for target in targets:
            add(word, reading, russian, parts, target,
                {"kind": "anki_chapter_and_literal_kanji", "note_id": item["note_id"],
                 "source_chapters": item["chapters"], "translation_origin": translation["origin"]})

    for line in ADDITIONS.splitlines():
        number, word, reading, parts, russian = line.split("\t")
        add(word, reading, russian, parts, by_number[int(number)], {"kind": "publisher_revision_list"})

    ordered = sorted(rows, key=lambda k: (k[3], k[0], k[1]))
    buffer = io.StringIO(newline="")
    writer = csv.writer(buffer, delimiter="|", lineterminator="\n")
    writer.writerow(FIELDS)
    writer.writerows(rows[key] for key in ordered)
    OUTPUT.write_bytes(buffer.getvalue().encode("utf-8-sig"))
    volumes = Counter(1 if by_symbol[k[2]]["chapter"] <= 22 else 2 for k in ordered)
    report = {
        "date": "2026-10-20", "csv": str(OUTPUT.relative_to(ROOT)),
        "sha256": hashlib.sha256(OUTPUT.read_bytes()).hexdigest(),
        "rows": len(rows), "unique_word_readings": len({k[:2] for k in rows}),
        "rows_by_volume": dict(volumes), "chapters": sorted({k[4] for k in rows}),
        "target_symbols": len({k[2] for k in rows}),
        "rows_without_reading_parts": sum(not rows[k][5] for k in rows),
        "changes": changes, "held_for_review": held,
        "association_method": "Anki lesson tags + literal occurrence of a kanji taught in that lesson; publisher additions have an explicit target. Not all associations are verified against book pages.",
        "special_target": "々, number 397: links to kanji.id 12293; its Kangxi radical is absent.",
    }
    (DOC / "validation.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    audit = [{"word": k[0], "reading": k[1], "target": k[2], "number": k[3],
              "chapter": k[4], "origins": provenance[k]} for k in ordered]
    (DOC / "provenance.json").write_text(json.dumps(audit, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({k: v for k, v in report.items() if k not in {"changes", "held_for_review", "association_method"}}, ensure_ascii=False, indent=2))
    print("Held:", held)


if __name__ == "__main__":
    main()
