---
name: reignofnether-codebase-map
description: Карта кодовой базы Reign of Nether — слои, точки входа, порядок инициализации и три шва между юнитами, зданиями и производством. Использовать в начале любой задачи по этому проекту, чтобы понять, где что лежит.
---

# Карта кодовой базы

Пакет-корень: `___temp/src/main/java/com/solegendary/reignofnether/`. NeoForge 21.1 / MC 1.21.1.
Ветка `wip/stage-d-deletions` — **чистый РТС-каркас**: контент фракций вырезан, юниты/здания/способности
**data-driven** (датапак JSON). Полная карта — `_GUIDES/00_обзор.md`, состояние — `docs/STATUS.md`.

Кодовых классов юнитов и зданий **больше нет**: нет `units/villagers/*`, нет `buildings/villagers/*`;
реестр `ReignOfNetherRegistries.BUILDING` пуст. Если в старой инструкции упоминаются такие классы — она
устарела (см. `docs/reference/`).

## Слои

```
registrars/     DeferredRegister'ы: сущности, блоки, блок-сущности, предметы, контейнеры, геймрулы,
                атрибуты, эффекты, частицы, звуки, пакеты, аргумент-типы
api/            ReignOfNetherRegistries — кодовые реестры BUILDING, PRODUCTION_ITEM, DATA_TYPE
mixin/          общие и клиентские миксины (@Mixin(Mob) implements Unit — UnitMobMixin)
unit/           ЮНИТЫ: Unit (интерфейс), UnitDefinition + UnitDefinitions (датапак-реестр),
                UnitDefinitionRuntime (спавн/атрибуты/способности), goals/, pathfinding/, packets/
building/       ЗДАНИЯ: Building + BuildingPlacement, BuildingDefinition + BuildingDefinitions,
                buildings/JsonBuilding + JsonBuildingManager, production/ (JsonProductionItem,
                ResearchProductionItem, JsonUpgradeProductionItem), addon/ (AddonSpec/AddonTypes/Addons),
                custombuilding/ (авторский инструмент)
ability/        Ability/HeroAbility/MenuAbility/CommandAbility/BuildMenuAbility/DataMenuAbility,
                AbilitySpec + MenuEntrySpec + AbilityTypes, Abilities (список у юнита/здания)
data/          ContentValidator (строгая проверка полей JSON по record'ам) +
                ContentValidationReloadListener (лог `[content-validation]` на загрузке мира/`/reload`)
research/       система исследований (реестр, save data, условия, JSON-загрузчик, ResearchProductionItem)
faction/        реестр фракций (датапак), меню выбора, /startrts
resources/      ResourceCost / ResourceName / пул ресурсов, ResourceGenerator (аддон H.9)
player/         вход/выход из РТС, старт матча, победа/поражение, save data
commands/       /rtsapi (RTSApiCommands: unit/building/player/execute), /startrts, /research
orthoview/      ортокамера; hud/, minimap/, guiscreen/ — интерфейс поверх неё
alliance/, nether/, time/, worldborder/, taskscheduler/, debug/, util/, network/, config/,
healthbars/, sounds/, particles/, items/, keybinds/, attackwarnings/, gamerules/, cursor/, blocks/
```

## Порядок инициализации (`ReignOfNether.java`, конструктор)

```
...Registrar'ы ... → GameRuleRegistrar.init() → Buildings.init() (пусто) → Addons.init() →
BuiltInAbilities.init() (heal/regeneration/summon/menu) → ProductionItems.init() → ...
```

Датапак-реестры (`rts_buttons`, `faction`, `unit`, `building`) регистрируются в
`loadDatapacks(DataPackRegistryEvent.NewRegistry)`; JSON-загрузчики (кастомные кнопки, исследования) —
в `reloadListener(AddReloadListenerEvent)`; `JsonBuildingManager.reload(server)` — на старте сервера.

## Три шва (знать при любой правке)

1. **Производство** — единственный путь получить юнита. Список производств живёт **на здании**
   (`production` в JSON → `JsonProductionItem`); идентификатор — `ProductionItem.getNetworkId()`
   (код-предметы — ключ в `PRODUCTION_ITEM`, JSON/исследования/апгрейды переопределяют). Юнит спавнится
   `UnitDefinitionRuntime.spawn(level, definitionId, pos, owner)`.
2. **Лимит армии** — база 1, прирост даёт столица (`populationSupply`, у JSON-здания — из определения).
3. **Гарнизон/атака здания** — реестр аддонов `AddonTypes` + `Building.onBuilt`/`tick` хуки.

## Гейты

`compileJava` → `validateMixins` → `runData` → `test`. `runServer` в этой среде **не запускать**
(процесс не завершается). Проверка в игре — `docs/RUNCLIENT_CHECKLIST.md`.

## Что почитать

* `docs/STATUS.md` — состояние и гейты; `docs/HANDOFF.md` — точка входа и журнал.
* `_GUIDES/00_обзор.md` — карта + порядок регистрации; `_GUIDES/README.md` — индекс гайдов.
* `docs/VANILLA_CHANGES.md` — что мод меняет в ванилле (читать перед правкой миксинов).
* `_GUIDES/` — пошаговые гайды; в каждом есть раздел «Строгая проверка полей».
* `docs/RUNCLIENT_CHECKLIST.md` — что проверить в игре за один `runClient`.
