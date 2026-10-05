# Производство, стоимости, ресурсы

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

`getStartButton(ProductionPlacement, Keybinding)` и `getCancelButton`. **По умолчанию
возвращают `null`** — если не переопределить, предмет не появится в UI.

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

**Правило владельца ветки:** базовый лимит **1 юнит**, прирост даёт ратуша фракции.

Текущий код так не устроен: `BuildingServerEvents.getTotalPopulationSupply` суммирует
`cost.population` построенных зданий, а `ProductionItem.canAffordPopulation` сравнивает с ним.
После этапа E.4 плана ожидается:

```java
public static int maxPopulation = 1;                                   // база
public static int getPopulationBonusFromCapitols(String ownerName) { ... }  // сумма по isCapitol
```

и сравнение `currentPop + population <= maxPopulation + бонус от ратуш`.

Счётчик `UnitServerEvents.getCurrentPopulation` считает население юнитов владельца плюс
предметы в очередях производства.

## Ресурсы

`Resources` — баланс на владельца: `food`, `wood`, `ore`, `emerald`, плюс поля
`foodToAdd` и подобные для анимации счётчика в HUD.

* `ResourcesServerEvents.resourcesList` — список балансов.
* `ResourcesServerEvents.canAfford(ownerName, cost)` — проверка.
* Пакеты синхронизации: `ResourcesClientboundPacket` (сервер → клиент),
  `ResourcesServerboundPacket` (клиент → сервер).

**Что удаляется на этапе D.14:** `ResourceSources` (перечень того, что собирается),
`ResourceIndex` и `ResourceChunk` (пространственный индекс блоков по чанкам),
`ResourcesSaveData`. Без них рабочими остаются баланс, списание, начисление и синхронизация.

Начальные объёмы задаются в `ResourcesServerEvents` константами.

## Сбор ресурсов

Цели сбора живут в `unit/goals/` и удаляются вместе с контентом. Если владелец захочет
снабжение рабочих — это новая цель поверх пула `Resources`.

## Проверка в игре

* Кнопка производства запускает очередь, стоимость списывается один раз.
* При нехватке ресурсов кнопка неактивна, в логе или чате — понятная причина.
* Лимит армии: при одной ратуше можно построить N юнитов, без ратуши — один.
* Смена стоимости в коде видна без перезапуска, если не требует CommonSetup.