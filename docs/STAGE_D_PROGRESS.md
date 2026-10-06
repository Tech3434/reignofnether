# Этап D — текущий прогресс

Ветка: `wip/stage-d-deletions`. База для справки по исходному коду: `5079004e`.

## Состояние сборки

| Метрика | Значение |
| --- | --- |
| Ошибок `compileJava` на старте сессии | 1235 |
| Ошибок `compileJava` сейчас | **0** ✅ |
| Гейты `validateMixins` | ✅ (49 injection point'ов, 31 mixin-класс) |
| Гейты `runData` / `runServer` / `runClient` | ещё не запускались |

## Как собирать

`./gradlew.bat` на этой машине не работает: враппер передаёт java одновременно
`-classpath ""` и `-jar`, и java падает с *"-classpath requires class path specification"*.

```
& "C:\Program Files\Java\jdk-21\bin\java.exe" -Dorg.gradle.appname=gradlew -jar gradle\wrapper\gradle-wrapper.jar compileJava --offline --console=plain
```

Логи компиляции: `D:\_MC\CODING_AREA\ReignOfNether\errs2.txt`.
Нарезка ошибок по файлу: `& D:\_MC\CODING_AREA\ReignOfNether\errs.ps1 -Filter "<путь-фрагмент>"`.
Массовое удаление строк: `& D:\_MC\CODING_AREA\ReignOfNether\delline.ps1 -File "<путь>" -Pattern "<регэксп>"`.

## Сделано

### Удалено (37 файлов, весь контент)

`ability/{BeaconAbility,EnchantAbility,EquipAbility,TradeAction}`,
`building/production/{GraveyardUnitProductionItem,HeroProductionItem,ReviveHeroProductionItem,StartPortalTransformButton}`,
`building/addon/ItemShopAddon`,
`blocks/{Bogged,Drowned,Husk,Stray}WallSkullBlock`, `blocks/HorizontalPortalBlock`, `blocks/RTSStartBlock`,
`hud/buttons/{StartButtons,UnitItemShopButton}`,
`items/{ItemShopMenu,StockedShopItem,ItemShopClientboundPacket,ThrowableTnt}`,
`particles/BigEnchantParticle`,
`research/{ResearchClient,ResearchClientboundPacket,ResearchServerboundPacket,ResearchServerEvents}`,
`unit/controls/{SlimeJumpMoveControl,SlimeRollMoveControl}`,
`unit/goals/{CastSummonVexesGoal,CreeperAttackUnitGoal,MeleeAttackSlimeUnitGoal,MoveToTargetBlockSlimeGoal,PossessSpellGoal,ThrowPotionGoal,MountGoal,SonicBoomGoal,StriderFloatGoal,UnitCrossbowAttackGoal,FlyingUsePortalGoal}`,
`unit/packets/BeaconSyncClientboundPacket`,
`mixin/{ItemInHandLayerMixin,ItemEntityMixin}` + записи в `reignofnether.mixins.json`.

Все 16 `rts_start_block_*`, 4 wall-skull блока и `horizontal_portal` убраны из `BlockRegistrar`
(у `rts_structure_block` и skull-блоков не было иных применений).

### Восстановлено / создано

- `building/buildings/placements/ProductionPlacement.java` — восстановлен из `5079004e`
  (план предупреждает, что он лежит в `placements/` и на него ссылается ядро).
- `resources/ResourceSource.java`, `resources/ResourceSources.java` — восстановлены как фреймворк.
- `resources/ResourceCosts.java` — переписан: 4 стоимости шаблонов (villager, vindicator,
  town centre, barracks) с исходными дефолтами, HUD-хелперы форматирования,
  `DEFAULT_MAX_POPULATION = 1`.
- `player/Cheats.java`, `player/CheatsClientboundPacket.java` — новое хранилище читов
  (вместо половины research).

### Переписано

- `unit/interfaces/Unit.java` — убраны `getFaction`, фракционные хилы, мост, подбор еды,
  fortifying, Scorching Gaze, `hasScenarioNpcOwner`, `isScout`, `getSpeedModifier(Unit)`,
  `hasAnyEnchants`, чарм-иконки. Регенерация в ауре ночного источника стала безфракционной.
- `gamemode/{GameMode,ClientGameModeHelper,GameModeClientboundPacket,GameModeServerEvents}.java`
  — C.5, остался один режим.
- `player/{RTSPlayer,PlayerAction,PlayerClientboundPacket,PlayerClientEvents}.java` — потеряли
  фракцию, роль сценария, маяк, ставки рынка, дроп предметов.
- `player/PlayerServerboundPacket.java` — один `START_RTS` вместо свича по фракции (F.3),
  добавлен `setCheat(String)`.
- `sandbox/{SandboxClientEvents,SandboxServer}.java` — гейт по правам оператора (C.6),
  чит-кнопки через `Cheats`/`PlayerServerboundPacket`, удалены кнопки сценария и фракций.
- `building/BuildingCommand.java` — удалён `ON_SCENARIO_START` (плюс `CustomBuildingMenu`).
- `unit/pathfinding/{MobilityClass,RtsPathfinder}.java`, `unit/goals/GatherResourcesGoal.java`.
- `config/ConfigClientEvents.java`, `mixin/{LevelChunkMixin,IronGolemMixin,FrustumMixin}.java`,
  `blocks/TemporaryWalkableMagmaBlock.java`, `items/CreativeModeTabsRegistrar.java`,
  `registrars/{PacketHandler,BlockRegistrar}.java`, `registrars/ItemRegistrar.java`.

## Принятые решения — их надо закрыть в документации

1. **`ResourceSource`/`ResourceSources` оставлены как фреймворк** (D.14 просил удалить).
   Причина: цикл, который план сохраняет — `WorkerUnit` → `GatherResourcesGoal` →
   `ReturnResourcesGoal` → здание с `canAcceptResources` — читает таблицу и ради блока,
   который рубится, и ради стоимости предмета, который несут домой. Без неё юнит не может
   нести ресурс вообще.
2. **`ResourceIndex` удалён по-настоящему.** Он существовал только ради экономики и кормил
   `GatherResourcesGoal`. Вместо него в `GatherResourcesGoal` добавлен локальный поиск
   `findClosest` — обход кольцами от юнита с проверкой `ResourceSources.getBlockResourceName`.
   Вызовы идут только на кулдауне поиска и с эскалацией `range`, так что цена ограничена.
3. **`ItemShopAddon`/`StockedShopItem` удалены**, хотя аддоны в §1 помечены «не трогать»:
   магазин продавал только `UnitItem`, а слой предметов удалён по D.15.
4. **Петля поедания еды убрана из `Unit`** — держалась на `items/unititems/EdibleFoodItem`
   и `ItemUtil.isPreparedEdibleFood`. Юниты по-прежнему носят предметы из мира и умеют
   применять их как способности (`UnitItemGoal`), просто не едят.
5. **Читы вынесены из research.** Research удалён, но читы — операторский инструмент, который
   нужен `PlayerServerEvents.onPlayerChat`. Переключение идёт через `PlayerAction.SET_CHEAT`
   и проверяется по `opOnlyActions` (право 4) — заодно закрыта недостающая проверка прав.
6. **Sandbox больше не режим.** `SandboxClientEvents.isSandboxPlayer` и
   `SandboxServer.isSandboxPlayer`/`isAnyoneASandboxPlayer` проверяют право оператора 2.
7. **`RtsPathfinder.canClimb` жёстко `false`**, спецслучай паука в `footprintRadiusFor` убран.
   Сетка всё ещё умеет лазить по стенам, просто ни один юнит этим не пользуется.
8. **`MobilityClass.of`** потерял проверки ботинок и страйдера. Класс `FIRE_IMMUNE` остаётся,
   но автоматически никто в него не попадает.

## Что осталось (по объёму ошибок)

| Файл | Ошибок |
| --- | --- |
| `hud/HudClientEvents.java` | 96 |
| `building/BuildingServerEvents.java` | 61 |
| `unit/UnitClientEvents.java` | 55 |
| `CommonModEvents.java` | 47 |
| `unit/UnitServerEvents.java` | 37 |
| `mixin/UnitInventoryMobMixin.java` | 33 |
| `building/BuildingPlacement.java` | 29 |
| `ClientModEvents.java` | 29 |
| `unit/interfaces/HeroUnit.java` | 24 |
| `minimap/MinimapClientEvents.java` | 22 |
| `building/BuildingClientEvents.java` | 21 |
| `hud/PortraitRendererModifiers.java` | 21 |
| `items/ItemClientEvents.java` | 18 |
| `util/MiscUtil.java` | 17 |
| `building/BuildingUtils.java` | 15 |
| `registrars/ClientEventRegistrar.java` | 13 |
| `building/BuildingClientboundPacket.java` | 13 |
| `commands/CommandsServerEvents.java` | 13 |
| `orthoview/OrthoviewClientEvents.java` | 13 |
| `sandbox/SandboxActionButtons.java` | 12 |
| `unit/units/villagers/VillagerUnit.java` | 12 |
| `ability/BuildingAbility*Packet.java` | 22 (оба файла) |
| `time/TimeClientEvents.java` | 11 |
| остальные (~60 файлов по 1-10) | |

Закрыты полностью: `PlayerServerEvents`, `RTSPlayer`, `RTSPlayerSaveData`,
`PlayerClientboundPacket`, `PlayerServerboundPacket`, `PlayerClientEvents`,
`PlayerAction`, `GameMode*`, `SandboxServer`, `ItemRegistrar`, `GameRuleRegistrar`,
`BlockRegistrar`, `PacketHandler`, `GatherResourcesGoal`, `MobilityClass`, `NightUtils`,
`BuildingCommand`, `Building.java`, `ReignOfNether.java`.

Отдельными задачами:
- геймрил `scenarioMode` и его ripples (`GameRuleRegistrar`, `GameruleAction.SET_SCENARIO_MODE`,
  `GameruleClient`/`ClientboundPacket`/`ServerEvents`, `TimeServerEvents`, `PlayerServerEvents`);
- `HeroUnit` — контракт сохранён по §1, но все его Collaborator-ы (`HeroClientboundPacket`,
  `HeroServerEvents`, `HeroProductionItem`) удалены как контент; нужно либо урезать контракт,
  либо записать исключение;
- затем гейты `compileJava` → `validateMixins` → `runData` → `runServer`/`runClient`.

### Решения, принятые во второй половине сессии

14. **E.1/E.2/E.3: чары удалены целиком.** 7 JSON-файлов (vigor, breaching, fortifying, gust, longshot, maiming, zeal) удалены. `EnchantmentRegistrar` переписан: удалены 7 `mod()`-поставщиков + хелпер `mod()`, оставлены `vanilla()`, `tryVanilla()`, `bind()`, `holder()`. `UnitServerEvents`: удалён блок BREACHING из `onLivingHurt`. `VindicatorUnit`: удалены `getMaimingLevel()` и `doHurtTarget` override.

## Решения, принятые во второй половине сессии

9. **`unit/goals/UnitItemGoal` удалён.** Он целиком опирался на `UnitItem`/`ItemUtil` — то есть на
   удалённый слой предметов. Юниты по-прежнему носят предметы из мира (`UnitInventory` + 6 слотов
   через миксин), но «использовать носимый предмет как способность» уходит вместе с этим goal.
   Новая система привязки способностей к предметам — это уже дизайн, а не чистка.
10. **`unit/goals/UsePortalGoal` удалён вместе с `canUsePortal()`/`getUsePortalGoal()`** в
    `Unit` и в `VillagerUnit`/`VindicatorUnit`. Порталы — удалённый контент (D.22 удаляет
    `portal`, `HorizontalPortal`), и `neutral_transport_portal` вместе с ним.
11. **`MatchStatRow` потерял поле `faction`** — таблица итогов матча больше не делится по фракциям;
    в `encode`/`decode` соответственно убраны `writeEnum`/`readEnum`.
12. **`PlayerServerEvents`**: удалены `startRTSScenario`, `publishScenarioMap`,
    `beaconVictory`, `getBeaconWinTime`, `updateMarketRates` (обе перегрузки), чит
    `elitetaurenchieftain`, победа по маяку. `startRTS(int, Vec3)` и `startRTSBot(String, Vec3)`
    больше не принимают фракцию; стартовый юнит задаётся константой
    `PlayerServerEvents.STARTING_WORKER_TYPE`, столица для «readied start» — `Buildings.TOWN_CENTRE`.
    Победа теперь засчитывается единственным оставшимся игроком (или всеми его союзниками) —
    маяк как объект условия победы удалён вместе с контентом.
13. **Чит-сообщения в `onPlayerChat`** теперь идут через `Cheats` + `Cheats.syncCheats`, ключ
    сообщения выбирается по результату (`enabled_cheat`/`disabled_cheat`), раньше ветвились вручную.


## Ловушки инструментов — не повторять

- `git show <ref>:<path> > <file>` под PowerShell 5.1 пишет **UTF-16LE**, и javac затем
  выдаёт тысячи фиктивных `unmappable character (0xFF)` в одном файле. Только
  `cmd /c "git show ... > file"`.
- Первую версию `delline.ps1` он *заменял* удаляемые строки маркером `<<<REMOVED n>>>`
  вместо их пропуска — из-за этого четыре файла падали с «illegal start of type».
  Скрипт исправлен, маркеры вычищены. Если массовое редактирование даёт синтаксические
  ошибки на незнакомых строках — сначала искать `<<<REMOVED`.
