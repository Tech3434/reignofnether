# Как создать свою фракцию с нуля

Практический гайд по mod Reign of Nether (NeoForge 1.21.1): что и где нужно менять, чтобы
заработала своя фракция со своими юнитами, строениями, исследованиями и стартовой позицией.

Все пути ниже относительно `src/main/java/com/solegendary/reignofnether/` внутри репозитория
`___temp/`, если не указано иное. Номера строк — на коммите `8323d802` (ветка `1.21.1-clean`).

---

## 0. Что мод называет «фракцией»

Это важно понять до того, как что-то писать.

**Реестра фракций нет.** `faction/Faction.java` — это обычный enum на шесть значений
(`VILLAGERS`, `MONSTERS`, `PIGLINS`, `NEUTRAL`, `RANDOM`, `NONE`), а всё поведение, которое
зависит от фракции, — это ручные `switch` по нему. В `api/ReignOfNetherRegistries.java`
регистрируются только `BUILDING`, `PRODUCTION_ITEM` и `DATA_TYPE` — реестра фракций среди них нет.
Ни datapack, ни конфиг, ни JSON фракции не описывают.

**Личность игрока — строка.** Ни UUID, ни объекта-игрока в сети нет: владелец везде хранится как
`String name` в `player/RTSPlayer.java:25`, и сравнение идёт через `String.equals`. Юнит хранит
`ownerName` в синхронизируемом `EntityDataAccessor`, здание — в поле `ownerName`.

Единственное, что действительно выглядит как реестр фракций, — таблица «строение → фракция →
хоткей» в `faction/FactionRegistries.java`, но она перечисляет только три контентные фракции и
заполняется вручную в методе `register()` (строки 31-89).

Отсюда практический вывод: **фракция — это не объект, а набор соглашений по коду.** Новая
константа enum требует правки во всех местах, где стоит `switch (faction)`, и их довольно много.

---

## 1. Сначала выберите масштаб задачи

| Вариант | Что означает | Объём |
|---|---|---|
| **A. Контент в существующей фракции** | Юниты/строения/исследования для `VILLAGERS`/`MONSTERS`/`PIGLINS` без новой константы `Faction` | §5, §6, §7, §8. Самый частый случай |
| **B. Отдельная константа `Faction`, не играбельная** | Своя «внутренняя» фракция: например для существ вне RTS-контроля. Никто не выбирает её через GUI | §2 + §5. Нужна, если требуется собственная иконка/цвет/логика |
| **C. Полноценная играбельная фракция** | Игрок выбирает её при старте матча | §2 целиком + §3, §4, §5, §6, §7, §8, §9 |

Вариант A — правильный по умолчанию: он не трогает код старта матча, сохранений и пакетов.

---

## 2. Вариант B/C: новая константа `Faction`

### 2.1 Обязательные правки

| Что | Файл:строка |
|---|---|
| Сама константа | `faction/Faction.java:3-10` |
| Поле `FactionRegister` + `switch` в `getRegister` | `faction/FactionRegistries.java:9-12, 22-29` |
| Запись в таблице «строение → хоткей» | `faction/FactionRegistries.java:31-89` (вызывается из `ReignOfNether.java:68`) |
| Какой юнит становится воркером фракции при старте | `player/PlayerServerEvents.java:463-474` |
| То же для ботов | `player/PlayerServerEvents.java:639-644` |
| Пул для `Faction.RANDOM` | `player/PlayerServerEvents.java:428-430`, клиентское зеркало `player/PlayerServerboundPacket.java:97-101` |
| Какое строение считается столицей на «готовой» стартовой позиции | `player/PlayerServerEvents.java:562-575` |
| Константа в enum действий старта | `player/PlayerAction.java:10-13`; диспетчеризация — `player/PlayerServerboundPacket.java:290-297`, отправка с клиента — `PlayerServerboundPacket.java:82-104` |
| Иконка фракции | `util/MiscUtil.java:993-1001` |
| Отображаемое имя | `util/MiscUtil.java:1012-1014` (по ключу `hud.faction.reignofnether.<имя>`) + §9 |
| Плитки на экране старта матча | `matchstart/MatchStartScreen.java:455` (`Faction[] order = {VILLAGERS, MONSTERS, PIGLINS, RANDOM}`) и `:481-499` |
| Цикл фракции в меню сценария | `scenario/ScenarioMenu.java:310-330` |
| Флаги «кто может строить» для кастомных строений | `building/custombuilding/CustomBuilding.java:93-95` (`buildableByVillagers/Monsters/Piglags`), NBT `:306-308, 326-328` |
| Выбор фракции для волн выживания | `survival/Wave.java:67-93`, `survival/spawners/*WaveSpawner.java` |
| Заголовок на титульном экране | `hud/TitleClientEvents.java:29-33, 37-54` |

Жёсткие ссылки, которые обходят `getFactionName()` и правятся руками:
`scenario/ScenarioMenu.java:323-328`, `sandbox/SandboxClientEvents.java:214-217`,
`survival/SurvivalClientEvents.java:137-144`.

### 2.2 Совместимость с сохранениями

`player/RTSPlayerSaveData.java:55, 98` сохраняет фракцию как строку и читает обратно через
`Faction.valueOf(name())`. **Переименование или удаление существующей константы ломает старые миры.**
Новые константы безопасны.

### 2.3 Что потребуется ещё

Своя фракция без собственного воркера и скаута не запустится: UI строителя зданий берётся из
`FactionRegistries` только в трёх местах, и все три — внутри рабочего юнита:

* `unit/units/villagers/VillagerUnit.java:341`
* `unit/units/monsters/ZombieVillagerUnit.java:215`
* `unit/units/piglins/GruntUnit.java:208`

(плюс `sandbox/SandboxClientEvents.java:71` для нейтральных кнопок). См. §4.

---

## 3. Стартовые позиции, карта, цвета

### 3.1 Стартовые позиции

`startpos/StartPos.java:19-46` — позиция хранит `BlockPos pos`, `Faction faction`, `String
playerName`, `int colorId`, `boolean ready`. Резервирование — из GUI или пакетом
`StartPosServerboundPacket.reservePos` (`startpos/StartPosServerboundPacket.java:92-124`).
Блоков-маркеров 16, по одному на `MapColor` (`registrars/BlockRegistrar.java:129-192`,
класс `blocks/RTSStartBlock.java` — без поведения, цвет из свойства блока `mapColor`).
Лимит `MAX_START_POSES = 16` (`startpos/StartPosServerEvents.java:37`).

Когда все игроки на позициях готовы, `StartPosServerEvents#onServerTick:175-226` запускает
отсчёт и вызывает `PlayerServerEvents.startRTS(...)` для каждого. Игроки на стартах одного цвета
автоматически становятся союзниками (`StartPosServerEvents.java:205-206`).

### 3.2 Карта матча — `rtsmap.json` в папке мира

Читается из `<world>/rtsmap.json` (`rtsmap/RTSMapInfoLoader.java:13-34`):

```java
private static final String FILE_NAME = "rtsmap.json";
public static RTSMapInfo load(ServerLevel level) {
    Path worldFolder = level.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath();
    Path jsonPath = worldFolder.resolve(FILE_NAME);
    if (!Files.exists(jsonPath)) return null;
    ...
}
```

Структура (`rtsmap/RTSMapInfo.java:9-38`): `name`, `author[]`, `description`, `version`,
`startPositions[]` (элементы `{id, x, y, z}`), `defaultMode`, `modes` (режим → список команд →
список id стартов). Метод `getTeams()` (`:41-67`) разворачивает `modes` в список координат.

Если файла нет — стартовые позиции берутся из `StartPosSaveData` и блоков-маркеров
(`startpos/StartPosServerEvents.java:239-281`).

Переключение режима — `/rtsapi player starting-teams-mode <mode>`.

**Важно:** файлы `src/main/resources/data/reignofnether/maps/*.json` (`test_map.json`,
`valley_of_deceit.json`) имеют другую схему (`name`, `players`, `description`, `gamerules`,
`image`) и **модом не читаются** — ссылок на них в `src/main/java` нет. Это примеры/заготовки,
не рабочий формат.

### 3.3 Цвета

Цвет — не свойство фракции, а свойство стартовой позиции. Таблица — `player/PlayerColors.java:73-96`
(16 выбираемых + 4 служебных для отношений). Выбор цвета игрока (`PlayerColors.java:123-153`):
кэш → `startPosColorId` → индекс в списке `rtsPlayers % 16` → белый.

Есть переключатель «цвета игроков / цвета союзов» (`ReignOfNetherClientConfigs.USE_PLAYER_COLORS`,
дефолт `false`).

Обратите внимание: таблица цветов продублирована ещё в двух местах —
`util/MiscUtil.java:1016-1051` (`ColorEntry[]`) и `startpos/StartPos.java:55-72` (`getHexColor()`).
Новый цвет требует правки **трёх** мест.

---

## 4. Воркер и скаут — обязательное условие

Фракция запускается только если у неё есть рабочий юнит (тот, кто строит) и разведчик.
Назначается в `player/PlayerServerEvents.java:463-474`:

```java
EntityType<? extends Unit> workerEntityType = switch (faction) {
    case VILLAGERS -> EntityRegistrar.VILLAGER_UNIT.get();
    case MONSTERS  -> EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get();
    case PIGLINS   -> EntityRegistrar.GRUNT_UNIT.get();
    default -> null;
};
EntityType<? extends Unit> scoutEntityType = switch (faction) {
    case VILLAGERS -> isDogPerson ? EntityRegistrar.SCOUT_DOG_UNIT.get() : EntityRegistrar.SCOUT_CAT_UNIT.get();
    case MONSTERS  -> EntityRegistrar.BAT_UNIT.get();
    case PIGLINS   -> EntityRegistrar.STRIDER_UNIT.get();
    default -> null;
};
```

Рабочий юнит реализует `WorkerUnit` — тогда он получает кнопки построек из
`FactionRegistries`. Скаут помечается через `Unit#isScout()`, который **захардкожен**
(`unit/interfaces/Unit.java:1070-1072`): `instanceof ScoutDogUnit || ScoutCatUnit || BatUnit ||
StriderUnit`. Нового скаута без правки этого метода не будет.

---

## 5. Юниты

### 5.1 Устройство класса

Абстрактного базового класса юнита нет. Юнит **наследует ванильного моба** и **реализует**
интерфейсы. Ограничение прямо в комментарии `unit/interfaces/Unit.java:132-137`:

```java
// Defines method bodies for Units
// workaround for trying to have units inherit from both their base vanilla Mob class and a Unit class
// Note that we can't write any default methods if they need to use Unit fields without a getter/setter
```

Интерфейсы в `unit/interfaces/`:

| Интерфейс | Назначение |
|---|---|
| `Unit` | ядро, обязателен |
| `AttackerUnit` | атака |
| `WorkerUnit` | строить/собирать ресурсы |
| `RangedAttackerUnit` | атака снарядами |
| `HeroUnit` (наследует `Unit`) | герои: уровни, мана, ранг |
| `ConvertableUnit` | превращение юнита в другого на месте |
| `ArmSwingingUnit` | процедурная анимация рук |
| `KeyframeAnimated` | gecko-анимации |

Инвентарь (`items/UnitInventory`) юниту **надо не реализовывать** — миксин
`mixin/UnitInventoryMobMixin.java:55-56` добавляет его **каждому** мобу, 6 слотов
(`items/UnitInventory.java:13`). Чтобы юнит им пользовался, переопределите `getItemGoal()`
(`unit/interfaces/Unit.java:1082-1085`, по умолчанию `null`) и добавьте `UnitItemGoal`.

### 5.2 Обязательные методы `Unit`

`setAnchor`/`getAnchor` (156-157), `getCheckpoints` (164), `getGarrisonGoal`/`canGarrison` (166-167),
`getUsePortalGoal`/`canUsePortal` (169-170), **`getFaction`** (172), `getAbilities` (173),
`getItems` (177), `getMaxResources` (178), `setEatingTicksLeft`/`getEatingTicksLeft` (193-194),
`getMoveGoal` (197), `getTargetGoal` (198), `getReturnResourcesGoal` (199), **`getCost`** (229),
`getFollowTarget` (231), `getHoldPosition`/`setHoldPosition` (232-233),
`getOwnerName`/`setOwnerName` (235-236), `getScenarioRoleIndex`/`setScenarioRoleIndex` (238-239),
`getOnDeathCommand`/`setOnDeathCommand` (241-242), `setFollowTarget` (756), `initialiseGoals` (758),
`updateAbilityButtons` (901), `getCooldowns` (969), `hasAutocast`/`setAutocast` (971-972),
`getCharges` (982).

Необязательные, но часто нужные: `getAbilityButtons` (174-176, по умолчанию
`getAbilities().getButtons(this)`), `setupEquipmentAndUpgradesServer/Client` (762/765),
`resetBehaviours` (747), `setMoveTarget` (751-753), `getInflatedSelectionBox` (1007-1009),
`getSunlightEffect` (662-672, по умолчанию `NONE`), `getSpeedModifier` (803-805),
`canPickUpEquipment`/`onPickupEquipment` (568-570), `getItemGoal` (1082-1085).

### 5.3 Атрибуты

Атрибуты **не** передаются в `EntityType.Builder` — их задаёт сам класс через
`createAttributes()`. База — `Unit.createDefaultAttributes()` (`unit/interfaces/Unit.java:773-795`),
который добавляет в том числе модификаторы из `AttributeRegistrar`
(`CRITICAL_HIT_CHANCE`, `EXPLOSIVE_HIT_CHANCE`, `BUILDING_DAMAGE_BONUS`, `LIFESTEAL`,
`MANA_ON_HIT`, `SCALE`).

### 5.4 Тик

Канонический порядок для бойца-рабочего (`unit/units/piglins/GruntUnit.java:269-275`):

```java
public void tick() {
    this.setCanPickUpLoot(true);
    super.tick();
    Unit.tick(this);
    AttackerUnit.tick(this);
    WorkerUnit.tick(this);
}
```

NBT: `addUnitSaveData`/`readUnitSaveData` (`Unit.java:616-660`) вызываются из
`addAdditionalSaveData`/`readAdditionalSaveData`.

### 5.5 Цели

Два метода: `initialiseGoals()` создаёт объекты целей, `registerGoals()` вызывает
`initialiseGoals()` и вешает их на ванильные селекторы. Канон (`GruntUnit.java:307-334`):
приоритет 1 — `FloatGoal`, 2 — все поведенческие цели и `targetGoal` на `targetSelector`,
3 — `moveGoal`, 4 — `RandomLookAroundUnitGoal`.

**Клиент не вызывает `registerGoals()`**, вместо этого `unit/UnitClientEvents.java:688-691`
сам вызывает `initialiseGoals()`. Значит `initialiseGoals()` должен быть идемпотентным и
безопасным при повторном вызове на новом инстансе.

Снятие ванильного поведения делают через `removeAllGoals`, например
`unit/units/neutral/BeeUnit.java:331-343`:

```java
@Override
protected void registerGoals() {
    super.registerGoals(); // drop vanilla bee behavior but also prevent NPEs
    this.goalSelector.removeAllGoals(g -> true);
    this.targetSelector.removeAllGoals(g -> true);
    initialiseGoals();
    ...
}
```

### 5.6 Способности

Реестра способностей нет. У юнита есть статический экземпляр `Abilities`, а инстанс хранит клон.
Два стиля объявления — статический блок (`unit/units/monsters/SkeletonUnit.java:57-61`) или список
в конструкторе (`unit/units/piglins/WildfireUnit.java:77-85`):

```java
public final Abilities ABILITIES = new Abilities(
        List.of(
                new Pair<>(new MoltenBomb(), Keybindings.abilitySlot1),
                new Pair<>(new ScorchingGaze(), Keybindings.abilitySlot2)
        )
);
```

У каждой способности нужна новая константа в `unit/UnitAction.java` (157 констант) и обработчик
пакета. Класс способности наследует `Ability` и **обязан** переопределить `getButton(...)`
(базовый возвращает `null` — кнопка в HUD не появится) и `use(...)`.

### 5.7 Чек-лист добавления одного юнита

Жёсткий код:

1. `unit/units/<фракция>/<X>Unit.java` — `extends <ванильный моб>, implements Unit, ...`;
   атрибуты, `initialiseGoals`/`registerGoals`, `tick()` с `Unit.tick(this)`, NBT через
   `addUnitSaveData`, три `EntityDataAccessor` (`ownerDataAccessor`, `scenarioRoleDataAccessor`,
   `onDeathCommandDataAccessor`) и `defineSynchedData`.
2. `unit/units/<фракция>/<X>Prod.java` — `extends ProductionItem` (или `HeroProductionItem` +
   `getHeroEntityType()`), из `ProductionItems`.
3. `registrars/EntityRegistrar.java` — блок `ENTITIES.register(...)` **и** `case "<X>"` в
   `getEntityType(String)`.
4. `registrars/ItemRegistrar.java` — спавн-яйцо `<x>_unit_spawn_egg`.
5. `resources/ResourceCosts.java` — `ResourceCost X` **и** `X.bakeValues(...)` в
   `deferredLoadResourceCosts()`.
6. `config/ReignOfNetherCommonConfigs.java` — `UnitCosts.X` + `define(BUILDER)`.
7. `building/production/ProductionItems.java` — `register(...)` и вписать в `ALL`.
8. `building/buildings/<фракция>/<здание>.java` — `this.productions.add(ProductionItems.X, Keybindings.abilitySlotN)`.
9. `ClientModEvents.java` — `evt.registerEntityRenderer(EntityRegistrar.X.get(), XRenderer::new)`,
   при своём модели — плюс `registerLayerDefinitions`.
10. `unit/UnitAction.java` — константа на каждую новую способность.
11. `ability/abilities/<X>.java` — наследник `Ability`/`HeroAbility`.
12. Для новой константы `Faction` — ещё §2.

Ассеты:

* `assets/reignofnether/models/item/<x>_unit_spawn_egg.json`
* `assets/reignofnether/lang/*.json`: `entity.reignofnether.<x>_unit`, тултипы,
  `item.reignofnether.<x>_unit_spawn_egg`
* `assets/reignofnether/textures/mobheads/<x>.png` (иконка кнопки производства) и
  `textures/entities/...` при своём рендерере
* `assets/reignofnether/sounds.json` + ключи `sounds.reignofnether.<x>_*`, если нужны звуки

**Не применимо:** рецепты (их в моде нет вообще), JSON-модели сущностей (модели только кодом),
датапаки для спавн-яиц.

### 5.8 Ловушка связности

`ProductionItems.getProductionItem` (`building/production/ProductionItems.java:196-203`) ищет
юнит **через** `EntityRegistrar.getEntityType(prodItem.getItemName())`. Поэтому строка
`itemName` в `Prod` обязана совпадать с `case` в свитче `getEntityType`, иначе кастомные
строения не смогут обучать юниту — молча, без ошибки.

---

## 6. Строения

### 6.1 Главное, что надо понять сразу

**У строения в моде нет ни блока, ни блок-сущности.** Ни `Block`, ни `BlockEntityType`, ни
`BlockItem`, ни лут-таблицы, ни рендерера, ни записи в креативную вкладку.
`registrars/BlockRegistrar.java` и `registrars/BlockEntityRegistrar.java` не содержат **ни
одной** регистрации строения.

Строение — это:

1. Java-объект в **собственном (не-ванильном) реестре** `api/ReignOfNetherRegistries.BUILDING`
   (`MappedRegistry<Building>` с `Lifecycle.experimental()`, то есть не синхронизируется с
   датапаком);
2. файл NBT ванильного structure block в `data/reignofnether/structures/<structureName>.nbt`;
3. в рантайме — экземпляр `BuildingPlacement` в статическом `ArrayList`
   (`BuildingServerEvents.buildings` / `BuildingClientEvents.buildings`), а не блок-сущность,
   поэтому NBT-сохранения в чанке нет: персистентность через собственный `SavedData`
   (`building/BuildingSaveData.java`).

Блоки, которые автор строения трогает, — только маркеры **внутри** NBT-структуры и блок
`rts_structure_block` для авторства кастомных строений.

### 6.2 Иерархия

```
Building (abstract, building/Building.java:26)
├── ProductionBuilding (building/production/ProductionBuilding.java:13)
│   ├── CustomBuilding (building/custombuilding/CustomBuilding.java:66)
│   ├── AbstractStockpile, AbstractMarket (building/buildings/shared/)
│   └── конкретные: ShrineOfProsperity, ArcaneTower, TownCentre, Castle, Beacon, …
├── AbstractFarm → FarmPlacement
├── AbstractBridge → BridgePlacement
└── HealingFountain
```

Каталог аддонов — **`building/addon/` (единственное число)**, не `addons/`.

У базового `Building` **всего два абстрактных метода** (`Building.java:100, 106`):

```java
public abstract Faction getFaction();
public abstract BuildingPlaceButton getBuildButton(Keybinding var1);
```

Поля, которые задаёт наследник в конструкторе: `name`, `structureName`, `icon`, `isCapitol`,
`portraitBlock`, `canAcceptResources`, `maxHealth`, `buildTimeModifier`, `startingBlockTypes`,
`foundationYLayers`, `captureRange`, `capturable`, `invulnerable`, `repairable`.

Переопределяемые хуки (все по умолчанию — заглушки/но-op): `getRelativeBlockData` (92-94),
`createBuildingPlacement` (96-98), `getUpgradeLevel` (102-104), `getUpgradedStructureName` (112-114),
`canDestroyBlock` (133-135), `onBlockBuilt` (137-139), `onBuilt` (141-143), `destroy` (145-147),
`tick(Level, BuildingPlacement)` (149-151), `getUpgradedName` (153-159).

### 6.3 Минимальный пример целиком

`building/buildings/villagers/VillagerHouse.java` — самое маленькое строение в моде:

```java
public class VillagerHouse extends Building {

    public final static String buildingName = "Villager House";
    public final static String structureName = "villager_house";
    public final static ResourceCost cost = ResourceCosts.VILLAGER_HOUSE;

    public VillagerHouse() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.OAK_LOG;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/oak_log.png");
        this.buildTimeModifier = 0.8f;
        this.startingBlockTypes.add(Blocks.SPRUCE_PLANKS);
        this.startingBlockTypes.add(Blocks.OAK_PLANKS);
        this.startingBlockTypes.add(Blocks.OAK_LOG);
        this.maxHealth = 125;
    }

    public Faction getFaction() {return Faction.VILLAGERS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(name, /* иконка */ hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.VILLAGER_HOUSE,   // выбрано
            () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.EXPLAIN_BUILDINGS),      // скрыто
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.TOWN_CENTRE) ||
                    ResearchClient.hasCheat("modifythephasevariance"),                        // доступно
            List.of(/* строки тултипа */), this);
    }
}
```

**Внимание, здесь баг в самом коде мода:** `name` считается как
`buildings.<фракция>.<namespace>.<path>` (например `buildings.villagers.reignofnether.villager_house`),
и такого ключа **нет ни в одном языковом файле**. Есть только
`buildings.reignofnether.villager_house`. Авторитетный формат —
`buildings.<namespace>.<path>` (`Building.getUpgradedName()`, `Building.java:153-159`).
Для нового кода используйте именно его.

### 6.4 Размер, HP, стоимость, время постройки — откуда берутся

* **Размер** не объявляется. `BuildingPlacement` (строки 271-291) вычисляет `minCorner`,
  `maxCorner`, `centrePos` из списка блоков.
* **HP**: `maxHealth` на `Building`. Если `maxHealth <= 0` — берётся
  `DEFAULT_HEALTH_PER_BLOCK = 2.0` на блок (`Building.java:58`).
* **Стоимость**: `ResourceCost cost`, см. §8.
* **Время постройки**: `baseMsPerBuild = 500` (`BuildingPlacement.java:137`), домножается на
  `buildTimeModifier` для первого постройка и на `repairTimeModifier` для ремонта; число
  рабочих ускоряет (`handleServerTick`, строки 1017-1024).
* **Из чего состоит**: целиком данные — `data/reignofnether/structures/<structureName>.nbt`.
* **Фундамент**: `startingBlockTypes` + `foundationYLayers`.

### 6.5 `originPos` против `minCorner`/`maxCorner`

`originPos` — **якорь**, по которому кликают; он же идентичность размещения во всех пакетах.
Абсолютные позиции блоков считаются в `BuildingUtils.getAbsoluteBlockData` (строки 244-259) с
**`+1` по Y**, то есть слой `y=0` из NBT встаёт на блок выше `originPos`. `minCorner`/`maxCorner` —
включительные углы габаритов. В рендере габарит брать как
`Vec3.atLowerCornerOf(maxCorner.offset(1,1,1))`.

### 6.6 UI: меню у строений нет

`registrars/ContainerRegistrar.java` регистрирует **ровно одно** меню во всём моде —
`topdowngui_container` для псевдо-инвентаря орторежима. Ни `MenuRegistrar`, ни контейнера на
строение, ни экрана. Всё UI построений — это HUD-кнопки:

1. кнопка постройки — `Building.getBuildButton(Keybinding)` → `BuildingPlaceButton`;
2. кнопки способностей — `BuildingPlacement.updateButtons()` (1341-1344) →
   `Abilities.getButtons(BuildingPlacement)`;
3. кнопки производства — `ProductionPlacement` (53-55) →
   `ProductionItem.getStartButton(placement, keybinding)`;
4. портрет — `hud/PortraitRendererBuilding.render(...)` (48-101), рисует `portraitBlock`.

`BuildingPlaceButton` (`building/BuildingPlaceButton.java:12-18`):
`(name, iconRl, hotkey, isSelected, isHidden, isEnabled, tooltipLines, building)`; клик →
`BuildingClientEvents.setBuildingToPlace(building)`.

### 6.7 Производство

1. Строение наследует `ProductionBuilding` (или `CustomBuilding`) — это даёт
   `public ProductionItemList productions`.
2. `ProductionItem` регистрируется в `building/production/ProductionItems.java` (строки 20-129)
   через `Registry.register(ReignOfNetherRegistries.PRODUCTION_ITEM, id, item)`.
   Для юнитов добавлять в список `ALL` (137-193).
3. В конструкторе строения: `this.productions.add(ProductionItems.X, Keybindings.abilitySlotN)`.

Абстрактный у `ProductionItem` только `getItemName()` (`:62`). `ProdDupeRule` (enum из трёх
значений, `building/production/ProdDupeRule.java:3-7`):

```java
public enum ProdDupeRule {
    DISALLOW,               // дублей нельзя нигде: глобальное исследование
    DISALLOW_FOR_BUILDING,  // дублей нельзя в одном строении: улучшение строения
    ALLOW                   // дубли всегда: обучение юнитов
}
```

Создание юнита — в `onComplete` (`unit/units/villagers/RoyalGuardProd.java:29-35`):

```java
this.onComplete = (Level level, ProductionPlacement placement) -> {
    if (!level.isClientSide())
        placement.produceUnit((ServerLevel) level, EntityRegistrar.ROYAL_GUARD_UNIT.get(), placement.ownerName, true);
};
```

### 6.8 Аддоны

Маркер-интерфейс `building/addon/BuildingAddon` **пустой**. Строение и `implements` интерфейс,
и вызывает `setActiveAddon(X.class, this, true)` в конструкторе (`Building.java:116-131`).
Набор: `RangeIndicatorAddon`, `GarrisonableBuildingAddon`, `NetherConvertingAddon`,
`NightSourceAddon`, `ItemShopAddon`.

Состояние аддонов хранится через `building/data/DataType` + `DataStorage`, которые
персистятся в `BuildingSaveData` автоматически.

### 6.9 Что строение обязано открыть «крестьянину»

* `getFaction()` — иначе не будет ни кнопки, ни прав;
* `cost.population` — запас населения;
* `repairable` — достраиваются ли уже законченные строения;
* `canAcceptResources = true` — если рабочие должны сдавать ресурсы (проверяется в
  `unit/goals/ReturnResourcesGoal.canDropOff()`, строки 89-94);
* если это ферма — наследовать `AbstractFarm`, чтобы placement стал `FarmPlacement`, который
  умеет отдавать себя цели сбора (`BuildRepairGoal.tick:83-86`).

### 6.10 Чек-лист добавления строения

1. NBT: сохранить структуру Structure Block'ом в
   `src/main/resources/data/reignofnether/structures/<structure_name>.nbt`.
2. `resources/ResourceCosts.java` → `public static final ResourceCost <NAME> = new ResourceCost(ID, "<NAME>");`
3. `config/ReignOfNetherCommonConfigs.java` →
   `ResourceCostConfigEntry.Building(food, wood, ore, popSupply, ResourceCosts.<NAME>, "<Name> Config")`
   **и** `<NAME>.define(BUILDER);`
4. Класс в `building/buildings/<фракция>/<Name>.java`: `getFaction()` + `getBuildButton()`.
5. `building/Buildings.java` →
   `register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "<snake_name>"), new <Name>());`
6. `faction/FactionRegistries.java` → `register(Faction.<X>, Buildings.<NAME>, Keybindings.<свободный слот>);`
7. Локализация `buildings.reignofnether.<snake_name>` (+ `.tooltipN`).
8. При необходимости: `ProductionItems`, `this.productions.add`, способности
   (`this.abilities.add`), свой подкласс `BuildingPlacement` в `building/buildings/placements/`.
9. **Ничего не нужно:** блок, блок-сущность, блок-айтем, меню, рендерер, лут-таблица,
   креативная вкладка, рецепт.

---

## 7. Исследования и технологии

### 7.1 Ключевой факт: класса `ResearchItem` не существует

Исследование — это **обычный `ProductionItem`**, производимый очередью производства
`ProductionBuilding`. Нет ни файла дерева технологий, ни экрана дерева, ни проверки
предпосылок между исследованиями. Всё дерево — это жёстко прописанные вызовы
`this.productions.add(...)` в конструкторах строений.

### 7.2 Шаблон исследования

`research/researchItems/ResearchSculkAmplifiers.java` — образец для копирования:

```java
public class ResearchSculkAmplifiers extends ProductionItem {

    public final static String itemName = "Sculk Amplifiers";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_SCULK_AMPLIFIERS;

    public ResearchSculkAmplifiers() {
        super(cost, ProdDupeRule.DISALLOW);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide()) {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_SCULK_AMPLIFIERS);
            }
        };
    }
    ...
    getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey):
      () -> ProductionItems.RESEARCH_SCULK_AMPLIFIERS.itemIsBeingProduced(prodBuilding.ownerName)
              || ResearchClient.hasResearch(ProductionItems.RESEARCH_SCULK_AMPLIFIERS),   // скрыто
      () -> BuildingClientEvents.hasFinishedBuilding(Buildings.STRONGHOLD)               // доступно
}
```

Регистрация — `building/production/ProductionItems.java:78-129`, ключ реестра
`reignofnether:<path>`.

### 7.3 Что может делать `onComplete`

1. **Открыть способность** — спрятать кнопку до исследования:
   `ability/abilities/ToggleShield.java:61-62`:
   ```java
   () -> !ResearchClient.hasResearch(ProductionItems.RESEARCH_BRUTE_SHIELDS) ||
           bruteUnit.getItemBySlot(EquipmentSlot.OFFHAND).getItem() != Items.SHIELD,
   ```
2. **Пересчитать снаряжение/характеристики** — пройтись по живым юнитам и вызвать
   `setupEquipmentAndUpgradesServer()` (`research/researchItems/ResearchVindicatorAxes.java:33-41`).
3. **Превратить юнитов** — `UnitServerEvents.convertAllToUnit(...)`
   (`research/researchItems/ResearchHusks.java:42-48`).
4. **Сменить структуру строения** — `placement.changeStructure(...)`
   (`ResearchGrandLibrary.java:29-33`).
5. **Выдать товары в магазин** — дописать в `ItemShopAddon.STOCKED_ITEMS`
   (`ResearchMarketUpgradeVillager.java:31-40`).

Серверная проверка — `ResearchServerEvents.playerHasResearch(owner, item)`;
клиентская — `ResearchClient.hasResearch(item)`; там же работает читерский доступ
`hasCheat("medievalman")` (даёт все исследования сразу).

### 7.4 Где дерево целиком

49 вызовов `productions.add(ProductionItems.RESEARCH_*, ...)` в конструкторах строений —
это и есть дерево. Разбивка по фракциям и строениям:

| Фракция | Строение | Исследования |
|---|---|---|
| MONSTERS | `Laboratory` | HUSKS, DROWNED, STRAYS, BOGGED, SPIDER_JOCKEYS, POISON_SPIDERS, SPIDER_WEBS, SLIME_CONVERSION, LAB_LIGHTNING_ROD, POSSESSION, SILVERFISH, SCULK_AMPLIFIERS, MASS_BURIAL |
| MONSTERS | `Graveyard` | OVERFLOWING_GRAVEYARD |
| MONSTERS | `MonsterMarket` | MARKET_UPGRADE_MONSTERS |
| VILLAGERS | `Blacksmith` | GOLEM_SMITHING, MILITIA_BOWS, SUPERIOR_BLACKSMITH |
| VILLAGERS | `WitchHut` | LINGERING_POTIONS, HEALING_POTIONS, WATER_POTIONS |
| VILLAGERS | `Library` | EVOKER_VEXES, UPGRADED_WINDCALLERS, GRAND_LIBRARY |
| VILLAGERS | `Castle` | RAVAGER_CAVALRY, CASTLE_FLAG |
| VILLAGERS | `VillagerMarket` | MARKET_UPGRADE_VILLAGER |
| PIGLINS | `PortalBasic` | PORTAL_FOR_CIVILIAN / MILITARY / TRANSPORT |
| PIGLINS | `HoglinStables` | HOGLIN_CAVALRY |
| PIGLINS | `Bastion` | BRUTE_SHIELDS, GREEDY_TRIDENTS, CLEAVING_FLAILS |
| PIGLINS | `Fortress` | ADVANCED_PORTALS, BLOODLUST, SOUL_FIREBALLS |
| PIGLINS | `BasaltSprings` / `FlameSanctuary` / `WitherShrine` / `PiglinMarket` | по одной-две |
| NEUTRAL | `Beacon` | BEACON_LEVEL_1..5 |

Предпосылки задаются двумя разными способами:

* **по кнопке**, клиентский `isEnabled` поставки — `Laboratory.java:102-104`:
  ```java
  () -> (BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) &&
          BuildingClientEvents.hasFinishedBuilding(Buildings.GRAVEYARD)) ||
          ResearchClient.hasCheat("modifythephasevariance"),
  ```
* **по счётчику**, серверный `canProduce` — `ResearchMarketUpgradeVillager.java:43-46`:
  ```java
  @Override
  public boolean canProduce(ProductionPlacement pp) {
      return pp.getUpgradeLevel() <= 0 &&
             BuildingUtils.numFinishedBuildings(pp.isClientSide(), Buildings.VILLAGER_HOUSE, pp.ownerName) >= 6;
  }
  ```

Сторонние проверки в логике обязательны в обоих местах — иначе рассинхрон.

### 7.5 Персистентность и её границы

`research/ResearchSaveData.java`, id `"saved-research-data"`, в мире —
`<world>/data/saved-research-data.dat`, структура:

```
{ researchItems: [ {ownerName:"Steve", researchKey:"reignofnether:sculk_amplifiers"}, ... ] }
```

То есть хранится плоский список «игрок X владеет ключом Y».

**Очередь производства не сохраняется.** `ResourcesServerEvents.saveResources`
(`resources/ResourcesServerEvents.java:98-112`) при сохранении возвращает стоимость всех
поставленных в очередь предметов с комментарием «they will be cancelled on server shutdown»,
а `PlayerServerEvents` очищает очереди на выходе.

`onComplete` **не откатывается** при `removeResearch` — снимается только флаг.

### 7.6 Подводные камни исследований

* **Ключ реестра ≠ ключ локализации** в пяти случаях (`ResearchClient.addResearch` строит
  ключ из пути реестра, а ключи написаны руками):

  | Путь в реестре | Ключ, который реально есть |
  |---|---|
  | `lab_lightning_rod` | `research.reignofnether.lightning_rod` |
  | `research_market_upgrade_villager` | `research.reignofnether.villager_market_upgrade` |
  | `research_market_upgrade_monster` | `research.reignofnether.monster_market_upgrade` |
  | `research_market_upgrade_piglin` | `research.reignofnether.piglin_market_upgrade` |
  | `research_mass_burial` | `research.reignofnether.research_mass_burials` |

  Для них тост «Upgrade completed: …» показывает сырой ключ.
* **Три исследования недостижимы:** `RESEARCH_VINDICATOR_AXES`,
  `RESEARCH_PILLAGER_CROSSBOWS`, `RESEARCH_HEAVY_TRIDENTS` — зарегистрированы, имеют
  стоимость и локализацию, но ни одно строение их не производит. Доступны только через
  `/rtsapi player research add`.
* `ResourceCosts.RESEARCH_RAVAGER_ARTILLERY` — запись конфига, у которой нет
  соответствующего `ProductionItem`.
* `ProductionItem.getItemName()` возвращает **жёстко заданную строку** (например
  `"Sculk Amplifiers"`), она же служит ключом производства для `CustomBuildingPlacement`, и
  **не локализована** (`hud/HudClientEvents.java:543` с пометкой `// TODO: translatable`).

---

## 8. Ресурсы и стоимости

### 8.1 Типы ресурсов

`resources/ResourceName.java` — enum из пяти значений: `FOOD`, `WOOD`, `ORE`, `EMERALD`,
`NONE`. Новый тип ресурса требует правки enum и всех `switch (resourceName)`.

**Валюты «незерит» не существует.** Незерит — это блок (`ANCIENT_DEBRIS`,
`NETHERITE_SCRAP`), дающий `ResourceName.ORE` стоимостью 120. Траты — абстрактные
`food` / `wood` / `ore` (и `emerald` для предметов).

### 8.2 Что считается собираемым

`resources/ResourceSources.java` (475 строк) — полный перечень блоков и предметов с
ценностью и временем сбора: FOOD_BLOCKS (162-312), WOOD_BLOCKS (314-388), ORE_BLOCKS (390-468).
Плюс общие правила по тегам `BlockTags.LOGS` / `BlockTags.LEAVES` (147-160).

Ресурсы — это **обычные блоки мира**, мод их не спавнит: `ResourceIndex`
(`resources/ResourceIndex.java`) индексирует существующие блоки по чанкам
(`ResourceChunk` — `EnumMap<ResourceName, LongOpenHashSet>`), поддерживается из
`mixin/LevelChunkMixin.java:25-33` на `setBlockState`, вычищается по
`resources/ResourceIndexEvents.java` на выгрузке чанка. Руда в мире — обычная датапак-генерация
(`data/minecraft/worldgen/**`).

### 8.3 Стоимость объявляется в двух местах

`resources/ResourceCosts.java` — только ручка без значений:

```java
public static final ResourceCost GRUNT = new ResourceCost(ID, "GRUNT");
```

Числа живут в конфиге `config/ReignOfNetherCommonConfigs.java` и попадают в
`reignofnether-common-<версия>.toml`:

```java
public static final ResourceCostConfigEntry GRUNT = ResourceCostConfigEntry.Unit(50,0,0,15,1, ResourceCosts.GRUNT, "Grunt Config");
```

и запекаются в `ResourceCosts.deferredLoadResourceCosts()`, который вызывается на
`FMLCommonSetupEvent` (`ReignOfNether.java:106-109`). **Читать значения конфига раньше
CommonSetup нельзя** — будет `IllegalStateException` (комментарий в
`ResourceCosts.java:215-218`).

Поэтому новый юнит/строение/исследование требует **трёх** правок: `ResourceCosts.<NAME>`,
`…Configs.<Category>.<NAME>` и `.define(BUILDER)`.

### 8.4 Уровень «слота» — предметный слой выключен

`items/UnitItem.java:48`:

```java
public static final boolean ENABLED = false;
```

Из-за этого весь слой геройских предметов, магазины и изумрудная валюта **отключены в этой
сборке** (`items/ItemServerEvents.java:28`, `items/ItemClientEvents.java:44` берут это же
значение). Практическое следствие: все стоимости используют food/wood/ore, а
`ResourceCost.emerald` всегда 0. Чтобы включить, надо выставить `ENABLED = true`.

Смежная ошибка: `ResourceCost.Emeralds(int emeralds)` (`resources/ResourceCost.java:58-60`)
конструктору передаёт `0` вместо аргумента.

---

## 9. Локализация

Файлы: `assets/reignofnether/lang/*.json`, 22 локали, эталон — `en_us.json` (~2300 строк).

Схема ключей — `<домен>.reignofnether.<путь>[.tooltipN|.pointN|.desc]`:

| Домен | Пример |
|---|---|
| Строения | `buildings.reignofnether.laboratory`, `.tooltip1..3` |
| Юниты | `entity.reignofnether.husk_unit` |
| Исследования | `research.reignofnether.sculk_amplifiers`, `.tooltip1..3` |
| Способности | `abilities.reignofnether.<ability>` |
| Предметы | `item.reignofnether.broadsword`, `.desc`, `.point1..N` |
| Ресурсы | `resources.reignofnether.food` (через `ResourceName.langKey()`) |
| Вкладки | `creativetab.reignofnether.unit_spawn_eggs` |
| Фракции | `hud.faction.reignofnether.villagers` (через `MiscUtil.getFactionName`) |
| Типы предметов | `unititemtype.reignofnether.consumable` |

Что нужно новой фракции: `hud.faction.reignofnether.<имя_фракции>` во всех 22 файлах,
плюс ключи строений, юнитов, исследований и способностей.

Аргументы тултипов передаются позиционно через `%d`; литеральный процент — `%%`.

---

## 10. Что проверить в игре

1. `/rtsapi building place reignofnether:<строение> <игрок> <x> <y> <z> true none` —
   флаг `fromCommand` пропускает проверку ресурсов/рельефа/тумана, `true` на позиции
   включает самостройку.
2. `/rtsapi unit summon reignofnether:<юнит> <владелец> <pos>`.
3. Кнопка постройки появилась у правильного рабочего юнита и у правильной фракции.
4. Исследование запускается, держит предпосылку, выдаётся один раз (`DISALLOW`).
5. Эффект исследования действительно применился: способность видна в HUD, юниты
   перевооружились, строение сменило структуру.
6. Строение держит чанк (`ChunkTicketUtil`) и отпускает его при сносе.
7. Юнит ходит: `initialiseGoals()` на клиенте вызывается отдельно, цели не должны быть
   `null` до первого тика.

Полезные команды: `/rtsapi player research add <item> <игрок>` (в том числе для
недостижимых исследований), `/rtsapi player victory|defeat <игрок>`,
`/rtsapi player resources add <resource> <points> <игрок>`.

---

## 11. Чего в моде нет (чтобы не искать)

* **Рецептов нет вообще.** Ни каталога `data/reignofnether/recipes/`, ни `RecipeRegistrar`,
  ни ссылок на `CraftingRecipe`/`RecipeManager`. Строения и юниты не крафтятся, стоимость
  берётся из пула ресурсов. Соответственно, предиката «есть исследование» в рецептах тоже
  нет — исследования проверяются прямо в коде.
* **Реестра фракций нет** (§0).
* **У регистра строений нет синхронизации с датапаком** — `Lifecycle.experimental()`, кастомные
  строения хранятся в `CustomBuildingSaveData`.
* **Креативных вкладок по фракциям нет.** Вкладки по типу контента, и живут они в
  `items/CreativeModeTabsRegistrar.java` (**не** в `registrars/`): `custom_buildings`,
  `unit_spawn_eggs`, `unit_items`. Маркера фракции у предметов и блоков нет, так что фильтровать
  по фракции нечем.
* **Меню и экранов у строений нет** (§6.6).
* **Датапак-расширения для HUD-кнопок есть, но данных не поставляется:**
  `reignofnether:rts_buttons` регистрируется как датапак-реестр, а мод ищет
  `reignofnether/custom_button_mappings.json`, которого в репозитории нет.
* **Карты**: `data/reignofnether/maps/*.json` (`test_map.json`, `valley_of_deceit.json`) имеют
  другую схему и **модом не читаются** — ссылок на них в `src/main/java` нет. Рабочий формат
  карты матча — `rtsmap.json` в папке мира (§3.2).
* **Сохранений союзов нет.** `AlliancesServerEvents.alliances` — приватная in-memory карта,
  она пересобирается при старте матча из цвета старта / правила coop / номера команды сценария.