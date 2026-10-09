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

Гейты на каждом шаге: `compileJava` → `validateMixins` → `runData`.
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
  "population": 1, "requiredResearch": [ { "research": "myns:x", "invert": false } ] }
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
- `inherits` (id другого определения) — зарезервировано, **разрешение наследования НЕ реализовано**.

### Способности — класс в коде + инстанс в JSON
- Класс регистрируется в `AbilityTypes.register(id, factory)` (id = `type` в JSON). Движковые типы —
  `ability/BuiltInAbilities.init()` (вызывается из конструктора мода); сейчас это `reignofnether:heal`
  (`SimpleHealAbility`, params `amount`), демо у `skeleton_unit`. Автор фракции регистрирует свои так же.
- Активная способность рассылается по `UnitAction`: `UnitActionItem` находит ability с `action == action`
  и зовёт `use(...)`; поэтому активному типу нужна **своя константа `UnitAction`**.
- `AbilitySpec` (Codec): `type` + `cooldown`/`mana`/`passive`/`requiredResearch`/`params`
  (`params` сейчас `Map<String,Double>` — только числа; расширять при необходимости).
- В `UnitDefinitionRuntime.buildAbilities` инстансы создаются и кладутся в `unit.getAbilities()`
  (из spec берутся только `cooldown`/`requiredResearch`).
- Конкретные «боевые» классы способностей пишет автор фракции — в каркасе один пример (`heal`).

### Здания — `data/<ns>/building/<name>.json` (реестр `reignofnether:building`)
```json
{ "structure": "reignofnether:barracks", "maxHealth": 150, "populationSupply": 0,
  "isCapitol": false,
  "flags": { "canAcceptResources": false, "buildTimeModifier": 1.0, "capturable": false,
             "invulnerable": false, "repairable": true, "drawAggro": true, "captureRange": 20 },
  "production": [ "reignofnether:vindicator_unit" ],
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
     `portrait`. Не портировано: милиция (система удалена).
3. ✅ **Ranged-юнит:** `equipment` + `projectile` (`ProjectileSpec`) в определении; спавн в
   `UnitMobMixin.performUnitRangedAttack`; демо `skeleton_unit` (лук+стрела) в казарме.
4. **lang-дочистка:** `entity.reignofnether.villager_unit*` (используется тултипами `VillagerProd`/`VindicatorProd`);
   в остальных локалях — по желанию.
5. **`runClient` — НИ РАЗУ не запускался с этими изменениями.** Всё клиентское (рендер юнитов/зданий теперь
   ванильный, меню фракций/исследований, размещение, меню воркера, герои) проверено только сборкой и
   `runServer`. **Обязательна ручная проверка** владельцем.
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
- **`validateMixins` не ловит часть mixin-ошибок** — всплывают в рантайме. Надёжный гейт — `runServer`/`runClient`.
- **`runServer`.** Запускать только через `background_process` monitor (`ready.pattern: "Done \\("`), затем
  `stop` в пределах 15 с. Не оставлять висящий процесс.
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
- `ability/`: AbilitySpec, AbilityTypes, BuiltInAbilities, SimpleHealAbility.
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
