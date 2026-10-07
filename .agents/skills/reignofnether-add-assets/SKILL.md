---
name: reignofnether-add-assets
description: Какие ассеты и переводы нужны для нового юнита или здания в Reign of Nether, и как их подключить. Использовать при добавлении текстур, моделей, звуков и локализации.
---

# Ассеты и локализация

Полный разбор — `_GUIDES/09_assets.md`. Ассеты трёх старых фракций удалены; инвентарь и способ
восстановления — `docs/reference/assets.md`.

## Что нужно для юнита

| Что | Куда |
|---|---|
| Модель | `assets/reignofnether/models/entity/**` + рендерер в `unit/modelling/renderers/**` |
| Текстура | `assets/reignofnether/textures/entity/**` |
| Голова (для иконок HUD) | `assets/reignofnether/textures/entity/*` или `mobheads` |
| Звуки | `assets/reignofnether/sounds/**` + `SoundRegistrar` |
| Спавн-яйцо | `assets/reignofnether/models/item/*` + текстура + `ItemRegistrar` |
| Переводы | `assets/reignofnether/lang/*.json`, ключи `unit*.reignofnether.<name>` |

## Что нужно для здания

| Что | Куда |
|---|---|
| Структура | `.nbt` (в старом дереве `data/reignofnether/structures/`) |
| Иконка | `assets/reignofnether/textures/icons/blocks/**` |
| Переводы | ключ `buildings.<faction>.reignofnether.<name>` (см. `getFaction()` у `Building`) |

## Правила локализации

* Ключи каркаса **не трогать**: `hud.*`, `abilities.*`, `unitstats.*`, `commands.*`,
  `creativetab.*`, `resources.*`, `server.*`, `unititemtype.*`.
* Правь **все** языковые файлы (`assets/reignofnether/lang/*.json`), но без потери запятых: язык —
  JSON, а строки могут быть CRLF (см. `reignofnether-line-endings`).
* Учти: `fa_ir.json` исторически невалиден (SyntaxError) — не делай его эталоном.

## Гейт

Ассеты проверяются `runData` (генерация/валидация ресурсов) и `runClient` (реально видно только
в игре).
