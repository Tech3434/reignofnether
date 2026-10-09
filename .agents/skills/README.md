# Навыки для ИИ-агентов (Reign of Nether)

Переиспользуемые инструкции для агентов, работающих над этим модом. Каждый навык —
`<name>/SKILL.md` с YAML-шапкой (`name`, `description`); грузить по имени.

> **Состояние на 2026-10-09.** Контент готовых фракций вырезан; юниты/здания/способности теперь
> **data-driven** (датапак JSON), кодовых классов юнитов/зданий нет — навыки `reignofnether-add-*`
> описывают именно этот путь. Система исследований переписана (`reignofnether-add-research`).
> Гейты — `compileJava`/`validateMixins`/`runData`/`test` (`runServer` не запускать); проверка в игре —
> `docs/RUNCLIENT_CHECKLIST.md`. Актуальное состояние — `docs/STATUS.md`, изменения ванили —
> `docs/VANILLA_CHANGES.md`. Часть навыков читается вместе с `_GUIDES/`.

## Глобальные (проект целиком)

| Навык | Когда применять |
|---|---|
| `reignofnether-codebase-map` | в начале любой задачи: где что лежит, три шва, порядок инициализации |
| `reignofnether-build` | собрать и прогнать гейты (`compileJava`…`runClient`); `gradlew.bat` не работает |
| `reignofnether-line-endings` | перед пакетной правкой файлов — CRLF-ловушка, иначе дифф «весь файл» |
| `reignofnether-commit` | при любой просьбе закоммитить: стиль сообщений, что стейджить, чек-лист |
| `reignofnether-recover-deleted` | вернуть класс/ассет, удалённый при вырезании фракций (`5079004e`) |

## Локальные (авторский контент)

| Навык | Когда применять |
|---|---|
| `reignofnether-add-faction` | новая играбельная фракция целиком |
| `reignofnether-add-unit` | новый юнит |
| `reignofnether-add-building` | новое здание |
| `reignofnether-add-ability` | новая способность |
| `reignofnether-add-hero` | геройский юнит |
| `reignofnether-add-production` | привязка юнита к зданию (очередь, стоимость) |
| `reignofnether-add-research` | исследование и гейт по нему |
| `reignofnether-add-commands` | команда/аргумент-селектор |
| `reignofnether-add-assets` | текстуры, модели, звуки, локализация |

## Источники

* `docs/STATUS.md` — состояние и гейты.
* `docs/PLAN_RTS_ONLY.md` — план A–H, все решения.
* `_GUIDES/` — подробные пошаговые гайды (на них ссылаются навыки).
* `docs/reference/` — что удалено и как вернуть.
