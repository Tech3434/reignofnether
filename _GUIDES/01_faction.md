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

## Выбор фракции игроком

Команда **`/startrts [<faction>]`** (требуется RTS-пропуск):

* **без аргумента** — открывает меню всех зарегистрированных фракций; выбрать можно любую;
* **с фракцией** — открывает то же меню, но выбрать можно **только эту**; остальные кнопки видны,
  но заблокированы (как недоступные абилки/строения/юниты).

Клик в меню стартует матч на позиции игрока с выбранной фракцией
(`PlayerServerboundPacket.startRTS(x, y, z, factionId)`). Список фракций синхронизируется клиентам
(`FactionClientboundPacket` → `FactionClientEvents`).

## Старт матча по фракции

`PlayerServerEvents.startRTS(playerId, pos, startPosColorId, factionId)` читает фракцию: ставит её
`capitol`, спавнит `starting_units` (с учётом `count`). Резолв столицы: код-`BUILDING` →
`JsonBuildingManager` → дефолт id `reignofnether:town_centre`; стартовый отряд — из фракции либо
фолбэк-константы (`STARTING_ARMY`/`STARTING_WORKER_DEF` — id определений). Путь входа (хоткей, команда,
стартовый блок) передаёт id фракции; дефолт — `FactionRegistries.DEFAULT_FACTION`.

## Шаги

1. **JSON фракции** — `data/<ns>/faction/<name>.json` (см. схему выше).
2. **Юнит(ы)** — `02_unit.md` (JSON-определение в `data/<ns>/unit/`); id определения указывается в
   `starting_units`. `EntityRegistrar` для data-driven юнитов **не нужен** (тело — ванильный моб).
3. **Столица** — `03_building.md` (JSON-здание в `data/<ns>/building/`); её id идёт в `capitol`.
4. **Производство** — `06_production.md`.
5. **Исследования** — `07_research.md`.
6. **Локализация** — `faction.<ns>.<name>`; имена зданий/юнитов — из их JSON (`name`), либо
   `buildings.<ns>.<path>` для код-зданий.
7. **Ассеты** — `09_assets.md`.

## Идентичность игрока

Владельца везде хранит **строка-имя** (не UUID): юнит — `ownerName` (синхронизируемый
`EntityDataAccessor`), здание — поле `ownerName`; сравнение через `String.equals`.

## Чего не делать

* Не заводить enum `Faction` и не возвращать старый `FactionRegistries` — это удалено.
* `Building.getFaction()` сейчас — **строковая метка** для код-зданий; у data-driven `JsonBuilding`
  возвращает `""` (имена берутся из JSON-определения). Явного поля «фракция» у здания пока нет.
