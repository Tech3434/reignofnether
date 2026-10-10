---
name: reignofnether-add-hero
description: Как добавить героя в Reign of Nether — role hero, уровни, мана, ранги способностей. Использовать при создании геройского юнита.
---

# Как добавить героя

Герой — это обычный data-driven юнит с `role: hero` (см. `reignofnether-add-unit`). JAVA-класса нет.

## Шаги

1. **Определение юнита** — `data/<ns>/unit/<name>.json`, `"role": "hero"`, тело `base` — ванильный/
   модовый моб.
2. **Прокачка** — необязательный блок `hero`:
   `{ "maxLevel": 10, "expReqMultiplier": 1.6 }` (по умолчанию 10 и 1.6, как было у код-героев).
   Формула опыта — `(getHeroLevel() + 1) * 100 * expReqMultiplier`.
3. **Мана/атрибуты** — обычные атрибуты определения; мана-атрибут даёт `Unit.getMaxMana`.
4. **Способности** — `abilities`; для геройских способностей указывай `mana` и ранги (`heroLevel`/
   `rank`) в spec. `HeroAbility` — базовый тип для таких способностей.
5. **Спавн** — `role: hero` при спавне применяет характеристики уровня 1 и далее тикается `tickHero`.

## Грабли

* Старые `HeroUnitSave`/`HeroUnitSaveData` и предметный слой героя удалены — не тяни их обратно;
  состояние героя (опыт/мана/ранги) хранится в `UnitMobMixin` и сохраняется через
  `addUnitSaveData`/`readUnitSaveData`.
* Гейт `ALLOWED_HEROES` (геймрул `allowedHeroes`) ограничивает число героев.
* Кнопка повышения ранга рисуется HUD'ом через `HeroAbility.isRankUpMenuOpen`/`allSkillsLearnt`.

## Гейт

`compileJava` → `validateMixins` → `runData` → `test`.
