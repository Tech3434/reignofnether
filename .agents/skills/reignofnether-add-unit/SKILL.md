---
name: reignofnether-add-unit
description: Как добавить юнита в Reign of Nether — JSON-определение, роль, атрибуты, способности, производство. Использовать при создании нового юнита.
---

# Как добавить юнита (data-driven)

Юнит — это **данные**, а не Java-класс: ванильный/модовый моб (`base`) + JSON-определение.
Полный разбор — `_GUIDES/02_unit.md`. Реестр — `reignofnether:unit` (датапак).

## Шаги

1. **Файл** `data/<namespace>/unit/<name>.json`, id = `<namespace>:<name>`.
2. **Тело** — `base`: id `EntityType` (напр. `minecraft:skeleton`). Моб рисует себя сам, своя
   модель/рендер не нужны.
3. **Роль** — `role`: `melee` | `ranged` | `worker` | `flying` | `hero`. Из неё
   `UnitMobMixin.initialiseGoals` строит цели (melee/ranged/worker/garrison по флагу).
4. **Флаги** — `flags`: `canGather`/`canBuild`/`canGarrison`/`holdPosition`/`canPickupEquipment`.
5. **Атрибуты** — карта `attribute-id → значение`; `scale` — ванильный `SCALE`. Модовые атрибуты
   `reignofnether:*` (`attack_damage`, `attacks_per_second`, `attack_range`, `sight_range`,
   `aggro_range`, `*_damage_resist`).
6. **Ranged** — `equipment` (предмет в руку) + `projectile` (`entity`/`velocity`/`damage`/`inaccuracy`).
7. **Способности** — `abilities: [ { "type": …, "cooldown": …, "params": {…} } ]`. Движковые типы:
   `heal`, `regeneration`, `summon`, `menu` (меню с `submenu`, см. `reignofnether-add-ability`).
8. **Экономика** — `cost` (`food`/`wood`/`ore`/`emerald`/`seconds`), `population`,
   `requiredResearch`.
9. **Воркер** — `worker: { "gatherable": ["food","wood","ore"], "buildSpeed": 1.0, "carryCapacity": 100 }`
   (только для `role: worker`).
10. **Герой** — `hero: { "maxLevel": 10, "expReqMultiplier": 1.6 }` (только для `role: hero`).
11. **Наследование** — `inherits: "<id>"`: незаданные поля берутся у родителя (атрибуты мержатся).
12. **Производство** — перечисляется **на здании** (`production`), а не на юните
    (`reignofnether-add-production`).

## Грабли

* Идентичность юнита — **определение**, а не `EntityType`: два юнита могут делить тело.
* Всё, что раньше брало имя/иконку у тела, берёт их из определения (см. `MiscUtil`).
* `instanceof Unit` заменён на `isRtsUnit()`/роль-флаги; не возвращай под-интерфейсы.
* Новая форма поведения (не покрытая ролью/типом способности) — это код-тип способности
  (`reignofnether-add-ability`), а не класс юнита.
* **Опечатки в полях ловятся явно:** имя каждого поля сверяется с record'ом `UnitDefinition` —
  `test` (`ContentValidationTest`) валит сборку, а в игре ошибка пишется в лог при загрузке мира и на
  `/reload`: `[content-validation] <file> '<path>': unknown field 'x' (accepted: …)`. Свободные карты
  (`attributes`, локализованный `name`) принимают любые ключи, `params` способностей — тоже.

## Гейт

`compileJava` → `validateMixins` → `runData` → `test` (декодирует все поставляемые JSON).
