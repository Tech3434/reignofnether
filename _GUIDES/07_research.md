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
  "type": "unlock",                                   // "unlock" (по умолчанию) | "attribute_boost"
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
* `attributes[].unit` — id типа сущности, если буст только для одного юнита.

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

Buildings.BARRACKS.requireResearch(
    ResearchCondition.of(ResourceLocation.parse("myfaction:barracks")),
    ResearchCondition.not(ResourceLocation.parse("myfaction:barracks_banned")));
```

Проверки выполняются **на сервере** (использование способности, старт производства, постановка
здания) и в HUD (кнопки серые, пока условие не выполнено). GM-команды постановки здания гейт
обходят.

## Атрибутные апгрейды

`type = "attribute_boost"` накладывает модификаторы на юнитов владельца
(`ResearchAttributeApplier`): при спавне юнита, при `grant/revoke/clear` и снимаются при
поражении/`resetRTS`. Модификатор именован по id исследования → идемпотентно.

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
* Генератор ресурсов — форма заложена (`ResourceGenerator` + `ResourceGenerators.produce`),
  конкретные генераторы пишет автор фракции.
