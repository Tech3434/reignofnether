# STATUS — единый статус-документ

**Дата обновления:** 2026-10-09
**Ветка:** `wip/stage-d-deletions`; `1.21.1-clean` держится синхронно (`git branch -f 1.21.1-clean wip/stage-d-deletions`).
**Точка входа для продолжения:** [`HANDOFF.md`](HANDOFF.md) (архитектура data-driven контента, журнал
инкрементов, трапы, инвентарь файлов). Этот файл — краткая сводка.

**Гейты (текущее состояние):**
- `compileJava` ✅
- `validateMixins` ✅ (все точки инъекции резолвятся; число — в свежем логе)
- `runData` ✅
- `test` ✅ 18 тестов: `DataCodecTest` (декодирует все поставляемые `unit`/`building`/`faction` JSON их
  кодеками) + `ContentValidationTest` (строгая проверка полей: неизвестное поле валит гейт с
  именем файла и пути) + `MiscUtilTest`
- `runServer` — ⚠ **не запускать** в этой среде (dev-сервер печатает `Done (...)`, но сам не завершается;
  см. `../AGENTS.md`). Достижение `Done (...)` уже доказывает рантайм-инициализацию.
- `runClient` — ⚠ **ни разу не запускался** с data-driven контентом; всё клиентское проверено только сборкой.
  Сценарий одного сеанса: [`RUNCLIENT_CHECKLIST.md`](RUNCLIENT_CHECKLIST.md) (все фичи за ОДИН прогон).

---

## 1. Что это за ветка

`NeoForge 1.21.1`, ранее — порт Reign of Nether. Ветка превращает мод в **чистый РТС-каркас**: механика
остаётся, контент готовых фракций вырезан. Поверх каркаса владелец пишет свою фракцию **данными**
(датапак JSON), без Java на каждый юнит/здание/способность. Удалённый контент — в `reference/`
(восстановление `git show 5079004e:<путь>`).

Два больших направления этой итерации:
1. **Data-driven юниты и здания** — код-классы удалены; юнит = ванильный/модовый `Mob` + `UnitDefinition`,
   здание = `JsonBuilding` + `BuildingDefinition`.
2. **Расширяемость** — фракции-датапак, исследования, аддоны, апгрейды, способности-заготовки.

---

## 2. Что сделано

Подробный журнал — `HANDOFF.md` §4. Сжато:

**Порт и сужение ванили.** Порт на NeoForge 21.1 завершён; изменений ванили минимум
(`VANILLA_CHANGES.md`). Этапы интрузивности/чистки — `CLEAN_FORK.md`, `INTRUSION_AUDIT.md` (историческое).

**РТС-механика.** Этапы A–H плана (`PLAN_RTS_ONLY.md`), включая §14 «как в Warcraft 3» (меню-способности,
приказы, производство-способности, вскапывание). §14.5 (урон зданию копанием) **откачен осознанно**.

**Система исследований** (`RESEARCH_AND_EXTENSIBILITY_PLAN.md`). Ядро на игрока, условия с инверсией,
JSON-загрузчик, гейты у способностей/производства/зданий, команды `/research`, атрибутные апгрейды.

**Фракции** — датапак-реестр `faction`, меню выбора, `/startrts [<faction>]`.

**Data-driven контент (эта итерация):**
- Юниты: `UnitDefinition` (`base`/`role`/`flags`/`attributes`/`equipment`/`projectile`/`cost`/`abilities`),
  `@Mixin(Mob) UnitMobMixin` строит goals, `UnitDefinitionRuntime` спавн/атрибуты. **Ranged-атака**
  реализована (`performUnitRangedAttack`), демо `skeleton_unit`. `inherits` зарезервирован, **не реализован**.
- Здания: `BuildingDefinition` (`structure`/`name`/`icon`/`cost`/`maxHealth`/`populationSupply`/`isCapitol`/
  `flags`/`production`/`researches`/`addons`/`upgrades`/`requiredResearch`), `JsonBuilding` + `JsonBuildingManager`.
  Полная клиентская синхронизация (place/сейв/HUD/команды), флаги здания (`flags`).
- **Аддоны** зданий (`addons`): реестр типов `AddonTypes`; движковые `night_source`, `range_indicator`,
  `garrison`, `nether_converting`; lifecycle-хуки `onBuildingBuilt`/`onBuildingTick`.
- **Исследования у зданий**: `ResearchProductionItem` делит очередь производства, отмена с возвратом,
  грант по завершении. Демо `example_research` на казарме.
- **Апгрейды зданий**: `UpgradeSpec`; уровни материализуются как `JsonBuilding`-варианты, placement
  переключается на вариант (`applyUpgradeBuilding`) — производство/исследования/аддоны/имя/иконка/
  структура/maxHealth per-level. Демо «Barracks II».
- Способности: `AbilitySpec` + `AbilityTypes`; движковый тип `reignofnether:heal` (`SimpleHealAbility`),
  регистрация в `BuiltInAbilities.init()`, демо у `skeleton_unit`. **Автор регистрирует свои типы**;
  активной способности нужна своя константа `UnitAction`.

**Багрепорты JSON-путей, поправленные по ходу:** клиентский place/sync, сетевые id производств
(`ProductionItem.getNetworkId`), персистентность (`jsonDefinitionId`), `populationSupply`/`requiredResearch`,
дроп-офф ресурсов (`canAcceptResources`), null-safe имена в HUD.

---

## 3. Что осталось

**Проверка (главное):**
1. **`runClient`** — ни разу не прогонялся на data-driven работе. Готовый сценарий одного сеанса —
   [`RUNCLIENT_CHECKLIST.md`](RUNCLIENT_CHECKLIST.md): постановка/рендер/выбор/сейв JSON-зданий,
   `costOverride`, меню (`row`/`col`, меню-в-меню, `building`/`command`), воркер (`gatherable`/
   `buildSpeed`), дроп-офф ресурсов, аддоны (garrison/night), исследования, апгрейды (варианты),
   ranged-юнит, `inherits`, кастомные кнопки, HUD/команды.
2. **Ручной прогон** G.2 (отсечение секций выше камеры) и G.5 (удушье камеры) — только в игре.

**Каркас (код):**
3. ✅ Data-driven способности: реестр `AbilityTypes` + движковые типф `reignofnether:heal`/`regeneration`/
   `summon`/`menu`; поля spec прокидываются; пассивки тикают через `Ability.tickPassive`; `params`
   типизированные (число/строка/`ResourceLocation`). Меню: вложенность, `command`, `row`/`col`. Дальше —
   авторские типы.
4. ✅ `UnitDefinition.inherits` — реализовано (наследование/мерж).
5. ✅ Исследования: эффект `equip` реализован. UI — только список статуса (запуск у зданий и сделан),
   дерева нет.
6. ✅ H.9: движковый аддон `reignofnether:resource_generator`.
7. JSON-здания: нет поля фракции (`getFaction()` == `""`), меню постройки не фильтруется (решение владельца).
   Кастомные кнопки цепляются к JSON-зданиям/юнитам по id. `startingBlockTypes` выводятся из NBT;
   `foundationYLayers` — нет (дефолт 1). `production` принимает `{unit, costOverride}` (`ProductionSpec`).
8. ✅ K4 — воркер данными: блок `worker` (`gatherable`/`buildSpeed`/`carryCapacity`). ✅ K5 — прокачка
   героя данными (`hero: { maxLevel, expReqMultiplier }`). ✅ K6 — submenu `row`/`col` (тип `reignofnether:menu`).

См. §7 — сессия 2026-10-09 нашла и исправила несколько регрессий рантайма JSON-контента
(здание-исчезновение, воркер-невозможность добычи, NPE населения, цели на загрузке, `inherits`-роли).
**Документация:** актуализируются `STATUS.md`, `HANDOFF.md`, `CONTENT_JSON_PLAN.md`, `docs/README.md`,
гайды `_GUIDES/` (`00/02/03/04/06/07`) и навыки `.agents/skills/*`; добавлен `RUNCLIENT_CHECKLIST.md`.
Корневой `README.md` — публичный README выпущенного мода (Forge 1.20.1) и ветку не описывает —
**сознательно не трогаем**. Исторические снимки (`FEATURES.md`, `INTRUSION_AUDIT.md`, `CLEAN_FORK.md`,
`PORT_STATUS.md`, `AGENT_HANDOFF.md`, `BUGS_RUNCLIENT.md`) помечены как исторические.

**Чистка (не срочно):** в `assets/reignofnether/structures/` лежит ~70 осиротевших `.nbt` от вырезанных
фракций (клиент грузит NBT из `assets/`, сервер — из `data/`; используются только `town_centre`/`barracks`).

---

## 4. Как собирать

`gradlew.bat` не работает (передаёт java одновременно `-classpath ""` и `-jar`). Только через java:

```powershell
cd ___temp
& "C:\Program Files\Java\jdk-21\bin\java.exe" '-Dorg.gradle.appname=gradlew' `
  -jar gradle/wrapper/gradle-wrapper.jar <task> --offline --console=plain
```

Гейты на каждом шаге: `compileJava` → `validateMixins` → `runData` → `test`.

---

## 5. Ловушки (не повторять)

1. **Переводы строк (CRLF).** Большинство `.java` хранят CRLF. Правка: нормализовать в LF, редактировать,
   вернуть CRLF; перед коммитом сверять `git diff --numstat` и `git diff --ignore-cr-at-eol --numstat`.
2. **`git show <ref>:<path> > <файл>`** под PowerShell 5.1 пишет UTF-16LE → javac `unmappable character`.
   Только `cmd /c "git show ... > file"`.
3. **`validateMixins` не ловит часть ошибок** (`@Shadow`/`@Redirect`) — всплывают в рантайме.
4. **Сервер.** `runServer` не запускать (см. AGENTS.md); безопасный набор гейтов — `compileJava`,
   `validateMixins`, `runData`, `test`.

---

## 6. Ссылки

| Файл | О чём |
|---|---|
| `HANDOFF.md` | **точка входа**: архитектура data-driven контента, журнал, трапы, инвентарь |
| `RUNCLIENT_CHECKLIST.md` | **что проверить в игре за один `runClient`**: пошаговый сценарий всех фич |
| `PLAN_RTS_ONLY.md` | исходный план A–H, решения владельца, §15 — остаток |
| `RESEARCH_AND_EXTENSIBILITY_PLAN.md` | исследования и расширяемость; остаток (G.2/G.5/H.9) |
| `CONTENT_JSON_PLAN.md` | JSON-юниты/здания/способности/аддоны/апгрейды |
| `VANILLA_CHANGES.md` | инвентаризация изменений ванильных механик |
| `reference/` | что удалено и как вернуть |
| `../_GUIDES/README.md` | как добавлять контент |
| `FEATURES.md`, `INTRUSION_AUDIT.md`, `CLEAN_FORK.md`, `PORT_STATUS.md`, `AGENT_HANDOFF.md`, `BUGS_RUNCLIENT.md` | **исторические снимки** (см. шапки файлов) |

---

## 7. Сессия 2026-10-09: багфиксы JSON-рантайма + каркас

Полный журнал — `HANDOFF.md` §9. Коммиты `21f3f1eb`…`66d22e2d` (12 шт), гейты зелёные после каждого,
`runClient` не запускался.

**Исправлено (только чтением/компиляцией, в игре не проверялось):**
- JSON-здание исчезало сразу после постановки (пустой `startingBlockTypes` → `shouldBeDestroyed` на 1-м тике);
- воркер вообще не добывал (`maxResources` = 0 → `atMaxResources` всегда true);
- NPE в подсчёте населения/HUD (`Unit.getCost()` == null);
- юниты без целей после перезагрузки мира;
- `inherits` не влиял на роль/цели (`skeleton_marksman` был melee);
- произведённые юниты были бесплатны/мгновенны (стоимость здания вместо юнита);
- у JSON-юнитов не было кнопок способностей на клиенте (abilities не строились на клиенте).

**Добавлено:** `production` `costOverride`; аддон `resource_generator` (H.9); `type: equip` у исследований;
namespace структур JSON-зданий; `carryCapacity` в определении юнита; убраны полоски здоровья над юнитами.

**Остаётся:** `runClient`-проверка (главное); lang-дочистка; осиротевшие `.nbt`. K4/K5/K6 закрыты
(см. §4). Новый гейт: `test` (см. §4).

---

## 8. Сессия 2026-10-09 (вторая): K4/K5/K6 + тест-гейт

Гейты: `compileJava` ✅ · `validateMixins` ✅ (49 точек, 31 миксин) · `runData` ✅ · `test` ✅
(7 тестов). `runClient` по-прежнему не запускался.

* **K6 — data-driven меню (`reignofnether:menu`).** `MenuEntrySpec` (инлайн `ability` либо `command` +
  `row`/`col`), `AbilitySpec` получил `name`/`icon`/`submenu` (кодек рекурсивный через `Codec.lazyInitialized`),
  `DataMenuAbility` открывает подменю (вложенные меню, стандартные приказы), рендер учитывает явные
  `row`/`col` (`Ability.getSubButtonPosition`). Демо — `skeleton_unit.json`; гайд — `_GUIDES/04_ability.md`.
* **K4 — воркер данными.** Блок `worker`: `gatherable` (список ресурсов), `buildSpeed` (множитель стройки),
  `carryCapacity` (перенесён с верхнего уровня сюда). Проводка: `Unit.getGatherableResources()`/`getBuildSpeed()`,
  цикл переключения сбора (`UnitActionItem`) и гард `GatherResourcesGoal.setTargetResourceName`,
  вес воркеров в `BuildingPlacement.handleServerTick`.
* **K5** был закрыт ранее (`2160358c`, `hero: { maxLevel, expReqMultiplier }`).
* **Новый гейт `test`:** `DataCodecTest` декодирует все поставляемые unit/building/faction JSON их
  кодеками (плюс точечные проверки `worker`-блока и `menu`-энтри). Без него кодек-регрессии ловились бы
  только при старте сервера (или не ловились бы совсем). Запускать `test` вместе с остальными гейтами.

---

## 9. Сессия 2026-10-09 (третья): ревью меню/гейтов + чек-лист одного `runClient`

Гейты: `compileJava` ✅ · `validateMixins` ✅ (49 точек, 31 миксин) · `runData` ✅ · `test` ✅.
`runClient` не запускался — вместо него подготовлен **один сценарий на один сеанс**:
[`RUNCLIENT_CHECKLIST.md`](RUNCLIENT_CHECKLIST.md).

**Исправлено по ревью кода:**
* **Невидимое меню.** `DataMenuAbility` отдаёт кнопки из `entries`, а «скрытость» наследовалась от
  `MenuAbility` через `subAbilities.isEmpty()` — меню из одних `building`/`command`-элементов нельзя
  было открыть. Введён `MenuAbility.hasNoEntries()` (переопределён в `DataMenuAbility` на `entries.isEmpty()`).
* **Общие синглтоны `CommandAbilities` больше не добавляются в `subAbilities`** (они попадали в общий
  плоский список способностей юнита `Abilities.get()` для всех меню и юнитов).
* **Research-гейт у инлайн-элементов меню** (`Abilities.applyResearchGate`, метод стал `public`).

**Данные:** у `skeleton_marksman` в `barracks.json` появился `costOverride` (food 55 / 8 с) — объектная
форма `production` проверяема из коробки.

**Известные ограничения:** `worker.buildSpeed` — только стройка, не ремонт; `gatherable` не включает
`EMERALD`; под-кнопки **код**-меню не гейтятся по research. Неизвестные поля JSON больше **не**
игнорируются молча — см. §10.

---

## 10. Сессия 2026-10-09 (четвёртая): строгая валидация определений

**Зачем.** Кодеки определений (`UnitDefinition`/`BuildingDefinition`/`Faction`) построены на
`RecordCodecBuilder`, который **молча отбрасывает** незнакомые поля. Опечатка (`"carryCapcity"`) или поле
из старой схемы (верхнеуровневый `carryCapacity`) не давали ни ошибки, ни предупреждения: файл грузился,
поле игнорировалось, а контент работал не так, как ожидал автор.

**Сделано:**
* `data/ContentValidator` — обход JSON по записи-получателю: имена полей берутся из компонентов записи
  (новое поле в записи автоматически разрешено), `JSON_FIELD_NAMES` закрывает немногочисленные случаи
  «компонент ≠ JSON-имя» (`Faction`: `nameKey`→`name`, `startingUnits`→`starting_units`,
  `startingFood/Wood/Ore/Emerald`→`food/wood/ore/emerald`; `StartingUnit.entityType`→`unit`;
  `ResearchCondition.researchId`→`research`). Свободные `Map` (`attributes`, `params`, локализованный
  `name`) принимают любые ключи; обе формы `production` (id-строка и объект) — валидны.
* `data/ContentValidationReloadListener` — `ResourceManagerReloadListener` (зарегистрирован в
  `ReignOfNether#reloadListener` рядом с `ResearchJsonLoader`): проверяет `data/<ns>/{unit,building,faction}`
  во всех датапаках и пишет в лог `[content-validation] <file>: '<path>': unknown field 'x' (accepted: …)`.
  Чистый проход — одна INFO-строка со числом проверенных файлов.
* Гейт `test`: `ContentValidationTest` (9 тестов) — поставляемые файлы чисты, а опечатки репортятся на
  корне/во вложенном объекте/в элементе списка/в меню; ловится и скаляр вместо объекта/массива.

**Почему «сообщает», а не «блокирует»:** датапак-реестры читает Minecraft до reload-листенеров, а кодек
к лишним ключам ленив — невалидный файл уже зарегистрирован и «пропустить» его нельзя. Поэтому рунтайм-
диагностика + жёсткий гейт в `test`. Проверено end-to-end: временный `"carryCapcity"` в
`villager_unit.json` уронил `test` с точным сообщением (файл и поле), затем восстановлен.

**Не покрыто:** `data/<ns>/research/*.json` (разбирается вручную `ResearchJsonLoader`) — лишние поля там
по-прежнему игнорируются.
