# STATUS — единый статус-документ

**Дата обновления:** 2026-10-09
**Ветка:** `wip/stage-d-deletions`; `1.21.1-clean` держится синхронно (`git branch -f 1.21.1-clean wip/stage-d-deletions`).
**Точка входа для продолжения:** [`HANDOFF.md`](HANDOFF.md) (архитектура data-driven контента, журнал
инкрементов, трапы, инвентарь файлов). Этот файл — краткая сводка.

**Гейты (текущее состояние):**
- `compileJava` ✅
- `validateMixins` ✅ (все точки инъекции резолвятся; число — в свежем логе)
- `runData` ✅
- `runServer` — ⚠ **не запускать** в этой среде (dev-сервер печатает `Done (...)`, но сам не завершается;
  см. `../AGENTS.md`). Достижение `Done (...)` уже доказывает рантайм-инициализацию.
- `runClient` — ⚠ **ни разу не запускался** с data-driven контентом; всё клиентское проверено только сборкой.

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
1. **`runClient`** — ни разу не прогонялся на data-driven работе. Проверить: постановку/рендер/выбор/
   сейв JSON-зданий, дроп-офф ресурсов, аддоны (garrison/night), исследования, апгрейды (варианты),
   ranged-юнит, HUD/команды.
2. **Ручной прогон** G.2 (отсечение секций выше камеры) и G.5 (удушье камеры) — только в игре.

**Каркас (код):**
3. Data-driven способности: реестр `AbilityTypes` + движковые типы `reignofnether:heal`/`regeneration`/
   `summon` заведены; поля spec прокидываются; пассивки тикают через `Ability.tickPassive`; `params`
   типизированные (число/строка/`ResourceLocation`). Дальше — авторские типы.
4. `UnitDefinition.inherits` — **реализовано** (наследование/мерж). Остаточное: строковые params способностей.
5. ✅ Исследования: эффект `equip` реализован (`ResearchEquipModifier`/`ResearchEquipApplier`,
   `type: "equip"` + `equip: [{item, slot, unit}]`). UI — только список статуса (запуск перенесён к
   зданиям и сделан), дерева нет.
6. ✅ H.9: добавлен движковый аддон `reignofnether:resource_generator` (`ResourceGeneratorAddon`,
   типизированные `params`).
7. JSON-здания: нет поля фракции (`getFaction()` == `""`), меню постройки не фильтруется (фракция задаёт
   только стартовых юнитов — решение владельца). Кастомные кнопки (`hud/custombutton`) уже цепляются к
   JSON-зданиям/юнитам по id. `startingBlockTypes` теперь выводятся из NBT; `foundationYLayers` — нет
   (дефолт 1). `production` принимает `{unit, costOverride}` (`ProductionSpec`).

См. §7 — сессия 2026-10-09 нашла и исправила несколько регрессий рантайма JSON-контента
(здание-исчезновение, воркер-невозможность добычи, NPE населения, цели на загрузке, `inherits`-роли).
**Документация:** этот коммит актуализирует `STATUS.md`, `README.md` и гайды `_GUIDES/`
(`00/04/06/08/09`); исторические снимки (`FEATURES.md`, `INTRUSION_AUDIT.md`, `CLEAN_FORK.md`,
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

Гейты на каждом шаге: `compileJava` → `validateMixins` → `runData`.

---

## 5. Ловушки (не повторять)

1. **Переводы строк (CRLF).** Большинство `.java` хранят CRLF. Правка: нормализовать в LF, редактировать,
   вернуть CRLF; перед коммитом сверять `git diff --numstat` и `git diff --ignore-cr-at-eol --numstat`.
2. **`git show <ref>:<path> > <файл>`** под PowerShell 5.1 пишет UTF-16LE → javac `unmappable character`.
   Только `cmd /c "git show ... > file"`.
3. **`validateMixins` не ловит часть ошибок** (`@Shadow`/`@Redirect`) — всплывают в рантайме.
4. **Сервер.** `runServer` не запускать (см. AGENTS.md); безопасный набор гейтов — `compileJava`,
   `validateMixins`, `runData`.

---

## 6. Ссылки

| Файл | О чём |
|---|---|
| `HANDOFF.md` | **точка входа**: архитектура data-driven контента, журнал, трапы, инвентарь |
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

**Остаётся:** `runClient`-проверка (главное); K4-полностью (список добычи/скорость/перенос), K5 (прокачка
героя), K6 (submenu `row`/`col`); lang-дочистка; осиротевшие `.nbt`.
