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

Общие поля `AbilitySpec` прокидываются в созданный инстанс (`UnitDefinitionRuntime.buildAbilities`):
`cooldown`, `range`, `radius`, `canTargetEntities`, `oneClickOneUse`, `passive` (у пассивки нет кнопки),
`mana` (для `HeroAbility`), `requiredResearch`. Для чисел `0`/`false` = «оставить дефолт класса».

`params` — типизированные: **число** (`spec.param("amount", 20)`) или **строка**
(`spec.stringParam(...)`, `spec.resourceParam("unit")` — парсит в `ResourceLocation`). Пример:
`{ "type": "reignofnether:summon", "cooldown": 600, "params": { "unit": "reignofnether:vindicator_unit", "count": 1 } }`
(Demo: движковый `reignofnether:summon` — `SummonUnitAbility`; `reignofnether:heal` умеет `params.sound`).

**Пассивки:** `passive: true` (или класс задаёт `passive = true` в конструкторе) — кнопки нет, действие
никогда не кликается; вместо этого сервер каждый тик зовёт `Ability.tickPassive(Unit)` (из `Unit.tick`).
Движковый пример — `reignofnether:regeneration` (`RegenerationAbility`, params `amount`/`interval`).
Свою пассивку делай так же: унаследуй `Ability`, переопредели `tickPassive`.

## Меню-способность (data-driven)

Движковый тип `reignofnether:menu` (`DataMenuAbility`) открывает подменю из поля `submenu`. Поле
`name` — ключ локализации (или строка) кнопки, `icon` — её иконка. Каждый элемент `submenu` — это
либо **инлайн-способность** (`ability: { type, … }`, может быть вложенным `reignofnether:menu`),
либо **стандартный приказ** (`command`), плюс необязательные `row`/`col`:

```json
{ "type": "reignofnether:menu", "name": "abilities.reignofnether.menu", "submenu": [
  { "ability": { "type": "reignofnether:heal", "cooldown": 200, "params": { "amount": 15 } }, "row": 0, "col": 0 },
  { "command": "stop", "row": 0, "col": 1 },
  { "command": "hold", "row": 1, "col": 0 },
  { "ability": { "type": "reignofnether:menu", "name": "abilities.reignofnether.menu", "submenu": [ { "command": "attack" } ] }, "row": 1, "col": 2 }
] }
```

* `command`: `attack`, `stop`, `hold`, `build`, `gather`, `garrison`, `ungarrison` (те же приказы, что
  в основном ряду кнопок).
* `building: "ns:barracks"` — кнопка **постановки здания** (та же, что в меню постройки воркера). Так автор
  курирует список и порядок зданий; работает и у не-воркера. Требует, чтобы здание существовало (JSON-здание
  или код-реестр).
* `row`/`col` 0-based; `col` — колонка элемента (кнопка «Назад» — левее колонки 0). Элементы **без**
  позиции раскладываются автоматически в оставшиеся ячейки; элементы **с** позицией её сохраняют.
* Вложенный `reignofnether:menu` даёт меню-в-меню; кнопка «Назад» закрывает уровень.
* Демо — у `skeleton_unit.json`.
* Иконка/имя элемента берутся из класса способности (инлайн `label`/`icon` у элемента не применяются).
* «Производство» элементом меню **не** является: состав производства перечисляет само здание
  (`production`). А «постановка здания» — это элемент `building`.
* Гейт по исследованию: у инлайн-способности с `requiredResearch` кнопка внутри меню серая
  (`Abilities.applyResearchGate`), у элемента `building` — research-гейт самого здания.

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