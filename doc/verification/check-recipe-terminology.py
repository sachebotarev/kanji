#!/usr/bin/env python3
"""Check current domain/Room names; immutable historical evidence is not rewritten."""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
OLD_TYPES = (
    'TrainingRecipe', 'TrainingMaterial', 'TrainingName', 'LearningRecipesRepository',
    'RoomLearningRecipesRepository', 'TrainingOverview', 'DashboardTrainingViewModel',
    'TrainingBuilderRoute', 'TrainingBuilderActions', 'PracticeSession', 'SessionHistoryWidget',
)
errors = []
for module in ('app', 'core', 'domain', 'data', 'feature'):
    for path in (ROOT / module).rglob('*.kt'):
        if 'build' in path.parts:
            continue
        found = re.findall(r'\b(?:' + '|'.join(OLD_TYPES) + r')\b', path.read_text())
        if found:
            errors.append(f'{path.relative_to(ROOT)}: {found}')
schema = json.loads((ROOT / 'data/catalogdb/schemas/ru.mobui.kanji.data.catalog.CatalogDatabase/7.json').read_text())['database']
assert schema['version'] == 7
entities = {entity['tableName']: entity for entity in schema['entities']}
required = {'recipes', 'recipes_translations', 'recipe_materials'} | {
    f'recipe_materials_{suffix}' for suffix in ('chapters', 'kanji', 'words', 'tags')
} | {
    'trainings', 'trainings_recipe_translations', 'training_tasks',
    'training_task_translations', 'training_task_symbols',
}
assert required <= entities.keys()
for suffix in ('chapters', 'kanji', 'words', 'tags'):
    entity = entities[f'recipe_materials_{suffix}']
    assert any(fk['table'] == 'recipe_materials' and fk['columns'] == ['material_id'] for fk in entity['foreignKeys'])
for name in ('recipes_translations', 'recipe_materials'):
    assert any(fk['table'] == 'recipes' and fk['columns'] == ['recipe_id'] for fk in entities[name]['foreignKeys'])
for locale in ('values', 'values-en'):
    strings = {}
    for path in (ROOT / f'feature/dashboard/src/main/res/{locale}').glob('*.xml'):
        for item in ET.parse(path).getroot():
            strings[item.attrib.get('name')] = ''.join(item.itertext())
    assert ('Рецепты' if locale == 'values' else 'Recipes') == strings['library_title']
    assert ('Новая тренировка' if locale == 'values' else 'New training') == strings['training_new']
    assert not any('session' in key for key in strings if key)
if errors:
    raise SystemExit('\n'.join(errors))
print(f'OK: Recipe domain, Room v7/{len(entities)} tables, material FKs, trainings, RU/EN recipe/training terms')
