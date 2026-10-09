# Исследования (новая система)

> Обновлено 2026-10-08. Прежняя система (`ResearchServerEvents`, флаги-`ProductionItem`,
> `ResearchClient`) удалена. Ниже — действующая система (план `docs/RESEARCH_AND_EXTENSIBILITY_PLAN.md`).

## Что это

Исследования — **на игрока**. Дерево с предпосылками; условие умеет **инверсию** (требовать не
только наличие, но и отсутствие технологии). Исследование открывает способности, новые
здания/юниты и/или усиливает атрибуты. **Поражение игрока обнуляет его исследования.**

Состояние: `ResearchSaveData` (`saved-research-data`, per-player, персистентно, лениво), клиенту
синхронизируется `ResearchClientboundPacket` → `ResearchClientEvents`.

## Определения — данными (датапак)

Файл: `data/<namespace>/research/<name>.json`, id исследования = `<namespace>:<name>`.
Перечитывается на `/reload` (`ResearchJsonLoader`).

```json
{
  "name": "research.reignofnether.example",          // ключ локализации (или литерал)
  "icon": "reignofnether:textures/icons/items/shovel.png",
  "type": "unlock",                                   // "unlock" (по умолч.) | "attribute_boost" | "equip"
  "cost": { "food": 0, "wood": 100, "ore": 0, "seconds": 20 },  // необязательно = бесплатно
  "prerequisites": [
    { "research": "reignofnether:base", "invert": false }        // invert=true → требуется ОТСУТСТВИЕ
  ],
  "attributes": [                                     // только для type = attribute_boost
    { "attribute": "minecraft:generic.attack_damage", "amount": 1.0,
      "unit": "reignofnether:villager_unit" }         // "unit" необязательно = всем юнитам владельца
  ]
}
```

* `icon` — полный путь текстуры (`<ns>:textures/...`); для ванили, напр.
  `minecraft:textures/item/iron_pickaxe.png`.
* `attributes[].attribute` — id ванильного или модового атрибута.
* `attributes[].unit` — id юнита, если буст только для одного: id типа сущности (для ванильного тела)
  **или** id определения юнита (`reignofnether:skeleton_unit`) для data-driven юнитов — матчится и то, и другое.

## Гейт (универсальный, с инверсией)

Условие: `ResearchCondition(researchId, invert)`. Проверка: `meets = hasResearch(id) != invert`.
Наличие/отсутствие — один и тот же вызов.

```java
// сервер (авторитетно):
ResearchUtils.meets(level, ownerName, conditions);
// клиент (для HUD):
ResearchUtils.meetsClient(ownerName, conditions);
```

Гейты контента уже встроены: у `Ability`, `ProductionItem` и `Building` есть поле
`requiredResearch` (список условий) и флюент-сеттер `requireResearch(condition...)`.

```java
new DigAbility(UnitAction.DIG_BLOCK)
    .requireResearch(ResearchCondition.of(ResourceLocation.parse("myfaction:toolsmith")));

// у код-здания (свой class Building):
myBuilding.requireResearch(
    ResearchCondition.of(ResourceLocation.parse("myfaction:barracks")),
    ResearchCondition.not(ResourceLocation.parse("myfaction:barracks_banned")));
```

У JSON-зданий/юнитов условия задаются в определении: `"requiredResearch": [ { "research": "myns:x", "invert": false } ]`
(здания, юниты, способности).

Проверки выполняются **на сервере** (использование способности, старт производства, постановка
здания) и в HUD (кнопки серые, пока условие не выполнено). GM-команды постановки здания гейт
обходят.

## Атрибутные апгрейды

`type = "attribute_boost"` накладывает модификаторы на юнитов владельца
(`ResearchAttributeApplier`): при спавне юнита, при `grant/revoke/clear` и снимаются при
поражении/`resetRTS`. Модификатор именован по id исследования → идемпотентно.

## Экипировка (`type = "equip"`)

Исследование-выдача снаряжения: по завершении владелец получает предметы у своих подходящих юнитов
(`ResearchEquipApplier`; идемпотентно, только сервер; `revoke` снаряжение не снимает).

```json
{
  "name": "research.myns.ironswords",
  "icon": "minecraft:textures/item/iron_sword.png",
  "type": "equip",
  "equip": [
    { "item": "minecraft:iron_sword", "slot": "mainhand", "unit": "myns:footman" },
    { "item": "minecraft:iron_helmet", "slot": "head" }
  ]
}
```

* `equip[].item` — id предмета; `slot` — `mainhand` (по умолч.) / `offhand` / `head` / `chest` / `legs` / `feet`.
* `equip[].unit` — необязательно: id типа сущности или id определения юнита.

## Команды (право 2)

```
/research grant  <player> <ns:id>
/research revoke <player> <ns:id>
/research clear  <player>
/research list   <player>
```

## Запуск исследований у здания (реализовано)

Здание перечисляет свои исследования в своём JSON:

```json
"researches": [ "myns:some_research" ]
```

Каждое превращается в `ResearchProductionItem`, который **делит очередь производств** здания: занимает слот,
отменяется с возвратом, а по завершении выдаёт исследование владельцу (грант в `ResearchSaveData` + синк
клиенту + `ResearchAttributeApplier.refreshForOwner`). Кнопки запуска живут в UI выбранного здания рядом с
производством (`ProductionAbility`), глобальная `ResearchMenu` остаётся панелью статуса. Предпосылки
проверяются при старте (`ResearchUtils.meets`), повторное исследование запрещено. Демо:
`data/reignofnether/research/example_research.json`, привязано к `barracks.json`.

## Что ещё не сделано

* **HUD-панель исследований** (дерево, стоимость, статус) — фаза 5 плана (статус уже есть).
* Генератор ресурсов — сделан как аддон здания `reignofnether:resource_generator`
  (`{ "resource": "wood", "amount": 5, "interval": 100, "capacity": 500 }`, см. `03_building.md`).
