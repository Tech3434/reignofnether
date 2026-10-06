# STATUS — единый статус-документ

**Дата:** 2026-10-06  
**Ветка:** `wip/stage-d-deletions`  
**HEAD:** `a2d1123e`  
**compileJava:** **0 ошибок** ✅  
**validateMixins:** ✅ (49 injection point'ов, 31 mixin-класс, все резолвятся)  
**runData:** ✅ (BUILD SUCCESSFUL, 33s)  
**runServer:** ✅ (Done (1.141s)!, 0 mixin apply failed)  
**runClient:** ⏳ (нужен интерактивный прогон)

---

## 1. Что сделано (этапы B–F)

### 1.1 Удалено

**Контент фракций:** ~385 файлов удалено на этапе D.

Включая:
- Юниты (все фракции, кроме `VillagerUnit`, `VindicatorUnit`), здания (все, кроме `TownCentre`, `Barracks`), способности (93), исследования (51)
- Фракции (`faction/**` — весь каталог)
- Сценарный режим, blood-moon, выживание (survival/**)
- Стартовые блоки (16 `rts_start_block_*`), wall-skulls (4), `horizontal_portal`, `RTSStartBlock`
- `ItemShopAddon`, `StockedShopItem`, `UnitItemGoal` (магазин продавал только удалённые `UnitItem`)

**Чары (E.1/E.2/E.3):** 7 чар датапака + registrar + call sites удалены:
`vigor`, `breaching`, `fortifying`, `gust`, `longshot`, `maiming`, `zeal`

**Миксины:** ~26 серверных миксинов удалены (было ~49, осталось 23 в json, 34 mixin-класса на диске).

**Способности и цели:** `BeaconAbility`, `EnchantAbility`, `EquipAbility`, `TradeAction`, `UsePortalGoal` и 11+ целей.

**Экономика:** `ResourceIndex` удалён (заменён локальным поиском в `GatherResourcesGoal`). `ResourceCosts` — 154 записи конфига удалены, оставлены 4 стоимости шаблонов.

### 1.2 Восстановлено / создано

**`building/buildings/placements/ProductionPlacement.java`** — восстановлен из `5079004e` (план предупреждает: он нужен ядру).

**`resources/ResourceSource.java`, `resources/ResourceSources.java`** — оставлены как фреймворк (решение 1). Причина: цикл `WorkerUnit` → `GatherResourcesGoal` → `ReturnResourcesGoal` → здание читает таблицу.

**`player/Cheats.java`, `player/CheatsClientboundPacket.java`** — новое хранилище читов (вместо половины research). Переключение через `PlayerAction.SET_CHEAT`, op-gated (право 4).

### 1.3 Переписано

**`resources/ResourceCosts.java`:** 4 стоимости шаблонов, HUD-хелперы, `DEFAULT_MAX_POPULATION = 1`.

**`unit/interfaces/Unit.java`:** убраны `getFaction`, фракционные хилы, мост, подбор еды, fortifying, Scorching Gaze, `hasScenarioNpcOwner`, `isScout`, `getSpeedModifier(Unit)`, `hasAnyEnchants`, чарм-иконки, `canUsePortal()`, `getUsePortalGoal()`.

**`gamemode/**` — остался один режим (`CLASSIC` / `NONE`).

**`player/**` — `RTSPlayer` потерял фракцию, `PlayerServerEvents` потерял `startRTSScenario`, `beaconVictory`, `updateMarketRates` и др. `startRTS` больше не принимает фракцию.

**`sandbox/**` — больше не режим, проверяет право оператора 2.

**`building/BuildingCommand.java`** — удалён `ON_SCENARIO_START`.

**`unit/pathfinding/**`:** `canClimb` жёстко `false`, `MobilityClass.of` потерял проверки ботинок.

**`unit/goals/GatherResourcesGoal`:** `ResourceIndex` удалён, добавлен `findClosest` (локальный обход кольцами).

**`MatchStatRow`** потерял поле `faction`.

**Крупные файлы, чистые от ошибок:** `hud/HudClientEvents.java` (96→0), `BuildingServerEvents.java` (61→0), `UnitClientEvents.java` (55→0), `CommonModEvents.java` (47→0), `UnitServerEvents.java` (37→0), `UnitInventoryMobMixin.java` (33→0), `BuildingPlacement.java` (29→0), `ClientModEvents.java` (29→0).

### 1.4 Решения (закрыты)

1. **`ResourceSource`/`ResourceSources` оставлены** (D.14 просил удалить). Цикл `WorkerUnit` → `GatherResourcesGoal` читает таблицу и ради блока, и ради стоимости предмета.
2. **`ResourceIndex` удалён по-настоящему.** Вместо него `findClosest` в `GatherResourcesGoal`.
3. **`ItemShopAddon`/`StockedShopItem` удалены**, хотя аддоны помечены «не трогать»: магазин продавал только `UnitItem`.
4. **Петля поедания еды убрана из `Unit`** — держалась на `items/unititems/EdibleFoodItem`.
5. **Читы вынесены из research.** Переключение через `PlayerAction.SET_CHEAT`, op-gated.
6. **Sandbox больше не режим.** Проверяет право оператора 2.
7. **`RtsPathfinder.canClimb` жёстко `false`**, спецслучай паука убран.
8. **`MobilityClass.of`** потерял проверки ботинок и страйдера.
9. **`unit/goals/UnitItemGoal` удалён.** Опирается на `UnitItem`/`ItemUtil`.
10. **`unit/goals/UsePortalGoal` удалён** вместе с `canUsePortal()`/`getUsePortalGoal()`.
11. **`MatchStatRow` потерял поле `faction`**.
12. **`PlayerServerEvents`:** удалены `startRTSScenario`, `beaconVictory`, `getBeaconWinTime`, `updateMarketRates`, чит `elitetaurenchieftain`. Победа: единственный оставшийся игрок.
13. **Лимит армии по умолчанию — 1 юнит** (`DEFAULT_MAX_POPULATION = 1`). Увеличение даёт ратуша.
14. **Четыре шаблона сохранены:** `VillagerUnit` (строитель), `VindicatorUnit` (боевой), `TownCentre` (ратуша), `Barracks` (производство).

---

## 2. Текущее состояние сборки

| Метрика | Значение |
| --- | --- |
| **compileJava** | **0 ошибок** ✅ |
| **validateMixins** | ✅ 49 injection point'ов, 31 mixin-класс, все резолвятся |
| **runData** | ✅ BUILD SUCCESSFUL in 33s (генерация прошла, `Missing:` нет) |
| **runServer** | ✅ `Done (1.392s)!`, 0 mixin apply failed, 0 ClassCastException/NoSuchMethod, RTS pathfinder pool стартанул |
| **runClient** | ещё не запускался (нужен интерактивный прогон) |

**Как собирать (`gradlew.bat` не работает — `gradle-wrapper.properties` без
`distributionUrl`, а `gradle-wrapper.jar` рабочий):**

```powershell
& "C:\Program Files\Java\jdk-21\bin\java.exe" "-Dorg.gradle.appname=gradlew" `
  -jar gradle\wrapper\gradle-wrapper.jar <tasks> --console=plain --no-daemon
```

**Что реально показывает `runServer` (важно для интерпретации):** конфигурация
мейн-мixin'ов выполняется **до** `Done!` — значит, `Done!` подтверждает, что все
`@Inject` в main-миксинах применились. Но `@Override`-модель (`ClientPacketListenerMixin`,
16 методов) и `IClientPacketListener` резолвятся только при входе в мир — это
покрывается **`runClient`**, а не `runServer`.

---

## 3. Что осталось (этапы G+)

### 3.1 Гейты

1. ~~**`runData`**~~ — ✅ **пройден** (BUILD SUCCESSFUL in 33s).
2. ~~**`runServer`**~~ — ✅ **пройден** (`Done (1.141s)!`, 0 `mixin apply failed`).
3. **`runClient`** — **остался**. Титольный экран + вход в мир. Это единственный гейт,
   который проверяет `@Override`-модель (`ClientPacketListenerMixin`, 16 методов) —
   `runServer` её не трогает.
4. **Ручной прогон** — геометрия на экране, зависание при выходе (только в игре).

### 3.2 Документация (~2–3 часа)

1. **`HOWTO_FACTION.md`** — убрать ссылки на удалённые фракции, research, economy.
2. **`README.md`** — описать текущее состояние.
3. **`FEATURES.md`** — актуализировать каталог.
4. **`INTRUSION_AUDIT.md`** — снять пункты, которые были удалены.

### 3.3 Опционально (по решению владельца)

1. **`HeroUnit`** — контракт §1, но все Collaborator-ы удалены. Либо урезать контракт, либо записать исключение.
2. **6 атрибутов без потребителей** (решение 12.5): `critical_hit_chance`, `explosive_hit_chance`, `lifesteal`, `mana_on_hit`, `scale`, `building_damage_bonus` — удалить.
3. **5 эффектов из удалённого контента** (решение 12.4): `zombie_infected`, `slime_infected`, `frost_damage`, `warm`, `villager_inspiration` — удалить.
4. **`EntityRegistrar.getEntityType`** — сопоставлять `ProductionItem` с `EntityType<?>` напрямую (решение 12.6).

---

## 4. Ловушки (не повторять)

1. **`git show <ref>:<path> > <file>`** под PowerShell 5.1 пишет UTF-16LE → javac: thousands of `unmappable character (0xFF)`. Только `cmd /c "git show ... > file"`.
2. **`delline.ps1`** (первая версия) заменял удаляемые строки маркером `<<<REMOVED n>>>` → «illegal start of type». Исправлен.
3. **`gradlew.bat`** передаёт java одновременно `-classpath ""` и `-jar` → java падает с
   `-classpath requires class path specification`. Использовать команду из §2.
4. **`validateMixins` не ловит часть mixin-ошибок** — они всплывают только в рантайме
   (`Invalid LVT row`, `Segmentation fault` в `org.spongepowered.asm.util.LVTWriter`,
   `InvalidInjectionException`). Единственный надёжный гейт — `runServer` / `runClient`.

---

## 5. Ссылки на детальные планы

| Файл | О чём |
|---|---|
| `PLAN_RTS_ONLY.md` | исходный план (этапы A–F, все решения, 666 строк) |
| `STAGE_D_PROGRESS.md` | прогресс этапа D (детали удалений и переписывания, 176 строк) |
| `FEATURES.md` | каталог функций (407 строк) |
| `INTRUSION_AUDIT.md` | аудит интрузивности (439 строк) |
| `CLEAN_FORK.md` | план деинтрузивности (464 строки) |
| `WORKLOG.md` | порт на 1.21.1 (438 строк, исправления, диагностика) |

---

**Документ актуален на HEAD `a2d1123e`.** compileJava зелёный, runData и runServer
пройдены. Следующий гейт — `runClient` (титольный экран + вход в мир).

