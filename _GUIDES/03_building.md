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
        this.scaffoldingBlock = Blocks.SCAFFOLDING; // см. ниже
    }

    @Override public Faction getFaction() { ... }              // удаляется на этапе F
    @Override public BuildingPlaceButton getBuildButton(Keybinding hotkey) { ... }
}
```

**Абстрактных методов у `Building` всего два:** `getFaction()` и `getBuildButton(Keybinding)`.
Первый удаляется вместе с `Faction` (этап F плана) — новым зданиям его реализовывать не нужно.

### Поля `Building`, которые задаёт наследник

`name`, `structureName`, `icon`, `portraitBlock`, `isCapitol`, `maxHealth`,
`buildTimeModifier`, `repairTimeModifier`, `startingBlockTypes`, `foundationYLayers`,
`canAcceptResources`, `captureRange`, `capturable`, `invulnerable`, `repairable`,
`shouldDestroyOnReset`, `scaffoldingBlock` (после этапа B.8.10).

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

## 6. Блок лесов

`B.8.10` плана добавляет поле `scaffoldingBlock`. Оно используется в
`BuildingServerEvents.placeScaffoldingUnder`. Если нужно «по биому», определить две
разновидности и переключать по `level.getBiome(pos)`.

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
