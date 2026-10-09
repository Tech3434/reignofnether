# Добавление юнита

> ⚠ **Актуально (2026-10-08): юниты — полностью data-driven.** Код-классы юнитов удалены, под-интерфейсы
> (`AttackerUnit`/`WorkerUnit`/`RangedAttackerUnit`/`HeroUnit`) схлопнуты в `Unit`. Юнит — это ванильный/модовый
> моб (`base`) + `data/<ns>/unit/<name>.json` (`UnitDefinition`). Разделы 1–5 ниже — **legacy** (описывают
> старый кодовый подход); используйте раздел «Юнит данными» сначала.

## Юнит данными (актуально)

`data/<namespace>/unit/<name>.json`, реестр `reignofnether:unit`; id определения = `<ns>:<name>`
(идентичность юнита — определение, а не `EntityType`).

```json
{
  "base": "minecraft:skeleton",
  "name": { "en_us": "Skeleton" },
  "icon": "reignofnether:textures/icons/items/bow.png",
  "role": "ranged",                       // melee | ranged | worker | flying | hero
  "flags": { "canGather": false, "canBuild": false, "canGarrison": true, "holdPosition": true },
  "equipment": "minecraft:bow",           // необязательно: предмет в главную руку при спавне
  "scale": 1.0,
  "attributes": {
    "minecraft:generic.max_health": 40,
    "reignofnether:attack_damage": 3,
    "reignofnether:attack_range": 16,
    "reignofnether:attacks_per_second": 0.6
  },
  "projectile": { "entity": "minecraft:arrow", "velocity": 1.6, "damage": 3, "inaccuracy": 1.0 },
  "cost": { "food": 60, "wood": 0, "ore": 0, "seconds": 15 },
  "population": 1,
  "carryCapacity": 100,
  "requiredResearch": [ { "research": "myns:x", "invert": false } ],
  "abilities": [ { "type": "myns:some_ability", "cooldown": 100 } ]
}
```

* `role` строит goals (`UnitMobMixin.initialiseGoals`): melee → melee-goals; ranged → `UnitBowAttackGoal`
  (+ `RangedAttackBuildingGoal`); worker → сбор/стройка; garrison — по флагу. Роль/goals берутся из
  **resolved** определения, т.е. `inherits` влияет на роль (см. ниже).
* Флаги из `flags` применяются: `holdPosition` → удержание позиции; `canGather` → `GatherResourcesGoal`
  (worker получает его всегда); `canBuild` → `BuildRepairGoal` + `ExploreBuildLocationGoal`; `canGarrison`
  → `GarrisonGoal`.
* `carryCapacity` (worker) — сколько ресурсов воркер несёт до автодропа (по умолчанию 100 для `worker`,
  0 иначе); порог автодропа — половина ёмкости (дефолт 50).
* `equipment` — id предмета в главную руку при спавне (нужно ranged-юниту с луком: goal требует оружие в руке).
* `projectile` (для `role: ranged`) — `entity` (id сущности-снаряда), `velocity` (по умолч. 1.6),
  `damage` (по умолч. −1 = урон юнита `getUnitAttackDamage()`), `inaccuracy`. Спавнится в
  `UnitMobMixin.performUnitRangedAttack` (сервер). Без `projectile` ranged-юнит стреляет `minecraft:arrow`.
* Данные-атрибуты `reignofnether:*` — id модовых атрибутов; `setStatsForLevel`/герой — из `UNIT`-атрибутов.
* `inherits` — id другого определения: незаданные поля берутся у родителя (атрибуты мержатся по ключам,
  ребёнок побеждает), транзитивно и без циклов (`UnitDefinitions.resolve`). Пример: `skeleton_marksman.json`
  наследует `skeleton_unit` и меняет имя/scale/урон/снаряд/стоимость.
* `population`/`cost`/`requiredResearch`/`abilities` — тоже наследуются, если не заданы в ребёнке.
* Демо: `skeleton_unit.json` (ranged, лук+стрела) и `skeleton_marksman.json` (наследник) в казарме.

---

## Legacy: кодовый юнит (устарело)

> Фракция: `Unit.getFaction()` — **строка-метка** (по умолчанию `""`), не enum; при использовании
> реестра фракций указывайте id фракции (см. `01_faction.md`).

Полный чек-лист. Пути относительно `src/main/java/com/solegendary/reignofnether/`.

## 1. Класс юнита

Создать `unit/units/<фракция>/<Имя>Unit.java`. Юнит **наследует ванильного моба** и
**реализует** интерфейсы. Абстрактного базового класса юнита нет, и это не позволяет
наследовать и от ванильного моба, и от своего класса — интерфейсы и есть «база».

Интерфейсы в `unit/interfaces/`:

| Интерфейс | Что даёт |
|---|---|
| `Unit` | ядро, обязателен |
| `AttackerUnit` | атака: урон, дальность, кулдаун, цели |
| `WorkerUnit` | стройка и сбор ресурсов |
| `RangedAttackerUnit` | атака снарядами |
| `HeroUnit` | уровни, мана, очки навыков (см. `05_hero.md`) |
| `ArmSwingingUnit` | процедурная анимация рук |
| `ConvertableUnit` | превращение в другой тип юнита |
| `KeyframeAnimated` | gecko-анимации |

Ограничение из комментария `Unit.java:132-137`: в интерфейсах нельзя писать default-методы,
которым нужны поля `Unit` без геттеров и сеттеров.

## 2. Обязательные элементы класса

```java
public class XUnit extends Zombie implements Unit, AttackerUnit {

    // 2.1 синхронизируемые данные
    private static final EntityDataAccessor<String> OWNER =
            SynchedEntityData.defineId(XUnit.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> OWNER_DATA =
            SynchedEntityData.defineId(XUnit.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SCENARIO_ROLE =
            SynchedEntityData.defineId(XUnit.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> ON_DEATH_COMMAND =
            SynchedEntityData.defineId(XUnit.class, EntityDataSerializers.STRING);

    @Override protected void defineSynchedData() {
        this.entityData.define(OWNER, "");
        this.entityData.define(OWNER_DATA, "");
        this.entityData.define(SCENARIO_ROLE, -1);
        this.entityData.define(ON_DEATH_COMMAND, "");
    }

    // 2.2 атрибуты — всегда от базовых
    public static AttributeSupplier.Builder createAttributes() {
        return Unit.createDefaultAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    // 2.3 цели: создать
    @Override public void initialiseGoals() {
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.attackGoal = new MeleeAttackUnitGoal(this, false);
    }

    // 2.4 цели: навесить
    @Override protected void registerGoals() {
        initialiseGoals();                       // клиент вызывает сам, см. UnitClientEvents
        this.goalSelector.addGoal(2, this.attackGoal);
        this.targetSelector.addGoal(2, this.targetGoal);
        this.goalSelector.addGoal(3, this.moveGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundUnitGoal(this));
    }

    // 2.5 тик — порядок важен
    @Override public void tick() {
        super.tick();
        Unit.tick(this);
        AttackerUnit.tick(this);
    }

    // 2.6 NBT
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        addUnitSaveData(tag);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readUnitSaveData(tag);
    }
}
```

### Приоритеты целей (соглашение по всему коду)

`1` — `FloatGoal`; `2` — все поведенческие цели и цель на `targetSelector`; `3` — цель
перемещения; `4` — `RandomLookAroundUnitGoal`.

### `initialiseGoals()` обязан быть идемпотентным

Клиент не вызывает `registerGoals()` — вместо этого `unit/UnitClientEvents` вызывает
`initialiseGoals()` на новом инстансе, чтобы работали цели выбора. Если метод создаёт цели
без проверки, второй вызов переуставит их.

Убрать ванильные цели (как делает `BeeUnit`):

```java
@Override protected void registerGoals() {
    super.registerGoals();                 // вызвать, иначе NPE
    this.goalSelector.removeAllGoals(g -> true);
    this.targetSelector.removeAllGoals(g -> true);
    initialiseGoals();
    ...
}
```

## 3. Обязательные методы `Unit`

`getOwnerName`/`setOwnerName`, `getScenarioRoleIndex`/`setScenarioRoleIndex`, `getCost`,
`getMoveGoal`, `getTargetGoal`, `getGarrisonGoal`/`canGarrison`,
`getUsePortalGoal`/`canUsePortal`, `getAbilities`, `getItems`, `getMaxResources`,
`getCheckpoints`, `setAnchor`/`getAnchor`, `getFollowTarget`/`setFollowTarget`,
`getHoldPosition`/`setHoldPosition`, `getOnDeathCommand`/`setOnDeathCommand`,
`setEatingTicksLeft`/`getEatingTicksLeft`, `getReturnResourcesGoal`, `updateAbilityButtons`,
`initialiseGoals`, `getCooldowns`, `hasAutocast`/`setAutocast`, `getCharges`.

Полный список с номерами строк — `../docs/FEATURES.md` §3.

## 4. Регистрация

1. `registrars/EntityRegistrar.java` — блок `ENTITIES.register(...)` (паттерн в
   `00_обзор.md`).
2. `registrars/ItemRegistrar.java` — спавн-яйцо:
   ```java
   public static final Supplier<DeferredSpawnEggItem> X_UNIT_SPAWN_EGG =
           ITEMS.register("x_unit_spawn_egg", () -> new DeferredSpawnEggItem(
                   EntityRegistrar.X_UNIT, 0xRRGGBB, 0xRRGGBB, new Item.Properties()));
   ```
3. `ClientModEvents.java` — рендерер в `registerRenderers`. Рендереры свободно делятся:
   несколько юнитов могут использовать один. Если нужна своя 3D-модель — дополнительно
   `registerLayerDefinitions`.

## 5. Производственный предмет

`unit/units/<фракция>/<Имя>Prod.java`:

```java
public class XProd extends ProductionItem {
    public final static String itemName = "X";
    public final static ResourceCost cost = new ResourceCost(MOD_ID, "X");

    public XProd() {
        super(cost, ProdDupeRule.ALLOW);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level,
                        EntityRegistrar.X_UNIT.get(), placement.ownerName, true);
        };
    }
    @Override public String getItemName() { return itemName; }
}
```

Зарегистрировать в `building/production/ProductionItems.java` (поле + в списке `ALL`).
Подробности — `06_production.md`.

## 6. Ассеты

`assets/reignofnether/models/item/x_unit_spawn_egg.json`:
```json
{ "parent": "item/template_spawn_egg" }
```

Ключи в `lang/en_us.json` (и остальные 21 язык): `entity.reignofnether.x_unit`,
`item.reignofnether.x_unit_spawn_egg`, `unitstats.reignofnether.*`, `abilities.reignofnether.*`.

Иконка кнопки производства: `assets/reignofnether/textures/mobheads/x.png`.
Подробнее — `09_assets.md`.

## 7. Проверка в игре

* Юнит спавнится через `/rtsapi unit summon reignofnether:x_unit <игрок>`.
* Приходит к точке и атакует: `left click` цель, `A` — атака-марш, `S` — стоп.
* Владелец определяется верно: в панели выделения имя игрока, цвет по отношениям.
* Юнит умирает и теряет инвентарь без ошибок в логе.
* Путь: `/gamerule rtsPathfinding true`, юнит обходит препятствия, а не идёт напролом.
