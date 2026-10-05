# Добавление способности

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

Если хоткей не указан, `Abilities.getButtons` назначит `abilitySlot1..6` по порядку.

## UnitAction

Идентификатор способности — константа в `ability/UnitAction.java`. Новые константы
добавляются туда, плюс обработчик в `unit/UnitActionItem.java`, если действие выдаётся
командой.

## Гейт по исследованию

Исследования удаляются (этап E.1 плана), поэтому `hasResearch` в коде не будет. Если
способность должна открываться позже, делай это своим флагом на `RTSPlayer` либо проверкой
владельца в `isEnabled`:

```java
() -> BuildingClientEvents.getPlayerHasUpgrade(playerName, ProductionItems.X)
```

Прежний вариант `() -> !ResearchClient.hasResearch(...)` сервер применения **не блокировал** —
серая была только кнопка. Не рассчитывай на это как на защиту.

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