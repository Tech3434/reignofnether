# HANDOFF — полный контекст для продолжения (JSON-каркас, 2026-10-09)

**Это файл передачи контекста.** Следующий ИИ-агент/разработчик начинает ОТСЮДА. Читать вместе с:
`docs/STATUS.md`, `docs/PLAN_RTS_ONLY.md`, `docs/RESEARCH_AND_EXTENSIBILITY_PLAN.md`,
`docs/CONTENT_JSON_PLAN.md`, `docs/VANILLA_CHANGES.md`, `docs/BUGS_RUNCLIENT.md`, `docs/reference/`,
`_GUIDES/`, `AGENTS.md` (в `___temp/`).

**Состояние на конец сессии**
- Ветка: `wip/stage-d-deletions`; `1.21.1-clean` держится синхронно (`git branch -f 1.21.1-clean wip/stage-d-deletions`).
- HEAD: см. `git log` (после этой сессии — за `ec652683`; всего ~75 коммитов поверх `ec35ca7d`).
- Гейты: `compileJava` ✅ · `validateMixins` ✅ · `runData` ✅ · `runServer` **не запускать** в этой среде
  (см. §6) · **`runClient` ⚠ НИ РАЗУ не запускался с data-driven изменениями.**
- Data-driven контент доведён: аддоны (обобщены), исследования у зданий, апгрейды (per-level),
  ranged-юнит, клиентская синхронизация/сейв/HUD/команды JSON-зданий, флаги зданий (см. §2/§4).

---

## 0. Как запускать (Windows, PowerShell 5.1)

`gradlew.bat` НЕ работает (передаёт java одновременно `-classpath ""` и `-jar`). Сборка только через java:

```
cd ___temp
& "C:\Program Files\Java\jdk-21\bin\java.exe" '-Dorg.gradle.appname=gradlew' -jar gradle/wrapper/gradle-wrapper.jar <task> --offline --console=plain
```

Гейты на каждом шаге: `compileJava` → `validateMixins` → `runData` → `test`.
`runServer` — **не запускать** обычным способом: dev-сервер в этой среде печатает `Done (...)` и сам не
завершается; процесс висит до ручного вмешательства владельца (см. `AGENTS.md` и §6 «Трапы»). Если всё же
надо — только через `background_process` monitor с `ready.pattern: "Done \\("` и `stop` в пределах 15 с.

**Директивы владельца:** мод в разработке, есть только у владельца, поэтому **репозиторий можно коммитить в
сломанном состоянии**; миграций/совместимости не требуется. «Делай всё; спрашивай, если сомневаешься» —
работать автономно, **но по геймдизайну спрашивать владельца**. Коммиты — по фазам, с зелёными гейтами
(в этой сессии так и делалось). В конце — выгрузить весь контекст в файлы (этот файл).

---

## 1. Что это за проект сейчас

Мод NeoForge 1.21.1 превращён в **чистый РТС-каркас**: контент прежних фракций вырезан, механика осталась.
Поверх каркаса владелец пишет свою фракцию **данными** (датапак JSON), без Java на каждый юнит/здание/способность.

Два больших изменения этой сессии:
1. **Юниты полностью data-driven:** код-классы юнитов удалены; юнит = ванильный/модовый `Mob` +
   `UnitDefinition` (JSON). Все под-интерфейсы (`WorkerUnit`/`AttackerUnit`/`RangedAttackerUnit`/`HeroUnit`)
   схлопнуты в **один** `Unit` + роль-флаги.
2. **Здания полностью data-driven:** код-классы `TownCentre`/`Barracks` и `TownCentrePlacement` удалены;
   здание = `JsonBuilding` + `BuildingDefinition` (JSON). Кодовый реестр `Buildings` пуст.

---

## 2. Архитектура data-driven контента (как добавлять)

### Юниты — `data/<ns>/unit/<name>.json` (реестр `reignofnether:unit`)
Идентичность юнита — **определение**, а не `EntityType` (два юнита могут делить тело).
```json
{ "base": "minecraft:villager", "role": "worker",
  "flags": { "canGather": true, "canBuild": true, "canGarrison": true },
  "scale": 1.0,
  "equipment": "minecraft:bow",
  "projectile": { "entity": "minecraft:arrow", "velocity": 1.6, "damage": -1, "inaccuracy": 1.0 },
  "attributes": { "minecraft:generic.max_health": 25, "reignofnether:attack_damage": 1 },
  "abilities": [ { "type": "myns:some_ability", "cooldown": 100, "params": { "amount": 1 } } ],
  "cost": { "food": 50, "wood": 0, "ore": 0, "emerald": 0, "seconds": 15 },
  "population": 1, "requiredResearch": [ { "research": "myns:x", "invert": false } ],
  "worker": { "gatherable": ["food", "wood", "ore"], "buildSpeed": 1.0, "carryCapacity": 100 } }
```
- `role`: `melee`/`ranged`/`worker`/`flying`/`hero`. Движок (`UnitMobMixin.initialiseGoals`) строит goals:
  melee→`MeleeAttackUnitGoal`+`MeleeAttackBuildingGoal`; ranged→`UnitBowAttackGoal`+`RangedAttackBuildingGoal`;
  worker→`GatherResourcesGoal`+`BuildRepairGoal`+`ExploreBuildLocationGoal`+`ReturnResourcesGoal`; garrison по флагу.
  Worker дополнительно получает `BuildMenuAbility` (`WorkerBuildMenu`). **Герой** при спавне применяет
  характеристики уровня 1 и далее тикается `tickHero` (мана/опыт/ранги).
- Атрибуты — `UnitDefinitionRuntime.applyAttributes`; `scale` — ванильный `SCALE`.
- `equipment` — предмет в главную руку при спавне (`UnitDefinitionRuntime.create`).
- `projectile` (ranged) — `ProjectileSpec` (entity/velocity/damage/inaccuracy); спавн —
  `UnitMobMixin.performUnitRangedAttack` (раньше был no-op). Без `projectile` — `minecraft:arrow`. Демо:
  `skeleton_unit.json` (лук+стрела), производится в казарме.
- `inherits` (id другого определения) — **реализовано**: незаданные поля берутся у родителя (атрибуты
  мержатся по ключам), транзитивно и с защитой от циклов (`UnitDefinitions.resolve`; все наследуемые поля
  в `UnitDefinition` теперь `Optional`, есть `withInherited`). Демо: `skeleton_marksman` ← `skeleton_unit`.

### Способности — класс в коде + инстанс в JSON
- Класс регистрируется в `AbilityTypes.register(id, factory)` (id = `type` в JSON). Движковые типы —
  `ability/BuiltInAbilities.init()` (вызывается из конструктора мода); сейчас это `reignofnether:heal`
  (`SimpleHealAbility`, params `amount`), демо у `skeleton_unit`. Автор фракции регистрирует свои так же.
- Активная способность рассылается по `UnitAction`: `UnitActionItem` находит ability с `action == action`
  и зовёт `use(...)`; поэтому активному типу нужна **своя константа `UnitAction`**.
- Пассивка (`passive: true`, кнопки нет) получает серверный тик через `Ability.tickPassive(Unit)` из
  `Unit.tick`. Движковый пример — `reignofnether:regeneration` (`RegenerationAbility`).
- `AbilitySpec` (Codec): `type` + `cooldown`/`mana`/`passive`/`requiredResearch`/`params`, а для меню —
  ещё `name`/`icon`/`submenu` (`List<MenuEntrySpec>`; рекурсивный кодек через `Codec.lazyInitialized`).
  `params` — типизированные (`Map<String, AbilityParam>`: число или строка); геттеры `param(double)`,
  `stringParam(...)`, `resourceParam(...)` (парсит `ResourceLocation` — сущность/звук/предмет).
- Движковые типы: `heal` (`amount`, `sound`), `regeneration` (`amount`/`interval`), `summon`
  (`unit`/`count`), `menu` (см. §10 «K6»: `submenu` из инлайн-`ability`/`command`/`building` + `row`/`col`).
- В `UnitDefinitionRuntime.buildAbilities` инстансы создаются и кладутся в `unit.getAbilities()`;
  из spec прокидываются `cooldown`/`range`/`radius`/`canTargetEntities`/`oneClickOneUse`/`passive`/`mana`
  (для `HeroAbility`)/`requiredResearch` (числа `0`/`false` = дефолт класса).
- Конкретные «боевые» классы способностей пишет автор фракции — в каркасе один пример (`heal`).

### Здания — `data/<ns>/building/<name>.json` (реестр `reignofnether:building`)
```json
{ "structure": "reignofnether:barracks", "maxHealth": 150, "populationSupply": 0,
  "isCapitol": false,
  "flags": { "canAcceptResources": false, "buildTimeModifier": 1.0, "capturable": false,
             "invulnerable": false, "repairable": true, "drawAggro": true, "captureRange": 20,
             "foundationYLayers": 1 },
  "production": [ "reignofnether:vindicator_unit",
                  { "unit": "reignofnether:skeleton_marksman", "costOverride": { "food": 55, "seconds": 8 } } ],
  "researches": [], "addons": [ { "type": "myns:garrison", "params": {} } ],
  "requiredResearch": [] }
```
- `JsonBuilding extends ProductionBuilding`; `JsonProductionItem` спавнит юнит по id определения, а
  `research/ResearchProductionItem` (из `researches`) кладётся в ту же очередь производств и выдаёт
  исследование владельцу по завершении (см. §«Исследования»).
- `JsonBuildingManager.reload(server)` (из `FactionServerEvents.onServerStarted`) создаёт по одному `JsonBuilding`
  на определение в **собственном** map, **не пересоздавая уже существующие** (идентичность важна: размещение
  держит ссылку на `Building`, а `BuildingSaveData.load` может создать его раньше). Кодовый
  `ReignOfNetherRegistries.BUILDING` заморожен — писать туда нельзя.
- Персистентность: `BuildingSaveData.save` пишет `jsonDefinitionId` (а не код-ключ) для `JsonBuilding`, при
  загрузке — `JsonBuildingManager.getOrCreate(level, id)` (из датапак-реестра; не зависит от порядка листенеров).
- Размещение: `BuildingServerboundPacket.resolveBuilding` — сначала код-реестр, затем `JsonBuildingManager`;
  клиентский `placeBuilding` шлёт `definitionId` для `JsonBuilding`. Синхронизация сервер→клиент —
  `BuildingAction.PLACE_JSON` + `JsonBuildingManager.getOrCreate(level, id)` (клиент строит из синхронизированного
  датапак-реестра). `ProductionItem.getNetworkId()` — стабильный id для очереди/кнопок (`JsonProductionItem` →
  id определения, `ResearchProductionItem` → id исследования, `JsonUpgradeProductionItem` → `upgrade:<n>`).
  `BuildingUtils.getKeyString` — null-safe id для HUD.
- **Апгрейды (полностью):** `upgrades: [...]` → `JsonBuildingManager` строит `JsonBuilding`-**вариант на
  уровень** (база + кумулятивные `UpgradeSpec`; поля переопределяются, `production`/`addons` — заменяются).
  `JsonUpgradeProductionItem` в очереди здания по завершении переключает placement на вариант
  (`BuildingPlacement.applyUpgradeBuilding`), поэтому производство/исследования/аддоны/способности/имя/
  иконка/структура/maxHealth меняются автоматически. Уровень — `BuildingPlacement.upgradeLevel`, синк
  `SET_UPGRADE_LEVEL`+`CHANGE_STRUCTURE`, сохраняется. Демо: `barracks.json` → «Barracks II» (+maxHealth,
  +night_source — показывает замену аддонов).
- Строится `structureName` (`.nbt`); оставлены `town_centre.nbt`/`barracks.nbt`.

### Аддоны зданий — `addons: [{type, params}]` (готово)
`buildings` могут нести аддоны — поведение, реализованное кодом и навешиваемое по id.
- **API `Building`:** `Map<Class<BuildingAddon>, BuildingAddon> activeAddons`;
  `<T extends BuildingAddon> T getActiveAddon(Class<T>)`; `setActiveAddon(Class<T>, T, boolean active)`;
  `hasActiveAddon(Class<? extends BuildingAddon>)`; `addActiveAddon(BuildingAddon)` — регистрирует аддон
  под **всеми** реализуемыми им `BuildingAddon`-интерфейсами (+ под конкретным классом). Интерфейс
  `building/addon/BuildingAddon` (+ lifecycle-хуки `onBuildingBuilt`/`onBuildingTick`, вызываются
  `Building.onBuilt`/`Building.tick`; hook-и нарочно **не** совпадают по имени с `Building.onBuilt`/`tick`,
  иначе `CustomBuilding` (addon=сам) рекурсировал бы).
- **Каркас:** `building/addon/AddonSpec.java` (record `type`+`params Map<String,Double>`, Codec),
  `building/addon/AddonTypes.java` (реестр `type` → `Factory.create(spec, building)`),
  `building/addon/Addons.java` (`init()` — регистрация движковых типов; вызывается из конструктора
  `ReignOfNether`).
- В `BuildingDefinition` поле `List<AddonSpec> addons` + строка в Codec.
- **Сделано:** `JsonBuilding` создаёт аддоны из `definition.addons()` через `AddonTypes.create(...)` и
  навешивает `addActiveAddon(...)`. Движковые типы (все — `AddonTypes` в `Addons.init()`): `night_source`
  (`NightSourceAddon`+`RangeIndicatorAddon`), `range_indicator` (`RangeIndicatorAddon`), `garrison`
  (`GarrisonableBuildingAddon`), `nether_converting` (`NetherConvertingAddon` + `onBuildingBuilt` создаёт
  `NetherZone`). Примеры данных: `town_centre.json` — `night_source` (range 24), `barracks.json` — `garrison`
  (capacity 3, attackRange 20). Гейты зелёные; в игре (`runClient`) не проверялось.

### Фракция — `data/<ns>/faction/<name>.json`
`{ name, icon, capitol, starting_units:[{unit,count}], food/wood/ore/emerald }`. Реестр `FactionRegistries.FACTION_KEY`
(`reignofnether:faction`), синхронизируется клиентам. Вход: `/startrts [<faction>]` (нужен RTS-пропуск) →
клиентское меню `FactionMenu`; с фракцией остальные кнопки заблокированы. Старт
(`PlayerServerEvents.startRTS`) резолвит столицу: фракция → код-`BUILDING` → `JsonBuildingManager` → дефолт
`reignofnether:town_centre` → код-`Buildings.TOWN_CENTRE`. Конфиг `defaultFaction`
(`ReignOfNetherCommonConfigs.DEFAULT_FACTION`, default `reignofnether:villagers`).
Идентификатор фракции передаётся в `PlayerServerboundPacket.START_RTS` (поле `factionId`).

### Исследования — `data/<ns>/research/<name>.json` (`ResearchJsonLoader`, `/reload`)
Поля: `name`, `icon`, `type` (`unlock`/`attribute_boost`), `cost`, `prerequisites` (с `invert`),
`attributes` (со `unit`). Гейт: `ResearchCondition(researchId, invert)` у
`Ability`/`ProductionItem`/`Building.requiredResearch` (серверные проверки + HUD). Состояние на игрока
(`ResearchSaveData`), обнуляется при поражении и в `resetRTS`. `ResearchAttributeApplier` (ATTRIBUTE_BOOST,
идемпотентно, `unitFilter` по типу сущности). Команды `/research grant|revoke|clear|list`.
Панель `ResearchMenu` — **только статус**. Запуск/очередь исследований **реализованы** на зданиях:
`researches: [ "id", … ]` у `BuildingDefinition` → `ResearchProductionItem` в очереди производств
здания (общая очередь, отмена с возвратом, кнопки в UI выбранного здания через `ProductionAbility`).
По завершении — грант владельцу, синк, пересчёт атрибутов. Демо: `data/reignofnether/research/example_research.json`
привязано к `barracks.json`.

---

## 3. Ключевые классы (карта правок)

- `unit/interfaces/Unit.java` — единственный контракт юнита: роль-флаги `isWorker()/isAttacker()/
  isRangedAttacker()/isHero()`, `isRtsUnit()` (у код-юнитов `true`), хелпер `Unit.isUnit(Object)`,
  goal-accessors (воркер/аттакер), attacker/hero `default`-методы, статики `tickWorker/tickAttacker/tickHero`,
  `getFlyingMaxY()`, `setStatsForLevel(boolean)`, геттеры/сеттеры героя (`getExperience/getMana/getMaxMana/
  getSkillPoints/isRankUpMenuOpen/getHeroAbilityRanks/needsStatSync/…`).
- `unit/UnitDefinition.java` (Codec), `unit/UnitDefinitions.java` (реестр `reignofnether:unit`),
  `unit/UnitDefinitionRuntime.java` (create/spawn + applyAttributes + buildAbilities).
- `unit/interfaces/DefinedUnit.java` — id определения на мобе.
- `mixin/UnitMobMixin.java` — `@Mixin(Mob) implements Unit, DefinedUnit`; `@Unique`-состояние (id определения,
  роль-флаги, hero: опыт/мана/`needsStatSync`/ранги/заряды); `isRtsUnit()` = есть определение;
  `initialiseGoals()` по роли/флагам; `getAttack*/getGather*/getBuild*`; @Shadow `goalSelector`/`targetSelector`;
  герой применяет статы уровня 1 в `initialiseGoals`. `Unit.tick` → `tickWorker/tickAttacker/tickHero`.
- `mixin/UnitInventoryMobMixin.java` — сосуществует на `Mob` (тоже конструктор `(EntityType<? extends
  LivingEntity>, Level)`).
- `ability/AbilitySpec.java`, `ability/AbilityTypes.java`.
- `building/BuildingDefinition.java`, `BuildingDefinitions.java`; `buildings/JsonBuilding.java`,
  `buildings/JsonBuildingManager.java`, `production/JsonProductionItem.java`, `WorkerBuildMenu.java`.
- `building/addon/BuildingAddon.java`, `AddonSpec.java`, `AddonTypes.java`.
- Поток размещения: `building/BuildingServerboundPacket.java` (`resolveBuilding`, `placeBuilding`).
- Старт: `player/PlayerServerEvents.java` (`STARTING_ARMY`/`STARTING_WORKER_DEF` — **id определений**;
  `startRTS`, `startRTSBot`, `neutraliseUnitsOf`, `restoreGameModeOnLeave`).
- Регистрация датапак-реестров: `ReignOfNether#loadDatapacks` (`unit`, `building`, `faction`, `rts_buttons`).
- Пакеты: `registrars/PacketHandler.java` (Faction*, Research*, …).
- Реестры кода: `ReignOfNetherRegistries.BUILDING` (пуст, заморожен), `Buildings` (пуст).

---

## 4. Что сделано в этой сессии (журнал инкрементов)

**Роли юнитов — схлопывание в `Unit`** (`c9bff871`, `d0f257da`, `c2cdd370`, `599c46a3`, `62f38c92`,
`f179a8b0`, `4bdde13d`): роль-флаги на `Unit`, члены `WorkerUnit`/`AttackerUnit`/`RangedAttackerUnit`/
`HeroUnit` перенесены, под-интерфейсы удалены.

**Unit-on-Mob ядро** (`5f846d18`, `b68e714e`, `5bfe7cce`, `56f0ef95`, `63c952e4`, `944982f3`, `d4a5c50b`):
`UnitDefinition` (реестр `unit`), `Unit.isRtsUnit()`/`isUnit`, `@Mixin(Mob) UnitMobMixin` (включён),
~197 проверок `instanceof Unit` → `isRtsUnit()`, `UnitDefinitionRuntime` (атрибуты/масштаб/спавн), базовые goals.

**Goals из определения** (`13138eea`), **JSON-шаблоны юнитов** (`795f7f04`), **удаление код-юнитов**
`VillagerUnit`/`VindicatorUnit` + рендереры/модели/`EntityType`/спавн-яйца (`ce5197ea`, `e0c40e46`);
производство и старт спавнят юнитов по id определения (ванильные тела villager/vindicator).

**AbilitySpec + AbilityTypes** (`36fc5cae`).

**Каркас зданий** (`f8b530eb` `da0adedb` `e371df0e` `b45f617c` `de6c42ed` `bc824899`): `BuildingDefinition`
+ реестр, `JsonBuilding`/`JsonBuildingManager`/`JsonProductionItem`, поток размещения, `town_centre.json`,
меню воркера.

**Удаление код-зданий** (`d04f3a3e`, `821584e8`): `TownCentre`/`Barracks`/`TownCentrePlacement` и регистрации
удалены; `Buildings` пуст; ссылки перенаправлены; здания только из JSON.

**Тик data-driven юнитов** (`1712d129`): `Unit.tick` + ролевые тики через `UnitMobMixin`, мемоизация роли.

**Состояние героя** (`ec652683`): `UnitMobMixin` хранит опыт/ману/`maxMana`/очки навыка/ранги
(`Object2ObjectArrayMap<HeroAbility,Integer>`)/`needsStatSync`/«меню прокачки»/заряды для сейва; оверрайды
геройских геттеров/сеттеров `Unit`; при спавне `role: hero` применяет `setStatsForLevel(true)`.

**Схлопывание ролей** завершено: `Unit` — единственный интерфейс.

**Аддоны зданий (`addon/AddonSpec`, `addon/AddonTypes`, `addon/Addons`, `addon/NightSourceBuildingAddon`):**
`JsonBuilding` создаёт аддоны из `BuildingDefinition.addons()` и навешивает их через
`Building.addActiveAddon(...)`; движковый тип `reignofnether:night_source` зарегистрирован и прописан в
`town_centre.json`. Гейты `compileJava`/`validateMixins`/`runData` зелёные; `runClient` не проверялся.

**Исследования у зданий (`research/ResearchProductionItem`):** исследование — это `ProductionItem`, поэтому
делит очередь и UI производства здания. `JsonBuilding` добавляет по одному item на `BuildingDefinition.researches()`
(общий кэш по id для защиты от дублей между зданиями). Заодно починена сетевая идентификация производств
(`ProductionItem.getNetworkId`, дефолтная кнопка отмены, JSON-ветки в clientbound/serverbound пакетах) и
клиентская синхронизация JSON-зданий (`PLACE_JSON` + `JsonBuildingManager.getOrCreate`), плюс null-safe
`BuildingUtils.getKeyString` в HUD (раньше `BUILDING.getKey(jsonBldg).toString()` падал). Демо:
`research/example_research.json` + `barracks.json`. Гейты зелёные; **`runClient` не проверялся**.

**Персистентность JSON-зданий:** `BuildingSaveData` сохраняет `jsonDefinitionId` и восстанавливает через
`JsonBuildingManager.getOrCreate` (раньше `BUILDING.getKey(jsonBldg)` падал на сохранении, и здание терялось).
`JsonBuildingManager.reload` теперь не пересоздаёт существующие инстансы. Гейты зелёные.

**JSON-здания в командах/HUD:** `/rtsapi building place <id>` принимает id JSON-здания (suggestions +
`CommandsServerEvents.resolveBuilding`), а `Building.getDisplayName()` даёт имя для HUD/портрета/порядка
выделения — раньше у `JsonBuilding` имя было пустым, а `getKey(...).toString()` падал. Гейты зелёные.

**Апгрейды зданий (полностью):** `UpgradeSpec` + `BuildingDefinition.upgrades` (+`withUpgrade`); уровни
материализуются как `JsonBuilding`-варианты (`JsonBuildingManager.getLevel/getOrCreateLevel`), placement
переключается на вариант через `BuildingPlacement.applyUpgradeBuilding` (структура/имя/иконка/maxHealth/
populationSupply/production/researches/addons — всё per-level). `JsonUpgradeProductionItem` в очереди здания,
синк `BuildingAction.SET_UPGRADE_LEVEL` + `CHANGE_STRUCTURE`, уровень сохраняется. Демо: `barracks.json` →
«Barracks II» (+maxHealth, +night_source — замена аддонов). Гейты зелёные; **`runClient` не проверялся**.

**Капитолий-специфика JSON-зданий:** `BuildingDefinition` получил `canAcceptResources` и `buildTimeModifier`;
`JsonBuilding` их применяет. `town_centre.json` → `canAcceptResources: true`, `buildTimeModifier: 0.328`
(раньше JSON-столица НЕ принимала ресурсы — экономика была сломана). Гейты зелёные.

**Флаги зданий в JSON:** `BuildingDefinition.Flags` (Codec, под `"flags"`): `canAcceptResources`,
`buildTimeModifier`, `captureRange`, `capturable`, `invulnerable`, `repairable`, `repairTimeModifier`,
`drawAggro`, `scaffoldFill`, `scaffoldBlock`, `portrait`; `JsonBuilding` их применяет, `drawAggro` теперь
учитывается для любого здания (`MiscUtil`), `Building.ScaffoldFill` стал `StringRepresentable`.
`town_centre.json` — под `flags` (+`icon`/`portrait` polished_granite). Гейты зелёные.**`runClient` не проверялся**.

**Мелкие остатки:** JSON-здания добавлены в suggestions `type` селектора зданий
(`BuildingSelectorOptions`); «лишние здания» в тултипе группового выделения HUD теперь используют
`Building.getDisplayName()` (у `JsonBuilding` раньше пропадали). Гейты зелёные.**`runClient` не проверялся**.

**Data-driven способности (каркас):** `AbilityTypes.register` теперь вызывается —
`ability/BuiltInAbilities.init()` (из конструктора мода) регистрирует движковый тип `reignofnether:heal`
(`SimpleHealAbility`, params `amount`; действие `UnitAction.HEAL_SELF`). Демо — у `skeleton_unit`. Убрана
неоднозначная перегрузка `AbilityTypes.register(BiFunction)`. Гейты зелёные.**`runClient` не проверялся**.

**AbilitySpec доведён:** добавлены поля `range`/`radius`/`canTargetEntities`/`oneClickOneUse`; `Ability` —
`passive` (пассивки не рисуют кнопку в `Abilities.getButtons`) и `canTargetEntities` больше не `final`.
`buildAbilities` прокидывает все общие поля (числа `0`/`false` = дефолт класса; `mana` — в `HeroAbility`).
Гейты зелёные.**`runClient` не проверялся**.

**Пассивные способности (хук):** `Ability.tickPassive(Unit)` (по умолчанию пусто) зовётся из `Unit.tick`
на сервере для всех `passive`-способностей; активный диспатч (`UnitActionItem`) пропускает пассивки, а
`buildAbilities` ставит `passive` только из `true`. Движковый пример — `reignofnether:regeneration`
(`RegenerationAbility`, params `amount`/`interval`), демо у `skeleton_unit`. Гейты зелёные.**`runClient` не проверялся**.

**`UnitDefinition.inherits`:** наследуемые поля сделаны `Optional`, добавлен `withInherited` (мерж: атрибуты
по ключам, остальное «ребёнок или родитель»); `UnitDefinitions.resolve(registryAccess, id)` резолвит
транзитивно с защитой от циклов; читатели (`UnitDefinitionRuntime`, `ProductionPlacement.produceUnit`,
`UnitMobMixin`) переведены на resolved-определения. Заодно: `produceUnit` теперь зовёт
`UnitDefinitionRuntime.buildAbilities` (раньше юниты из JSON-зданий **не получали способности** от
определения). Демо: `skeleton_marksman` ← `skeleton_unit`. Гейты зелёные.**`runClient` не проверялся**.

**Аудит data-driven путей (багфиксы):** (1) `ProductionPlacement.produceUnit` не надевал `equipment` —
произведённый лучник не держал лук (ranged-goal не срабатывал); вынесено в
`UnitDefinitionRuntime.applyEquipment` и вызывается в обоих путях спавна. (2) Завершение производства
слало `getItemName()`, а клиент сравнивал `getNetworkId()` — рассинхрон; теперь слать `getNetworkId()`.
(3) `BuildingServerEvents.placeBuilding` падал на null-здании (неизвестный id) — добавлен guard.
(4) `WorkerBuildMenu` на удалённом клиенте был пуст (`JsonBuildingManager.all()` server-only) — фолбэк на
синхронизированный датапак-реестр. (5) `JsonProductionItem` показывал иконку меча и голый id — теперь
имя/иконка/тултип из определения. Гейты зелёные.**`runClient` не проверялся**.

**Аудит-2 (герой/аддоны/апгрейды):** найдена **регрессия персистентности юнитов** — удалённые код-классы
вызывали `Unit#addUnitSaveData` из `addAdditionalSaveData`, а `UnitMobMixin` — нет, поэтому у data-driven
юнитов терялись owner/anchor/hero-состояние **и id определения** при перезагрузке мира. Добавлены
`@Inject` в `UnitMobMixin` (зовёт `addUnitSaveData`/`readUnitSaveData`), а сам `addUnitSaveData` теперь
пишет `unitDefinitionId`. Nether-зоны при загрузке восстанавливаются отдельно (`NetherZoneSaveData` +
`getActiveAddon(NetherConvertingAddon)`), addon-lifecycle там ок. В `changeStructure` добавлен warning,
если структура апгрейда имеет другой габарит (границы placement не пересчитываются). Гейты зелёные.**`runClient` не проверялся**.

**Аудит-3 (JSON-здания/юниты в UI):** проверены билдинг-интеграции (place/валидаторы/fog/миникарта/портрет/
селекторы) — все используют здания обобщённо, code-key NPE нет. Найден видимый дефект имён **юнитов**: HUD
показывал имя ванильного тела (`Skeleton`) вместо `UnitDefinition.name` (напр. «Skeleton Marksman»).
`MiscUtil.getSimpleEntityName` теперь берёт имя из resolved-определения. Гейты зелёные.**`runClient` не проверялся**.

**`rts_buttons` для JSON-контента:** маппинг кнопок больше не привязан к код-реестру зданий/телам.
`data/<ns>/reignofnether/custom_button_mappings.json` получил секцию `unit_definitions`; `buildings`
ключуется по **id** (код-ключ или id определения JSON), юниты — сначала по id определения
(`unit_definitions`), затем по телу (`entities`). Сервер/клиент/пакет переведены на id-ключи
(`BuildingUtils.getResourceId`, `CustomButtonClientEvents.unitDefinitionMappings`, новый case 5 пакета).
Гайд — `_GUIDES/10_custom_buttons.md`. Гейты зелёные.**`runClient` не проверялся**.

**Типизированные params способностей:** `AbilityParam` (число или строка, `Codec.either`); `AbilitySpec.params`
теперь `Map<String,AbilityParam>` с геттерами `param(double)`/`stringParam`/`resourceParam` (парсит
`ResourceLocation`). Демо: `reignofnether:summon` (`SummonUnitAbility`, `params.unit`/`count` — спавн юнита по
определению, новое `UnitAction.SUMMON_UNIT`), `reignofnether:heal` умеет `params.sound`. Демо в
`skeleton_unit`. Гейты зелёные.**`runClient` не проверялся**.

**Ranged-юнит (projectile):** `UnitDefinition` получил `equipment` (предмет в руку) и `projectile`
(`ProjectileSpec`: entity/velocity/damage/inaccuracy); `UnitMobMixin.performUnitRangedAttack` (был no-op)
спавнит снаряд на сервере (owner, урон = `damage` или `getUnitAttackDamage()`), дефолт — стрела.
Демо `skeleton_unit.json` (база skeleton, лук+стрела) в производстве казармы. Гейты зелёные;**`runClient` не проверялся**.

**Обобщение аддонов:** движковые типы `range_indicator`/`garrison`/`nether_converting` добавлены
(`RangeIndicatorBuildingAddon`/`GarrisonBuildingAddon`/`NetherConvertingBuildingAddon`) и регистрируются в
`Addons.init()`; `BuildingAddon` получил lifecycle-хуки `onBuildingBuilt`/`onBuildingTick`, которые
`Building.onBuilt`/`Building.tick` вызывают у всех **уникальных** аддонов (важно: имена хуков не совпадают
с `Building.onBuilt`/`tick`, иначе `CustomBuilding` как addon рекурсировал бы). Демо: `garrison` на
`barracks.json`. Гейты зелёные; **`runClient` не проверялся**.

**Багфикс JSON-зданий:** `JsonBuilding` применяет `populationSupply` и `requiredResearch` из определения
(раньше — нет: JSON-капитолий не давал лимит армии, а `requiredResearch` игнорировался). Гейты зелёные.

---

## 5. Что осталось (по приоритету)

1. ✅ **Аддоны доведены и обобщены:** `Addons.init()` регистрирует движковые типы `night_source`,
   `range_indicator`, `garrison`, `nether_converting`; добавлены lifecycle-хуки (`onBuildingBuilt`/
   `onBuildingTick`). Демо: `night_source` (town_centre), `garrison` (barracks).
2. ✅ **Исследования у зданий** (`ResearchProductionItem`, демо `example_research`). ✅ **Апгрейды (полностью)**
   (`UpgradeSpec`/`upgrades` + `JsonBuilding`-варианты на уровень; per-level производство/исследования/аддоны/
   способности/имя/иконка/структура). Дальше по зданиям:
   - Флаги здания из JSON (`flags`): `canAcceptResources`, `buildTimeModifier`, `captureRange`, `capturable`,
     `invulnerable`, `repairable`, `repairTimeModifier`, `drawAggro`, `scaffoldFill`, `scaffoldBlock`,
     `portrait`, `foundationYLayers`. Не портировано: милиция (система удалена).
3. ✅ **Ranged-юнит:** `equipment` + `projectile` (`ProjectileSpec`) в определении; спавн в
   `UnitMobMixin.performUnitRangedAttack`; демо `skeleton_unit` (лук+стрела) в казарме.
4. **lang-дочистка:** `entity.reignofnether.villager_unit*` (используется тултипами `VillagerProd`/`VindicatorProd`);
   в остальных локалях — по желанию.
5. **`runClient` — НИ РАЗУ не запускался с этими изменениями.** Всё клиентское (рендер юнитов/зданий теперь
   ванильный, меню фракций/исследований, размещение, меню воркера/способностей, герои) проверено только
   сборкой и `test`. **Обязательна ручная проверка** владельцем — сценарий одного сеанса:
   [`RUNCLIENT_CHECKLIST.md`](RUNCLIENT_CHECKLIST.md).
6. ✅ **Документация актуализирована** (2026-10-09): `docs/STATUS.md`, `docs/README.md` переписаны;
   гайды `_GUIDES/` `00/04/06/08/09` обновлены под data-driven; исторические снимки
   (`FEATURES`/`INTRUSION_AUDIT`/`CLEAN_FORK`/`PORT_STATUS`/`AGENT_HANDOFF`/`BUGS_RUNCLIENT`) помечены.
   Осталось: `CONTENT_JSON_PLAN.md` — сверять по мере продвижения.

---

## 6. Трапы и грабли (важно)

- **CRLF.** Большинство `.java` хранят CRLF (`.md` в `docs/` — LF). Инструменты могут переписать файл в LF →
  дифф «весь файл». Перед коммитом сверять `git diff --numstat` и `git diff --ignore-cr-at-eol --numstat`
  (должны совпадать). Правка CRLF-файла: нормализовать в LF → редактировать → вернуть CRLF.
- **Скриптовые regex-замены.** Скрипты дважды ломали код: (а) заворачивали хелперы в бесконечную рекурсию;
  (б) цепляли идентификаторы, начинающиеся с имени типа (`UnitInventory`, `UnitRangedAttackGoal`,
  `UnitBowAttackGoal`) из-за `instanceof Unit`-паттерна. После массовых замен проверять
  `Select-String ... 'Unit\.is\w+\(.*\)[A-Za-z_]'` и `'\(Unit\)\w'`, затем перекомпилировать.
- **`@Mixin(Mob.class)`.** `UnitMobMixin` и `UnitInventoryMobMixin` сосуществуют; каждому нужен конструктор
  `(EntityType<? extends LivingEntity>, Level)`. Mixin-поля — `@Unique`, без инициализаторов (ленивая
  инициализация в геттерах).
- **Датапак-реестры** (`unit`, `building`, `faction`, `rts_buttons`) регистрируются в `loadDatapacks` и
  синхронизируются клиентам. `ReignOfNetherRegistries.BUILDING` (кодовый) **заморожен** к моменту загрузки
  датапака — не писать туда из JSON-логики. У `JsonBuilding` нет код-ключа, поэтому нейтрально к null:
  `ReignOfNetherRegistries.BUILDING.getKey(jsonBuilding) == null` (использовать `BuildingUtils.getKeyString`).
- **Производство/исследования по сети** идентифицируются `ProductionItem.getNetworkId()`, а не код-реестром
  `PRODUCTION_ITEM` (JSON/исследования там не зарегистрированы). При добавлении новых `ProductionItem`
  переопределять `getNetworkId`, если предмет не в код-реестре.
- **`validateMixins` не ловит часть mixin-ошибок** (`@Shadow`/`@Redirect`, `Invalid LVT row`) — они всплывают
  в рантайме. **`runServer` в этой среде не запускать** (dev-сервер печатает `Done (...)`, но сам не
  завершается — висит до ручного вмешательства владельца). Надёжный оставшийся гейт — `test`
  (кодеки/схемы) и ручной `runClient` по `RUNCLIENT_CHECKLIST.md`.
- **`git show <ref>:<path> > file`** под PowerShell 5.1 пишет UTF-16LE → javac `unmappable character`.
  Только `cmd /c "git show ... > file"`.
- **Лимит армии** — база 1, прирост даёт столица (`populationSupply`). Цены — на определении юнита
  (здание может переопределить; пока не реализовано).

---

## 7. Инвентарь файлов (новые/удалённые в сессии)

**Новые:**
- `research/`: Research, ResearchCondition (Codec), ResearchType, ResearchAttributeModifier, ResearchRegistry,
  ResearchSaveData, ResearchUtils (+ client mirror), ResearchClientEvents, ResearchClientboundPacket,
  ResearchDefinitionsClientboundPacket, ResearchJsonLoader, ResearchCommand, ResearchServerEvents, ResearchMenu,
  ResearchAttributeApplier, ResearchProductionItem.
- `faction/`: StartingUnit, Faction (Codec), FactionRegistries, FactionClientboundPacket, FactionClientEvents,
  FactionMenu, FactionCommand, FactionServerEvents.
- `unit/`: UnitDefinition, UnitDefinitions, UnitDefinitionRuntime; `interfaces/DefinedUnit`.
- `ability/`: AbilitySpec, AbilityParam, AbilityTypes, BuiltInAbilities, SimpleHealAbility, RegenerationAbility,
  SummonUnitAbility.
- `building/`: BuildingDefinition, BuildingDefinitions, UpgradeSpec, WorkerBuildMenu; `buildings/JsonBuilding`,
  `buildings/JsonBuildingManager`, `buildings/JsonUpgradeProductionItem`;   `production/JsonProductionItem`;
  `addon/AddonSpec`, `addon/AddonTypes`, `addon/Addons`, `addon/NightSourceBuildingAddon`,
  `addon/RangeIndicatorBuildingAddon`, `addon/GarrisonBuildingAddon`, `addon/NetherConvertingBuildingAddon`.
- `mixin/`: UnitMobMixin.
- Data JSON: `data/reignofnether/faction/villagers.json`, `unit/villager_unit.json`, `unit/vindicator_unit.json`,
  `building/town_centre.json`, `building/barracks.json`.

**Удалено:** `VillagerUnit`, `VindicatorUnit`, их рендереры/модели/`EntityType`/спавн-яйца;
`VillagerUnitProfession`(+Layer); `WorkerUnit`/`AttackerUnit`/`RangedAttackerUnit`/`HeroUnit` (интерфейсы);
`TownCentre`/`Barracks`/`TownCentrePlacement`; `ArmorStandMixin`; пакет `gamemode/`; 65 NBT-структур
(оставлены `town_centre.nbt`/`barracks.nbt`) и `data/reignofnether/maps`; `RandomItemDropRule`; читы; песочница.

---

## 8. Ключевые коммиты (ориентир)

`668a6877` (багфиксы 2–5) · `e87b66db` (сужение ванили) · `398bc780`…`9e44abf9` (исследования 1–4) ·
`35ea74b9`/`6c8c8a43` (планы) · `5f846d18`/`b68e714e` (UnitDefinition, isRtsUnit) · `4bdde13d`
(схлопывание в Unit) · `13138eea` (goals из определения) · `795f7f04` (JSON-шаблоны юнитов) ·
`ce5197ea` (удаление код-юнитов) · `36fc5cae` (AbilitySpec) · `f8b530eb`/`da0adedb`/`e371df0e`
(каркас зданий) · `b45f617c` (меню воркера) · `de6c42ed` (поток размещения) · `bc824899`
(`town_centre.json`) · `d04f3a3e`/`821584e8` (удаление код-зданий) · `1712d129` (тик юнитов) ·
`ec652683` (состояние героя).

---

## 9. Сессия 2026-10-09 (аудит JSON-рантайма + каркас)

Ветка `wip/stage-d-deletions`; коммиты `21f3f1eb`…`66d22e2d` (12 шт). Гейты `compileJava`/
`validateMixins`/`runData` зелёные после каждого; `runClient` по-прежнему не запускался.

### 9.1 Найденные и исправленные баги (важно — рантайм JSON-контента)

Это первый проход, который системно искал регрессии от удаления код-классов героев/зданий. Всё
найдено чтением (без `runClient`), исправления компилируются, но **в игре не проверялись**.

1. **JSON-здание исчезало сразу после постановки** (`21f3f1eb`). У JSON-здания не был заполнен
   `startingBlockTypes` (его выводили только код-здания и `CustomBuilding`), поэтому новый placement
   ничего не ставил в `blockPlaceQueue`, а `BuildingPlacement.shouldBeDestroyed()` при
   `getBlocksPlaced() <= 0` на первом серверном тике удалял здание. Теперь нижний слой NBT задаёт
   `startingBlockTypes` (по образцу `CustomBuilding`).
2. **Воркер не мог добывать вообще** (`6c1ac8b9`). `ron$maxResources` нигде не присваивался (=0),
   `Unit.atMaxResources` = «сумма ≥ 0» всегда true → `GatherResourcesGoal.isGathering()` всегда false.
   Добавлено поле `carryCapacity` в `UnitDefinition` (дефолт 100 для worker; у удалённого
   `VillagerUnit` было 100), `getMaxResources()` выводится из определения, порог автодропа = половина
   ёмкости (дефолт 50 — как было).
3. **NPE в подсчёте населения/HUD** (`32045f99`). `Unit.getCost()` возвращал null у data-driven
   юнитов, а `unit.getCost().population` читается в `UnitServerEvents/UnitClientEvents`,
   `UnitSyncClientboundPacket`, `CursorClientEvents`, `ObserverPlayerDisplay`. Теперь стоимость берётся
   из определения (никогда не null).
4. **Инертные юниты после перезахода** (`b4cee1db`). На сервере `initialiseGoals()` звался только при
   спавне; загруженный из сейва юнит не имел целей, а `tickWorker` затем падал бы. Теперь `initialiseGoals`
   зовётся в конце `readAdditionalSaveData` (идемпотентно). Заодно null-guard'ы для опциональных
   worker-целей (`canGather` без `canBuild` и т.п.).
5. **`inherits` игнорировался для роли/целей** (`a15532f8`). `ron$role()` и `initialiseGoals()` читали
   определение из реестра напрямую, а не через `UnitDefinitions.resolve`, поэтому `skeleton_marksman`
   (наследует ranged-роль у `skeleton_unit`) становился melee с melee-целями.
6. **Произведённые юниты были бесплатны и мгновенны** (`f30d926f`). `JsonProductionItem` использовал
   стоимость здания (у демо-зданий её нет → 0), а не юнита; теперь берёт `cost`/`population` из
   `UnitDefinition`. `isTypeOf` у JSON-зданий сравнивает по `definitionId` (апгрейд больше не ломает
   проверки «есть готовое такое здание»). Убраны полоски здоровья над юнитами (по плану).

7. **У юнитов не было способностей на клиенте** (`66d22e2d`). Сервер синкает только кулдауны/заряды «по индексу», а клиент abilities не строил → кнопки способностей у JSON-юнитов не появлялись.
   Теперь `UnitClientEvents.onEntityJoin` строит их из синхронизированного определения (если список ещё пуст).

### 9.2 Новые возможности каркаса

- **`costOverride`** (`cb00550d`): `production: [ "id", { "unit": "id", "costOverride": { … } } ]`
  (`ProductionSpec`, either-кодек как у `AbilityParam`).
- **`reignofnether:resource_generator`** (`f30d926f`, план H.9): аддон здания
  `{ "resource": "wood", "amount": 5, "interval": 100, "capacity": 500 }`. `AddonSpec.params`
  стали типизированными (число или строка), как у способностей.
- **`type: equip` у исследований** (`725e89f7`): `equip: [ { "item": "…", "slot": "mainhand", "unit": "…" } ]`
  выдаёт снаряжение юнитам владельца (`ResearchEquipApplier`). Фильтры исследований (`attributes`/`equip`)
  теперь матчатся и по id определения юнита, а не только по телу.
- **namespace структур** (`c24ece5f`): JSON-здание грузит `.nbt` по полному id (`<ns>:structures/<path>.nbt`),
  а не только из `reignofnether`; отсутствующий файл — лог + пустой список вместо NPE.

### 9.3 Что осталось (после этой сессии)

1. **`runClient` — по-прежнему НИ РАЗУ не запускался.** Обязательна ручная проверка владельцем; все
   правки выше проверены только компиляцией/`runData`. Особенно: постановка JSON-здания (не исчезает),
   добыча воркером, население/производство, апгрейд «Barracks II», `resource_generator`, `equip`.
2. **Кодек `ProductionSpec`** (either-кодек `production`) не проверялся рантаймом: при старте сервера
   он парсит `barracks.json`/`town_centre.json`. Паттерн тот же, что у `AbilityParam` (работает), но
   подтвердить в игре нужно.
3. ✅ **K4 (данные воркера)** — блок `worker` (`gatherable`/`buildSpeed`/`carryCapacity`), см. §10.
4. ✅ **K5 (прокачка героя)** — `hero: { maxLevel, expReqMultiplier }` (`2160358c`).
5. ✅ **K6 (submenu-раскладка `row`/`col`)** — тип `reignofnether:menu`, см. §10.
6. lang-дочистка `entity.reignofnether.villager_unit*`; осиротевшие `.nbt` (~65) в
   `assets/reignofnether/structures/` — не удалялись (владелец: «не срочно»).

---

## 10. Сессия 2026-10-09 (вторая): K4/K5/K6 + тест-гейт

Гейты: `compileJava` ✅ · `validateMixins` ✅ (49 точек/31 миксин) · `runData` ✅ · **`test` ✅ (новый)**:
7 тестов. `runClient` по-прежнему не запускался. Коммитов в этой сессии нет (рабочее дерево: правки
K6 лежали незакоммиченными с прошлого раза + код и данные K4).

### K6 — data-driven меню способностей (`reignofnether:menu`)

* `MenuEntrySpec` — элемент меню: инлайн `ability` (может быть вложенным `menu`) **или** `command`
  (`attack`/`stop`/`hold`/`build`/`gather`/`garrison`/`ungarrison`), плюс `row`/`col` (0-based).
* `AbilitySpec` получил `name`/`icon` (кнопка меню) и `submenu`; кодек рекурсивный —
  `submenu` читается через `Codec.lazyInitialized(() -> MenuEntrySpec.CODEC)` (цикл `AbilitySpec` ↔
  `MenuEntrySpec`), `MenuEntrySpec.CODEC` ссылается на `AbilitySpec.CODEC`.
* `DataMenuAbility extends MenuAbility`: строит детей из `submenu`, помнит `row`/`col` на ребёнка,
  отдаёт их рендеру через `Ability.getSubButtonPosition(Ability)`. Вложенный `menu` даёт меню-в-меню.
* `HudClientEvents.renderAbilitySubmenu`: параллельный `subPositions` (по кнопкам) — явные `row`/`col`
  перебивают авто-раскладку (колонка сдвигается на 1: колонка 0 — кнопка «Назад»).
* Зарегистрировано в `BuiltInAbilities.init()` (`reignofnether:menu`); `abilities.reignofnether.menu`
  добавлен в `en_us.json`. Демо — `skeleton_unit.json`; гайд — `_GUIDES/04_ability.md`.
* Элемент меню не несёт собственных `label`/`icon` (кнопку рисует класс способности). Виды элементов:
  инлайн `ability`, `command`, `building` (кнопка постановки здания — курируемый список для воркера и
  не только). «Производство» элементом меню **не** является: состав производства перечисляет само здание.

### K4 — воркер данными (блок `worker`)

```json
"worker": { "gatherable": ["food", "wood", "ore"], "buildSpeed": 1.0, "carryCapacity": 100 }
```

* `UnitDefinition.WorkerSpec` (`gatherable`: `List<ResourceName>`, `buildSpeed`, `carryCapacity`).
  `carryCapacity` **перенесён** с верхнего уровня в этот блок (иначе рекорд превышал 16 полей
  `RecordCodecBuilder.group`). `ResourceName` стал `StringRepresentable`.
* `Unit.getGatherableResources()`/`getBuildSpeed()` (default = все три/1.0), переопределены в
  `UnitMobMixin` из resolved-определения.
* Гард в `GatherResourcesGoal.setTargetResourceName` (нельзя выбрать/добывать ресурс вне списка),
  цикл `UnitActionItem` (TOGGLE_GATHER_TARGET) идёт по `[NONE, ...gatherable]`.
* `BuildingPlacement.handleServerTick`: вклад воркеров — сумма `buildSpeed` (было число воркеров);
  эффект спешки по-прежнему = «+1 воркер». Дефолт 1.0 сохраняет прежнюю формулу.
* Демо/данные: `villager_unit.json` (тот же `carryCapacity: 100`, теперь в блоке `worker`);
  гайд `_GUIDES/02_unit.md`.

### Новый гейт `test`

`src/test/java/com/solegendary/reignofnether/DataCodecTest.java` декодирует **все** поставляемые
`unit`/`building`/`faction` JSON их кодеками (`JsonOps`) и точечно проверяет `worker`-блок и `menu`
(вложенность/`command`/`row`/`building`). Ловит кодек-/схемные регрессии без клиента/сервера. Запускать
`test` вместе с остальными гейтами (`compileJava`/`validateMixins`/`runData`).

---

## 11. Сессия 2026-10-09 (третья): ревью меню/гейтов + чек-лист одного `runClient`

Гейты: `compileJava` ✅ · `validateMixins` ✅ (49 точек/31 миксин) · `runData` ✅ · `test` ✅.
`runClient` по-прежнему не запускался — вместо этого сделан сценарий одного сеанса:
**`docs/RUNCLIENT_CHECKLIST.md`** (идти сверху вниз, все новые фичи проверяются за один прогон).

**Найдено и исправлено (код-ревью этого шага):**

1. **Меню из одних `building`- или `command`-элементов было невидимым.** `MenuAbility.getButton`
   передавал в `isHidden` проверку `subAbilities.isEmpty()`, а `DataMenuAbility` кладёт в `subAbilities`
   только инлайн-способности (`building`/`command` там не появляются) → кнопка меню пряталась навсегда.
   Введён `MenuAbility.hasNoEntries()` (default — `subAbilities.isEmpty()`), `DataMenuAbility` его
   переопределяет на `entries.isEmpty()`.
2. **Общие синглтоны `CommandAbilities` больше не попадают в `subAbilities`.** Раньше каждый элемент
   `command` добавлялся в `subAbilities` того же меню, то есть в общий плоский список способностей
   юнита (`Abilities.get()`), где лежат одни и те же статические инстансы для всех меню/юнитов.
   Теперь в `subAbilities` идут только инлайн-`ability` (им действительно нужен серверный диспатч);
   `command`-кнопки и так строятся из `entries` и отправляют приказ кликом.
3. **Research-гейт у элементов меню.** `DataMenuAbility.getSubButtons` теперь навешивает
   `Abilities.applyResearchGate` на инлайн-способности с `requiredResearch` (метод стал `public`).
   Ограничение: у **код-меню** (`MenuAbility`/`BuildMenuAbility`) под-кнопки гейтятся только на верхнем
   уровне (`Abilities.getButtons`), как и раньше.

**Демо/данные:** `barracks.json` использует `costOverride` у `skeleton_marksman` (food 55 / 8 с вместо
определения 80 / 18 с) — теперь объектная форма `production` проверяема в игре из коробки.

**Документация:** `docs/RUNCLIENT_CHECKLIST.md` (новый), `_GUIDES/04_ability.md` (`building`-элемент и
гейт), `docs/CONTENT_JSON_PLAN.md` (пример `submenu` приведён к реализации), `docs/STATUS.md`,
`docs/README.md` (карта документов), `_GUIDES/03_building.md` (`foundationYLayers` в списке флагов),
`_GUIDES/00_обзор.md`, и `.agents/skills/*` (гейт `test`, чек-лист вместо `runServer`).

**Известные ограничения (не баги, зафиксировано):**

* Неизвестные поля JSON теперь **не игнорируются молча** (см. §12): `ContentValidator` +
  `ContentValidationReloadListener` дают `[content-validation] <файл> '<путь>': unknown field 'x' (accepted: …)`
  в логе при загрузке мира и на `/reload`; `ContentValidationTest` ловит это в гейте `test`. Старый
  верхнеуровневый `carryCapacity` теперь диагностируется явно — писать `worker.carryCapacity`.
* `worker.buildSpeed` учитывается при **строительстве** (`BuildingPlacement.handleServerTick`), но не при
  ремонте (`BuildRepairGoal` использует свой темп).
* `gatherable` не может включить `EMERALD` (исключён из цикла как специальный ресурс).
* Под-кнопки **код**-меню не гейтятся по research (у data-меню — гейтятся).

---

## 12. Сессия 2026-10-09 (четвёртая): строгая валидация определений

Проблема: кодеки `RecordCodecBuilder` у `UnitDefinition`/`BuildingDefinition`/`Faction` **молча
отбрасывают** неизвестные поля — опечатка (`"carryCapcity"`) или поле из старой схемы
(верхнеуровневый `carryCapacity`) не давали ни ошибки, ни предупреждения, а контент просто работал
не так, как ожидал автор.

**Что сделано:**

| Класс | Роль |
|---|---|
| `data/ContentValidator` | обход JSON по **записи-получателю** (`UnitDefinition`/`BuildingDefinition`/`Faction` и все вложенные record'ы). Имена полей берутся из компонентов записи, поэтому новое поле в записи автоматически становится разрешённым. Возвращает по одному сообщению на проблему с путём внутри файла. |
| `data/ContentValidationReloadListener` | `ResourceManagerReloadListener`, зарегистрирован рядом с `ResearchJsonLoader` в `ReignOfNether#reloadListener`. Проходит `unit`/`building`/`faction` во всех датапаках и пишет ошибки в лог с именем файла. |
| `test/ContentValidationTest` | 9 тестов: поставляемые файлы чисты; опечатка на корне/во вложенном объекте/в элементе списка/в меню — репортится; свободные map (`attributes`/`params`/`name`) — нет; имена полей `faction` (`food`/`starting_units`) — ок; обе формы `production` — ок; скаляр вместо объекта/массива — репортится. |

**Правила схемы:** только **имена** полей; значения/диапазоны/обязательность — по-прежнему на кодеке.
Свободные `Map` (`attributes`, `params`, локализованный `name`) принимают любые ключи.
`JSON_FIELD_NAMES` покрывает немногочисленные расхождения «компонент ≠ JSON-имя»:
`Faction` (`nameKey`→`name`, `startingUnits`→`starting_units`, `startingFood/Wood/Ore/Emerald`→`food/wood/ore/emerald`),
`StartingUnit` (`entityType`→`unit`), `ResearchCondition` (`researchId`→`research`). Ренейм поля **в кодеке**
(а не в записи) надо дублировать в эту таблицу — это единственное место, которое нельзя вывести из кода.

**Порядок загрузки:** датапак-реестры читает Minecraft, и только потом идут reload-листенеры, а кодеки
ленивы к лишним ключам — поэтому «пропустить» невалидный файл нельзя, и валидация **сообщает**, а не
блокирует. Жёсткий гейт — `ContentValidationTest` (файл с опечаткой валит `test`).

**Проверено:** временно добавили `"carryCapcity": 100` в `villager_unit.json` → `test` упал с
`unit/villager_unit.json 'worker': unknown field 'carryCapcity' (accepted: buildSpeed, carryCapacity, gatherable)`;
файл восстановлен, гейты снова зелёные.

**Не покрыто:** JSON исследований (`data/<ns>/research/*.json` разбирает вручную `ResearchJsonLoader`,
не кодек) — там лишние поля по-прежнему игнорируются. Кандидат на такую же проверку со своей таблицей.
