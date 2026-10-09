# Команды и аргумент-селекторы

## Регистрация

`commands/CommandsServerEvents.java` → `RTSApiCommands.register(dispatcher)`; класс регистрируется и на
клиентском, и на серверном событийном бусе (иначе не работает в одиночной игре).

```java
LiteralArgumentBuilder<CommandSourceStack> RTSApiBuilder = Commands.literal("rtsapi")
        .requires(cs -> cs.hasPermission(2));
BuildingCommands.register(RTSApiBuilder);
UnitCommands.register(RTSApiBuilder);
PlayerCommands.register(RTSApiBuilder);
dispatcher.register(RTSApiBuilder);
```

ℹ В `CommandsServerEvents` **остались и легаси-команды** `rtsapi-place-building`, `rtsapi-summon`,
`rtsapi-change-resources` и т.п. (плоские, без дерева). Они не удалены — используй `/rtsapi …` как
основной путь.

## Поддеревья `/rtsapi`

| Поддерево | Файл |
|---|---|
| `building place/destroy/owner/tag/hurt/heal/command` | `commands/rtsapi/BuildingCommands.java` |
| `unit …` | `commands/rtsapi/UnitCommands.java` |
| `player owner/ally/resources/victory/defeat/camera` | `commands/rtsapi/PlayerCommands.java` |

### Отдельные команды (не под `/rtsapi`)

| Команда | Что |
|---|---|
| `/startrts [<faction>]` | меню выбора фракции при входе в РТС (см. `01_faction.md`) |
| `/research grant\|revoke\|clear\|list <player> [<ns:id>]` | управление исследованиями игрока (см. `07_research.md`) |
| `/rts-reset`, `/rts-hard-reset` | сброс матча |
| `/rts-lock enable\|disable` | заморозка матча |

**Читы удалены** — `Cheats`/`PlayerAction.SET_CHEAT`/`onPlayerChat` и все `warpten`/`greedisgood`/
`allcheats` вырезаны вместе с lang-ключами (этап удаления читов).

## Аргумент-типы

`commands/rtsapi/argument/`:

| Тип | Что даёт |
|---|---|
| `UnitArgument.units()` | выделение юнитов рамкой |
| `BuildingArgument.buildings()` | выделение зданий (в т.ч. JSON-зданий — id определения) |
| `PlayerNameArgument.players()` | игрок по имени или селектору |
| `BuildingSelector` + `BuildingSelectorParser` + `BuildingSelectorOptions` | язык селекторов зданий (`type=` знает и JSON-здания) |

## `execute rts-related` — полезно для ГМа

Регистрируется миксином `ExecuteCommandMixin` в ванильный `/execute`, работает в командных блоках:

```
/execute as @a[rts-related=enemies] run ...
/execute if entity @e[rts-related=owner,team=blue] run ...
```

Поддеревья: `owner`, `allies`, `enemies`, `attacker`, `target`, плюс `building`/`unit`.

## Команды, полезные ГМу

| Команда | Что делает |
|---|---|
| `/rtsapi unit …` | спавн/владение/действия юнитов |
| `/rtsapi building place <ns:id> [pos]` | поставить здание (в т.ч. JSON-здание по id определения) |
| `/rtsapi building destroy/hurt/heal` | управление зданиями |
| `/rtsapi player resources add/remove/set/get` | ресурсы |
| `/rtsapi player owner entity\|building` | смена владения |
| `/rtsapi player camera move\|fade` | удалённое управление камерой |
| `/rtsapi player victory\|defeat` | завершение матча |
| `/rts-reset`, `/rts-hard-reset` | сброс матча |

## Права

Корень `/rtsapi` требует права 2. Легаси-команды и часть старых подкоманд (`/sendfood` и др.) — из
старого набора; при работе с ними проверяй `.requires(...)` в коде (часть могла быть удалена вместе с
экономикой/волнами). **Читов больше нет.**

## Клавиатура

`keybinds/Keybindings.java` — собственный поллинг, **не** `KeyMapping`. Четыре модификатора
(`pause`, `shiftMod`, `ctrlMod`, `altMod`) не переназначаются. Хоткеи способностей — `abilitySlot1..8`.
Если собственные бинды конфликтуют по клавише, это видно в HUD/настройках; при сомнении сверяйся с
`Keybindings.java`.
