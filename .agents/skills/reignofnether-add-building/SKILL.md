---
name: reignofnether-add-building
description: Как добавить здание в Reign of Nether — JSON-определение, NBT-структура, производство, исследования, аддоны, апгрейды. Использовать при создании нового строения.
---

# Как добавить здание (data-driven)

Здание — **данные**: NBT-структура + JSON-определение. Полный разбор — `_GUIDES/03_building.md`.
Реестр — `reignofnether:building` (датапак).

## Шаги

1. **Структура** — построй здание Structure Block-ом, экспортируй `.nbt`. Нужны **обе** копии:
   `data/<ns>/structures/<name>.nbt` (сервер) и `assets/<ns>/structures/<name>.nbt` (клиент).
2. **Файл** `data/<namespace>/building/<name>.json`, id = `<namespace>:<name>`; поле
   `structure` = `<ns>:<name>`.
3. **Базовые поля** — `name` (`{ "en_us": … }`), `icon`, `cost`, `maxHealth`, `populationSupply`,
   `isCapitol`, `requiredResearch`.
4. **Флаги** — `flags`: `canAcceptResources`, `buildTimeModifier`, `captureRange`, `capturable`,
   `invulnerable`, `repairable`, `repairTimeModifier`, `drawAggro`, `scaffoldFill`, `scaffoldBlock`,
   `portrait`, `foundationYLayers` (сколько нижних Y-слоёв — фундамент: их типы становятся
   `startingBlockTypes` и пре-ставятся при размещении; дефолт 1).
5. **Производство** — `production: [ "ns:unit", { "unit": "ns:unit", "costOverride": {…} } ]`.
6. **Исследования** — `researches: [ "ns:research", … ]` (общая с производством очередь,
   `reignofnether-add-research`).
7. **Аддоны** — `addons: [ { "type": "reignofnether:garrison", "params": {…} } ]`. Движковые типы:
   `night_source`, `range_indicator`, `garrison`, `nether_converting`, `resource_generator`.
8. **Апгрейды** — `upgrades: [ { "structure": …, "name": {…}, "icon": …, "cost": {…}, "maxHealth": …,
   "populationSupply": …, "production": […], "researches": […], "addons": […] } ]`. Каждый уровень —
   отдельный `JsonBuilding`-вариант; производство/исследования/аддоны/имя/иконка/структура per-level.
9. **Кнопка постройки** — приходит автоматически (`JsonBuilding.getBuildButton`); воркер видит все
   здания в меню постройки. Кастомно отфильтровать список — через `reignofnether:menu` с элементами
   `building` (`reignofnether-add-ability`).

## Грабли

* Кодового реестра `ReignOfNetherRegistries.BUILDING` для JSON-зданий нет (он пуст и заморожен):
  id — это `definitionId`, сохраняется как `jsonDefinitionId`.
* Пустой `startingBlockTypes` → здание считалось «уничтоженным» на 1-м тике; поэтому
  `foundationYLayers` важен, когда у структуры есть фундамент.
* Производство/исследования идентифицируются по `ProductionItem.getNetworkId()`, а не код-реестром.
* Кастомные строения (`building/custombuilding/**`) — отдельный инструмент, не смешивай.
* **Опечатки в полях ловятся явно:** имя каждого поля сверяется с record'ом `BuildingDefinition` —
  `test` (`ContentValidationTest`) валит сборку, а в игре ошибка пишется в лог при загрузке мира и на
  `/reload`: `[content-validation] <file> '<path>': unknown field 'x' (accepted: …)`. Свободные карты
  (`params` аддонов, локализованный `name`) принимают любые ключи; обе формы `production` — валидны.

## Гейт

`compileJava` → `validateMixins` → `runData` → `test`.
