# Перенос апстрима 1.20.1 (1.5.0 beta) в неофордж-порт 1.21.1 — статус

Дата: 2026-10-09. Автор заметки: агент-сессия (порт).
Этот файл относится **только к проекту порта**. Каркас (data-driven RTS-фреймворк) — отдельный
проект в этом же репозитории, см. раздел «Разделение проектов».

---

## 1. Разделение проектов (важно)

| проект | ветки | содержимое | правило |
|---|---|---|---|
| **A. Порт** (этот файл) | `1.21.1-dev` (43df3fdea; в основе мерж f167c4e2), `1.21.1-dev-old` (8323d802) | форж 1.20.1 → неофордж 1.21.1, перенос апстрима | переносим сюда коммиты оригинала |
| **B. Каркас** (data-driven) | `1.21.1-rts-carcass` (2bd7ccb8), `1.21.1-rts-carcass-dev` (bdc70c9e) | JSON-фреймворк (unit/building/ability/faction JSON), `docs/`, `_GUIDES/`, `.agents/` | **другой проект**: не мержить в апстрим и не тащить в проект A |
| **C. Оригинал** | `1.20.1-dev` (локально), `upstream/1.20.1-dev` (58854329) | мод автора (SoLegendary) | только чтение; ничего туда не пушим |

`1.21.1-rts-carcass` стоит на верхушке порта (8323d802) + 150 коммитов каркаса — это не
«продолжение порта», а другой продукт на той же базе. `1.21.1-rts-carcass-dev` — рабочий WIP
каркаса (бывший `wip/stage-d-deletions`). `upstream` — read-only зеркало автора, прав на запись нет
и не требуется.

---

## 2. Что сделано в этой сессии

1. **Коммиты оригинала перенесены в порт.** Смержен `upstream/1.20.1-dev` (58854329) в
   `1.21.1-dev` (тогда ещё `port-1.21.1-plus-1.5.0b`) → коммит **f167c4e2**
   (`Merge remote-tracking branch 'upstream/1.20.1-dev' into port-1.21.1-plus-1.5.0b`).
2. **Перенесённый инвентарь** (`c68e0de..58854329`, 7 коммитов автора):

   | коммит | что это | статус в порте |
   |---|---|---|
   | 356d9e4a | fix: grass block dirt sides turning green from fog tint (#497) | перенесён (авто, компилируется) |
   | 56830b23 | 1.20.1 1.5.0 beta 3 (#498) | перенесён (авто) |
   | **1645aaf7** | **Rewrite Faction (#494)** | **перенесён частично → 95 ошибок компиляции (см. §4)** |
   | f860f51f | FIx Wave Bug (#499) | перенесён (авто) |
   | 1f37cd46 | beta-4 | перенесён (авто) |
   | 8c99c37a | todo | перенесён (авто) |
   | 58854329 | feat(translation): update zh-cn (#500) | перенесён (авто) |

   Диф относительно порта до мержа: 262 файла (234 изменено, 21 переименование, 5 добавлено,
   2 удалено). Добавлено апстримом: `building/production/IUnitProductionItem.java`,
   `faction/Factions.java`, `faction/New Faction.md`,
   `research/researchItems/ResearchItemBackpacks.java`,
   `assets/reignofnether/textures/icons/items/bundle.png`.
3. **Документация и бэкап.** Эта заметка + пуш ветки-бэкапа (см. §6).

---

## 3. Почему появились ошибки: слияние «наша сторона побеждает» + splice

Слияние выполнялось с приоритетом нашей стороны (`-X ours`): спорные файлы остались нашими,
неспорные изменения апстрима доехали «вставками» (splice). Там, где апстрим менял участок, который
наш порт не трогал, его код лёг рядом с нашим — и в этих местах появился **смешанный** код: часть
файла использует старую (нашу) энум-`Faction`, часть — новый класс `Faction` из #494.

Итог: 95 ошибок компиляции, почти все — следствие `Rewrite Faction (#494)`, который в порт ещё
**не перенесён по-настоящему**. Это ожидаемая промежуточная точка, а не регрессия основной ветки:
сам порт (`8323d802`) остаётся зелёным.

---

## 4. Текущий статус проверок на `1.21.1-dev` (мерж f167c4e2 + заметки)

| гейт | результат |
|---|---|
| `compileJava` | **FAILED — 95 errors** (лог: `/tmp/port_compile.log`) |
| `validateMixins` | не запускался после мержа (блокируется компиляцией) |
| `runData` | не запускался после мержа (блокируется компиляцией) |

`runServer` не запускаем (см. AGENTS.md).

Ошибки по файлам (доминанта — старый `Faction` из энума):

- `player/PlayerServerboundPacket.java` (12)
- `startpos/StartPosClientboundPacket.java` (9)
- `scenario/ScenarioServerboundPacket.java` (7)
- `unit/UnitClientEvents.java` (6)
- `startpos/StartPosServerboundPacket.java` (5)
- `unit/interfaces/Unit.java`, `unit/units/StriderUnit.java`, `unit/units/MarauderUnit.java`,
  `player/PlayerClientEvents.java`, `building/buildings/villagers/IronGolemBuilding.java`,
  `faction/Factions.java` (по 4)
- `mixin/UnitInventoryMobMixin.java`, `hud/TutorialClientEvents.java`, `sound/SoundClientEvents.java`,
  `player/PlayerServerEvents.java` (по 3)
- `blocks/WalkableMagmaBlock.java`, `survival/SurvivalServerEvents.java`,
  `scenario/ScenarioMenu.java` (по 2)

Типовые причины:

1. `cannot find symbol: variable NONE / VILLAGERS / PIGLINS / MONSTERS / NEUTRAL / RANDOM`
   — наш код (и десятки файлов, ошибки в которых «висят» на `Faction.java`) обращается к константам
   старого энума, а `faction/Faction.java` после мержа заменён новым классом #494.
2. `cannot find symbol: class FactionRegistries` (`ReignOfNether.java`) — апстрим удалил
   `faction/FactionRegistries.java` и `faction/FactionRegister.java`, заменив их на `Factions.java`.
3. `package net.minecraftforge.fml.loading does not exist` / `ForgeRegistries` в `faction/Factions.java`
   и `player/PlayerServerEvents.java` — код апстрима ещё на Forge-API, его надо переписать на неофордж
   (`net.neoforged.*`, `BuiltInRegistries`/`NeoForgeRegistries`).
4. `mixin/UnitInventoryMobMixin.java`: `@Shadow … interact(Player, InteractionHand)` без импортов
   (`Player`/`InteractionHand`/`InteractionResult` — не 1.20.1-имена).

---

## 5. Что осталось сделать (план переноса #494 и хвостов)

Выбранный подход — как в исходном плане апстрима: **не переписывать порт целиком, а довести
`Rewrite Faction` вручную**, оставив нашу рабочую ветку компилируемой на каждом шаге.

1. **Решить, как жить с `Faction`.** Два варианта:
   - ~~*5a. Отложить #494:* вернуть наш энум `Faction` и файлы из §4 к версии порта~~ —
     **испробовано 2026-10-09, не сработало:** откат только «ошибочных» файлов к 8323d802 дал не 0, а
     **352 ошибки** (лог `/tmp/port_compile2.log`; исходное состояние — 95 ошибок,
     `/tmp/port_compile4.log`). Причина: изменения 1.5.0 beta 3/beta-4 и `#494` уже размазаны по
     сотням не-ошибочных файлов — откат части файлов ломает их контрагентов. Ветка возвращена в
     f167c4e2 (`git reset --hard`, дерево чистое, снова 95 ошибок).
   - *5b. Довести #494 до конца (рекомендуется):* перенести новый `Faction`/`Factions` на неофордж
     (`net.neoforged.*`, `BuiltInRegistries`), вернуть `faction/FactionRegistries.java` в новой
     редакции и переписать все обращения к старому энуму. Дороже, но это единственный путь,
     который не ломает уже перенесённый 1.5.0.
2. После выбора — прогнать полный набор гейтов: `compileJava`, `validateMixins`, `runData`.
3. Только после зелёных гейтов — продолжать перенос остальных фич 1.5.0 (не перенесённые вставки).

Полезное для 5b: список затронутых файлов лежит в `/tmp/err_files.txt` (28 файлов), причины — в
`/tmp/port_compile.log`; «эталон» нового `Faction`/`Factions` — в `upstream/1.20.1-dev`
(`src/main/java/com/solegendary/reignofnether/faction/`).

---

## 6. Ветки форка `Tech3434/reignofnether` — финальный набор

После переименования 2026-10-09 (см. §9) в форке оставлены ровно пять веток:

| ветка | SHA | проект | состояние |
|---|---|---|---|
| `1.20.1-dev` | 58854329 | C. оригинал (зеркало) | = верхушка `upstream/1.20.1-dev`; ветка по умолчанию форка, не трогали |
| `1.21.1-dev` | 43df3fdeab | A. порт | WIP-мерж апстрима (f167c4e2 + эта заметка). **Компиляция не зелёная: 95 ошибок** |
| `1.21.1-dev-old` | 8323d80220 | A. порт | порт до мержа апстрима — «чистая рабочая» версия 1.21.1 |
| `1.21.1-rts-carcass` | 2bd7ccb841 | B. каркас | JSON-каркас RTS (ff на 136 коммитов каркаса) |
| `1.21.1-rts-carcass-dev` | bdc70c9e5a | B. каркас | WIP стадии D каркаса (бывший `wip/stage-d-deletions`) |

`upstream/*` (`SoLegendary/reignofnether`) — read-only зеркало автора: ничего туда не пушим
(и прав на запись нет). Ветки проекта A не содержат файлов каркаса, ветки B — не содержат
переноса 1.5.0.

---

## 7. Правила безопасности (повтор)

- Ни один бранч проекта B (каркас) не мержится и не пушится в апстрим (`SoLegendary`).
- Ветки проекта A не содержат файлов каркаса (`docs/`, `_GUIDES/`, `.agents/`, JSON-фреймворк) —
  добавлять их туда не нужно.
- `runServer` не запускать (AGENTS.md: процесс не завершается сам).
- CRLF: правки в `.java`/`lang/*.json` — только с сохранением CRLF, сверять
  `git diff --numstat` и `git diff --ignore-cr-at-eol --numstat`.

---

## 8. Команды гейтов (как проверялось)

```
cd ___temp
"C:\Program Files\Java\jdk-21\bin\java.exe" -Dorg.gradle.appname=gradlew \
  -jar gradle/wrapper/gradle-wrapper.jar compileJava --offline --console=plain
```

Логи: `/tmp/port_compile.log` (мерж f167c4e2, 95 ошибок), `/tmp/port_compile2.log` (эксперимент 5a,
352 ошибки), `/tmp/port_compile4.log` (подтверждение отката, 95 ошибок). `gradlew.bat` не работает,
`runServer` не запускаем (см. AGENTS.md). Документ не влияет на компиляцию (`.md`), поэтому повторный
прогон гейта после его добавления не требуется.

---

## 9. Переименование веток форка (2026-10-09)

Задача: оставить в форке ровно пять веток. Выполнено двумя `git push` (создание новых имён через
явные SHA, затем `git push origin --delete` старых). Перед удалением проверено: у форка нет
открытых PR, ветка по умолчанию `1.20.1-dev` не затрагивалась, ни одна ветка не является head
чужого PR. Итог подтверждён `git ls-remote --heads origin`.

| было | стало | SHA (не менялся) |
|---|---|---|
| `neoforge-1.21.1-port` + дубликат `port-1.21.1-plus-1.5.0` | `1.21.1-dev-old` | 8323d802202e |
| `port-1.21.1-plus-1.5.0b` | `1.21.1-dev` | 43df3fdeab00 |
| `1.21.1-clean` | `1.21.1-rts-carcass` | 2bd7ccb84179 |
| `wip/stage-d-deletions` | `1.21.1-rts-carcass-dev` | bdc70c9e5a08 |
| `1.20.1-dev` | без изменений | 58854329bd64 |

Локальные ветки выровнены по новым именам и снова нацелены на соответствующие `origin/*`;
локальный дубликат `port-1.21.1-plus-1.5.0` удалён (тот же коммит 8323d802, ничего не потеряно).
Любую старую ветку можно вернуть по SHA из таблицы: `git push origin <SHA>:refs/heads/<имя>`.
В refs локального клона остались устаревшие `refs/remotes/2b2m/*` — это следы другого (уже
убранного из конфига) remote, к этому форку отношения не имеют.

---

## 10. Компиляция и гейты на `1.21.1-dev` (2026-10-09, вторая сессия)

Ветка доведена до зелёных гейтов: `compileJava`, `validateMixins` (all injection points resolve),
`runData` (BUILD SUCCESSFUL, мод конструируется и фракции регистрируются в рантайме).
Затронуто 28 файлов.

**Причина прежних 95 ошибок — сплайс:** часть файлов взяла версию 1.5.0, часть осталась нашим
портом до 1.5.0, поэтому в дереве одновременно жили старый энум `Faction` и новый data-driven
`Faction`/`Factions` (объекты в `ReignOfNetherRegistries.FACTIONS`).

### Что менялось

Фракционная система (все вызовы переведены на новый API, как в апстриме):
- `Faction.X` → `Factions.X` — StartPosClientbound/ServerboundPacket, PlayerClientEvents,
  TutorialClientEvents, OrthoviewClientEvents, UnitClientEvents, IllagerWaveSpawner, PlayerServerEvents;
- `unit.getFaction()` → `Factions.getFaction(unit)` — WalkableMagmaBlock, MagmaBlockMixin,
  NonUnitServerEvents; в StriderUnit/MarauderUnit метод `getFaction()` удалён, как в апстриме;
- `switch (faction)` → сравнение с `Factions.*`: PlayerServerboundPacket теперь шлёт один
  `START_RTS` с фракцией в пакете, ScenarioServerboundPacket — `SET_ROLE_FACTION` с фракцией,
  SoundClientEvents — `faction.sound`;
- `Faction.valueOf(nbt)` → `Factions.getFaction(ResourceLocation.parse(...))` с фолбэком на старые
  имена (`VILLAGERS`/`MONSTERS`/`PIGLINS`) — RTSPlayerSaveData (совместимость со старыми сейвами);
- `FactionRegistries.register()` → `Factions.register()`, и вызов перенесён из конструктора мода в
  `FMLCommonSetupEvent#enqueueWork`: до завершения register-событий `DeferredHolder.get()` падает
  с «Trying to access unbound value».

Прочие следы сплайса:
- Unit: `getCharges`/`getCooldowns` → `getAbilityCharges`/`getAbilityCooldowns`, возвращён импорт
  `items.UnitInventory`;
- Factions: неофордж-импорт `FMLEnvironment` + проверка `dist == Dist.CLIENT`;
- PlayerServerEvents: `ForgeRegistries.ENTITY_TYPES` → `BuiltInRegistries.ENTITY_TYPE`;
- ClientModEvents: `new FogTintingBlockColor(existing, biomeTinted.contains(block))` и убран
  лишний `continue`, из-за которого биомные блоки вообще не получали fog-тинт;
- восстановленные импорты: `Component` (IronGolemBuilding), `InteractionHand/InteractionResult/Player`
  (UnitInventoryMobMixin), `static getAbsoluteBlockData` (BeaconPlacement), `static fcs` (PoisonSpiderUnit).

### Доведённые до работоспособного состояния фичи 1.5.0

- 14 звуков: `item_drop_common/rare/legendary`, `building_smash`, `buzzy_nest`, `critical_hit`,
  `ghost_cloak`, `gong_of_weakning`, `ice_wand`, `shadow_shifter`, `tome_of_duplication`,
  `totem_place`, `war_horn`, `potion_pop`. Энум `SoundAction`, `sounds.json` и .ogg уже были из
  мержа, но регистраций в `SoundRegistrar` и записей в `SOUND_MAP` не было — теперь есть, так что
  вызовы в `UnitItems`/`TotemItem` действительно начинают звучать;
- Vigor → мана героя в `Unit.tick` (апстримный блок, в порте отсутствовал);
- `MiscUtil.getNextItem`/`getLastItem` (нужны SandboxClientEvents/ScenarioMenu).

### Известные пробелы 1.5.0 (не перенесено)

- IllagerWaveSpawner: апстрим выдаёт снаряжение с зачарованиями (SHARPNESS/QUICK_CHARGE/MULTISHOT)
  и лидер-баннер (`Raid.getLeaderBannerInstance()`); в порте этого нет;
- UnitClientEvents: апстримная карта `mobEffectIcons` и часть отрисовки/подсказок;
- остальные расхождения девяти файлов, где мерж оставил наш код (ReignOfNether, UnitClientEvents,
  PlayerClientEvents, SoundClientEvents, OrthoviewClientEvents, TutorialClientEvents,
  IllagerWaveSpawner, StriderUnit, MarauderUnit), в основном наши неофордж-адаптации событий,
  но нужен отдельный ревизионный проход на предмет мелких фич;
- `mixin/fogofwar/LiquidBlockRendererMixin` целится в фордж-класс
  `net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions` — это было и до мержа
  (есть в `1.21.1-dev-old` и в апстриме), компилируется, но в неофордже цель может не найтись.

### Проверки

```
cd ___temp
java -Dorg.gradle.appname=gradlew -jar gradle/wrapper/gradle-wrapper.jar compileJava  --offline --console=plain
java ... validateMixins --offline --console=plain   # validateMixins: all injection points resolve
java ... runData        --offline --console=plain   # BUILD SUCCESSFUL
```

Логи: `/tmp/c6.log` (compileJava), `/tmp/vm3.log` (validateMixins), `/tmp/rd4.log` (runData).
`runServer` не запускался (AGENTS.md). CRLF: правки делались байтовым патчером, `git diff --numstat`
совпадает с `git diff --ignore-cr-at-eol --numstat`.
