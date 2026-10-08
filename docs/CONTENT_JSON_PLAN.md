# План: JSON-контент (юниты, здания, способности)

Согласовано с владельцем 2026-10-08. Цель — автор фракции описывает юнитов, здания и способности
**данными**, без Java на каждый юнит. Часть рантайма (`Unit` на `Mob`) — код один раз.

## Принятые решения

1. **Юниты — чистый JSON** поверх Code-«движка»: `@Mixin(Mob) implements Unit` + per-unit
   `UnitDefinition` (датапак). Юнит — это обычный `Mob`; кастомная модель/рендер не нужны (тело —
   ванильный или модовый `EntityType`, он рисует себя сам).
2. **Основа и вариации.** `base` — `EntityType` (тело/модель). `inherits` — id другого определения
   (данные). Пример: `wolf` и `big_wolf` оба `base: minecraft:wolf`; большой волк `inherits: myns:wolf`
   и переопределяет `scale: 1.5` и способности.
3. **Идентичность юнита — определение, а не EntityType.** Вариант хранится на экземпляре
   (синхронизируемо), поэтому два юнита могут делить тело, но различаться статами/способностями.
4. **Дискриминатор.** `Unit.isRtsUnit()` = у моба есть определение. Все проверки `instanceof Unit`
   закрываются этим флагом (иначе все мобы мира стали бы «юнитами»).
5. **Способности — `type` + параметры.** `type` = id зарегистрированного код-класса способности; у
   класса свой `Codec` для параметров. Пример: `{ "type": "poison_on_hit", "amount": 1 }`.
6. **Поведение — `role` + флаги.** `role`: `melee`/`ranged`/`worker`/`flying`/`hero`; флаги:
   `canGather`, `canBuild`, `canGarrison`, `holdPosition`, … Покрывает большинство юнитов без
   JSON-списков целей.
7. **Цена/население/исследование** — на определении юнита; здание может переопределить.
8. **Производство** перечисляется **на здании**: `production: [ { "unit": "…", … } ]`.
9. **Ranged** — из атрибутов (`attack_range`, `attacks_per_second`, урон) + поле типа снаряда
   (`arrow`/`fireball`/…).
10. **Герой** — `role: hero` + поля прокачки (макс. уровень, формула опыта, приросты за уровень).
11. **Атрибуты** — карта `attribute-id → значение`; `scale` — ванильный атрибут `SCALE`.

## Схема юнита (черновик)

`data/<namespace>/unit/<name>.json`, id = `<namespace>:<name>`.

```json
{
  "base": "minecraft:wolf",
  "inherits": "myns:wolf",
  "icon": "minecraft:textures/item/bone.png",
  "role": "melee",
  "flags": { "canGather": false, "canBuild": false, "canGarrison": false, "holdPosition": true },
  "scale": 1.5,
  "attributes": {
    "minecraft:generic.max_health": 12,
    "minecraft:generic.attack_damage": 4,
    "minecraft:generic.movement_speed": 0.32,
    "reignofnether:attack_range": 1.5,
    "reignofnether:attacks_per_second": 1.0,
    "reignofnether:sight_range": 16
  },
  "abilities": [
    { "type": "poison_on_hit", "amount": 1 },
    { "type": "some_active", "cooldown": 20, "mana": 10 }
  ],
  "cost": { "food": 50, "wood": 0, "ore": 0, "seconds": 10 },
  "population": 1,
  "requiredResearch": [ { "research": "myns:beasts", "invert": false } ]
}
```

Герой (`role: hero`) добавляет блок прокачки (макс. уровень, опыт/уровень, приросты HP/маны/урона) и
ранги способностей.

## Реализация (код, один раз)

* `unit/UnitDefinition` (Codec) + датапак-реестр `reignofnether:unit` (`DataPackRegistryEvent`).
* `@Mixin(Mob.class) implements Unit`:
  * `@Unique`-состояние (владелец, якорь, checkpoints, cooldowns/charges, items, goals, definitionId);
    владелец и definitionId — в синхронизируемых данных (по образцу `UnitInventoryMobMixin`).
  * `initialiseGoals()` — цели по `role`/флагам; `updateAbilityButtons()` — из `abilities`.
  * `isRtsUnit()` — есть ли определение.
* Атрибуты: базовые модовые атрибуты добавляются к `EntityType` через `EntityAttributeModificationEvent`;
  значения из определения применяются при спавне.
* Все `instanceof Unit` (и, где нужно, `WorkerUnit`/`AttackerUnit`/`RangedAttackerUnit`) переводятся на
  `isRtsUnit()`/флаги — это самая широкая, но механическая часть.

## Фазы

1. **Ядро:** `UnitDefinition` (Codec) + датапак-реестр + `MobMixin implements Unit` + `isRtsUnit()` +
   перевод проверок `instanceof Unit` на флаг (базовое: выделение, приказы, спавн/владение).
2. **Данные юнита:** атрибуты/scale, `role`/флаги, цели, способности (`type`+параметры) с реестром
   типов и Codec'ами.
3. **Производство и меню:** список на здании, цена/население/исследование, портрет/иконка.
4. **Герои и ranged:** `role: hero` (прокачка/ранги), `role: ranged` (атрибуты + снаряд).
5. **Здания — полностью JSON** (структура + параметры + `production` + `researches` + аддоны).

## Риски

* Широкая правка `instanceof` — делать поэтапно с гейтами, без «полу-состояний».
* Два юнита с общим телом различаются только определением → всё, что читает ванильный `EntityType`
  для имени/иконки, должно уметь смотреть определение.
* Модовые атрибуты на ванильных типах — через `EntityAttributeModificationEvent` (штатный путь).
