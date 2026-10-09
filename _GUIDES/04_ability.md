# Добавление способности

> Гейт по исследованию: `ability.requireResearch(ResearchCondition...)` — проверяется на сервере и
> блокирует кнопку в HUD (см. `07_research.md`; условие инвертируемо).

## Два способа

Способность — это код-класс (`extends Ability`) **и** инстанс, который объявляется двумя путями:

* **данными** — `AbilitySpec` (`type` = id в реестре `AbilityTypes`) в списке `abilities` у
  `UnitDefinition` (и зданий); `UnitDefinitionRuntime.buildAbilities` создаёт инстанс;
* **кодом** — статикой класса-юнита (описано ниже).

⚠ Активная способность рассылается по {@link UnitAction}: серверный `UnitActionItem` находит
способность, у которой `ability.action == action`, и вызывает `use(...)`. Поэтому у активной способности
должна быть **своя константа `UnitAction`** (одна на класс способности). Пассивные эффекты (атрибуты)
можно навешивать прямо в фабрике при создании.

**Data-driven способности работают:** движковый тип `reignofnether:heal` (`SimpleHealAbility`,
params `amount`) зарегистрирован в `BuiltInAbilities.init()` (вызывается из конструктора мода),
демо — у `skeleton_unit`. Автор фракции регистрирует свои типы так же:
`AbilityTypes.register(id, (spec, unit) -> new MyAbility(spec))`. Тип, который не зарегистрирован,
даёт `null` и способность молча не добавляется.

## Устройство

База — `ability/Ability.java`. Способность не регистрируется в реестре: юнит или здание
объявляет список у себя.

```java
public class XAbility extends Ability {
    public static final int CD_SECONDS = 30;
    public static final int RANGE = 8;
    public static final int RADIUS = 0;          // <=0 — одиночная цель

    public XAbility() {
        super(UnitAction.CAST_X,
              CD_SECONDS * ResourceCost.TICKS_PER_SECOND,
              RANGE, RADIUS,
              true,   // canTargetEntities
              false);  // oneClickOneUse
    }

    @Override
    public boolean isCasting(Unit unit) { return ...; }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton("X", TEXTURE, hotkey,
                () -> false,            // isHidden
                () -> canUse(unit),     // isEnabled
                List.of(tooltip), this, unit);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity target) { ... }
}
```

Четыре перегрузки `use`, все с базовой реализацией-заглушкой:
`use(Level, Unit, LivingEntity)`, `use(Level, Unit, BlockPos)`,
`use(Level, BuildingPlacement, BlockPos)`, `use(Level, BuildingPlacement, LivingEntity)`.
Переопределяй нужную; неиспользуемые можно не трогать.

`getButton` **обязателен**: базовый возвращает `null`, и кнопка в HUD не появится.

## Объявление у юнита

```java
public static final Abilities ABILITIES = new Abilities();
static { ABILITIES.add(new XAbility(), Keybindings.abilitySlot1); }

private final Abilities abilities = ABILITIES.clone();   // инстанс
@Override public Abilities getAbilities() { return abilities; }
@Override public void updateAbilityButtons() { abilities = ABILITIES.clone(); }

private final Object2ObjectArrayMap<Ability, Float> cooldowns = Unit.createCooldownMap();
private final Object2ObjectArrayMap<Ability, Integer> charges = new Object2ObjectArrayMap<>();
@Override public Object2ObjectArrayMap<Ability, Float> getCooldowns() { return cooldowns; }
@Override public Object2ObjectArrayMap<Ability, Integer> getCharges() { return charges; }

private Ability autocast;
@Override public boolean hasAutocast(Ability ability) { return autocast == ability; }
@Override public void setAutocast(Ability autocast) { this.autocast = autocast; }
```

Кулдауны и заряды тикаются централизованно в `Unit.tick` — отдельно ничего делать не нужно.

Если хоткей не указан, `Abilities.getButtons` назначит `abilitySlot1..8` по порядку.

## UnitAction

Идентификатор способности — константа в `ability/UnitAction.java`. Новые константы
добавляются туда, плюс обработчик в `unit/UnitActionItem.java`, если действие выдаётся
командой.

## Гейт по исследованию

Система исследований реализована (см. `07_research.md`): `Ability.requiredResearch(ResearchCondition...)`
проверяется на сервере при использовании и блокирует кнопку в HUD (`isEnabled`). Условие инвертируемо
(`invert`). Грант/отзыв — командой `/research grant|revoke`.

```java
() -> ResearchUtils.meets(level, ownerName, List.of(new ResearchCondition(id, false)))   // сервер
() -> ResearchUtils.meetsClient(ownerName, List.of(new ResearchCondition(id, false)))    // клиент/HUD
```

Клиентская проверка — только для HUD; серверная — обязательная защита.

## Способность, привязанная к юниту

Если способность кастует что-то специфичное, она **жёстко привязана к классу юнита**:

```java
@Override public void use(Level level, Unit unitUsing, LivingEntity target) {
    ((XUnit) unitUsing).getXGoal().setAbility(this);
    ((XUnit) unitUsing).getXGoal().setTarget(target);
}
```

Это нормально, но значит, что способность нельзя перенести на другого юнита без правки
класса способности. 36 из 93 удалённых способностей были устроены именно так — это
основная причина, по которой удаление контента тянет за собой способности.

## Выполнение как поведение

Если способность — не мгновенное действие, а каст с каналом, делай отдельную цель в
`unit/goals/`:

```java
this.castXGoal = new GenericTargetedSpellGoal(
        this, 20 /* windup */, Integer.MAX_VALUE, UnitAnimationAction.CAST_SPELL,
        null, this::doX, null);
```

Потом: тик цели в `tick()`, сброс в `resetBehaviours()`, и запрет команд на время каста
через `Unit.ignoreNonStopCommands()`.

## Ассеты

Иконка: `textures/icons/abilities/x.png`. Ключи: `abilities.reignofnether.x` и
`.point1..N` для тултипов.

## Проверка в игре

* Кнопка появилась в панели выделения на слоте, назначенном в `ABILITIES`.
* Нажатие запускает каст, кулдаун виден и тикает.
* Автокаст переключается и работает.
* Заряды тратятся и восстанавливаются, если заданы `maxCharges`.
* На сервере в логе нет исключений при применении.