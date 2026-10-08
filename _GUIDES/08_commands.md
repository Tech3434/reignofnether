# Команды и аргумент-селекторы

## Регистрация

`commands/CommandsServerEvents.java` → `RTSApiCommands.register(dispatcher)`:

```java
LiteralArgumentBuilder<CommandSourceStack> RTSApiBuilder =
        Commands.literal("rtsapi").requires(cs -> cs.hasPermission(2));
BuildingCommands.register(RTSApiBuilder);
PlayerCommands.register(RTSApiBuilder);
UnitCommands.register(RTSApiBuilder);
```

Класс регистрируется и на клиентском, и на серверном шине событий — иначе не работает в
одиночной игре.

## Поддеревья `/rtsapi`

| Поддерево | Файл |
|---|---|
| `building place/destroy/owner/tag/hurt/heal/command` | `commands/rtsapi/BuildingCommands.java` |
| `unit summon/owner/anchor/action/enemysearch` | `commands/rtsapi/UnitCommands.java` |
| `player owner/ally/resources/victory/defeat/research/camera/teammode` | `commands/rtsapi/PlayerCommands.java` |
| `execute rts-related ...` | `commands/rtsapi/ExecuteCommands.java` |

Легаси-команды `/rtsapi-*` удаляются.

### Отдельные команды (не под `/rtsapi`)

| Команда | Что |
|---|---|
| `/startrts [<faction>]` | открыть меню выбора фракции при входе в РТС (см. `01_faction.md`) |
| `/research grant\|revoke\|clear\|list <player> [<ns:id>]` | управление исследованиями игрока (право 2, см. `07_research.md`) |

Читы удалены (этапы C/D плана).

## Аргумент-типы

`commands/rtsapi/argument/`:

| Тип | Что даёт |
|---|---|
| `UnitArgument.units()` | выделение юнитов рамкой |
| `BuildingArgument.buildings()` | выделение зданий |
| `PlayerNameArgument.players()` | игрок по имени или селектору |
| `BuildingSelector` + `BuildingSelectorParser` + `BuildingSelectorOptions` | язык селекторов зданий |

## `execute rts-related` — самое полезное для ГМа

Позволяет делать селекторы по фракциям прямо в ванильном `/execute`, то есть работает в
командных блоках:

```
/execute as @a[rts-related=enemies] run ...
/execute if entity @e[rts-related=owner,team=blue] run ...
```

Поддеревья: `owner`, `allies`, `enemies`, `attacker`, `target`, плюс `building` и `unit`
как отдельные формы.

⚠ Источник отношений после удаления `Faction` — имя владельца и союзы
(`unit/Relationship.java`: свои и союзники — `FRIENDLY`, пустое имя — `NEUTRAL`, остальное —
`HOSTILE`). Селекторы надо будет переписать на эти правила, если `Faction` исчезнет.

## Команды, полезные ГМу

| Команда | Что делает |
|---|---|
| `/rtsapi unit summon <id> [pos] [nbt]` | спавн любого юнита с NBT |
| `/rtsapi player owner entity\|building <targets>` | смена владения |
| `/rtsapi player resources add\|remove\|set` | выдача ресурсов |
| `/rtsapi player camera move\|fade <pos> <player>` | удалённое управление камерой |
| `/rtsapi building place\|destroy\|hurt\|heal` | управление зданиями |
| `/rts-reset`, `/rts-hard-reset` | сброс матча |
| `/rts-lock enable\|disable` | заморозка матча |
| `force-loose` | симуляция проигрыша (по решению владельца, этап H.5) |

## Права

Корень `/rtsapi` требует права 2. Проверки прав **нет** у: `/sendfood`, `/sendwood`,
`/sendore`, `/sendemerald`, `/rts-fog`, `/rts-scores`, `/ally*`, `/disband`, `/allycontrol`,
`/debug-end-wave`, `/debug-next-night`. Часть из них удаляется, оставшимся нужно добавить
проверку (этап A.4).

## Читы

Вводятся **одним сообщением в чат** от игрока с правами 4; сообщение скрывается от других.
Разбор слов — `player/PlayerServerEvents.onPlayerChat`, список — `singleWordCheats`,
хранение — `research/ResearchServerEvents`, синхронизация — `ResearchClientboundPacket`.

| Чит | Что делает | Статус |
|---|---|---|
| `warpten` | мгновенное стротельство и производство | остаётся |
| `modifythephasevariance` | игнорировать требования построек | остаётся |
| `wouldyoukindly` | управлять не-юнитовыми мобами | остаётся |
| `thereisnospoon` | режим ГМа для волн | волны удаляются — чит уходит |
| `greedisgood [res] N` | выдать ресурсы | остаётся |
| `elitetaurenchieftain` | все герои на 10 уровень | остаётся, пока есть герои |
| `operationcwal` | ускоренный сбор ресурсов | экономика удаляется — уходит |
| `foodforthought` | игнорировать лимит армии | лимит переделан — пересмотреть |
| `medievalman` | все исследования | исследования удаляются — уходит |
| `slipslopslap` | нежить не горит днём | остаётся, если будет своя нежить |
| `thebeastofcaerbannog` | спавн Killer Rabbit | гаммит, на усмотрение |
| `allcheats` | все читы | **удаляется**: был захардкожен на два авторских ника |

⚠ Четыре из них (`medievalman`, `operationcwal`, `foodforthought`, `thereisnospoon`) опираются
на удаляемые подсистемы. Их список нужно пересобрать в том же проходе, что удаляет
исследования, экономику и волны — иначе останутся читы, которые ничего не делают.

## Клавиатура

`keybinds/Keybindings.java` — собственный поллинг, **не** `KeyMapping`. Четыре модификатора
(`pause`, `shiftMod`, `ctrlMod`, `altMod`) не переназначаются. Категории: камера (9),
RTS (19), группы контроля (10), функциональные клавиши (12, где F1 — HUD, F12 — орторежим,
F7 — отладка путей).

⚠ Дефолты **конфликтуют**: `hold` и `hotkey10` оба на H, `stop` и `openShop` оба на S,
`gather` и `item4` оба на G, `build` и `item6` оба на B. При удалении лишних биндов это
уходит само собой.