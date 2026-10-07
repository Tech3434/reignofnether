---
name: reignofnether-add-production
description: Как добавить элемент производства (обучение юнита) в Reign of Nether — ProductionItem, стоимость, очередь, getEntityType. Использовать при привязке юнита к зданию.
---

# Как добавить элемент производства

Полный разбор — `_GUIDES/06_production.md`. Каркас — `building/production/**` (не удалялся).

## Шаги

1. **Класс** `unit/units/<group>/<Name>Prod.java extends ProductionItem`. Обязательно override
   `getEntityType()`, возвращающий `EntityType<? extends Mob>` этого юнита — именно так элемент
   сопоставляется с юнитом (решение 12.6, этап E.9).
2. **Стоимость** — живёт **на самом `ProductionItem`** (`ResourceCost`), а не в списке юнитов по
   enum. `ReignOfNetherCommonConfigs` сведён к пустому spec.
3. **Регистрация** — реестр `ReignOfNetherRegistries.PRODUCTION_ITEM` (через
   `ProductionItems.init()`), статическое поле в `ProductionItems`.
4. **Привязка к зданию** — `this.productions.add(ProductionItems.X, Keybindings.abilitySlotN)` в
   конструкторе здания.
5. **Дублирование** — `ProdDupeRule`; очередь — `ActiveProduction` на `ProductionPlacement`.

## Грабли

* **Не** возвращайся к строковому сопоставлению по `itemName` — только `EntityType<?>`.
* `ProductionItems.getProductionItem(EntityType)` сравнивает `prodItem.getEntityType() == entityType`.
* Лимит армии: `ProductionItem.isBelowMaxPopulation` считает `maxPopulation + бонус ратуши`.
* Для кастомных строений `CustomProductionItem` копирует `entityType` из исходного item — не забудь.

## Гейт

`compileJava` → `validateMixins` → `runData` → `runServer`.
