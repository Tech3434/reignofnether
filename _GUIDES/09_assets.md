# Ассеты и локализация

> Для **data-driven** контента имя/иконка берутся из JSON определения, а тело — ванильный моб/блок.
> Отдельные спавн-яйца и модели для такого контента обычно **не нужны**.

## Юниту (data-driven)

| Ассет | Откуда | Обязателен |
|---|---|---|
| Тело | поле `base` (`minecraft:villager`, `minecraft:skeleton`, …) — ванильная модель/рендерер | да |
| Имя | `name: { "en_us": "…" }` в `UnitDefinition` | да (иначе путь id) |
| Иконка производства | `icon: "ns:textures/…png"` в определении | если есть производство |
| Предмет в руку | `equipment: "minecraft:bow"` | для ranged с луком |
| Способности/снаряд | `abilities`/`projectile` (см. `02_unit.md`) | по желанию |
| Спавн-яйцо / своя модель | **нет** — только если заводишь собственный `EntityType`/рендерер кодом | нет |

Свой рендерер/модель нужен только для нестандартного тела: `ClientModEvents.registerLayerDefinitions`
+ `registerEntityRenderer`. Несколько юнитов могут делить один рендерер.

## Зданию (data-driven)

| Ассет | Откуда |
|---|---|
| Имя | `name: { "en_us": "…" }` в `BuildingDefinition` |
| Иконка кнопки | `icon: "ns:textures/…png"` |
| Портрет | `flags.portrait` — **id блока** (не текстура), напр. `minecraft:polished_granite` |
| Структура (блоки) | NBT `structures/<name>.nbt` в **обеих** ветках: `assets/reignofnether/structures/` (клиент) и `data/reignofnether/structures/` (сервер) |

`structure` в JSON — id без пути NBT: `Building.Builder` берёт `structureName = structure.getPath()`,
а `BuildingBlockData` ищет `reignofnether:structures/<structureName>.nbt`.

⚠ В `assets/reignofnether/structures/` лежит **много осиротевших NBT** от вырезанных фракций
(используются только `town_centre.nbt`/`barracks.nbt`, и те продублированы в `data/`). Кандидаты на
чистку; перед удалением проверь `reference/` и гайды.

## Ключи локализации

Схема: `<домен>.reignofnether.<путь>[.tooltipN|.pointN|.desc]`.

| Домен | Пример | Откуда имя |
|---|---|---|
| Строение | `buildings.reignofnether.<path>` | код-здания; data-здания берут из JSON `name` |
| Юнит | `entity.reignofnether.<path>` | код-юниты; data-юниты берут из JSON `name` |
| Исследование | `research.<ns>.<path>` | `name` в `data/<ns>/research/<name>.json` |
| Способность | `abilities.reignofnether.<x>` | код-способности; JSON-способности — из spec |
| Предмет | `item.reignofnether.<x>`, `.desc`, `.point1..N` | код |
| Ресурс | `resources.reignofnether.food` | код |
| Серверные сообщения | `server.reignofnether.*` | код |

Чары перенесены на `Holder<Enchantment>`; имя чар читается из датапак-описания, ключ
`enchantment.reignofnether.<id>`.

## Языки

22 файла в `assets/reignofnether/lang/`, эталон — `en_us.json`. Остальные можно не трогать:
недостающий ключ отдаёт пустую строку. `ru_ru.json` ведётся вручную.

## Формат lang-файлов

Словарь с одним уровнем вложенности: строка начинается с отступа в 4 пробела, после двоеточия
один пробел. Проверять после правки:

```powershell
Get-Content <файл> -Raw | ConvertFrom-Json
```

Правка через `Set-Content -Encoding utf8` в PowerShell портит кодировку кириллицы. Безопасный
способ — `[System.IO.File]::WriteAllText($p, $t, (New-Object System.Text.UTF8Encoding($false)))`.
Тот же приём спасает, если `Set-Content` уже испортил файл: прочитать как cp1251 и переписать
как UTF-8.

## Историческое

Раньше под каждый юнит заводились спавн-яйцо (`models/item/x_unit_spawn_egg.json`) и
`entity.reignofnether.x_unit` в lang; текстуры моделей и звуки лежали в
`assets/reignofnether/textures/{entities,mobheads}/`. Для data-driven контента это не требуется —
тело ванильное, имя/иконка из JSON. Папки удалённого контента (`assets/minecraft/**`, звуки/вокал)
вычищены при вырезании фракций.
