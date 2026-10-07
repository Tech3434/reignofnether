---
name: reignofnether-add-commands
description: Как добавить команду и аргумент-селектор в Reign of Nether (/rtsapi, execute-селекторы). Использовать при расширении командной поверхности мода.
---

# Как добавить команду

Полный разбор — `_GUIDES/08_commands.md`. Поверхность: `commands/**` (`/rtsapi`,
`execute rts-related`, аргумент-селекторы) и `CommandsServerEvents`.

## Шаги

1. **Команда** — в `CommandsServerEvents.onRegisterCommands` (или рядом) зарегистрируй узел через
   Brigadier: `dispatcher.register(...)`.
2. **Права** — гейти правами оператора там, где команда меняет состояние. Пример в кодовой базе:
   `CustomBuildingServerboundPacket.handle` требует `hasPermissions(2)`; опасные
   `PlayerAction` (`RESET_RTS`, …) — `hasPermissions(4)`.
3. **Аргумент-селекторы** — для `execute if/unless data <target>` есть типы аргументов
   (напр. `building`, `targets`). Регистрация — в `commands/**`; см. `BuildingCommands`,
   `UnitCommands`.
4. **Действие** — вызывай серверные события напрямую (например `Unit.setOwnerName` +
   `UnitSyncClientboundPacket`, `BuildingServerEvents.removeBuildingPlacement`), а не через
   `PlayerAction`-обёртки, где это возможно.

## Грабли

* Читы (`greedisgood`, `allcheats`, `onPlayerChat`) и песочница **удалены** — не добавляй
  операторские «чит-команды», это отвергнуто.
* Ванильные команды, которые мод расширял (`/execute if data …`), несут риск «выполняет
  произвольные команды с правом 2» — проверяй права явно (см. `INTRUSION_AUDIT.md`).

## Гейт

`compileJava` → `validateMixins` → `runData` → `runServer` (регистрация команд видна на сервере).
