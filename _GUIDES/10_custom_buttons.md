# Кастомные кнопки (`rts_buttons`)

Датапак-кнопки **без кода**: описываешь кнопку и её действия, движок рисует её на портрете выбранного
здания/юнита и по клику выполняет действие. Альтернатива способности (`AbilityTypes`) там, где хватит
датапак-логики.

## 1. Кнопка — `data/<ns>/rts_buttons/<name>.json`

```json
{
  "name": "Trade",
  "icon": "myns:textures/icons/items/emerald.png",
  "x": 20, "y": 0,
  "icon_size": 20,
  "light_up_on_hover": true,
  "enable": true,
  "left_click_actions": [ { "type": "run_function", "function": "myns:trade" } ],
  "right_click_actions": []
}
```

Поля: `name`, `icon` (текстура), `x`/`y` (смещение на экране; кнопка рисуется справа, к позиции
`screenWidth - iconSize*2 - x, y`), `icon_size`, `light_up_on_hover`, `enable`. Клики — списки действий
`left_click_actions`/`right_click_actions`.

## 2. Действия (`type`)

| type | Поля | Что делает |
|---|---|---|
| `run_command` | `command` | выполняет команду от имени владельца (`withPermission(2)`) |
| `run_function` | `function` | выполняет функцию датапака (`myns:trade`) |
| `experience` | `points`, `levels` | даёт опыт (игроку или орбы) |
| `loot` | `loots` (список лут-таблиц) | выкидывает/выдаёт предметы |

## 3. Привязка — `data/<ns>/reignofnether/custom_button_mappings.json`

```json
{
  "entities":        { "minecraft:villager": [ "myns:trade" ] },
  "buildings":       { "reignofnether:barracks": [ "myns:trade" ] },
  "unit_definitions":{ "myns:skeleton_marksman": [ "myns:volley" ] }
}
```

* `entities` — ключ = **id типа сущности**, кнопки на все юниты этого тела.
* `buildings` — ключ = **id здания**: код-ключ ИЛИ id определения JSON-здания (`JsonBuildingManager`),
  напр. `reignofnether:town_centre`.
* `unit_definitions` — ключ = **id определения юнита** (`UnitDefinition`), точно на конкретный вариант
  (в отличие от `entities`, различает два определения с одним телом).

При выборе юнита движок сначала смотрит `unit_definitions` по id определения и лишь затем `entities` по
телу; при выборе здания — `buildings` по id (код-ключ или определение JSON). Кнопки, не привязанные ни к
чему, рендерятся **всегда** (`alwaysRenderButtons`).

## 4. Примечания

* Кнопки не заменяют постановку зданий (её даёт `BuildMenuAbility`/`WorkerBuildMenu`) — это
  дополнительные HUD-кнопки на портрете.
* Реестр кнопок (`rts_buttons`) и маппинг загружаются на старте сервера и синхронизируются клиентам
  (`CustomButtonClientboundPacket`).
* Для «настоящего» поведения с кулдауном/маной/гейтами используй способность (`04_ability.md`); для
  простых триггеров (открыть меню-функцию, выдать лут, дать опыт) — `rts_buttons`.
