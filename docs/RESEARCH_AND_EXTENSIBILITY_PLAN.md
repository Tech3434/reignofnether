# План: система исследований и расширяемость контента

Согласовано с владельцем 2026-10-08. Цель — «полностью пригодный РТС-каркас»: автор новой фракции
добавляет юнитов, здания и исследования без правок ядра.

## Требования владельца (дословно по смыслу)

1. **Исследования — на игрока.** Каждый исследует сам; **поражение обнуляет исследования** игрока.
2. **Что открывает исследование:** способности, новые здания/юниты, апгрейды (урон/броня/другой
   атрибут). В идеале **через API можно заблокировать что угодно** до изучения технологии.
3. **Инверсия условия.** Исследование-гейт умеет требовать как наличие, так и **отсутствие**
   технологии (условие с инверсией).
4. **Дерево с предпосылками**, предпосылок может быть много (в т.ч. инвертированных).
5. **Контент — данными.** Здания, юниты и **способности** описываются **JSON в датапаке**
   (инстансы); их **классы** — код. **Тип** исследования — код (исследование, повышающее указанный в
   JSON атрибут; исследование, открывающее войска — указывается у юнита/здания).
6. **Инстанс исследования** = имя, иконка, список предпосылок (много, с инверсией), цена, необязательный
   список усиливаемых атрибутов.
7. **Юниты/здания** имеют поля требуемых исследований; без них их нельзя произвести/построить.

## Архитектура

### Типы (код)

- `research/ResearchCondition` — `{ ResourceLocation researchId; boolean invert; }`.
  Инверсия — базовый примитив: `meets = isResearched(id) != invert`.
- `research/Research` — инстанс исследования:
  `{ ResourceLocation id; String nameKey; ResourceLocation icon; ResourceCost cost;
     List<ResearchCondition> prerequisites; List<ResearchAttributeModifier> attributeModifiers; }`.
- `research/ResearchAttributeModifier` — `{ Attribute attribute; double amount; Operation op;
     @Nullable ResourceLocation unitFilter; }` (код-тип; список — данными).
- `research/ResearchType` (код) — семантика: `ATTRIBUTE_BOOST`, `UNLOCK`. `UNLOCK` сам ничего не
  «делает», его читают юниты/здания через свои `requiredResearch`.
- `research/ResearchRegistry` — регистрация/поиск по id (как `ReignOfNetherRegistries`).

### Данные (датапак)

- `data/<ns>/research/<id>.json` — имя, иконка, цена, предпосылки (с `invert`), атрибуты.
- `data/<ns>/units/*.json`, `buildings/*.json`, `abilities/*.json` — инстансы контента (**фаза 4**).
  Класс способности (код) задаёт поведение; JSON-инстанс — имя, иконка, кулдаун, требуемые
  исследования и ссылку на класс. Способности так же data-driven, как юниты и здания, но их классы —
  код.
- Загрузчик через `SimpleJsonResourceReloadListener` (сервер + клиент, синхронно на reload).

### Состояние (на игрока)

- `research/ResearchSaveData` — по имени игрока набор изученных id; **ленивое** (см. E.5),
  персистентно, синхронизируется клиенту пакетом `ResearchClientboundPacket`.
- Сброс: `PlayerServerEvents.defeat` → очистить исследования игрока (рядом с
  `neutraliseUnitsOf`), синхронизировать клиенту.

### API-гейт (универсальный)

- `research/ResearchUtils.hasResearch(playerName, id)` и
  `meetsConditions(playerName, List<ResearchCondition>)`.
- Любая точка гейта вызывает это; инверсия даёт «заблокировано, пока НЕ изучено».
- Точки гейта (фаза 2):
  * **способность**: `Ability.requiredResearch` → проверка на сервере при `use`/`useArea` и в HUD
    (серая кнопка + подпись «требуется …»);
  * **производство**: `ProductionItem.requiredResearch` → `ProductionPlacement` не ставит в очередь;
  * **строительство**: `Building.requiredResearch` → кнопка постройки заблокирована;
  * **атрибуты**: изученный `ATTRIBUTE_BOOST` применяет модификаторы к юнитам владельца.

### Команды

- `/research grant|revoke|list <player> [id]` (право 2) — для тестов и ГМа.

## Фазы (каждая — отдельный коммит с гейтами 1–4)

1. **Ядро исследований (код).** `Research`, `ResearchCondition`, `ResearchAttributeModifier`,
   `ResearchType`, `ResearchRegistry`, `ResearchSaveData`, `ResearchUtils`,
   `ResearchClientboundPacket`, команды, сброс при поражении. Пока без JSON и без гейтов.
2. **Гейты.** `requiredResearch` у `Ability`, `ProductionItem`, `Building`; серверные проверки +
   блокировка в HUD; инверсия.
3. **Атрибуты.** Применение `ATTRIBUTE_BOOST` к юнитам владельца; пересчёт при изучении.
4. **JSON-контент.** Загрузчик `research/*.json`; затем `abilities/*.json`, `units/*.json` и
   `buildings/*.json` с полями `requiredResearch`; строительное меню из данных. Отладка в игре.
   (Самая крупная фаза.)
5. **UI.** Панель исследований в HUD (дерево, статус, стоимость), привязанная к `ResearchSaveData`.

> **Отложено (нужна совместная работа в игре).** Дата-ориентированные **способности** (JSON-инстансы
> поверх код-классов) и их настройка/отладка потребуют совместной работы в майнкрафте. Делается
> позже — после ядра (фаза 1) и гейтов (фаза 2).

## Точки интеграции (для справки)

- `ability/Ability` (+ `UnitActionItem`, HUD-кнопки) — гейт способностей.
- `building/production/ProductionItem`, `ProductionPlacement` — гейт производства.
- `building/Building`, `BuildingPlaceButton` — гейт строительства.
- `player/PlayerServerEvents.defeat` — сброс исследований.
- `registrars/PacketHandler`, `api/ReignOfNetherRegistries` — регистрация.
- `data/<ns>/research/*.json` — данные.

## Риски и решения

- **Реестр заморожен** к моменту загрузки датапака → «юниты/здания данными» реализуются как
  определения поверх зарегистрированных в коде базовых типов (фаза 4), а не как новые `EntityType`
  из JSON.
- **Инверсия** — базовый примитив условия, а не частный случай: `meets = has != invert`.
- **Синхронизация**: исследования меняются редко — шлём по изменению и при входе, как геймрулы.
- **Атрибуты**: не трогаем ванильные атрибуты глобально; модификаторы вешаются на юнитов-владельца
  через `AttributeRegistrar`-совместимый путь.

## Остаток оригинального плана (`PLAN_RTS_ONLY.md`)

Проверка 2026-10-08. В оригинале пункты помечены `☐` повсеместно (галочки не обновлялись), поэтому
сверено по факту кода и `STATUS.md`. Реально **не выполнено** только:

- **G.2 — отсечение секций выше камеры.** Не сделано; проверяемо только в игре (нужна проверка
  владельца). Замысел и безопасные альтернативы — `PLAN_RTS_ONLY.md` §9 G.2.
- **G.5 — камера на высоте не застревает в блоках.** Не сделано; только в игре.
- **H.9 — класс генератора ресурсов.** Форма заложена: интерфейс `ResourceGenerator`
  (`getTickInterval`/`getResourceAmount`/`getResourceType`/`getCapacity`) и готовый путь начисления
  `ResourceGenerators.produce(ownerName, generator)`. Конкретные генераторы пишет автор фракции.
- **§14.5 — снос строения уроном.** Был реализован, затем **осознанно откачен** (копание по
  строению отменяется без урона).

Всё остальное (A–F, §13, §14.1–§14.4, H.1–H.8) закрыто — см. `STATUS.md` и
`VANILLA_CHANGES.md`. Ветку `wip/stage-d-deletions` остаётся смёржить в `1.21.1-clean` (П5).

## Статус

- [x] Фаза 1 — ядро исследований: `ResearchCondition` (инверсия), `ResearchType`, `Research`,
  `ResearchAttributeModifier`, `ResearchRegistry`, `ResearchSaveData` (персистентно, лениво,
  per-player), `ResearchUtils` (серверный гейт + клиентское зеркало), `ResearchClientboundPacket` +
  `ResearchClientEvents`, команды `/research grant|revoke|clear|list`, синк при входе
  (`ResearchServerEvents`), сброс при поражении и в `resetRTS`. Гейты 1–4 зелёные.
- [x] Фаза 2 — гейты: `Ability`/`ProductionItem`/`Building` несут `requiredResearch`; серверные
  проверки при использовании способности (`UnitActionItem`, `BuildingAbilityServerboundPacket`),
  старте производства (`ProductionItem.canProduce` ← `ProductionPlacement.startProductionItem`) и
  постановке здания (`BuildingServerEvents.placeBuilding`, GM-команды обходят); HUD-блокировка кнопок
  способностей (`Abilities.getButtons`) и строительного меню (`VillagerUnit.getBuildingButtons`).
- [x] Фаза 3 — атрибутные апгрейды: `ResearchAttributeApplier` накладывает `ATTRIBUTE_BOOST` на
  юнитов владельца (модификатор именован по исследованию → идемпотентно; `unitFilter` — по типу
  сущности). Хуки: спавн юнита (`UnitServerEvents.onEntityJoin`), grant/revoke/clear
  (`ResearchCommand` → `refreshForOwner`), сброс при поражении/`resetRTS` (`removeFor`).
- [x] Фаза 4 (частично) — загрузчик `research/*.json` (`ResearchJsonLoader`, `ResourceManagerReloadListener`,
  перечитывается на `/reload`): id = `<ns>:<name>`, поля `name`/`icon`/`type`/`cost`/`prerequisites`
  (с `invert`)/`attributes`. JSON для **юнитов/зданий/способностей** — отложено до совместной сессии
  (дизайн-решения + отладка в игре).
- [x] Фаза 5 — UI исследований (статус): определения синхронизируются клиенту
  (`ResearchDefinitionsClientboundPacket` → `ResearchClientEvents.definitions`); панель `ResearchMenu`
  в HUD (кнопка-тоггл + список со статусом researched/available/locked). **Панель только для
  просмотра** — запуск исследований переносится в UI здания (см. §«Отложено: исследования у зданий»).

### Отложено: исследования у зданий (решения владельца 2026-10-08)

Запуск и очередь исследований делаются вместе с фазой «здания через JSON»:

* Исследования привязываются к зданию, как абилки/производство: **здание указывает в своём JSON,
  какие исследования оно предлагает** (может не иметь ни одного).
* У здания своя **очередь, общая с производством юнитов** (слоты занимаются по порядку).
* Несколько зданий → несколько параллельных очередей.
* Исследование занимает `cost.ticks`; **отмена — с возвратом** списанных ресурсов.
* Кнопки запуска — **в UI выбранного здания** (ряд как у производства/абилок).
* По завершении — выдача игроку (`ResearchSaveData`), синк клиенту
  (`ResearchClientboundPacket`), применение атрибутных апгрейдов (`ResearchAttributeApplier`).
* Глобальная панель `ResearchMenu` остаётся **только для просмотра** статуса/дерева.

## П4 — прочие швы

- [x] **H.9 — форма генератора ресурсов:** интерфейс `ResourceGenerator` + `ResourceGenerators.produce`.
- [x] **D.21 — настраиваемый блок лесов:** уже реализован (`Building.scaffoldFill` / `scaffoldBlock`).

## П5 — мерж

- [x] `wip/stage-d-deletions` **fast-forward'нут** в `1.21.1-clean` (`0` позади / `49` впереди →
  fast-forward, конфликтов нет).
