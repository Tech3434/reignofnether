---
name: reignofnether-codebase-map
description: Карта кодовой базы Reign of Nether — слои, точки входа, три шва между юнитами и зданиями. Использовать в начале любой задачи по этому проекту, чтобы понять, где что лежит.
---

# Карта кодовой базы

Пакет-корень: `___temp/src/main/java/com/solegendary/reignofnether/`. Версия 1.4.4d, MC 1.21.1,
NeoForge 21.1.250. Ветка `wip/stage-d-deletions` — «чистый РТС-каркас»: контент фракций,
чары, туман войны, песочница и читы удалены.

## Слои

```
registrars/     DeferredRegister'ы и параметры: сущности, блоки, предметы, геймрулы,
                атрибуты, эффекты, частицы, звуки, пакеты
api/            ReignOfNetherRegistries — кастомные реестры BUILDING, PRODUCTION_ITEM, DATA_TYPE
unit/           интерфейсы юнитов, события, goals/ (ИИ), pathfinding/, controls/,
                units/villagers/{VillagerUnit,VindicatorUnit} — шаблоны
building/       Building + BuildingPlacement (система), production/ (очередь),
                buildings/villagers/{TownCentre,Barracks} — шаблоны, addon/ (контракты),
                custombuilding/ (авторский инструмент ГМа)
ability/        Ability/HeroAbility/Abilities — базовые классы (каркас)
resources/      ResourceCost(ы), пул ресурсов, ResourceSource(s)
research/       состояние флагов исследования (предметы удалены)
player/         RTSPlayer, вход/выход из РТС-режима, победа/поражение, пакеты
commands/       /rtsapi, execute-селекторы, аргумент-типы
orthoview/      ортокамера (клавиша), hud/, minimap/ — интерфейс поверх неё
alliance/       союзы игроков (граф имён в памяти)
gamerules/      меню и синхронизация геймрулов
```

## Порядок инициализации

`ReignOfNether.java` (конструктор): `ItemRegistrar` → `EntityRegistrar` → `BlockRegistrar` →
`BlockEntityRegistrar` → `GameRuleRegistrar` → `Buildings.init()` → `ProductionItems.init()`.
Стоимости читать **не раньше** `FMLCommonSetup` (`ResourceCosts.deferredLoadResourceCosts()`),
иначе `IllegalStateException`.

## Три шва (знать при любой правке)

1. **`ProductionItem` / `ProductionPlacement`** — единственный путь получить юнита. Список
   производств живёт на здании (`this.productions.add(...)`), очередь — на размещении.
   Сопоставление с юнитом — по `EntityType<?>` напрямую (E.9), не по строке.
2. **Лимит армии** — база `DEFAULT_MAX_POPULATION = 1`, прирост даёт ратуша
   (`Building.populationSupply`, `TownCentre` = 10) через `getPopulationBonusFromCapitols`.
3. **`GarrisonableBuildingAddon` + `MeleeAttackBuildingGoal`** — здание как цель атаки и гарнизон.

## Что почитать

* `docs/STATUS.md` — текущее состояние и гейты.
* `docs/PLAN_RTS_ONLY.md` — исходный план A–H, все решения.
* `_GUIDES/00_обзор.md` — карта + порядок регистрации.
* `docs/INTRUSION_AUDIT.md` — что мод ломает в обычном мире (читать перед правкой миксинов).
