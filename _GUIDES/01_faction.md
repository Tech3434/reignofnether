# Как создать свою фракцию

> Переписано 2026-10-08 под текущий каркас. Enum `Faction` и старый `FactionRegistries.java` удалены.

## Фракция — объект реестра (датапак)

Фракция — это объект **datapack-реестра** (`data/<namespace>/faction/<name>.json`, id =
`<namespace>:<name>`); реестр синхронизируется клиентам. Доступ в коде:
`FactionRegistries.get(server, id)`; дефолт — `FactionRegistries.DEFAULT_FACTION`.

```json
{
  "name": "faction.<ns>.<id>",
  "icon": "minecraft:textures/block/polished_granite.png",
  "capitol": "reignofnether:town_centre",
  "starting_units": [ { "unit": "reignofnether:villager_unit", "count": 1 } ],
  "food": 0, "wood": 0, "ore": 0, "emerald": 0
}
```

## Старт матча по фракции

`PlayerServerEvents.startRTS(playerId, pos, startPosColorId, factionId)` читает фракцию: ставит её
`capitol`, спавнит `starting_units` (с учётом `count`). Если фракция или её энтри не найдены —
фолбэк на константы (`STARTING_ARMY`, `Buildings.TOWN_CENTRE`). Путь входа (хоткей, команда,
стартовый блок) передаёт id фракции; дефолт — `FactionRegistries.DEFAULT_FACTION`.

## Шаги

1. **JSON фракции** — `data/<ns>/faction/<name>.json` (см. схему выше).
2. **Юнит(ы)** — `02_unit.md`; зарегистрировать (`EntityRegistrar`) и указать в `starting_units`.
3. **Столица** — `03_building.md`; её id идёт в `capitol`.
4. **Производство** — `06_production.md`.
5. **Исследования** — `07_research.md`.
6. **Локализация** — `faction.<ns>.<name>`, `buildings.<faction>.<ns>.<path>`, `units…`.
7. **Ассеты** — `09_assets.md`.

## Идентичность игрока

Владельца везде хранит **строка-имя** (не UUID): юнит — `ownerName` (синхронизируемый
`EntityDataAccessor`), здание — поле `ownerName`; сравнение через `String.equals`.

## Чего не делать

* Не заводить enum `Faction` и не возвращать старый `FactionRegistries` — это удалено.
* `Unit.getFaction()`/`Building.getFaction()` сейчас — **строковая метка** для lang-ключей
  (`buildings.<faction>.<ns>.<path>`); при использовании реестра фракций указывайте там id фракции.
