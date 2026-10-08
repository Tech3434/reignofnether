# Добавление здания

## Что из чего состоит

У здания **нет блока и нет блок-сущности**. Здание — это:

1. Java-объект в кастомном реестре `ReignOfNetherRegistries.BUILDING`;
2. файл NBT ванильного structure block в `data/reignofnether/structures/<structureName>.nbt`;
3. в рантайме — экземпляр `BuildingPlacement` в статическом списке `BuildingServerEvents.buildings`.

Персистентность — через собственный `SavedData` (`building/BuildingSaveData.java`), не через чанк.

## 1. Файл структуры

Сохранить постройку Structure Block'ом в
`src/main/resources/data/reignofnether/structures/<structure_name>.nbt`.

Из содержимого выводится всё остальное:

| Что | Откуда берётся |
|---|---|
| Габарит `minCorner`/`maxCorner` | из списка блоков |
| HP | по умолчанию 1 на блок, если не задано `maxHealth` |
| `startingBlockTypes` | слой `y = 0` |
| Портрет (варианты) | все не-маркерные блоки |

При загрузке (`BuildingBlockData.getBuildingBlocksFromNbt`) падающие брёвна превращаются в
обычные, а **текучая вода, отличная от источника, отбрасывается**.

## 2. Класс здания

```java
public class XBuilding extends ProductionBuilding {

    public final static String buildingName = "X";
    public final static String structureName = "x_building";
    public final static ResourceCost cost = ResourceCosts.X;

    public XBuilding() {
        super(structureName, cost, false);       // structureName, cost, isCapitol
        this.name = buildingName;
        this.portraitBlock = Blocks.OAK_LOG;
        this.icon = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/icons/buildings/x.png");
        this.maxHealth = 300;                     // 0 = 1 HP на блок
        this.buildTimeModifier = 1.0f;
        this.startingBlockTypes.add(Blocks.COBBLESTONE);
        this.scaffoldFill = ScaffoldFill.SCAFFOLDING; // SCAFFOLDING | CUSTOM (тогда scaffoldBlock) | BIOME_AWARE
    }

    public String getFaction() { return "villagers"; }         // строковая метка (имя кнопки от неё не зависит)
    @Override public BuildingPlaceButton getBuildButton(Keybinding hotkey) { ... }
}
```

**Абстрактный метод один — `getBuildButton(Keybinding)`.** `getFaction()` не абстрактный и теперь
возвращает **строку-метку** (по умолчанию `""`), не enum; на имя кнопки он не влияет.

### Поля `Building`, которые задаёт наследник

`name`, `structureName`, `icon`, `portraitBlock`, `isCapitol`, `maxHealth`,
`buildTimeModifier`, `repairTimeModifier`, `startingBlockTypes`, `foundationYLayers`,
`canAcceptResources`, `captureRange`, `capturable`, `invulnerable`, `repairable`,
`shouldDestroyOnReset`, `scaffoldFill`/`scaffoldBlock` (`ScaffoldFill`), `requiredResearch`.

### Переопределяемые хуки

| Хук | Зачем |
|---|---|
| `createBuildingPlacement(level, pos, rotation, ownerName)` | выбрать подкласс размещения |
| `getRelativeBlockData(level)` | свой список блоков вместо NBT |
| `getUpgradeLevel(placement)` | уровень улучшения из внешних признаков |
| `getUpgradedStructureName(int)` | другая NBT при улучшении |
| `canDestroyBlock(relativeBp, placement)` | защита маркерных блоков |
| `onBlockBuilt`, `onBuilt`, `destroy`, `tick` | поведение |
| `getUpgradedName(placement)` | отображаемое имя |

### Кнопка постройки

```java
@Override
public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
    ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
    String name = I18n.get("buildings." + key.getNamespace() + "." + key.getPath());
    return new BuildingPlaceButton(
        name, this.icon, hotkey,
        () -> BuildingClientEvents.getBuildingToPlace() == Buildings.X,   // выбрано
        () -> false,                                                    // скрыто
        () -> BuildingClientEvents.hasFinishedBuilding(Buildings.Y),     // доступно
        List.of(/* строки тултипа */), this);
}
```

⚠ Ключ локализации — `buildings.<namespace>.<path>`. В коде до этапа C был вариант
`buildings.<фракция>.<ns>.<path>`, которого нет ни в одном lang-файле: использовать первый.

## 3. Регистрация

`building/Buildings.java`:

```java
public static final XBuilding X = register(
        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "x_building"), new XBuilding());
```

Регистрация происходит в статическом инициализаторе класса; `Buildings.init()` — пустая
заглушка, вызываемая из конструктора мода.

## 4. Производство (если здание производит)

```java
this.productions.add(ProductionItems.X_UNIT, Keybindings.abilitySlot1);
```

Вызывать в конструкторе. Список — `building/production/ProductionItemList`, это
`LinkedHashMap<ProductionItem, Keybinding>`.

### Производство и исследования в JSON-зданиях

У data-driven здания (`JsonBuilding`) состав берётся из определения:

```json
"production": [ "myns:some_unit" ],
"researches": [ "myns:some_research" ]
```

`production` → `JsonProductionItem` (спавн юнита по id определения), `researches` → `ResearchProductionItem`
(общая очередь с производством, отмена с возвратом, по завершении — грант исследования владельцу).
Оба — обычные `ProductionItem`, поэтому их кнопки автоматически появляются в UI выбранного здания.

### Апгрейды (JSON)

```json
"upgrades": [
  { "structure": "myns:barracks_ii", "name": { "en_us": "Barracks II" },
    "maxHealth": 250, "cost": { "wood": 75, "seconds": 15 } }
]
```

Список — цепочка уровней. Каждый элемент — `JsonUpgradeProductionItem` в очереди здания: покупается из UI,
поднимает уровень placement на 1, меняет NBT-структуру (`structure`, путь без namespace), имя/иконку и
`maxHealth`, синкается клиенту. Уровень хранится на `BuildingPlacement.upgradeLevel` и сохраняется
(`BuildingSaveData`). **Пока не идут по уровням** `production`/`researches`/`addons`/способности.
⚠ Апгрейд-структура должна иметь **тот же габарит/фундамент**, что и базовая: границы placement (`minCorner`/
`maxCorner`/`centrePos`) считаются один раз при постановке и при смене структуры не пересчитываются.

## 5. Аддоны

Маркер-интерфейс `building/addon/BuildingAddon` пустой. Здание и `implements` интерфейс, и
вызывает `setActiveAddon(X.class, this, true)` в конструкторе.

| Аддон | Что даёт | Обязательные методы |
|---|---|---|
| `GarrisonableBuildingAddon` | гарнизон, дальность атаки | `getAttackRange`, `getExternalAttackRangeBonus`, `getEntryPosition`, `getExitPosition`, `getCapacity` |
| `RangeIndicatorAddon` | кольцо подсветки (только клиент) | `getRange`, `showOnlyWhenSelected` |
| `NetherConvertingAddon` | конвертация террейна | `getMaxNetherRange`, `getStartingNetherRange` |
| `NightSourceAddon` | источник ночи | `getNightRange`, `getDefaultNightRange` |
| `ItemShopAddon` | магазин за изумруды | `buyItem` |

Состояние аддонов пишется через `building/data/DataType` и сохраняется в
`BuildingSaveData.dataStorage` автоматически.

### JSON-аддоны (data-driven, готово)

У data-driven зданий (`JsonBuilding`) аддоны задаются прямо в JSON:

```json
"addons": [ { "type": "reignofnether:night_source", "params": { "range": 24 } } ]
```

Тип — id в реестре `building/addon/AddonTypes`. `JsonBuilding` создаёт аддон через
`AddonTypes.create(spec, building)` и навешивает `Building.addActiveAddon(...)`, который регистрирует его
под **всеми** реализуемыми `BuildingAddon`-интерфейсами (поэтому `getActiveAddon(RangeIndicatorAddon.class)`
и т.п. находят его). Движковые типы регистрируются в `building/addon/Addons.init()` (вызывается из
конструктора мода); автор фракции регистрирует свои так же:

```java
AddonTypes.register(ResourceLocation.fromNamespaceAndPath("myns", "my_addon"),
        (spec, building) -> new MyAddon(spec));
```

Движковые типы (`Addons.init()`): `reignofnether:night_source` (`range`/`showOnlyWhenSelected`),
`reignofnether:range_indicator` (`range`, `showOnlyWhenSelected`), `reignofnether:garrison`
(`capacity`, `attackRange`, `externalAttackRangeBonus`, `entryX/Y/Z`, `exitX/Y/Z`),
`reignofnether:nether_converting` (`maxRange`, `startingRange`). Свой аддон при необходимости может
реализовать lifecycle-хуки `BuildingAddon.onBuildingBuilt`/`onBuildingTick` (вызываются
`Building.onBuilt`/`Building.tick`).

## 6. Блок лесов

Когда под фундаментом пропасть глубже пары блоков, мод заполняет её. Поведение задаётся
двумя полями на `Building`:

```java
public ScaffoldFill scaffoldFill = ScaffoldFill.SCAFFOLDING;  // по умолчанию
public Block scaffoldBlock = null;                            // только для CUSTOM
```

| `ScaffoldFill` | Что ставится |
|---|---|
| `SCAFFOLDING` | блок самого мода (прежнее поведение, по умолчанию) |
| `CUSTOM` | один выбранный блок на всю колонну, берётся из `scaffoldBlock` |
| `BIOME_AWARE` | трава в верхнем заполненном слое, земля во всех нижельных — чтобы срез в склоне не оставлял серую полосу лесов по поверхности |

Заполнение выполняется в `BuildingServerEvents.placeScaffoldingUnder` в момент размещения,
глубина ограничена `MAX_SCAFFOLD_DEPTH = 5`. На ровной земле не срабатывает никогда.
При сносе здания леса убираются вместе с остальными блоками.

Тот же параметр нужно завести в `CustomBuildingAction`, если кастомные строения должны его
поддерживать.

## 7. Кастомные строения вместо своего класса

Если здание не требует особого кода, используй `rts_structure_block`: он захватывает
постройку и создаёт `CustomBuilding` с набором настраиваемых параметров (HP, стоимость,
гарнизон, радиусы ночи и конвертации, флаги).

Механика: `blocks/RTSStructureBlockEntity.java` — наследник `StructureBlockEntity`, который
запрещает режим `LOAD` и при сохранении вызывает
`CustomBuildingServerEvents.createAndRegisterNewCustomBuilding(...)`. Тот читает
`StructureTemplate` через `StructureTemplateManager` уровня, требует хотя бы один
нежидкий твёрдый блок и сразу размещает постройку. `RTSStructureBlockEntity` требует
`StructureBlockEntityAccessor`, потому что `updateBlockState` и `getRelatedCorners` стали
приватными в 1.21.1.

Всё в `CustomBuilding` выводится из NBT автоматически: `startingBlockTypes` из слоя `y = 0`,
варианты портрета из всех не-маркерных блоков, позиции входа/выхода гарнизона и точки
производства из маркерных блоков, `maxHealth` из числа блоков. Производство синтезируется из
спавн-яиц, найденных в инвентарях сундуков внутри структуры: каждое яйцо даёт кнопку, а NBT
яйца может переопределить `foodCost`, `woodCost`, `oreCost`, `ticksToTrain` и тултипы.

Аддоны `CustomBuilding` подключает все четыре сразу.

## 8. Побочные эффекты размещения

Они **остаются** (решение владельца, §12.1 плана) и должны учитываться при проектировании:

* подготовка площадки: нужно ровное основание, либо включать геймрул `slantedBuilding`,
  который **сносит рельеф** в габарите здания;
* леса ставятся под фундамент до 5 блоков глубиной;
* животные в радиусе 10 вытесняются, юниты изнутри выталкиваются;
* переход между измерениями внутри строения отменяется;
* внутри габарита тушатся пожары и подавляется рост культур.

## 9. Проверка в игре

* `/rtsapi building place reignofnether:x <игрок> <x> <y> <z> true none` — ставится мгновенно.
* Повреждение блоков здания уменьшает HP, при 50 % блоков здание разрушается.
* Кнопка производства в панели выделения запускает очередь и списывает стоимость.
* При потере здания владелец теряет сессию, юниты становятся нейтральными.
