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

## 3. Этапы

| Этап | Содержание | Признак готовности |
|---|---|---|
| **0** | Документация: инвентаризация (этот файл), гайд по фракциям, worklog | сделано |
| **1** | Убрать автоопределение RTS-карты по границе мира; преварм только по явному флагу и с лимитом радиуса | чужой мир с малой границей не генерируется при старте |
| **2** | Поведенческие флаги в конфиге: `fogOfWar`, `spawnModification`, `timeSkip`, `griefing`, `hudOverlay`, `titleScreenButtons`, `chunkTickets`, `borderManipulation`, `commandNamespace` | всё выключается из конфига, дефолты — ванильные |
| **3** | Ленивое создание `SavedData` | пустой мир не содержит файлов мода |
| **4** | Снятие тикетов при удалении юнита/здания | удаление юнита не оставляет загруженный чанк |
| **5** | Разделение миксинов на обязательные/опциональные по флагам | `fogofwar/*` не применяются без флага |
| **6** | Клиентские миксины под флагами (`TitleScreenMixin`, `MusicManagerMixin`, `MixinMinecraft`) | без флага главное меню ванильное |
| **7** | Переименование неванильных геймрулов в namespaced-имена; пересмотр дефолтов | нет конфликтов имён с другими модами |
| **8** | Тест на чистом vanilla-профиле: `runServer` без RTS-игроков | `diff` мира до/после пуст |

Каждый этап — отдельный коммит, гейты (`compileJava`, `validateMixins`, `runData`, `runServer`)
должны быть зелёными.

---

## 4. Чего делать нельзя

* **Удалять** RTS-функциональность — она работает и нужна; только отключать по флагу.
* **Ломать** `GameRuleRegistrar`: регистрация геймрула безопасна и не считается интрузией.
* **Опираться** на `isRtsOptimisedMap` (размер границы) как на признак — это эвристика,
  которая срабатывает на чужих картах.
* Трогать `ChunkTicketUtil` в сторону уменьшения ниже радиуса 0 — это уже минимум.

---

## 5. Связанные документы

* `HOWTO_FACTION.md` — как создать свою фракцию (строения, юниты, исследования).
* `WORKLOG.md` — что сделано в этой ветке и на чём остановились.
* `../../AGENT_HANDOFF.md`, `../../AGENT_MEMO.md`, `../../PORT_STATUS.md` — состояние порта
  на 1.21.1. Эти файлы лежат в корне `ReignOfNether/`, который git-репозиторием **не**
  является, поэтому в коммит они не попадают.