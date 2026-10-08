# STATUS — единый статус-документ

**Дата:** 2026-10-08
**Ветка:** `wip/stage-d-deletions`
**HEAD:** `67e020fd` + незакоммиченные правки повторного прогона (см. `BUGS_RUNCLIENT.md` §«Инкремент 4»)
**compileJava:** ✅ 0 ошибок
**validateMixins:** ✅ 48 injection point'ов / 31 mixin-класс, все резолвятся
**runData:** ✅ BUILD SUCCESSFUL
**runServer:** ✅ `Done (1.031s)!`, 0 `mixin apply failed`, 0 SEVERE/ERROR
**runClient:** ⚠ прогонялся владельцем — найдено 9 багов, разбор и правки в `BUGS_RUNCLIENT.md`

Версия: 1.4.4d, MC 1.21.1, NeoForge 21.1.250.

---

## 1. Что сделано

### Этап E — дыры каркаса (закрыт)

| Пункт | Что |
|---|---|
| E.2 | Удалены 6 атрибутов без потребителей + геттеры в `AttackerUnit` + lang-ключи |
| E.3 | Удалены 5 эффектов + `MobEffectIcon` + `@Inject` морозного урона |
| E.4 | Лимит армии: база 1, ратуша даёт `populationSupply` (TownCentre = 10) |
| E.5 | Ленивое `SavedData`: `getLoaded()`/`isStored()`, пустой мир не пишет файлов |
| E.9 | Прямое сопоставление `ProductionItem` ↔ `EntityType<?>`; убран чит Killer Rabbit |
| HeroUnit | Удалены `HeroUnitSave`/`HeroUnitSaveData`/`ResearchSaveData` и их вызовы |
| — | Песочница удалена целиком (меню, кнопки, пакеты, все `isSandboxPlayer`-гейты) |

### Удаление читов

`Cheats`, `CheatsClientboundPacket`, `PlayerAction.SET_CHEAT`, `onPlayerChat` и все читы
(`warpten`, `foodforthought`, `operationcwal`, `slipslopslap`, `modifythephasevariance`,
`wouldyoukindly`, `greedisgood`, `allcheats`) **вырезаны** вместе с гейтами и lang-ключами.
`wouldyoukindly` унёс и управление ванильными мобами: не-принадлежащих юнитов больше не
командуешь.

### Этап G — камера

| Пункт | Статус |
|---|---|
| G.1 | Высота камеры пинится (`setCameraHeight`/`clearCameraHeightOverride`) — сделано раньше |
| G.2 | Отсечение секций выше камеры — **не сделано**, проверяемо только в игре |
| G.3 | Вход в орторежим больше не стирает зелья и не поднимает Peaceful→Easy |
| G.4 | `TopdownGui` больше не форсит `guiScale` и не глотает клавиши инвентаря/достижений |
| G.5 | Проверка удушья камеры — только в игре (удушье возвращено ваниле ещё в этапе B) |

### Этап F — хвосты

| Пункт | Что |
|---|---|
| F.4 | `CustomBuilding`: три `buildableBy*` флага сведены к одному (`buildableByVillagers`) |
| F.7 | Удалены ключи `hud.faction.reignofnether.*` из 17 языковых файлов |
| C.3 | Удалены мёртвые геймрулы `coopMode` и `scenarioMode` (режимы сценария/кооператива) |

### Документация

* `docs/reference/` — новая папка: инвентарь удалённого контента + способ восстановления
  (`git show 5079004e:<путь>`).
* `.agents/skills/` — навыки для будущих ИИ-агентов (как коммитить, как собирать, как добавить
  фракцию и т. д.).

---

## 2. Что осталось

### Гейты и проверки в игре

1. **`runClient`** — единственный непройденный гейт; проверяет `@Override`-модель
   (`ClientPacketListenerMixin`, 16 методов), которую `runServer` не трогает.
2. **Ручной прогон** — G.2 (секции выше камеры), G.5 (удушье), геометрия на экране, зависание
   при выходе из мира.

### Этап H — РТС-режим для обычной игры — закрыт

H.1–H.8 сделаны (`15bd1a69`): пропуск = право оператора 2, вход по команде и хоткею, конец сессии
по потере последнего здания с условием «здание вообще было», `force-loose`, юнит за границей мира
заталкивается внутрь, спавн рядом с игроком, хук стартовой армии.

### Система юнитов и строений «как в Warcraft 3» (§14 плана) — закрыта

§14.1–§14.4 сделаны; §14.5 (урон по HP от копания здания) **откачен** — см. ниже. Подробности и
остаток — `PLAN_RTS_ONLY.md` §15. Вкратце: подменю способностей (`Ability.subAbilities`,
`MenuAbility`, стек меню в HUD); приказы и кнопки постройки — способности (`CommandAbility`,
`CommandAbilities`, `BuildMenuAbility`, `ActionButtons` удалён`); производство зданий — способности
(`ProductionAbility`, HUD рисует один список); вскапывание `DIG_BLOCK`/`DIG_AREA` (`DigAbility`,
дроп в `UnitInventory`) с рамкой box-select для области.

**§14.5 откачен.** Копание по блоку строения **отменяется**: ни копания, ни дропа, ни урона.
Процентный урон `DigAbility.BUILDING_DAMAGE_PERCENT_PER_HIT` и `BuildingPlacement.demolishTopDown`
удалены (коммит `445447ad`). Здания ломаются обычной атакой.

**Повторный `runClient` (правки не закоммичены).** `DIG_AREA` не копал, потому что при постановке
отбрасывались блоки дальше `DIG_RANGE` — теперь копится вся очерченная область, а юнит идёт к
недосягаемому блоку; смена режима сбора больше не сбрасывает приказы (`TOGGLE_GATHER_TARGET`);
счётчик переноски снова пересобирается с нуля (`syncUnitResources`). Подробности —
`BUGS_RUNCLIENT.md` §«Инкремент 4».

**Прогон 2 `runClient` (правки не закоммичены).** По 8 пунктам владельца: `DIG_AREA` удалён
целиком; режим сбора больше не сбрасывается через пару секунд (корень — `syncFromServer` затирал
`targetResourceName` в `NONE`); иконка вскопки — железная кирка; листва: сброс режима при выходе из
РТС и ускоренное переключение «вокруг юнитов/курсора»; миникарта ускорена (участок 4 тика);
подменю абилок рисуется на месте нижнего ряда; профессии и ветеранство `VillagerUnit` удалены
(вместе с `VillagerUnitProfession`, `VillagerUnitProfessionLayer`, синком `MAKE_VILLAGER_VETERAN` и
бонусами). Гейты: `compileJava` ✅, `validateMixins` ✅ (48/31), `runData` ✅, `runServer` ✅
`Done (1.120s)!`. Разбор — `BUGS_RUNCLIENT.md` §«Прогон 2».

**Прогон 3.** После выхода из РТС игрок оставался в `SPECTATOR` и не мог ломать блоки нигде.
Исправлено восстановление режима игры на выходе: `PlayerServerEvents.restoreGameModeOnLeave`
вызывается из `closeTopdownGui` и `disableOrthoview`, `SPECTATOR` больше не сохраняется как
исходный, `resetRTS` не перезаписывает исходные режимы. Заодно `Abilities.getButtons` расширен с 6
до 8 слотов хоткеев (7-я абилка больше не роняет клиент). Гейты: `compileJava` ✅, `validateMixins`
✅ (48/31), `runData` ✅, `runServer` ✅ `Done (0.956s)!`. Разбор — `BUGS_RUNCLIENT.md` §«Прогон 3».

**Прогон 4.** Ломание блоков приведено к желаемому: блоки мира — обычная ванильная добыча в любом
режиме (удалена отмена в `BlockServerEvents.onPlayerBlockBreak`); блоки строений РТС — поломка
моментально отменяется, блок остаётся, а строение получает урон по HP
(`ResourcesServerEvents.onPlayerBlockBreak` → `destroyRandomBlocks`). Геймрул `doPlayerGriefing`
стал неиспользуемым. Гейты: `compileJava` ✅, `validateMixins` ✅ (48/31), `runData` ✅,
`runServer` ✅ `Done (0.991s)!`. Разбор — `BUGS_RUNCLIENT.md` §«Прогон 4».

**Прогон 5.** При поражении юниты игрока не становились нейтральными (только при новой игре):
`defeat` ставил `ownerName = ""` после `resetBehaviours()`, и падение сброса обрывало цикл. Владение
теперь очищается первым, а сброс поведения вынесен в `PlayerServerEvents.neutraliseUnitsOf` с
`try/catch` на юнит. Гейты: `compileJava` ✅, `validateMixins` ✅ (48/31), `runData` ✅,
`runServer` ✅ `Done (1.011s)!`. Разбор — `BUGS_RUNCLIENT.md` §«Прогон 5».

**Инвентаризация изменений ванильных механик** — `docs/VANILLA_CHANGES.md`.

**Сужение ванильных изменений (2026-10-08).** Удалён `ArmorStandMixin`; удалены 8 геймрулов без
читателей (`doPlayerGriefing`, `groundYLevel`, `flyingMaxYLevel`, `allowBeacons`, `pvpModesOnly`,
`beaconWinMinutes`, `allowedHeroes`, `randomItemDrops`) — потолок полёта теперь
`Unit.getFlyingMaxY()` (default 320, оверрайд на юнит); `AbstractArrowMixin` больше без `@Overwrite`
(ванильные стрелы не трогаются); `LivingEntityMixin` сужен (партиклы левитации только юнитам,
мёртвый код убран). Командные миксины `/data`/`/execute` и `UnitInventoryMobMixin` оставлены по
решению владельца. Гейты: `compileJava` ✅, `validateMixins` ✅ (47/30), `runData` ✅,
`runServer` ✅ `Done (0.964s)!`. Разбор — `BUGS_RUNCLIENT.md` §«Сужение», сводка —
`docs/VANILLA_CHANGES.md`.

**Чистка и доки (2026-10-08).** Из `data/reignofnether` удалены 65 неиспользуемых
`structures/*.nbt` (остались `town_centre.nbt` и `barracks.nbt` — их грузит `Building.structureName`)
и `maps/*.json`. `FEATURES.md`, `INTRUSION_AUDIT.md` и `PLAN §15.1/§15.3/§15.4` приведены к
текущему состоянию (DIG_AREA удалён, §14.5 откачен, геймрулов 12, миксинов 21/11). Пакет `gamemode/`
удалён (единственный режим `CLASSIC`). План исследований/расширяемости —
`RESEARCH_AND_EXTENSIBILITY_PLAN.md`.

**Фаза 1 исследований (2026-10-08).** Добавлено ядро: `research/ResearchCondition` (с инверсией),
`ResearchType`, `Research`, `ResearchAttributeModifier`, `ResearchRegistry`, `ResearchSaveData`
(per-player, персистентно), `ResearchUtils` (+ клиентское зеркало), `ResearchClientboundPacket` +
`ResearchClientEvents`, команды `/research grant|revoke|clear|list`, синк при входе, сброс при
поражении и в `resetRTS`. Гейты: `compileJava` ✅, `validateMixins` ✅ (47/30), `runData` ✅,
`runServer` ✅ `Done (0.909s)!`. Гейты контента (способности/производство/строительство) и
JSON-загрузчик — фазы 2/4.

**Фаза 2 исследований (2026-10-08).** `requiredResearch` добавлен в `Ability`, `ProductionItem` и
`Building`. Серверные гейты: использование способности юнита (`UnitActionItem`), способность здания
(`BuildingAbilityServerboundPacket`), старт производства (`ProductionItem.canProduce`), постановка
здания (`BuildingServerEvents.placeBuilding`; GM-команды обходят). HUD: кнопки способностей
(`Abilities.getButtons`) и строительного меню (`VillagerUnit.getBuildingButtons`) блокируются через
`isEnabled`. Ланг-ключ `server.reignofnether.research_required`. Гейты `compileJava`/`validateMixins`/
`runData` ✅; `runServer` доходит до `Done (...)`, но dev-сервер в этой среде сам не завершается —
`runServer` больше не поднимаю (см. AGENTS.md). JSON-загрузчик (фаза 4) — следующий шаг.

**Фаза 3 исследований (2026-10-08).** `ResearchAttributeApplier` применяет `ATTRIBUTE_BOOST` к
юнитам владельца (модификаторы именованы по id исследования — идемпотентно; `unitFilter` — по типу
сущности). Хуки: спавн (`UnitServerEvents.onEntityJoin`), grant/revoke/clear (`ResearchCommand`),
сброс при поражении и в `resetRTS`. Гейты `compileJava`/`validateMixins`/`runData` ✅.

**Фаза 4 исследований (частично, 2026-10-08).** `ResearchJsonLoader` читает
`data/<ns>/research/<name>.json` (`/reload`), id = `<ns>:<name>`; поля `name`, `icon`, `type`
(`unlock`/`attribute_boost`), `cost`, `prerequisites` (с `invert`), `attributes` (со `unit`).
JSON для юнитов/зданий/способностей — отложено до совместной сессии. Гейты ✅.

**П4 (2026-10-08).** Форма генератора ресурсов заложена: интерфейс `ResourceGenerator`
(`getTickInterval`/`getResourceAmount`/`getResourceType`/`getCapacity`) и `ResourceGenerators.produce`
для начисления владельцу. Настраиваемый блок лесов (D.21) уже был реализован
(`Building.scaffoldFill`/`scaffoldBlock`). Гейты ✅.

**Реестр фракций и старт (2026-10-08).** Фракция — объект datapack-реестра
(`data/<ns>/faction/<name>.json`; `FactionRegistries.FACTION_KEY`, синхронизируется клиентам);
поля `name`/`icon`/`capitol`/`starting_units`/ресурсы. `PlayerServerEvents.startRTS` получил
перегрузку с `factionId`: ставит столицу фракции и спавнит её стартовый отряд (фолбэк — константы
`STARTING_ARMY`/`Buildings.TOWN_CENTRE`). Гайд `_GUIDES/01_faction.md` и навык
`reignofnether-add-faction` переписаны под реестр; `_GUIDES/05_hero.md` — под уровень/ману/ранги
героя. Гейты `compileJava`/`validateMixins`/`runData` ✅.

**Выбор фракции при входе (2026-10-08).** Команда `/startrts [<faction>]` (нужен RTS-пропуск)
открывает клиентское меню фракций: без аргумента — выбирай любую; с фракцией — только её, остальные
кнопки видны, но заблокированы. Список синхронизируется (`FactionClientboundPacket` →
`FactionClientEvents` → `FactionMenu` в HUD); клик стартует матч с выбранной фракцией
(`PlayerServerboundPacket.START_RTS` теперь несёт `factionId`). Гейты ✅.

**HUD исследований (2026-10-08).** Определения исследований синхронизируются клиенту
(`ResearchDefinitionsClientboundPacket` → `ResearchClientEvents.definitions`); в HUD добавлена
информационная панель `ResearchMenu` (кнопка-тоггл + список со статусом researched/available/locked).
Запуск исследования из UI — пока нет (нужен серверный запрос со стоимостью/временем). Гейты ✅.

**Запуск исследований (2026-10-08).** `ResearchServerboundPacket` (клик в панели) — сервер проверяет
предпосылки и стоимость (`ResourcesServerEvents`), списывает ресурсы, выдаёт исследование, синкает
клиенту и применяет атрибутные апгрейды (`ResearchAttributeApplier.refreshForOwner`). Длительность
(`cost.ticks`) пока не enforced — мгновенно. Гейты ✅.

**П5 (2026-10-08).** `1.21.1-clean` fast-forward'нут на текущую работу (`wip/stage-d-deletions`);
обе ветки указывали на `ec35ca7d`.

### Документация

* `FEATURES.md` и `INTRUSION_AUDIT.md` — актуализировать (снять удалённое, отметить читы/туман).
* Сверить `docs/README.md` (ссылка на `reference/` уже поправлена).

---

## 3. Как собирать

`gradlew.bat` не работает (передаёт java одновременно `-classpath ""` и `-jar`). Запускать через java:

```bash
cd ___temp
JAVA="/c/Program Files/Java/jdk-21/bin/java.exe"
"$JAVA" -Dorg.gradle.appname=gradlew -jar gradle/wrapper/gradle-wrapper.jar <task> --offline --console=plain
```

`runServer` при `--offline` и закрытом stdin завершается сам (`Done (...)`, затем `Stopping server`).

---

## 4. Ловушки (не повторять)

1. **Переводы строк.** Репозиторий хранит CRLF в большинстве `.java` и частью в `lang/*.json`.
   `sed -i`/`perl`-скрипты, меняющие `\r\n`↔`\n`, превращают дифф в «весь файл изменён».
   Правку CRLF-файла делать так: `perl -i -pe 's/\r\n/\n/g'`, затем редактировать, затем
   `perl -i -pe 's/\r?\n$/\r\n/'`. Перед коммитом проверять `git diff --numstat`.
2. **`git show <ref>:<path> > <файл>`** под PowerShell 5.1 пишет UTF-16LE → javac: `unmappable
   character`. Только `cmd /c "git show ... > file"`.
3. **`validateMixins` не ловит часть mixin-ошибок** — они всплывают в рантайме. Надёжный гейт —
   `runServer` / `runClient`.
4. **Сервер.** Поднимать сервер можно, но **обязательно завершать в течение 15 секунд** — иначе
   процесс висит до ручного вмешательства владельца. Единственный безопасный вариант — `runServer
   --offline` при закрытом stdin: он печатает `Done (...)` и сам делает `Stopping server`. Любые
   интерактивные/долгоживущие серверные процессы запускать нельзя.

---

## 5. Ссылки

| Файл | О чём |
|---|---|
| `PLAN_RTS_ONLY.md` | исходный план A–H, все решения |
| `reference/README.md` | инвентарь удалённого контента |
| `../_GUIDES/README.md` | как добавить юнит/здание/способность/фракцию |
| `FEATURES.md` | каталог функций с вердиктами |
| `BUGS_RUNCLIENT.md` | баги после первого `runClient`: разбор причин, вопросы, план правок |
| `INTRUSION_AUDIT.md` | что мод ломал в обычном мире |
| `VANILLA_CHANGES.md` | текущая инвентаризация изменений ванильных механик |
| `RESEARCH_AND_EXTENSIBILITY_PLAN.md` | план системы исследований и JSON-расширяемости |
| `../.agents/skills/` | навыки для ИИ-агентов |

**Аудит доков (2026-10-08).** Удалены устаревшие `docs/_end.md`, `docs/STAGE_D_PROGRESS.md`,
`docs/WORKLOG.md` и апстримные `TODO*.txt` (описывали удалённое). Индекс `docs/README.md` переписан;
висячие ссылки на `HOWTO_FACTION`/`WORKLOG` поправлены; навык `reignofnether-add-research` переписан
под новую систему; в `_GUIDES/README` и в самих гайдах помечены требующие ревизии (`00_обзор`,
`01_faction`, `05_hero`). Удалены все `.py`/`.log` и явный мусор в корнях репозитория и воркспейса.
`.md` в корне воркспейса (вне git) оставлены по решению владельца. Позже переписаны гайды
`_GUIDES/00_обзор.md` (карта проекта), `01_faction.md` (реестр фракций), `05_hero.md` (уровень/мана),
`07_research.md` (новая система исследований).
