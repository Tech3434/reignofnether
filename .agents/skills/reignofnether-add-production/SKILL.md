---
name: reignofnether-add-production
description: Как привязать обучение юнита к зданию в Reign of Nether — production в JSON-здании, costOverride, очередь. Использовать при добавлении юнита в производство.
---

# Как добавить производство

Производство перечисляется **на здании** (не на юните). Полный разбор — `_GUIDES/06_production.md`.

## Шаги (данные)

1. В `data/<ns>/building/<name>.json` добавь `production`:
   ```json
   "production": [ "ns:unit", { "unit": "ns:unit", "costOverride": { "food": 80, "seconds": 20 } } ]
   ```
   Элемент — либо строка-id юнита, либо объект `{ unit, costOverride? }` (`ProductionSpec`).
2. Стоимость и население берутся из **определения юнита** (`cost`/`population`), `costOverride`
   переопределяет для этого здания.
3. Юнит спавнится на маркер-блоке `production_spawn_block` (если есть; иначе рядом со зданием) и идёт
   к ралли-точке.
4. Очередь общая с исследованиями/апгрейдами; отмена — с возвратом.

## Когда нужен код

Если автору нужно производство, которое не выражается `ProductionSpec` (особая логика завершения),
пишется свой `ProductionItem`:

1. Класс `... extends ProductionItem`, `getNetworkId()` — стабильный строковый id (обязательно, если
   предмет не в код-реестре).
2. Регистрация — реестр `ReignOfNetherRegistries.PRODUCTION_ITEM` (`ProductionItems.init()`), если
   предмет должен быть в код-реестре; иначе достаточно строкового `getNetworkId`.
3. Привязка — к `productions` здания (код) или через JSON `production` (данные).

## Грабли

* Сетевой id производств — `ProductionItem.getNetworkId()`, а не код-реестр: JSON/исследования там
  не зарегистрированы.
* `ProductionItems.getProductionItem(EntityType)` — только для код-предметов; для JSON-зданий
  используется `JsonProductionItem` по id определения.
* Лимит армии: база + прирост от столицы (`populationSupply`).

## Гейт

`compileJava` → `validateMixins` → `runData` → `test`.
