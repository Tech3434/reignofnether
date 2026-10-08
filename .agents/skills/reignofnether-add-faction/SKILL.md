---
name: reignofnether-add-faction
description: Как собрать фракцию на РТС-каркас Reign of Nether: JSON-реестр фракции, старт, юниты, здания, исследования, локализация.
---

# Как собрать фракцию

Опора — `_GUIDES/01_faction.md` (реестр + старт), `00_обзор.md` (карта), `07_research.md`.

## Фракция — объект реестра (датапак)

`data/<ns>/faction/<name>.json`, id = `<ns>:<name>`: `name`, `icon`, `capitol` (id здания-столицы),
`starting_units` (`[{ "unit": "…", "count": 1 }]`), `food`/`wood`/`ore`/`emerald`. Реестр —
`FactionRegistries.FACTION_KEY` (datapack, синхронизируется клиентам); дефолт —
`FactionRegistries.DEFAULT_FACTION` (`reignofnether:villagers`).

## Старт

`PlayerServerEvents.startRTS(playerId, pos, startPosColorId, factionId)` читает фракцию: ставит её
`capitol` и спавнит `starting_units`; если фракция не найдена — фолбэк на константы. Путь входа
(хоткей/команда/стартовый блок) передаёт id фракции.

## Порядок (снизу вверх)

1. Атрибуты/эффекты, если нужны новые (`registrars/AttributeRegistrar`, `MobEffectRegistrar`).
2. Юниты — `reignofnether-add-unit`.
3. Способности — `reignofnether-add-ability`.
4. Производство — `reignofnether-add-production`.
5. Здания — `reignofnether-add-building`.
6. Исследования — `reignofnether-add-research`.
7. Герой — `reignofnether-add-hero` (это юнит с уровнем: прокачка, мана, ранги способностей).
8. JSON фракции + локализация (`faction.<ns>.<name>`) + ассеты — `reignofnether-add-assets`.

## Лимит армии

База — 1 юнит; прирост даёт столица (`Building.populationSupply`, напр. ратуша = 10).

## Гейты

`compileJava` → `validateMixins` → `runData` (см. `reignofnether-build`). **`runServer` не
поднимать** (см. `AGENTS.md`).
