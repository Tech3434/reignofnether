# Ветка `1.21.1-clean` — план «менее интрузивный мод»

Ветка отходит от `port-1.21.1-plus-1.5.0` (`8323d802`) и существует для одной цели: **убрать
поведение мода, которое портит обычную ванильную игру и конфликтует с другими модами**, не ломая
 RTS-функциональность для тех, кто её хочет.

Ветка функционально полная — это не «мод без фич». Каждая интрузивная подсистема должна стать
**выключаемой** и по умолчанию выключенной в «чистом» профиле, а не удалённой.

Документ содержит инвентаризацию (что именно сегодня трогает ваниллу) и поэтапный план
(что делать дальше). Фактическое состояние кода — источник истины; числа ниже проверены по
репозиторию на `8323d802`.

---

## 1. Инвентаризация интрузивности

### 1.1 Миксины — главная поверхность воздействия

`src/main/resources/reignofnether.mixins.json`:

| Секция | Количество | Значение |
|---|---|---|
| `mixins` (общие, работают и на сервере) | **49** | меняют поведение ванильного мира на dedicated-сервере |
| `client` | **27** | клиентский рендер/UI |
| **Итого** | **76** | |

Главные группы воздействия на ваниллу:

* **Туман войны** — `mixin/fogofwar/` (15 миксинов): `ChunkMapMixin`, `ChunkMapInitialSendMixin`,
  `ChunkHolderMixin`, `TrackedEntityMixin`, `ServerLevelParticleMixin`, `ClientChunkCacheMixin`,
  `ClientPacketListenerLightMixin`, `CompiledChunkMixin`, `EntityRenderDispatcherMixin`,
  `EntityShadowMixin`, `ItemEntityRendererMixin`, `LiquidBlockRendererMixin`,
  `SingleQuadParticleMixin`, `BiomeColorsMixin`, `LevelRendererMixin`.
  По сути это запрет ванильному серверу отправлять клиенту чанки/сущности/свет — то есть
  несовместимость с любым модом, меняющим видимость или рендер.
* **Мобы** — `MobMixin`, `LivingEntityMixin`, `BlazeMixin`, `HoglinMixin`, `IronGolemMixin`,
  `VexMixin`, `WitchMixin`, `ZoglinMixin`, `PanicGoalMixin`, `BaseSpawnerMixin`,
  `EvokerFangsMixin`, `WitherRoseMixin`, `goals/MobMixin`, `goals/RandomStrollGoalMixin`,
  `WebBlockMixin`, `PowderSnowBlockMixin`, `LeavesBlockMixin`, `fire/*` (5 миксинов).
* **Игрок и пакеты** — `PlayerMixin`, `ServerGamePacketListenerImplMixin`, `ExecuteCommandMixin`,
  `ServerLevelMixin`, `LevelChunkMixin`, `DataCommandsMixin`.
* **Клиент, затрагивающий не-RTS контент** — `TitleScreenMixin` (кастомные кнопки lilypad/discord
  на главном меню), `MusicManagerMixin`, `SplashRendererAccessor`, `MixinMinecraft`,
  `MusicManagerMixin`, `ChatComponentMixin`, `ItemInHandLayerMixin`, `FrustumMixin`,
  `ResourceLoadStateTrackerMixin`, `ClientLevelMixin`, `ClientPacketMixin`, `OrthoViewMixin`,
  `CameraMixin`.

**Что делать.** Разделить миксины на три группы и сделать включаемыми:

1. * RTS-необходимые* (`UnitInventoryMobMixin`, `PathNavigationMixin`, `goals/*`,
   `EntitySelectorAccessor`, `ObjectiveCriteriaAccessor`, `StructureBlockEntityAccessor`,
   `LivingEntityAccessor`) — остаются всегда.
2. * Опциональные, по геймрулю/конфигу* (туман войны, `doUnitGriefing`-семейство, спавн-блокировка,
   `TitleScreenMixin`, `MusicManagerMixin`) — каждый за своим флагом.
3. * Только для владельцев RTS-игроков в этом мире* (`ChunkMapMixin`/`ChunkHolderMixin`
   фильтруют рассылку по игрокам — их можно активировать лениво) — включать, когда в мире есть
   RTS-игрок.

Пункт 1 — самый недооценённый: `fogofwar/*` можно регистрировать **только если** в мире включён
туман, иначе они не нужны вовсе.

### 1.2 Геймрулы — 23 штуки, часть с неванильными дефолтами

`registrars/GameRuleRegistrar.java`. Дефолты, которые стоит проверить на «чистоту»:

| Геймрул | Дефолт | Комментарий |
|---|---|---|
| `doNetherConversion` | `true` | превращает постройки в Нижнем мире; при `true` работает постоянно |
| `buildingsOutsideBorder` | `true` | здания можно ставить за границей мира |
| `doPlayerGriefing` | `true` | расширяет правило грифа на RTS-юнитов игроков |
| `neutralAggro` | `true` | НЕ ванильный геймрул (`doMobGriefing`-семейство) |
| `allowBeacons` | `true` | маяки как условие победы |
| `doLogFalling` | `true` | ванильный геймрул с не-ванильным применением |
| `maxPopulation`, `groundYLevel`, `flyingMaxYLevel`, `beaconWinMinutes`, `randomItemDrops` | числовые | не ванильные, требуют решений по умолчанию |
| `rtsPathfinding` | `false` | корректный не-интрузивный дефолт |
| `reignofnetherForceFog` | `false` | корректный не-интрузивный дефолт |
| `doUnitGriefing`, `scenarioMode`, `coopMode`, `slantedBuilding`, `lockAlliances`, `pvpModesOnly` | `false` | корректные |

**Что делать.** Дефолты `doNetherConversion`, `buildingsOutsideBorder`, `neutralAggro`,
`allowBeacons` — пересмотреть в сторону ванильного поведения. `doMobGriefing` уже есть в ванилле,
неванильный `neutralAggro` стоит переименовать в namespace мода (например
`reignofnetherNeutralAggro`), чтобы не занимать короткое имя в общем пространстве.

### 1.3 Автоматические включения при старте мира — самое неприятное

`worldborder/WorldBorderServerEvents.java:38` `onServerStarted`:

```java
WorldBorder border = level.getWorldBorder();
// A small world border marks an RTS-optimised map; a vanilla-sized border is left fully untouched.
if (!isRtsOptimisedMap(level))
    return;
...
server.getGameRules().getRule(GameRuleRegistrar.RTS_PATHFINDING).set(true, server);
UnitServerEvents.rtsPathfinding = true;
...
prewarmNavmesh(level, border);
```

То есть **любой мир с уменьшенной границей** мод молча считает RTS-картой, включает геймруль
и **синхронно генерирует весь мир внутри границы** (`prewarmNavmesh` → `level.getChunk(cx, cz,
ChunkStatus.FULL, true)` в двойном цикле по границе).

Это самое разрушительное поведение мода для чужого мира: маленькая граница — обычная практика
модпаков на Survival-картах, и мод на них запускает форсированную генерацию.

**Что делать.** Убрать автоопределение либо сделать его явным геймрулем
(`reignofnetherRtsMap`, дефолт `false`). Преварм — только по явному запросу и с ограничением
радиуса, не по всей границе.

### 1.4 Мир сохраняется: 8 файлов `SavedData`

Проверено на дев-мире (`___temp/run/world/data/`):

```
saved-building-data.dat
saved-custom-building-data.dat
saved-herounit-data.dat
saved-netherzone-data.dat
saved-resources-data.dat
saved-rtsplayer-data.dat
saved-scenario-data.dat
saved-target-resources-data.dat
```

Плюс `neoforge_data_attachments.dat`. Мод пишет их в **любой** мир, где установлен, даже если
RTS-игроков нет.

**Что делать.** Создавать `SavedData` лениво — только после первого RTS-события. Пустой
`SavedData` не должен появляться в мире, где мод не используется.

### 1.5 Конфигурация — только стоимости

`config/ReignOfNetherCommonConfigs.java` (~21 КБ) целиком состоит из записей стоимости
(`UnitCosts.*`, `BuildingCosts.*`, `ResearchCosts.*`, `AbilityCosts.*`). **Ни одного
поведенческого переключателя нет ни в common-, ни в client-конфиге.** Клиентский конфиг — это
цвета игроков, чувствительность камеры и `square_minimap`.

**Что делать.** Это основная работа ветки: завести поведенческие флаги. См. §2.

### 1.6 Билеты чанков и форсированная загрузка

`util/ChunkTicketUtil.java` держит чанк юнита загруженным (радиус 0 — это минимум, который вообще
нужен). `BuildingPlacement#forceChunk` — то же для зданий. Плюс `WraithSnowBlockEntity` вешает
`TicketType.FORCED` (радиус 12 по умолчанию из ванильного `addRegionTicket(..., 1, ...)`).

**Что делать.** Тикеты должны сниматься при удалении юнита/здания. В `UnitServerEvents#onEntityLeave`
снятие билета **закомментировано** (это поведение апстрима 1.20.1, не регрессия порта):

```java
//ChunkAccess chunk = evt.getLevel().getChunk(entity.getOnPos());
//ForgeChunkManager.forceChunk((ServerLevel) evt.getLevel(), ..., entity, ...x, ...z, false, true);
//forcedUnitChunks.removeIf(p -> p.getFirst() == entity.getId());
```

Два других места с тем же паттерном принудительной загрузки (пока под `isRtsOptimisedMap`, но
выстрелят, как только включится §1.3):

* `WorldBorderServerEvents.java:96` — преварм навмеша по всей границе.
* `FogChunkSnapshot.java:75` — снимок тумана по всей границе, `getChunk(cx, cz, true)`.

### 1.7 Прочее

* Команды в неймспейсе `/rtsapi` (см. `src/main/java/.../commands/README.md`).
* Клавиатурные бинды (`keybinds/Keybindings.java`) — регистрируются глобально.
* Управление временем (`time/TimeServerEvents.java:94`) — `onWorldTick` принудительно
  возвращает `setDayTime(serverStartTime)`, но **только** когда включён геймрул
  `scenarioMode` (дефолт `false`) и в мире нет RTS-игроков. То есть при выключенном
  сценарии мод времени не трогает.
* HUD-оверлей и камера орторежима.

---

## 2. Целевой «чистый профиль»

Мир, в котором мод установлен, но никто не играет в RTS, должен вести себя **как ваниллу**.
Минимум:

1. Ни одного файла `SavedData` в мире.
2. Ни одного тикета чанка от мода.
3. Ни одного активного миксина из опциональных групп (в т.ч. `fogofwar/*`).
4. Ни одного форсированного изменения границы мира, времени, спавна.
5. Геймрулы мода зарегистрированы (это не intrusive — регистрация геймрула безопасна), но
   все неванильные ведут себя как ваниллу по умолчанию.
6. Клиент: главное меню, музыка и HUD не изменяются, пока RTS не активирован.

## 3. План работ — исполняемый

Этапы упорядочены по убыванию «разрушительности на единицу усилий». Каждый этап — отдельный
коммит, с гейтами и ручной проверкой. Перед началом этапа прочитать §1 (инвентаризация) и
`WORKLOG.md`.

Обозначения: **УМ** — уже сделано, **Эn** — этап n, ☐ — пункт внутри этапа.

---

### Этап 1 — убрать автоопределение RTS-карты и принудительную генерацию

**Зачем.** Любой мир с уменьшенной границей мира (обычная практика модпаков на Survival-картах)
мод молча считает RTS-картой, включает геймрул `rtsPathfinding` и **синхронно генерирует весь
мир внутри границы**. Это самое разрушительное поведение мода для чужого мира.

**Ключевое решение: не удалять концепцию, а сделать её явной.** `isRtsOptimisedMap` нужен
туману войны и преварму — они требуют ограниченной области. Поэтому режим не убирается, а
перестаёт включаться сам.

☐ 1.1 Новый геймрул `reignofnetherRtsMap` (boolean, дефолт `false`) в
`registrars/GameRuleRegistrar.java` + поле `RTS_MAP` в ключи `GameruleAction`
(`gamerules/GameruleAction.java`) и синхронизация на клиент в
`gamerules/GameruleServerEvents.java` + клиентское зеркало в `gamerules/GameruleClient.java`.
☐ 1.2 `WorldBorderServerEvents#isRtsOptimisedMap` начинает возвращать
`уровень && геймрул RTS_MAP && border <= RTS_OPTIMIZED_BORDER`.
**Все потребители при этом становятся опциональными автоматически**, трогать их не нужно:
`FogOfWarServerEvents:266,279,333`, `PlayerServerEvents:476`.
☐ 1.3 Клиентское зеркало `FogOfWarClientEvents:217-220` — добавить проверку геймрула.
☐ 1.4 Преварм больше **не генерирует** terrain: заменить
`level.getChunk(cx, cz, ChunkStatus.FULL, true)` (`WorldBorderServerEvents.java:96`) на
загрузку только уже существующих чанков. Плюс ограничить радиус преварма константой,
чтобы даже в явном RTS-режиме не форсировать всю границу.
☐ 1.5 `FogChunkSnapshot#captureFogChunks` (строка 75, `getChunk(cx, cz, true)`) — то же:
никогда не генерировать, только индексировать уже существующие чанки.
☐ 1.6 Локализация: `commands.reignofnether.gamerule.*` в `en_us.json` (и `ru_ru.json`),
описание флага для админов.
☐ 1.7 Убрать `RTS_OPTIMIZED_BORDER`-эвристику как **единственный** триггер и явно
задокументировать в логе, почему режим выключен.

**Проверка.**
* Гейты: `compileJava`, `validateMixins`, `runData`, `runServer`.
* `runClient` в мире с маленькой границей: в логе **нет** строк
  `RTS-optimised map detected` и `Prewarming RTS navmesh`.
* `/gamerule reignofnetherRtsMap true` в том же мире: строки появляются, преварм
  отрабатывает, `rtsPathfinding` включается.
* Ни одного нового файла чанков не создаётся за время преварма (сверить `region/` до/после).
* Проверить, что туман войны включается только при `rtsMap = true`.

**Не трогать:** сам механизм тумана, `FogOfWarServerEvents` в остальном,
`RTSMapInfoServerEvents:76` (только читает `prewarmedNavmesh`).

**Откат:** revert коммита; других точек отката нет, состояние только в рантайме.

---

### Этап 2 — поведенческие флаги конфига

**Зачем.** Конфиг (`config/ReignOfNetherCommonConfigs.java`, ~21 КБ) содержит **только
стоимости** — ни одного переключателя поведения. Пока нет флагов, этапы 3–6 невозможны.

☐ 2.1 Завести в `ReignOfNetherCommonConfigs` секцию `Behaviour` (или отдельный
`ReignOfNetherBehaviourConfigs`) с флагами:
`fogOfWar`, `spawnModification`, `timeOverride`, `griefing`, `hudOverlay`,
`titleScreenButtons`, `musicOverride`, `chunkTickets`, `borderManipulation`,
`commandNamespace`, `resourceIndexing`, `walkabilityCache`.
☐ 2.2 Каждый флаг — `BooleanValue`, дефолт `false`, кроме явно ванильных.
☐ 2.3 Флаги, меняющие только поведение, а не данные, — с `Config` без `worldRestart`;
`chunkTickets` и `borderManipulation` — с пометкой о том, что нужно перезапустить мир.
☐ 2.4 Кэш значений, чтобы не читать конфиг в tick.

**Проверка:** все флаги видны в сгенерированном `reignofnether-common-*.toml` и в
`ReignOfNetherConfigScreen`; выключенный флаг реально отключает поведение (проверяется на
примере `titleScreenButtons` — главное меню должно быть ванильным).

**Не трогать:** блок `UnitCosts` / `BuildingCosts` / `ResearchCosts` / `AbilityCosts`.

---

### Этап 3 — ленивое создание `SavedData`

**Зачем.** Мод пишет 8 файлов в **любой** мир, где установлен, даже без RTS-игроков.

☐ 3.1 Найти/создать единую точку ленивого доступа. Файлы: `player/RTSPlayerSaveData.java`,
`building/BuildingSaveData.java`, `building/custombuilding/CustomBuildingSaveData.java`,
`resources/ResourcesSaveData.java`, `unit/TargetResourcesSaveData.java`,
`research/ResearchSaveData.java`, `unit/HeroUnitSaveData.java`, `scenario/ScenarioRoleSaveData.java`,
`startpos/StartPosSaveData.java`.
☐ 3.2 Каждый `getInstance(level)` → создавать запись только если её реально положили.
Для этого `SavedData` не должен писаться в диск при пустом содержимом — проверить, когда
`DataStorage` вызывает `save()`.
☐ 3.3 Проверить, что пустой мир после старта/остановки сервера не содержит файлов мода.

**Проверка:** гейты; создать новый мир, запустить `runServer`, остановить, сравнить
`run/world/data/` до и после — папка должна остаться без файлов мода.

**Риск:** `DataStorage.save()` может писать все зарегистрированные `SavedData` разом. Если
так — откат и другая стратегия (удалять пустые файлы после сохранения мира).

---

### Этап 4 — тикеты чанков

☐ 4.1 Вернуть снятие билета в `UnitServerEvents#onEntityLeave` (закомментировано ещё в
апстриме 1.20.1, см. `WORKLOG.md` §2) — через `ChunkTicketUtil`.
☐ 4.2 Убрать тикеты при удалении здания: `BuildingPlacement#forceChunk(false)` уже есть в
`destroy` (строка 684) — проверить, что вызывается во всех путях сноса.
☐ 4.3 `WraithSnowBlockEntity:80-98` — `addRegionTicket(TicketType.FORCED, pos, 1, pos)`
даёт радиус 12; заменить на радиус 0.
☐ 4.4 Под флаг `chunkTickets`: когда выключен — не добавлять билеты вовсе.

**Проверка:** удалить юнита → его чанк выгружается; удалить здание → то же. В логе нет
`Can't keep up!` после массового удаления юнитов.

---

### Этап 5 — разделение миксинов по флагам

**Зачем:** 49 серверных миксинов применяются всегда. Часть из них обязательна для RTS,
часть — поведение (туман войны, спавн-блокировка, грифеж).

☐ 5.1 Разделить список `mixins` в `src/main/resources/reignofnether.mixins.json` на
`reignofnether-core.mixins.json` (обязательные) и `reignofnether-optional.mixins.json`
(выключаемые флагом).
☐ 5.2 Определить «ядро». По смыслу обязательны:
`UnitInventoryMobMixin`, `PathNavigationMixin`, `goals/*`, аксессоры
(`EntitySelectorAccessor`, `ObjectiveCriteriaAccessor`, `StructureBlockEntityAccessor`,
`LivingEntityAccessor`, `BlockColorsAccessor`, `SplashRendererAccessor`),
`MixLevelChunkMixin`/`LevelChunkMixin`/`MobMixin`/`LivingEntityMixin`/`EntityMixin`
(базовые хуки).
☐ 5.3 Под вопросом (требуют проверки в игре): `PlayerMixin`,
`ServerGamePacketListenerImplMixin`, `ExecuteCommandMixin`, `ServerLevelMixin`,
`DataCommandsMixin`, `BaseSpawnerMixin`.
☐ 5.4 Остальное — в опциональные: `fogofwar/*` (15), мобы, клиентские
(`TitleScreenMixin`, `MusicManagerMixin`, `MixinMinecraft`, `OrthoViewMixin`).

**Проверка:** `validateMixins` + `runServer` в мире с выключенными флагами + `runClient`
с открытым миром. Гейт `validateMixins` ловит отсутствующие цели, но **не** проверяет
семантику — нужен ручной прогон.

**Откат:** revert; если опциональные миксины ломают что-то неожиданное, этот этап откатывается
целиком и вместо него остаются только флаги в поведении.

---

### Этап 6 — клиентские миксины под флаги

☐ 6.1 `TitleScreenMixin` (кнопки lilypad/discord на главном меню) — под
`titleScreenButtons`, дефолт `false`.
☐ 6.2 `MusicManagerMixin` — под `musicOverride`.
☐ 6.3 `MixinMinecraft`, `OrthoViewMixin`, `ClientLevelMixin`, `ClientPacketMixin` — под
`hudOverlay` / `orthoviewEnabled`.
☐ 6.4 `fogofwar/*` клиентские — под `fogOfWar`.

**Проверка:** главное меню ванильное; в мире без RTS HUD пустой; включение флагов
возвращает поведение.

---

### Этап 7 — переименование и дефолты геймрулов

☐ 7.1 Неванильные геймрулы переименовать в namespace мода (или в `rtsapi:`, если
`_generic` не переименовать): `neutralAggro` → `reignofnetherNeutralAggro`,
`maxPopulation`, `groundYLevel`, `flyingMaxYLevel`, `beaconWinMinutes`,
`slantedBuilding`, `allowedHeroes`, `lockAlliances`, `scenarioMode`, `coopMode`,
`animalSpawnYDiff`, `randomItemDrops`, `buildingsOutsideBorder`, `pathfindingThreads`,
`pathfindingChunkBuildsPerTick`.
⚠️ Переименование ломает существующие миры и конфиги — нужен миграционный путь в
`GameruleServerEvents` либо решение «ломать».
☐ 7.2 Пересмотреть дефолты на ванильные: `doNetherConversion` → `false`,
`buildingsOutsideBorder` → `false`, `neutralAggro` → `false`, `allowBeacons` → `false`,
`doPlayerGriefing` → `false`, `doLogFalling` → решить отдельно.
☐ 7.3 Обновить `GameruleAction`, синхронизацию, клиентское зеркало, локализацию.

**Проверка:** старый мир с прописанным геймрулом открывается и не падает.

---

### Этап 8 — финальная проверка чистоты

☐ 8.1 Чистый vanilla-профиль: новый мир, `runServer`, 5 минут, остановка.
☐ 8.2 `diff` папки мира до/после: файлов мода быть не должно.
☐ 8.3 Проверить отсутствие тикетов, изменений границы/времени/спавна.
☐ 8.4 Прогон с `1.21.1-clean`-конфигом и RTS-картой: RTS по-прежнему работает полностью.

---

## 4. Реестр решений (обязательно к заполнению по ходу)

| Вопрос | Решение | Где зафиксировано |
|---|---|---|
| Удалять RTS-функциональность или отключать? | Отключать флагами, функциональность не трогаем | §4 «чего нельзя делать» |
| Эвристика `border <= 1280` — оставлять? | Оставить как **дополнительное** условие, но не как единственный триггер; основной триггер — явный геймрул | этап 1 |
| Сценарий (scenario) — это «чистый» мир? | Да, при `scenarioMode = false` мод ничего не делает; но `scenarioMode = true` в чужом мире — по явному решению админа | §1.7 |
| Как называть новые геймрулы? | Префикс `reignofnether` | этап 7 |
| Переименование геймрулов ломает миры — делать? | Требует миграционного пути; отложено | этап 7, ⚠️ |
| Страница 1 vs `_generic` неймспейс | Решить перед этапом 7 | — |

## 5. Что было сделано

| Этап | Статус |
|---|---|
| **УМ** Инвентаризация интрузивности | сделано — §1 |
| **УМ** План работ | сделано — §3 |
| **УМ** Гайд по фракциям | `HOWTO_FACTION.md` |
| **УМ** Коммит `8323d802` (рендер + зависание) | на `port-1.21.1-plus-1.5.0` |
| **Э1** Убрать автоопределение RTS-карты | **сделано** — `737a53e2` |
| Э2 … Э8 | не начаты |

### Э1 — что именно сделано и что осталось

Сделано в `737a53e2`:

* новый геймрул `reignofnetherRtsMap` (дефолт `false`) — точка 1.1;
* `isRtsOptimisedMap` проверяет геймрул **первым** — это автоматически сделало
  опциональными всех потребителей без правки каждого — точки 1.2 и 1.3;
* `onServerStarted` разделен на три явных ветки с разными сообщениями в лог;
* преварм ограничен `PREWARM_MAX_CHUNKS = 4096` — пункт 1.4 (половина);
* клиентский `isOnRtsOptimisedMap` удалён, локальный отказ `/rts-fog enable` убран в пользу
  серверного решения.

**Не сделано:**

* пункт 1.4 наполовину — преварм по-прежнему **генерирует** terrain для каждого чанка, который
  трогает. Это осознанно: преварм по определению требует загрузки чанков, а отказ от него означал
  бы отказ от оптимизации целиком (сетка и так строится лениво через `getOrBuild`). Защита теперь
  состоит из явного opt-in плюс потолка. Если понадобится абсолютно — сделать преварм по
  `getChunkNow`, но тогда он не будет ничего прогревать.
* пункт 1.5 `FogChunkSnapshot:75` — тронут только гейтом (`isRtsOptimisedMap`),
  сам `getChunk(cx, cz, true)` остался: при opt-in это осознанно, а без него код не достигается.
* пункт 1.6 — ключ локализации добавлен в `en_us.json` и `ru_ru.json`; остальные 20 языков не
  трогались (обычная практика — ключ отсутствует, мод падает на fallback).
* синхронизация геймрула на клиент **не делалась**: `GameruleAction` её не поддерживает и для
  половины существующих геймрулов её тоже нет. Клиенту она не нужна — единственный потребитель
  был локальный отказ команды, который удалён.

**Проверено:** `compileJava`, `validateMixins`, `runData`, `runServer` — зелёные. В логе
`Not treating this world as an RTS map (reignofnetherRtsMap is off)`, строк преварма нет, новых
region-файлов в дев-мире не появилось.

**Не проверено:** ветка opt-in. Граница дев-мира ванильная (60 млн блоков), поэтому условие
`border <= 1280` там никогда не выполняется и включающий путь прогоном не покрыт. Нужен мир с
маленькой границей: `/gamerule reignofnetherRtsMap true`, перезапуск, ожидание строк
`RTS map mode enabled by gamerule` и `Prewarming RTS navmesh`.

---

## 6. Чего делать нельзя

* **Удалять** RTS-функциональность — она работает и нужна; только отключать по флагу.
* **Ломать** `GameRuleRegistrar`: регистрация геймрула безопасна и не считается интрузией.
* **Опираться** на `isRtsOptimisedMap` (размер границы) как на признак — это эвристика,
  которая срабатывает на чужих картах.
* Трогать `ChunkTicketUtil` в сторону уменьшения ниже радиуса 0 — это уже минимум.

---

## 7. Связанные документы

* `HOWTO_FACTION.md` — как создать свою фракцию (строения, юниты, исследования).
* `WORKLOG.md` — что сделано в этой ветке и на чём остановились.
* `../../AGENT_HANDOFF.md`, `../../AGENT_MEMO.md`, `../../PORT_STATUS.md` — состояние порта
  на 1.21.1. Эти файлы лежат в корне `ReignOfNether/`, который git-репозиторием **не**
  является, поэтому в коммит они не попадают.