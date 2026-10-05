# Ассеты и локализация

## Юниту нужно

| Ассет | Путь | Обязателен |
|---|---|---|
| Спавн-яйцо | `assets/reignofnether/models/item/x_unit_spawn_egg.json` | да |
| Имя и тултип | `lang/*.json`: `entity.reignofnether.x_unit`, `.tooltip1..N` | да |
| Иконка производства | `assets/reignofnether/textures/mobheads/x.png` | да, если есть производство |
| Текстура модели | `assets/reignofnether/textures/entities/x.png` | если своя модель |
| Модель | только кодом: `unit/modelling/models/` | если ванильной модели не хватает |
| Звуки | `assets/reignofnether/sounds.json` + ключи `sounds.reignofnether.x_*` | по желанию |

Модель **не** описывается JSON'ом — это не ваниль. Только код + текстура.

Спавн-яйцо — единственный файл, который можно просто скопировать из работающего:

```json
{ "parent": "item/template_spawn_egg" }
```

## Зданию нужно

| Ассет | Путь |
|---|---|
| Имя и тултип | `lang/*.json`: `buildings.<ns>.<path>`, `.tooltip1..N` |
| Иконка кнопки | `assets/reignofnether/textures/icons/buildings/x.png` |
| Портрет | **блок**, не текстура: поле `portraitBlock` на здании |
| Структура | `assets/reignofnether/data/reignofnether/structures/x.nbt` |

## Ключи локализации

Схема: `<домен>.reignofnether.<путь>[.tooltipN|.pointN|.desc]`.

| Домен | Пример |
|---|---|
| Строение | `buildings.reignofnether.laboratory` |
| Юнит | `entity.reignofnether.husk_unit` |
| Исследование | `research.reignofnether.sculk_amplifiers` |
| Способность | `abilities.reignofnether.sonic_boom` |
| Предмет | `item.reignofnether.broadsword`, `.desc`, `.point1..N` |
| Тип предмета | `unititemtype.reignofnether.consumable` |
| Ресурс | `resources.reignofnether.food` |
| Вкладка | `creativetab.reignofnether.unit_spawn_eggs` |
| Серверные сообщения | `server.reignofnether.*` |
| Чары | удалены |

⚠ **Имя чар** читается из датапак-описания, ключ `enchantment.reignofnether.<id>`. Таких ключей
в lang-файлах не было — имена показывались сырыми ключами. Если чары вернутся, добавлять ключ
обязательно.

## Языки

22 файла в `assets/reignofnether/lang/`, эталон — `en_us.json`. Остальные можно не трогать:
недостающий ключ отдаёт пустую строку. Но если контент должен читаться по-русски,
`ru_ru.json` придётся вести вручную.

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

## Свои модели

`ClientModEvents.registerLayerDefinitions` — регистрация слоя:

```java
event.registerLayerDefinition(XUnitModel.LAYER_LOCATION, XUnitModel::createBodyLayer);
```

Рендерер — `event.registerEntityRenderer(EntityRegistrar.X_UNIT.get(), XRenderer::new)`.
Несколько юнитов могут делить один рендерер — это нормально.

База для своего рендерера: `unit/modelling/renderers/AbstractVillagerUnitRenderer.java`.

## Что удаляется

* `assets/minecraft/**` — подмена ванильных ресурсов (этап B.2).
* Текстуры, звуки и ключи удаляемого контента — этап D.19.
* Фракционные темы в `sounds.json` и вокал врайтов/Wildfire — удаляются вместе с контентом.
