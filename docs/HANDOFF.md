# HANDOFF — состояние и продолжение работы (JSON-каркас, 2026-10-08)

Передача контекста для следующего ИИ-агента/разработчика. Читать вместе с: `docs/STATUS.md`,
`docs/PLAN_RTS_ONLY.md`, `docs/RESEARCH_AND_EXTENSIBILITY_PLAN.md`, `docs/CONTENT_JSON_PLAN.md`,
`docs/VANILLA_CHANGES.md`, `docs/BUGS_RUNCLIENT.md`, `AGENTS.md` (в корне проекта) и `_GUIDES/`.

## 0. Как запускать (Windows, PowerShell 5.1)

`gradlew.bat` НЕ работает. Сборка только через java:

```
cd ___temp
& "C:\Program Files\Java\jdk-21\bin\java.exe" '-Dorg.gradle.appname=gradlew' -jar gradle/wrapper/gradle-wrapper.jar <task> --offline --console=plain
```

Гейты: `compileJava` → `validateMixins` → `runData`. `runServer` — по возможности (см. трапы).
Репозиторий: `___temp/` (ветка `wip/stage-d-deletions`; `1.21.1-clean` держится синхронно через
`git branch -f 1.21.1-clean wip/stage-d-deletions`).

## 1. Что это за проект сейчас

Мод NeoForge 1.21.1 превращён в **чистый РТС-каркас** — контент прежних фракций вырезан, механика
осталась. Поверх каркаса владелец пишет свою фракцию **данными** (датапак JSON), без Java на каждый
юнит/здание.

В этой сессии два больших дуговых изменения:
1. **Юниты полностью data-driven:** код-классы юнитов удалены; юнит = ванильный/модовый `Mob` +
   `UnitDefinition` (JSON). Все под-интерфейсы (`WorkerUnit`/`AttackerUnit`/`RangedAttackerUnit`/
   `HeroUnit`) схлопнуты в один `Unit` + роль-флаги.
2. **Здания**: заложен полностью-данными каркас (`BuildingDefinition` + `JsonBuilding`), пока
   сосуществует с код-зданиями `TownCentre`/`Barracks` (их удаление — следующий шаг).

## 2. Архитектура data-driven контента (как добавлять)

### Юниты — `data/<ns>/unit/<name>.json` (реестр `reignofnether:unit`)
Идентичность юнита — **определение**, а не `EntityType` (два юнита могут делить тело).
```json
{ "base": "minecraft:villager", "role": "worker",
  "flags": { "canGather": true, "canBuild": true, "canGarrison": true },
  "scale": 1.0,
  "attributes": { "minecraft:generic.max_health": 25, "reignofnether:attack_damage": 1, ... },
  "abilities": [ { "type": "myns:some_ability", "cooldown": 100, "params": { "amount": 1 } } ],
  "cost": { "food": 50, "wood": 0, "ore": 0, "seconds": 15 }, "population": 1 }
```
- `role`: `melee`/`ranged`/`worker`/`flying`/`hero`. Движок (`UnitMobMixin.initialiseGoals`) сам
  строит goals: melee→`MeleeAttackUnitGoal`+`MeleeAttackBuildingGoal`; ranged→`UnitBowAttackGoal`+
  `RangedAttackBuildingGoal`; worker→`GatherResourcesGoal`+`BuildRepairGoal`+
  `ExploreBuildLocationGoal`+`ReturnResourcesGoal`; garrison по флагу. Worker дополнительно получает
  `BuildMenuAbility` (`WorkerBuildMenu`).
- Атрибуты применяются в `UnitDefinitionRuntime.applyAttributes`; `scale` — ванильный `SCALE`.
- `inherits` (id другого определения) — зарезервировано (пока НЕ реализовано разрешение наследования).

### Способности — класс в коде + инстанс в JSON
- Аннотация-класс регистрируется в `AbilityTypes.register(id, factory)` (id = `type` в JSON).
- `AbilitySpec` (Codec): `type` + `cooldown`/`mana`/`passive`/`requiredResearch`/`params`
  (`params` сейчас `Map<String,Double>` — только числа; расширить при необходимости).
- В `UnitDefinitionRuntime.buildAbilities` инстансы создаются и кладутся в `unit.getAbilities()`.
- **Баговые классы способностей (пример `poison_on_hit` и т.п.) в коде каркаса НЕТ** — их пишет автор
  фракции и регистрирует.

### Здания — `data/<ns>/building/<name>.json` (реестр `reignofnether:building`)
```json
{ "structure": "reignofnether:barracks", "maxHealth": 150, "populationSupply": 0,
  "isCapitol": false, "production": [ "reignofnether:vindicator_unit" ], "researches": [] }
```
- `JsonBuilding extends ProductionBuilding`; `JsonProductionItem` спавнит юнит по id определения.
- `JsonBuildingManager.reload(server)` (вызов из `FactionServerEvents.onServerStarted`) строит по
  одному `JsonBuilding` на каждое определение в собственном map (кодовый
  `ReignOfNetherRegistries.BUILDING` уже заморожен на момент загрузки датапака — писать туда нельзя).
- Размещение: `BuildingServerboundPacket.resolveBuilding` — сначала код-реестр, затем
  `JsonBuildingManager`; клиентский `placeBuilding` шлёт `definitionId` для `JsonBuilding`.
- **НЕ реализовано:** `researches` (общая очередь с производством), `addons` `{type,…}`, `upgrades`.

### Фракция — `data/<ns>/faction/<name>.json`
`{ name, icon, capitol, starting_units:[{unit,count}], food/wood/ore/emerald }`. Вход: `/startrts
[<faction>]` (нужен RTS-пропуск) → клиентское меню (`FactionMenu`); с фракцией — остальные кнопки
заблокированы. Старт (`PlayerServerEvents.startRTS`) резолвит столицу: фракция → код-`BUILDING` →
`JsonBuildingManager` → дефолт `reignofnether:town_centre` → код-`Buildings.TOWN_CENTRE`.

### Исследования — `data/<ns>/research/<name>.json` (`ResearchJsonLoader`)
Гейт: `ResearchCondition(researchId, invert)` у `Ability`/`ProductionItem`/`Building.requiredResearch`
(серверные проверки + HUD). Состояние на игрока (`ResearchSaveData`), обнуляется при поражении.
Панель `ResearchMenu` — **только статус**. Запуск/очередь исследований отложены в фазу JSON-зданий
(исследования у здания, очередь общая с производством, отмена с возвратом).

## 3. Ключевые классы (карта правок)

- `unit/interfaces/Unit.java` — единственный контракт юнита; роль-флаги `isWorker()/isAttacker()/
  isRangedAttacker()/isHero()`, `isRtsUnit()`, `isUnit(Object)`, goal-accessors (воркер/аттакер),
  attacker/hero `default`-методы, статики `tickWorker/tickAttacker/tickHero/…`.
- `unit/UnitDefinition.java`, `unit/UnitDefinitions.java` (реестр), `unit/UnitDefinitionRuntime.java`
  (create/spawn + applyAttributes + buildAbilities).
- `unit/interfaces/DefinedUnit.java` — id определения на мобе.
- `mixin/UnitMobMixin.java` — `@Mixin(Mob) implements Unit, DefinedUnit`; всё состояние дженерик;
  `isRtsUnit()` = есть определение; `initialiseGoals()` по роли/флагам; `getAttack*/getGather*/
  getBuild*` accessors; `goalSelector`/`targetSelector` @Shadow.
- `ability/AbilitySpec.java`, `ability/AbilityTypes.java`.
- `building/BuildingDefinition.java`, `BuildingDefinitions.java`, `buildings/JsonBuilding.java`,
  `buildings/JsonBuildingManager.java`, `production/JsonProductionItem.java`, `WorkerBuildMenu.java`.
- Поток размещения: `building/BuildingServerboundPacket.java` (`resolveBuilding`, `placeBuilding`).
- Старт: `player/PlayerServerEvents.java` (`STARTING_ARMY`/`STARTING_WORKER_DEF` — **id определений**;
  `startRTS`, `startRTSBot`; `neutraliseUnitsOf`).
- Регистрация датапак-реестров: `ReignOfNether#loadDatapacks` (unit, building, faction, rts_buttons).
- Пакеты: `registrars/PacketHandler.java` (FactionClientboundPacket, Research*, …).

## 4. Что осталось (по приоритету)

1. **Удалить код-здания `TownCentre`/`Barracks`** и их `getBuildButton`/структуры из кода:
   - `building/WorkerBuildMenu.buildButtons()` сейчас добавляет `Buildings.TOWN_CENTRE`/`BARRACKS`
     И JSON — оставить только JSON (+ custom).
   - Убрать регистрации в `Buildings`/`EntityRegistrar`-подобных и всё, что ссылается на
     `Buildings.TOWN_CENTRE`/`BARRACKS` (команды `CommandsServerEvents`/`BuildingCommands`, тесты,
     `startRTS` фолбэк). `structures/town_centre.nbt` и `barracks.nbt` **оставить** — их грузит
     `structureName` JSON-зданий.
2. **Здания из JSON:** `researches` (очередь общая с производством, отмена с возвратом — см.
   `RESEARCH_AND_EXTENSIBILITY_PLAN.md` §Отложено), `addons: [{type,…}]`, `upgrades` (цепочка
   уровней: структура/имя/стоимость/характеристики/производство/способности/аддоны/исследования).
   `researches`/`addons`/`upgrades` пока НЕ в `BuildingDefinition.CODEC`.
3. **Фаза 4 JSON-контракта:** герой (`role: hero` — блок прокачки/маны/рангов), ranged
   (`projectile` + параметры). Часть инфраструктуры уже есть в `Unit` (hero defaults).
4. **lang-дочистка:** `entity.reignofnether.villager_unit*` (используется тултипами `VillagerProd`/
   `VindicatorProd`); в остальных локалях — по желанию.
5. **`runClient` — НИ РАЗУ не запускался с этими изменениями.** Всё клиентское (рендер юнитов/
   зданий теперь ванильный, меню фракций/исследований, размещение) проверено только сборкой и
   `runServer`. Обязательна ручная проверка.
6. **Документация:** обновить статусы в `PLAN_RTS_ONLY.md`/`CONTENT_JSON_PLAN.md`; при желании
   дописать `_GUIDES/` про JSON-юниты/здания/способности.

## 5. Трапы и грабли (важно)

- **CRLF.** Большинство `.java` хранят CRLF. Инструменты редактирования могут переписать файл в LF →
  дифф «весь файл». Перед коммитом сверять `git diff --numstat` и
  `git diff --ignore-cr-at-eol --numstat` (должны совпадать).
- **Скриптовые regex-замены.** В этой сессии скрипты дважды ломали код: (а) заворачивали сами
  хелперы в бесконечную рекурсию; (б) цепляли идентификаторы, начинающиеся с имени типа
  (`UnitInventory`, `UnitRangedAttackGoal`, `UnitBowAttackGoal`) из-за `instanceof Unit`-паттерна.
  После массовых замен ВСЕГДА проверять:
  `Select-String ... 'Unit\.is\w+\(.*\)[A-Za-z_]'` (повреждения), `'\(Unit\)\w'` и перекомпилировать.
- **`@Mixin(Mob.class)`.** `UnitMobMixin` и `UnitInventoryMobMixin` сосуществуют на `Mob`; каждому
  нужен конструктор `(EntityType<? extends LivingEntity>, Level)`. Mixin-поля — `@Unique`; без
  инициализаторов (ленивая инициализация в геттерах).
- **Датапак-реестры** (`unit`, `building`, `faction`, `rts_buttons`) регистрируются в `loadDatapacks`
  и синхронизируются клиентам. `ReignOfNetherRegistries.BUILDING` (кодовый) **заморожен** к моменту
  загрузки датапака — не писать туда из JSON-логики.
- **`runServer`.** dev-сервер иногда печатает `Done (...)` и НЕ завершается. Запускать через
  `background_process` (monitor с `ready.pattern: "Done \\("`), затем `stop` в пределах 15 с; либо он
  сам останавливается через ~20–90 с (в последних прогонах так и было). Не оставлять висящий процесс.
- **`git show <ref>:<path> > file`** под PowerShell 5.1 пишет UTF-16LE → javac `unmappable character`.
  Только `cmd /c "git show ... > file"`.
- **Лимит армии** — база 1, прирост даёт столица (`populationSupply`). Цены — на определении юнита
  (здание может переопределить; пока не реализовано).

## 6. Новые файлы этой сессии (инвентарь)

- `research/`: Research, ResearchCondition (Codec), ResearchType, ResearchAttributeModifier,
  ResearchRegistry, ResearchSaveData, ResearchUtils, ResearchClientEvents, ResearchClientboundPacket,
  ResearchDefinitionsClientboundPacket, ResearchJsonLoader, ResearchCommand, ResearchServerEvents,
  ResearchMenu, ResearchAttributeApplier.
- `faction/`: StartingUnit, Faction (Codec), FactionRegistries, FactionClientboundPacket,
  FactionClientEvents, FactionMenu, FactionCommand, FactionServerEvents.
- `unit/`: UnitDefinition, UnitDefinitions, UnitDefinitionRuntime; `interfaces/DefinedUnit`.
- `ability/`: AbilitySpec, AbilityTypes.
- `building/`: BuildingDefinition, BuildingDefinitions, WorkerBuildMenu;
  `buildings/JsonBuilding`, `buildings/JsonBuildingManager`; `production/JsonProductionItem`.
- `mixin/`: UnitMobMixin.
- Data JSON: `data/reignofnether/faction/villagers.json`, `data/reignofnether/unit/villager_unit.json`,
  `unit/vindicator_unit.json`, `data/reignofnether/building/barracks.json`, `building/town_centre.json`.

**Удалено этой сессией:** `VillagerUnit`, `VindicatorUnit`, их рендереры/модели, `EntityType`/
спавн-яйца; `VillagerUnitProfession`(+Layer); `WorkerUnit`/`AttackerUnit`/`RangedAttackerUnit`/
`HeroUnit` (интерфейсы); `ArmorStandMixin`; пакет `gamemode/`; контент-доки и пр.; 65 NBT-структур и
`maps/`; `RandomItemDropRule`.

## 7. Ключевые коммиты (для ориентира)

`668a6877` (багфиксы 2–5) · `e87b66db` (сужение ванили) · `398bc780`…`9e44abf9` (исследования 1–4) ·
`35ea74b9`/`6c8c8a43` (планы) · `5f846d18`/`b68e714e` (UnitDefinition, isRtsUnit) · `4bdde13d`
(схлопывание в Unit) · `13138eea` (goals из определения) · `795f7f04` (JSON-шаблоны юнитов) ·
`ce5197ea` (удаление код-юнитов) · `36fc5cae` (AbilitySpec) · `f8b530eb`/`da0adedb`/`e371df0e`
(каркас зданий) · `b45f617c` (меню воркера) · `de6c42ed` (поток размещения) · `bc824899`
(`town_centre.json`).
