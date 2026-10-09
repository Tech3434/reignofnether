# Производство, стоимости, ресурсы

> Гейт по исследованию: `ProductionItem.requireResearch(ResearchCondition...)` учитывается в
> `canProduce` (см. `07_research.md`).

## Цепочка

```
кнопка "старт" -> BuildingProductionServerboundPacket.startProduction(production)
  -> ProductionPlacement.startProductionItem(prodItem, ticksLeft)
       проверки: ProdDupeRule, canProduce, canAfford, лимит армии
       списание стоимости
  -> каждый тик: ProductionItem.tick(placement, activeProduction)
       active.ticksLeft--; по достижении 0 -> complete()
  -> ActiveProduction.complete(placement)
       recordScore(); onComplete.accept(level, placement)
  -> снятие с очереди + пакет клиенту
```

## `ProductionItem`

`building/production/ProductionItem.java`. Абстрактный метод один — `getItemName()`.

| Поле | Смысл |
|---|---|
| `defaultCost` | `ResourceCost` |
| `onComplete` | `BiConsumer<Level, ProductionPlacement>` — что происходит по завершении |
| `dupeRule` | `ProdDupeRule` |

| `ProdDupeRule` | Когда применяется |
|---|---|
| `ALLOW` | обучение юнитов, дубли разрешены |
| `DISALLOW_FOR_BUILDING` | улучшение конкретного здания |
| `DISALLOW` | глобальное исследование, одно на владельца |

Переопределяемые методы: `canProduce`, `getProduceErrorMsg`, `getCost(boolean isClientSide, String owner)`,
`canAfford`, `canAffordPopulation`, `isBelowPopulationSupply`, `isBelowMaxPopulation`,
`getStartButton`, `getCancelButton`, `recordScore`, `tick`.

## Кнопки

`getStartButton(ProductionPlacement, Keybinding)` по умолчанию возвращает `null` — легаси-путь
`ProductionItemList.getButtons` его пропускает. У JSON-предметов (`JsonProductionItem`/`ResearchProductionItem`/
`JsonUpgradeProductionItem`) кнопку запуска даёт `ProductionAbility` (из `getAbilities()` здания),
поэтому `getStartButton` им не нужен. `getCancelButton` по умолчанию возвращает `StopProductionButton`
(кнопка отмены в очереди) — переопределяй только для особой логики.

```java
@Override
public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
    return new StartProductionButton(
        getItemName(), iconRl, hotkey,
        () -> isBeingProduced(prodBuilding.ownerName) || alreadyOwned(prodBuilding.ownerName),
        () -> BuildingClientEvents.hasFinishedBuilding(Buildings.Y));
}
```

## Стоимость

`resources/ResourceCost.java` — четыре числа (`food`, `wood`, `ore`, `emerald`), плюс `ticks`
и `population`. После этапа D.14 плана:

* **154 записи конфига удалены**;
* стоимость хранится **на самом `ProductionItem`**, а не строится по списку юнитов —
  иначе нельзя завести свои ресурсы на фракцию;
* `population` из стоимости уходит в лимит армии (см. ниже).

Фабрики: `ResourceCost.Unit(food, wood, ore, seconds, population)`,
`ResourceCost.Research(...)`, `ResourceCost.Building(food, wood, ore, supply)`,
`ResourceCost.Enmeralds(int)` — последняя в текущем коде игнорирует аргумент, при
использовании это исправить.

## Лимит армии

**Правило владельца ветки:** базовый лимит **1 юнит**, прирост даёт столица фракции.

Реализовано (этап E.4): `BuildingServerEvents.getTotalPopulationSupply` суммирует `populationSupply`
построенных зданий (у JSON-здания — из определения), а `ProductionItem.canAffordPopulation` сравнивает
текущее население (юниты владельца + предметы в очередях) с `maxPopulation = 1 + Σ populationSupply`.

## JSON-предметы производства

У data-driven здания (`JsonBuilding`) очередь наполняется из определения:

* `production: [ "ns:unit" ]` → `JsonProductionItem` (спавн юнита по id определения);
* `researches: [ "ns:research" ]` → `ResearchProductionItem` (общая очередь; по завершении — грант
  исследования владельцу + пересчёт атрибутов);
* `upgrades: [ … ]` → `JsonUpgradeProductionItem` (поднимает уровень здания, переключает на вариант —
  см. `03_building.md` «Апгрейды»).

Сетевой id предмета — `ProductionItem.getNetworkId()`: код-предметы берут ключ из
`ReignOfNetherRegistries.PRODUCTION_ITEM`, JSON/исследования/апгрейды переопределяют его (id определения/
исследования/`upgrade:N`). Клиент шлёт этот id в `BuildingProductionServerboundPacket`, сервер резолвит
через `JsonBuilding.getProductionItem(id)`.

## Ресурсы

`Resources` — баланс на владельца: `food`, `wood`, `ore`, `emerald`, плюс поля
`foodToAdd` и подобные для анимации счётчика в HUD.

* `ResourcesServerEvents.resourcesList` — список балансов.
* `ResourcesServerEvents.canAfford(ownerName, cost)` — проверка.
* Пакеты синхронизации: `ResourcesClientboundPacket` (сервер → клиент),
  `ResourcesServerboundPacket` (клиент → сервер).

## Сбор ресурсов

`ResourceSources` (что выпадает с животных/блоков) и цели сбора (`unit/goals/GatherResourcesGoal`,
`ReturnResourcesGoal`, `BuildRepairGoal`) — рабочий контур рабочего: добыча → переноска → доставка в
здание с `flags.canAcceptResources` (у столицы — `true`). Начальные объёмы — константы в
`ResourcesServerEvents`. Режим сбора — `UnitAction.TOGGLE_GATHER_TARGET` (Food/Wood/Ore).

## Проверка в игре

* Кнопка производства запускает очередь, стоимость списывается один раз.
* При нехватке ресурсов кнопка неактивна, в логе или чате — понятная причина.
* Лимит армии: при одной ратуше можно построить N юнитов, без ратуши — один.
* Смена стоимости в коде видна без перезапуска, если не требует CommonSetup.