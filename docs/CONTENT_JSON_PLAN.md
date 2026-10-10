# План: JSON-контент (юниты, здания, способности)

Согласовано с владельцем 2026-10-08. Цель — автор фракции описывает юнитов, здания и способности
**данными**, без Java на каждый юнит. Часть рантайма (`Unit` на `Mob`) — код один раз.

## Принятые решения

1. **Юниты — чистый JSON** поверх Code-«движка»: `@Mixin(Mob) implements Unit` + per-unit
   `UnitDefinition` (датапак). Юнит — это обычный `Mob`; кастомная модель/рендер не нужны (тело —
   ванильный или модовый `EntityType`, он рисует себя сам).
2. **Основа и вариации.** `base` — `EntityType` (тело/модель). `inherits` — id другого определения
   (данные) — **реализовано**: наследуются все незаданные поля (атрибуты мержатся по ключам, ребёнок
   побеждает), транзитивно и с защитой от циклов (`UnitDefinitions.resolve`). Пример: `wolf` и `big_wolf`
   оба `base: minecraft:wolf`; большой волк `inherits: myns:wolf` и переопределяет `scale: 1.5` и способности.
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
  "equipment": "minecraft:bow",
  "projectile": { "entity": "minecraft:arrow", "velocity": 1.6, "damage": -1, "inaccuracy": 1.0 },
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

Герой (`role: hero`) добавляет блок прокачки `hero` (макс. уровень, множитель опыта) и ранги способностей.
Воркер (`role: worker`) добавляет блок `worker` — `gatherable` (список ресурсов для цикла сбора),
`buildSpeed` (множитель вклада в стройку, дефолт 1.0) и `carryCapacity` (перенесён с верхнего уровня).
Старое верхнеуровневое `carryCapacity` больше не читается — его теперь явно диагностирует валидация
(см. «Валидация JSON» ниже).

## Здания (полностью JSON)

`data/<namespace>/building/<name>.json`, id = `<namespace>:<name>`.

* **Структура** — NBT-файл (`structureName`, автор строит в игре Structure Block-ом; как сейчас).
  Реализованы: `cost`, `maxHealth`, `isCapitol`, `populationSupply`, `requiredResearch`, `icon`/`name`.
  Флаги под `"flags"`: `canAcceptResources`, `buildTimeModifier`, `captureRange`, `capturable`,
  `invulnerable`, `repairable`, `repairTimeModifier`, `drawAggro`, `scaffoldFill`, `scaffoldBlock`,
  `portrait`, `foundationYLayers` (сколько нижних Y-слоёв структуры — фундамент: их типы становятся
  `startingBlockTypes` и пре-квеятся при размещении; дефолт 1 — иначе здание считалось бы
  «уничтоженным» на первом тике).
* **Производство** — на здании: `production: [ { "unit": "…", "key": "abilitySlot1", "costOverride": {…} } ]`.
* **Исследования** (реализовано) — на здании:
  `researches: [ "…", … ]` — какие исследования здание предлагает; очередь общая с производством
  (`ResearchProductionItem`), отмена с возвратом, завершение выдаёт исследование владельцу и пересчитывает
  атрибуты. В демо: `example_research` у `barracks`.
* **Аддоны** (реализовано) — `addons: [ { "type": "reignofnether:garrison", "params": {…} }, … ]`
  (реестр код-типов аддонов с Codec'ами, как способности). Движковые типы: `night_source`
  (`range`, `showOnlyWhenSelected`), `range_indicator` (`range`, `showOnlyWhenSelected`),
  `garrison` (`capacity`, `attackRange`, `externalAttackRangeBonus`, `entryX/Y/Z`, `exitX/Y/Z`),
  `nether_converting` (`maxRange`, `startingRange`). Автор фракции расширяет через `AddonTypes.register`.
* **Апгрейды** (реализованы) — `upgrades: [ { "structure": "…", "name": { "en_us": "…" }, "icon": "…",
  "cost": {…}, "maxHealth": …, "populationSupply": …, "production": […], "researches": […],
  "addons": […] }, … ]`. Каждое поле — необязательное переопределение предыдущего уровня. Уровни
  материализуются как `JsonBuilding`-варианты (`JsonBuildingManager`); апгрейд в очереди здания
  (`JsonUpgradeProductionItem`) переключает placement на вариант следующего уровня, поэтому
  производство/исследования/аддоны/способности/имя/иконка меняются автоматически.
* **Кастомные кнопки** (реализовано) — отдельный датапак `rts_buttons` + маппинг
  `data/<ns>/reignofnether/custom_button_mappings.json`; привязываются к зданиям по id (код или JSON) и к
  юнитам по id определения (`unit_definitions`) или телу (`entities`). Гайд `_GUIDES/10_custom_buttons.md`.

## Способности (класс — код, инстанс — JSON)

> **Реализовано:** реестр `AbilityTypes`; движковые типы `reignofnether:heal`, `regeneration`, `summon`,
> `menu`. Меню (`submenu`/`name`/`icon`, вложенность, `command`, `row`/`col`) — см. раздел «Сабменю».
>
> **Реализовано (базово):** реестр `AbilityTypes` + движковый тип `reignofnether:heal`
> (`SimpleHealAbility`, params `amount`), регистрация в `BuiltInAbilities.init()`; `UnitDefinitionRuntime.
> buildAbilities` создаёт инстансы из `abilities` и прокидывает `cooldown`/`range`/`radius`/
> `canTargetEntities`/`oneClickOneUse`/`passive`/`mana` (для героя)/`requiredResearch`. Активная
> способность требует своей константы `UnitAction` (по ней её находит `UnitActionItem`).

Общие поля инстанса: `type` (id код-класса), `cooldown`, `mana`, `requiredResearch`,
`heroLevel`/`rank` (для героев), `autocast`, `range`, `passive` (пассивный эффект — яд при ударе и
т.п.), `submenu`/`row`/`col`. Классовый `Codec` добавляет свои параметры (напр. `poison_on_hit` →
`amount`). `params` — типизированные: число или строка (`resourceParam` парсит `ResourceLocation`), напр.
`"params": { "unit": "reignofnether:skeleton_unit", "count": 1 }`.

* **Пассивки** — это те же способности с `passive: true` (отдельного списка `passives` нет); кнопки нет,
  сервер каждый тик зовёт `Ability.tickPassive(Unit)`. Движковый пример — `reignofnether:regeneration`
  (`amount`/`interval`).
* **Генератор ресурсов (H.9)** — тоже способность (пассивная у здания), тип `resource_generator`
  с параметрами `{ resource, amount, interval, capacity }`.
* **Ranged** — атрибуты (`attack_range`, `attacks_per_second`, урон) + **тип снаряда**; набор
  снарядов — **расширяемый реестр** (ванильные: `arrow`, `spectral_arrow`, `fireball`,
  `small_fireball`, `wither_skull`, `snowball`, `egg`, `trident`, …; позже можно добавить модовые).

## Сабменю (меню способностей) — ✅ реализовано

Движковый тип `reignofnether:menu` (`DataMenuAbility`): `name`/`icon` — кнопка меню, `submenu` — список
элементов. Раскладка по умолчанию — автоматическая; `row`/`col` **имеют приоритет** над автораздачей.
Гайд — `_GUIDES/04_ability.md`; демо — `skeleton_unit.json`.

```json
"submenu": [
  { "row": 0, "ability": { "type": "myns:rally", "cooldown": 200 } },
  { "row": 0, "building": "myns:barracks" },
  { "row": 1, "ability": { "type": "reignofnether:menu", "submenu": [ { "command": "attack" } ] } },
  { "row": 1, "command": "attack" }
]
```

Виды элементов (реализованы): **инлайн-способность** (`ability: {type,…}`, может быть вложенным меню) и
**команда** (`command`: `attack`/`stop`/`hold`/`build`/`gather`/`garrison`/`ungarrison`), плюс
**постановка здания** (`building: "ns:id"`). Ссылок на способность по id и элементов «производство»
**нет**: список производств перечисляет само здание (`production`), меню лишь открывает уже доступные
кнопки.

## Имена, иконки, фракции

* **Фракции у контента нет.** Юниты/здания не тегируются фракцией: меню, производство,
  строительство и исследования задаются **явными ссылками** (что кем производится/строится/изучается).
  Фракция-реестр остаётся только для старта матча (столица + стартовые юниты).
* **Имена:** необязательное поле `name` — карта `язык → строка` прямо в JSON
  (`{ "en_us": "…", "ru_ru": "…" }`); **ключи локализации** (`unit.<ns>.<id>`, `building.<ns>.<id>`)
  имеют **приоритет** над JSON.
* **Иконка/портрет:** по умолчанию из `base`-моба (спавн-яйцо/голова/текстура); переопределяется
  полем `icon`.

## Спавн произведённых юнитов и ралли

Уже реализовано: маркер-блок **`production_spawn_block`** («где спавнятся обученные юниты») и
**ралли-точки** у производящего здания (правый клик по земле/юниту при выбранном здании; несколько
точек; флаг `attackRally`).

Дизайн: при производстве юнит спавнится на `production_spawn_block` (если блоков несколько —
**случайный**), иначе **рядом со зданием**. Затем: если задана ралли-точка — идёт туда, иначе
остаётся у здания.

## Отображение владельца и здоровья

Владелец показывается **цветом** (как сейчас). **Полоски здоровья над юнитами — удалить** (задача в
плане работ).

## Воркер (сбор/стройка) — ✅ реализовано

Блок `worker` в определении юнита: `gatherable` (список `food`/`wood`/`ore`, между которыми воркер
переключается), `buildSpeed` (множитель вклада в стройку), `carryCapacity` (запас переноски, по умолч. 100).
Реплант — по-прежнему общий параметр (`ResourceCosts.REPLANT_WOOD_COST`).

## Валидация JSON

* **Схема файла (реализовано):** `ContentValidator` обходит JSON по записи-получателю и сообщает
  **каждое незнакомое поле** с путём: `<file> 'flags': unknown field 'foundationYLayer'
  (accepted: buildTimeModifier, canAcceptResources, …)`. Лог — `ContentValidationReloadListener`
  (мир загружен / `/reload`), жёсткий гейт — `ContentValidationTest` в `test`.
  Проверяются имена полей (значения — кодек); свободные `Map` (`attributes`, `params`, локализованный
  `name`) принимают любые ключи. Покрыто: `unit`/`building`/`faction`. Пока **не** покрыто:
  `research` (разбирается вручную `ResearchJsonLoader`).
* **Тип/диапазон/обязательность:** по-прежнему кодек — например, отсутствие `structure` у здания даёт
  ошибку загрузки реестра (с именем файла), а валидация полей её дополняет.
* **Одно плохое поле не роняет остальные файлы:** определение регистрируется, а поле игнорируется
  (кодек ленив к лишним ключам), поэтому диагностика — единственный способ это заметить.
* **Пропуск невалидного файла целиком:** не реализовано (реестры читает Minecraft до reload-листенеров).

## Способности в UI

* **Хоткеи — авто** по порядку (`abilitySlot1..8`); **тултип** — из имени (lang/JSON).

## Исследования в UI

* **Отдельного «дерева» нет.** Исследования — элементы UI **здания** (как производство/абилки);
  здание перечисляет `researches`, предпосылки лишь **гейтят доступность** (кнопка серая). Глобальная
  панель `ResearchMenu` — опциональный обзор.

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
