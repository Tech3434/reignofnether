---
name: reignofnether-add-ability
description: Как добавить способность (класс + JSON-инстанс) или data-driven меню в Reign of Nether. Использовать при создании новой способности или подменю.
---

# Как добавить способность

Способность = **код-класс** (`extends Ability`) + **инстанс** из данных. Полный разбор —
`_GUIDES/04_ability.md`. Юнит/здание получают её через `abilities` в своём JSON.

## Инстанс (данные)

`{ "type": "ns:id", "cooldown": …, "range": …, "radius": …, "passive": true, "mana": …,
   "requiredResearch": […], "params": { "amount": 5, "unit": "ns:unit" } }`.

* `params` — типизированные: число или строка (`resourceParam` парсит `ResourceLocation`).
* `passive: true` — кнопки нет; сервер каждый тик зовёт `Ability.tickPassive(Unit)`.
* Активная способность рассылается по `UnitAction`: **своя константа `UnitAction`** + обработчик.

## Класс (когда нужен новый тип)

1. `class XAbility extends Ability`, в конструкторе — кулдаун/радиус/`action` (или через `AbilitySpec`).
2. Зарегистрируй тип: `AbilityTypes.register(id("x"), (spec, unit) -> new XAbility(spec))`. Движковые
   регистрируются в `ability/BuiltInAbilities.init()`. Тип, который не зарегистрирован → `null`,
   способность молча не добавляется.
3. Для пассивки переопредели `tickPassive(Unit)`; для активной — `use(...)` (4 перегрузки).
4. Гейт по исследованию — `requiredResearch` в spec (проверяется на сервере и в HUD).
5. Иконка/имя — `ability.getButton(...)`; строка тултипа из lang.

## Меню (`reignofnether:menu`)

Движковый тип для подменю: `name`/`icon` (кнопка) + `submenu`. Элемент меню:

```json
{ "ability": { "type": "ns:x", "cooldown": 100 }, "row": 0, "col": 0 }
{ "command": "stop" }
{ "building": "ns:barracks" }
{ "ability": { "type": "reignofnether:menu", "name": "…", "submenu": [ … ] } }
```

* `row`/`col` 0-based (`col` — колонка элемента, «Назад» левее колонки 0); элементы без позиции
  раскладываются автоматически.
* Вложенный `menu` = меню-в-меню.
* `building: "ns:barracks"` — кнопка постановки здания (курируемый список; работает и у не-воркера).
* Вложенные **инлайн**-способности диспатчатся: `Abilities.get()` разворачивает меню, а `UnitActionItem`
  ищет действие по этому плоскому списку. `command`-элементы в него не добавляются — они отправляют
  приказ кликом (клик по кнопке), а инстансы `CommandAbilities` общие для всех меню и юнитов.
* Меню из одних `building`/`command`-элементов — норма: скрытость кнопки считается по числу **элементов**
  (`MenuAbility.hasNoEntries()`), а не по `subAbilities`.
* Research-гейт инлайн-элемента применяется (`requiredResearch` → кнопка серая); у `building`-элемента —
  гейт самого здания.

## Грабли

* Активной способности нужна **своя** `UnitAction`, иначе `UnitActionItem` не найдёт её.
* `Ability.action == null` у меню — не сравнивай `action.equals(...)` без null-проверки.
* `getButton` по умолчанию возвращает `null` → кнопки не будет.
* Опечатка в имени поля **не пройдёт незамеченной**: имена сверяются с record'ами (`ContentValidator`),
  `test` валит сборку, а в игре ошибка пишется в лог — `unknown field 'coolDown' (accepted: cooldown, …)`.
  Свободная карта `params` принимает любые ключи (они проверяются самим классом способности).

## Гейт

`compileJava` → `validateMixins` → `runData` → `test`.
