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

## Тихие грабли ассетов

Всё ниже уже стоило времени: дефекты не роняют ни сборку, ни сервер, поэтому находятся только
глазами или по шуму в логе.

* **`*.png`, который не PNG.** Расширение ничего не гарантирует: в дереве лежал
  `textures/item/scroll_of_recall.png`, а на самом деле — WebP (`RIFF....WEBP`). Клиент пишет
  `java.io.IOException: Bad PNG Signature` и подставляет missing texture, в остальном всё
  «работает». Проверять сигнатуру, а не имя: `head -c 8 <file> | xxd -p` → должно быть
  `89504e470d0a1a0a`; конвертация — Pillow (`RGBA`, формат `PNG`) или `dwebp`/`ffmpeg`.
* **Субтитры звуков.** Если `sounds.json` объявляет `subtitle`, ключ обязан существовать в
  `lang/*.json`. Проверка (`SoundManager.apply` под `SharedConstants.IS_RUNNING_IN_IDE`) идёт
  **только в dev-прогоне**: в продакшене молчит, а в dev-клиенте печатает строку на каждый звук.
  Чинить **добавлением** ключей, а НЕ удалением `subtitle` из `sounds.json` — удаление тоже убирает
  ошибку, но меняет поведение. Для субтитров достаточно одного `en_us`: `ClientLanguage` всегда
  кладёт его в тот же `Map`, что и выбранный язык, поэтому проверка проходит при любой локали
  (правило «правь все языковые файлы» из раздела выше остаётся правилом для ключей контента —
  названий юнитов, зданий, подсказок).
* **Тинты блоков.** В 1.21.1 `BlockColors` держит провайдеров в
  `IdentityHashMap<Block, BlockColor>` — ключ это `Block`, а не `Holder`. Поиск вида
  `map.get(BuiltInRegistries.BLOCK.wrapAsHolder(block))` молча возвращает `null` (стирание
  дженериков: ни ошибки компиляции, ни падения миксина), и блок остаётся без цвета — например,
  трава и листва становятся серыми. В этом каркасе ванильные провайдеры никто не оборачивает
  (обёртка была нужна только туману войны, который вырезан), поэтому свой тинт регистрируется
  прямо в `ClientModEvents#onBlockColourEvent`, без аксессора к карте.
