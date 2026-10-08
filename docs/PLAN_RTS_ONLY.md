# План: оставить только РТС-каркас, контент вынести в документацию

Цель ветки: мод становится **чистым каркасом РТС-системы**. Всё, что является контентом
готовых фракций — юниты, здания, способности, исследования, предметы, ресурсы, волны —
удаляется из кода и сохраняется как документация. Остаётся механика: интерфейсы и цели,
производство, здания как система, атрибуты, эффекты, чары, камера, HUD, команды, читы.
Позже владелец ветки пишет на этом каркасе свою фракцию.

Обоснование: `FEATURES.md` (каталог), `INTRUSION_AUDIT.md` (инвентаризация вреда),
`HOWTO_FACTION.md` (как добавлять контент). Состояние кода при составлении: `5079004e`.

## Принятые решения

1. **Удаляются «дефолтные фракции» жителей / нежити / пиглинов** вместе со всем
   содержащимся контентом. `Faction` удаляется полностью, абстракцией не заменяется.
2. **Удалённый контент сохраняется как документация** — переносится в `docs/reference/`, а не
   остаётся в дереве. Код и так сохраняется в git поимённо.
3. **Каркас РТС сохраняется целиком**: интерфейсы юнитов и зданий, цели, производство,
   гарнизоны, ауры, атрибуты, эффекты, камера, HUD, команды, читы, кастомные строения.
   Чары уходят (решение 9).
4. **Режимы, кроме `CLASSIC`, удаляются.**
5. **Интрузивность убирается.**
6. **Туман войны удаляется целиком.** Совместимой переработки владелец не заказывал, а текущая
   конструкция совместимости не имеет: она сокрывает данные, а не рисует, поэтому конфликтует с
   любым модом рендера и протекает мимо фильтра пакетов. Разбор — `INTRUSION_AUDIT.md` §4.2.
7. **Остаются как рабочий шаблон:** `VillagerUnit` (строитель), `TownCentre` (ратуша),
   `Barracks` (производственное здание), `VindicatorUnit` (боевой юнит).
8. **Остаётся палитра спецблоков** — под будущие способности новых юнитов.
9. **Остаётся инвентарь юнита** (`UnitInventory` и его миксин) — чтобы юниты носили предметы
   из мира.
10. **Чары и способности — в документацию и удаляются** (решение владельца).
11. **Побочные эффекты размещения здания остаются все** (B.8), но блок лесов становится
    настраиваемым: универсальный блок или трава сверху и земля ниже — см. §12.1.
12. **Вход в РТС-режим — горячей клавишей (как сейчас) и командой**; пропуск = право
    оператора, без пропуска ничего не происходит. Выход как сейчас. Конец сессии — потеря
    последнего здания, после чего юниты нейтральны и игрок может начать заново. Пока зданий
    нет, поражение не фиксируется. Плюс команда `force-loose` — см. §12.2.
13. **Лимит армии по умолчанию 1**, прирост даёт ратуша фракции; ресурсы остаются, конфиг
    стоимостей удаляется — см. §12.3.

## ⚠ Последствие, о котором надо знать до начала

После удаления контента в моде останется **четыре юнита-шаблона и два здания** (решение 7).
Мод почти перестанет быть играбельным: гейты `compileJava`, `validateMixins`, `runData`,
`runServer`, `runClient` будут зелёными, но игровой функционал сведутся к двум зданиям.

Отсюда порядок этапов: сначала убрать интрузивность (этапы A–C), пока контент ещё есть и
этапы можно проверить в игре, и только потом удалять контент (этап D).

Практическая рекомендация: держать промежуточную точку. Пока каркас пустой, ветка почти
непригодна для игры — если нужен играбельный вариант, его стоит держать на отдельной ветке от
этапа C.

---

## 1. Что остаётся — каркас (не трогать ни в одном этапе)

| Подсистема | Файлы | Зачем |
|---|---|---|
| Интерфейсы юнитов | `unit/interfaces/{Unit,AttackerUnit,WorkerUnit,RangedAttackerUnit,HeroUnit,ConvertableUnit,ArmSwingingUnit,KeyframeAnimated}` | контракт юнита |
| Статика юнита | `Unit.createDefaultAttributes()`, `Unit.tick()`, `Unit.createCooldownMap()` | база атрибутов и тик |
| Цели и ИИ | `unit/goals/**` (34 файла) | вся навигация и поведение |
| Пути | `unit/pathfinding/**` (13 файлов) | гейт тройной, ваниллу не вредит |
| События юнитов | `unit/UnitServerEvents`, `UnitClientEvents`, `NonUnitServerEvents` | спавн, команды, управление ванильными мобами |
| Инвентарь юнита | `items/UnitInventory` | 6 слотов, добавляется миксином каждому мобу |
| Базовые классы способностей | `ability/{Ability,HeroAbility,Abilities}`, `AbilityButton`, `ability/UnitAction` | контракт способности |
| Здание как система | `building/{Building,BuildingPlacement,BuildingUtils,BuildingValidators,BuildingBlockData,BuildingPlaceButton}`, `building/buildings/placements/**` | ядро зданий |
| Производство | `building/production/{ProductionItem,ActiveProduction,ProductionItemList,ProdDupeRule,ProductionBuilding,ProductionPlacement}` | очередь, стоимость, `onComplete` |
| Аддоны | `building/addon/**` (5 контрактов) | гарнизон, аура, конвертация, магазин |
| Данные размещения | `building/data/{DataType,DataStorage}` | персистентность аддонов |
| Кастомные строения | `building/custombuilding/**` + `RTSStructureBlockEntity` | инструмент автора, полностью самодостаточен |
| Атрибуты | `registrars/AttributeRegistrar` | 20 атрибутов, работают на любой сущности |
| Эффекты | `registrars/MobEffectRegistrar` | 28 эффектов |
| Частицы | `registrars/ParticleRegistrar` | 6 типов |
| ~~Чары~~ | ~~`registrars/EnchantmentRegistrar`~~ | **удаляются по решению владельца**, см. §7 E.1 |
| Реестры | `api/ReignOfNetherRegistries` (`BUILDING`, `PRODUCTION_ITEM`, `DATA_TYPE`) | точка регистрации |
| Камера и UI | `orthoview/**`, `guiscreen/TopdownGui*`, `minimap/**`, `hud/**` | рабочий инструмент ГМа |
| Команды | `commands/**` | `/rtsapi`, `execute rts-related`, аргумент-селекторы |
| Читы | `player/PlayerServerEvents.onPlayerChat`, список читов | операторские переключатели |
| Союзы | `alliance/**` | командные отношения |
| Игроки и матч | `player/{RTSPlayer,RTSPlayerSaveData,PlayerServerEvents,PlayerClientEvents}` | вход, выход, счёт |
| Песочница-инструмент | `sandbox/{SandboxServer,SandboxClientEvents,SandboxActionButtons}` | спавн юнитов, якоря, переключатель отношений |
| Утилиты | `util/{MyRenderer,LevelRenderCompat,ArrayUtil,SavedDataCompat,ChunkTicketUtil,MiscUtil}` | отрисовка и мелкие помощники |
| Прочее | `keybinds/**`, `gamerules/**`, `time/TimeUtils`, `api/**`, `config/**` | инфраструктура |

## 2. Что уходит в документацию

| Подсистема | Файлов | Куда |
|---|---|---|
| Юниты | `unit/units/**` — 54 класса + `*Prod` | `docs/reference/units.md` |
| Здания | `building/buildings/**` — 51 + абстрактные базы | `docs/reference/buildings.md` |
| Способности | `ability/abilities/**`, `ability/heroAbilities/**` — 93 | `docs/reference/abilities.md` |
| Чары | `registrars/EnchantmentRegistrar`, `data/reignofnether/enchantment/*.json` — 7 | `docs/reference/enchantments.md` |
| Туман войны | `mixin/fogofwar/**` (19), `FogOfWar*`, `FogChunkSnapshot`, `FogTintingBlockColor`, геймрул `reignofnetherForceFog` | `docs/reference/fog-of-war.md` — почему сокрытие данных несовместимо с модами рендера и что делала визуальная альтернатива |
| Исследования | `research/researchItems/**` — 51 | `docs/reference/research.md` |
| Экономика | `resources/{ResourceSources,ResourceIndex,ResourceChunk,ResourcesServerEvents}` | `docs/reference/economy.md` |
| Предметы юнитов | `items/{UnitItem,UnitItemBuilder,UnitItems,unititems/**}` | `docs/reference/items.md` |
| Волны | `survival/**` — 15 | `docs/reference/waves.md` |
| Сценарий, обучение, старт | `scenario/**`, `tutorial/**`, `startpos/**`, `matchstart/**`, `rtsmap/**` | `docs/reference/match-setup.md` |
| Фракция | `faction/**` | `docs/reference/faction.md` |
| Ассеты контента | текстуры юнитов/зданий, `models/item/*` спавн-яиц, звуки юнитов, lang-ключи | `docs/reference/assets.md` |
| Структуры | `data/reignofnether/structures/*.nbt` — 67 | `docs/reference/buildings.md` |

Восстановление любого класса: `git show 5079004e:src/main/java/com/solegendary/reignofnether/<путь>`.

---

## 3. Этап A — закрыть реальные поломки

Пока контент есть, этап проверяем в игре.

☐ A.1 Убрать форс `disableElytraMovementCheck` — `player/PlayerServerEvents.java:183`.
☐ A.2 Вернуть ванильную валидацию движения — `mixin/ServerGamePacketListenerImplMixin.java:11-25`.
☐ A.3 Дублирование пакетов частиц — `mixin/ServerLevelMixin.java:16-30`.
☐ A.4 Права команд: `/sendfood`, `/sendwood`, `/sendore`, `/sendemerald`
(`resources/ResourcesServerEvents.java:340-375`), `/rts-fog`
(`fogofwar/FogOfWarServerEvents.java:623-635`). Минимум `hasPermission(2)`.
☐ A.5 Ограничить `BuildingCommand` (`building/BuildingCommand.java:138-164`) — сейчас
выполняет произвольные команды с правом 2.
☐ A.6 Убрать форс `allcheats` на два авторских ника (`player/PlayerServerEvents.java:859-862`).
☐ A.7 Вернуть ключи локализации 7 чар (`enchantment.reignofnether.*`).

**Проверка:** гейты 1–4 плюс ручная — разгон не рвёт соединение, частицы не двоятся,
`/sendfood` без прав отвечает отказом.

---

## 4. Этап B — убрать интрузивность

Порядок из `INTRUSION_AUDIT.md` §9. Каждый пункт отдельный коммит.

### B.1 Генерация мира
☐ B.1.1 Удалить `data/minecraft/**`, кроме `tags/blocks/mineable/axe.json`.
Возвращает пещеры и каньоны (`probability` 0.00), ванильную плотность рельефа, 14 ванильных
биомов без руды на поверхности.
☐ B.1.2 Удалить мёртвое: `data/flat_dimensions/**`, `data/overworldify/**`,
`data/tectonic/**`, `data/minecraft/tags/functions/**`, `data/reignofnether/functions/**`.

### B.2 Ванильные ассеты
☐ B.2.1 Удалить `assets/minecraft/`, кроме `textures/item/trident.png` и
`textures/entity/trident.png` (решение при удалении `BlazeMixin` — если он не нужен, удалить и их).
Убирает перекраску всех GUI, рождественского кота, подмену сплэшей и логотипа меню.

### B.3 Туман войны — удаляется целиком

Текущий туман сокрывает **данные**, а не рисует: `ChunkMapMixin` выкидывает затемнённых
игроков из получателей чанка, `TrackedEntityMixin` отменяет синхронизацию сущностей,
`ServerLevelParticleMixin` режет частицы, `ChunkMapInitialSendMixin` отдаёт снимок с диска,
`ClientChunkCacheMixin` заставляет клиент держать устаревший снимок, `ClientLevelMixin
.setServerVerifiedBlockState` отбрасывает серверные обновления блоков,
`ClientPacketListenerLightMixin` сохраняет клиентский свет. Из-за этого он несовместим с любым
модом рендера и любым модом, шлющим свои пакеты по чанку. Решение владельца — удалить.

☐ B.3.1 Удалить `mixin/fogofwar/` целиком (19 миксинов) и убрать их из
`reignofnether.mixins.json`.
☐ B.3.2 Удалить подсистему: `FogOfWarServerEvents`, `FogOfWarClientEvents`,
`FogChunkSnapshot`, `FogTintingBlockColor`, `FogOfWarServerboundPacket`,
`FogChunkServerboundPacket`, `FogChunkClientboundPacket`, `FogBuildingClientboundPacket`,
`FogOfWarServerEvents`-связанные пакеты в `registrars/PacketHandler`.
☐ B.3.3 Удалить `ClientModEvents.onBlockColourEvent` и `ModelEvent.ModifyBakingResult`
(обёртка каждого блока и каждой модели) вместе с `mixin/fogofwar/BlockColorsAccessor`.
☐ B.3.4 Удалить `FogOfWarClientEvents.onRenderLivingEvent` — отмену рендера живых сущностей
за границей мира.
☐ B.3.5 Удалить геймрул `reignofnetherForceFog` и его отображение в
`gamerules/GameruleClientboundPacket`, `GameruleServerboundPacket`, `GameruleAction`,
`GameruleClient`, локализации.
☐ B.3.6 **Свип зависимостей.** Триан удаляемых компонентов имеют читателей вне подсистемы:
`RangedAttackerUnit.getFogRevealDuration` (читает GhastUnit, который удаляется),
`NightUtils.isInRangeOfNightSource` (комментирует использование ауры ночи),
`BloodMoon` (читает `isBlockVisibleFor`, удаляется вместе с NecromancerUnit),
`MinimapClientEvents` (`DARK_TERRAIN_PARTITIONS_MAX` — слой затемнённого рельефа становится
мёртвым, миникарту оставляем как инструмент ГМа, слой удалить),
`UnitServerEvents`/`FogOfWarClientEvents` в HUD. Найти все и почистить.
☐ B.3.7 Проверить, что `Mausoleum`/`Stronghold` (`NightSourceAddon`) не ссылаются на туман.
Они удаляются, но `NightSourceAddon` остаётся как контракт — комментарии в нём обновить.

### B.4 Перекраска за границей мира — попадает в B.3

☐ B.5.1 Ветка `0x252933` в `FogTintingBlockColor`, `BiomeColorsMixin`,
`LiquidBlockRendererMixin` уходит вместе с B.3.
☐ B.5.2 Скрытие предметов за границей — `fogofwar/ItemEntityRendererMixin.java:72` уходит
вместе с миксином. Проверить, нет ли не-туманного назначения у этого миксина.
☐ B.5.3 **Стена границы мира рисуется** (решение владельца). Удалить
`mixin/fogofwar/WorldBorderRenderMixin.java`: он отменяет `LevelRenderer.renderWorldBorder`.
Удаление миксина предпочтительнее инверсии условия — это возвращает ровно ванильное
поведение.

### B.5 Дефолты геймрулов

**Решение владельца:** поведение должно совпадать с ванильным.

☐ B.5.1 `doNetherConversion` → `false` (сейчас порталы зданий переписывают террейн).
☐ B.5.2 `buildingsOutsideBorder` → `false`.
☐ B.5.3 `neutralAggro` → `false`.
☐ B.5.4 **`doPlayerGriefing` → `false`.** Это ванильный геймрул, и мод регистрирует его с
дефолтом `true`, то есть меняет ванильное поведение (в ванилле ломание блоков выключено по
умолчанию). Возврат `false` — это и есть «как в ванильной игре».
☐ B.5.5 `doLogFalling` → `false` (ломание бревна превращает соседние в модовые блоки).
☐ B.5.6 **`doUnitGriefing` — геймрула в ванилле нет.** Сейчас при `false`
`BuildingServerEvents.onExplosion:958-964` срезает урон взрывов по блокам до листвы и TNT,
то есть криперы и TNT в мире не разрушают ничего — это не ванильное поведение. Варианты:
(a) удалить геймрул и перехватывать только тот урон, что попадает по
`BuildingPlacement`, оставив всё остальное ванилле; (b) оставить геймрул, но дефолт `true`.
Рекомендация — (a): геймрул без ванильного аналога в общем пространстве имён только
конфликтует.

### B.6 Безусловные миксины
☐ B.6.1 `fire/FireBlockMixin` — вернуть ванильную таблицу горючести, горение до age 15, убрать
горючесть обсидиана и `tryCatchFire`.
☐ B.6.2 `fire/BaseFireBlockMixin`, `CampfireBlockMixin`, `MagmaBlockMixin` — вернуть ванильные
ставки урона.
☐ B.6.3 `BaseSpawnerMixin` — убрать форс 600 тиков на все спавнеры; роуковскую часть оставить
(со спавнерами мода уйдёт).
☐ B.6.4 `SculkCatalystBlockEntityMixin` — вернуть ванильный bloom.
☐ B.6.5 `BlazeMixin` — вернуть ванильный `aiStep`.
☐ B.6.6 `WitherRoseMixin`, `LeavesBlockMixin`, `PanicGoalMixin` — вернуть ваниллу.
☐ B.6.7 `EntityMixin` — вернуть урон от удушья и полную заморозку. **Внимание:** с отключённым
удушьем камера орторежима на высоте безопасна; если удушье вернуть — проверить, что камера не
застревает в блоках (этап G).
☐ B.6.8 `EvokerFangsMixin` — вернуть хитбокс.
☐ B.6.9 `ThrownTridentMixin`, `ThrownPotionMixin`, `CrossbowMixin.getChargeDuration` — вернуть
ванильные конвейеры.
☐ B.6.10 `UnitInventoryMobMixin` — писать `reignofnether:UnitItems` в NBT только при
непустом инвентаре, а не всем мобам.
☐ B.6.11 `AbstractArrowMixin` — объявить `canHitEntity` явно или убрать; убрать повторное
применение урона.
☐ B.6.12 `PathNavigationMixin` — ограничить `@ModifyConstant(doubleValue = 1.0)` через
`require = 0`.
☐ B.6.13 `UnitServerEvents` — не конвертировать юнита после смерти; починить
`ConvertableUnit`, который не удаляет исходную сущность.
☐ B.6.14 Удалить `PlayerMixin` из списка миксинов (тело закомментировано) и незарегистрированный
`ZoglinMixin`.

### B.7 Клиентские миксины без гейта
☐ B.7.1 `TitleScreenMixin` — за флагом, дефолт `false`. Сейчас главное меню перехватывается
целиком безусловно.
☐ B.7.2 `MusicManagerMixin` — за флагом.
☐ B.7.3 `ClientLevelMixin.tickTime` — снять безусловный `cancellable`.
☐ B.7.4 `LevelRendererMixin` (`renderLevel` TAIL) — оверлей разрушения блока с 32 до 256.

### B.8 Экология выживания
Всё за флагом `survivalEcology`, дефолт `false`. Часть пунктов уходит сама при удалении
контента — отмечать при удалении, не дублировать.
☐ B.8.1 Отмена ванильного лута животных при охоте — `unit/UnitServerEvents.java:680-729`.
☐ B.8.2 Спавн животных вокруг столицы — `building/BuildingPlacement.java:881-888`.
☐ B.8.3 Вытеснение животных вокруг нового здания — `building/BuildingServerEvents.java:617-626`.
☐ B.8.4 Подавление роста культур в здании — `resources/ResourcesServerEvents.java:261-265`.
☐ B.8.5 Ломание блока в здании → AIR — `resources/ResourcesServerEvents.java:277-283`.
☐ B.8.6 `DIRT_PATH`→`DIRT` и уничтожение растений при смерти юнита у скульк-катализатора —
`unit/UnitServerEvents.java:497-537`.
☐ B.8.7 Отмена перехода между измерениями в здании — `building/BuildingServerEvents.java:968-989`.
☐ B.8.8 `FIRE` в центре порталов каждый тик.
☐ B.8.9 Установка `Blocks.SCAFFOLDING` под фундаментом — оставить как часть зданий или снять.
☐ B.8.10 **Настраиваемый блок лесов** (решение 11). Добавить на `Building` поле
`public Block scaffoldingBlock = Blocks.SCAFFOLDING;` и вариант «по биому». Требует нового
элемента в `BiomeColors`-подобном переборе: верхний слой — трава, ниже — земля.
Место изменения — `building/BuildingServerEvents.placeScaffoldingUnder`.
Обязательный подэтап D.21: то же самое в `CustomBuildingAction`/`CustomBuilding`.

**Проверка B:** создать новый мир на ванильном профиле, сравнить `run/world` до/после;
ручная проверка огня, спавнеров, скулька, блейзов, руды, границы мира.

---

## 5. Этап C — режимы

☐ C.1 Удалить `scenario/**` (10 файлов) с `GameMode.SCENARIO`, `ScenarioRole`,
`ScenarioSaveData`, `ScenarioRoleSaveData`, пакетами и ветками меню.
☐ C.2 Удалить `survival/**` (15 файлов) с `GameMode.SURVIVAL`, `WaveDifficulty`,
`SurvivalSaveData`, пакетами, `/debug-next-night`, `/debug-end-wave`.
⚠ Таблицы юнитов и ИИ волн — в `docs/reference/waves.md` (этап D, до удаления).
☐ C.3 Удалить геймрул `coopMode` — он существует только чтобы отключить автопобеду.
☐ C.4 Удалить `tutorial/**`, `startpos/**`, `matchstart/**`, `rtsmap/**`.
☐ C.5 Оставить `GameMode.CLASSIC` как единственный; убрать ветки выбора режима из HUD
(`hud/HudClientEvents.java:1626-1660`) и из `ClientGameModeHelper`.
☐ C.6 Перевести песочницу с признака `Faction.NONE` в `RTSPlayer` на **проверку прав
оператора**. Песочница — инструмент ГМа, а не режим, и она остаётся.
☐ C.7 Убрать `PlayerServerboundPacket.startRTSScenario` и `publishScenarioMap`
(`player/PlayerServerEvents.java:1328-1385`).

---

## 6. Этап D — удалить контент

**Сначала** перенести в `docs/reference/`, **потом** удалять файлы.

☐ D.1 `docs/reference/units.md` — таблица ростера со статами и ролью каждого юнита; анатомия
класса юнита: `createAttributes`, `defineSynchedData`, `initialiseGoals`/`registerGoals`,
`tick`, NBT через `addUnitSaveData`, интерфейсы. Уже частично есть в `HOWTO_FACTION.md` §5 —
дополнить числами.
☐ D.2 `docs/reference/buildings.md` — таблица 51 здания по группам; анатомия класса здания;
контракты пяти аддонов; `BuildingPlacement` как система (HP по блокам, `minBlocksPercent`,
строительство, тикеты, разрушение).
☐ D.3 `docs/reference/abilities.md` — 93 способности с эффектом, кулдауном и гейтом по
исследованию; анатомия `Ability`; список 36 способностей, жёстко привязанных кастом к
классу юнита, — это и есть причина, по которой способность нельзя перенести без юнита.
☐ D.4 `docs/reference/production.md` — как `ProductionItem`, очередь и стоимость работают
вместе; чем заменять шов, если владелец захочет создавать юниты без зданий.
☐ D.5 `docs/reference/research.md` — как устроено гейтирование способностей по исследованиям
и почему гейт только клиентский.
☐ D.6 `docs/reference/economy.md` — `ResourceSources`, `ResourceIndex`, население, лимиты.
☐ D.7 `docs/reference/waves.md` — планировщик волн как готовый пример «спавнить волны по
таймеру»: таблицы T1–T6, `WaveEnemy`, `WaveDifficulty`, выбор точек.
☐ D.8 `docs/reference/items.md` — `UnitItem`/`UnitItemBuilder`, категории, инвентарь юнита;
отметить, что слой был выключен `ENABLED = false` и наполнен на 7 из 30 пунктов.
☐ D.9 `docs/reference/assets.md` — что нужно из ассетов для нового юнита/здания: модель,
текстура, `mobheads`, звуки, lang-ключи.

☐ D.10 Удалить `unit/units/**` (264 файла в пакете `unit`, из них контент — все кроме
`unit/interfaces`, `unit/goals`, `unit/pathfinding`, `unit/controls` и четырёх
`*ServerEvents`/`*ClientEvents`).
**Исключения по решению владельца (решение 7): `unit/units/villagers/VillagerUnit`,
`unit/units/villagers/VindicatorUnit` и их `*Prod` остаются как рабочий шаблон.**
☐ D.11 Удалить `building/buildings/**`.
**Исключения по решению владельца (решение 7): `building/buildings/villagers/TownCentre`
и `building/buildings/villagers/Barracks` остаются как рабочий шаблон.**
Абстрактные базы (`shared/AbstractFarm`, `AbstractStockpile`, `AbstractBridge`, `AbstractMarket`,
`piglins/AbstractPortal`) удаляются — они часть контента, а не каркаса.
☐ D.12 Удалить `ability/abilities/**`, `ability/heroAbilities/**` (решение владельца: в
документацию). В `ability/UnitAction` удалить константы способностей, оставив те, что нужны
механике (проверить по `UnitActionItem` и `Ability`).
☐ D.13 Удалить `research/researchItems/**`. Проверить `ResearchSaveData` — оставить как
инфраструктуру флагов.
☐ D.14 Удалить `resources/{ResourceSources,ResourceIndex,ResourceChunk,ResourcesSaveData,
ResourcesServerEvents}`. `ResourceCost`/`ResourceCosts` **остаются** (решение 13) — они нужны
`ProductionItem.getCost()`. Удаляются 154 записи из `ReignOfNetherCommonConfigs`, и стоимость
перестаёт строиться по списку юнитов: она хранится на самом `ProductionItem`.
Поле `population` в `ResourceCost` уходит в E.4.
☐ D.15 Удалить `items/UnitItem*`, `items/unititems/**`, `items/ItemUtil` (частично),
`items/ItemServerEvents`, `items/ItemClientEvents`. `items/UnitInventory` **остаётся** по
решению владельца (решение 9) — под перенос предметов из мира.
☐ D.16 Удалить `faction/**`.
☐ D.17 Удалить `survival/**`, `scenario/**`, `tutorial/**`, `startpos/**`, `matchstart/**`,
`rtsmap/**` (если не сделано в C).
☐ D.18 Удалить `data/reignofnether/structures/**` (67 NBT).
☐ D.19 Почистить ассеты: текстуры юнитов и зданий, `models/item/*` спавн-яиц, сплэши и
переводы контента. **Оставить** переводы каркаса: `hud.*`, `abilities.*`, `unitstats.*`,
`commands.*`, `creativetab.*`, `resources.*`, `server.*`, `unititemtype.*`,
`enchantment.reignofnether.*`.
☐ D.20 Почистить реестры. `EntityRegistrar` — 67 типов, остаются 4 юнита-шаблона.
`BlockRegistrar` — 47 блоков: **остаётся палитра** по решению владельца (решение 8) —
`walkable_magma_block`, `temporary_walkable_magma_block`, `unextinguishable_soul_fire`,
`spider_friendly_barrier`, `wraith_snow_layer_block`, `decayable_nether_wart_block`,
`decayable_warped_wart_block`, `rts_structure_block`, четыре маркера гарнизона и производства;
**удаляются** 16 `rts_start_block_*` (неразрушимые, лежат в ванильной вкладке), 8 блоков голов
и стеновых черепов, `horizontal_portal`. `ItemRegistrar`, `BlockEntityRegistrar`,
`MobEffectRegistrar`, `ParticleRegistrar`, `SoundRegistrar` — по спискам из решений ниже.

---

## 7. Этап E — дыры каркаса, найденные при удалении

Обнаруживаются только когда контента нет. Каждый пункт — либо починить, либо осознанно
оставить с записью.

☐ E.1 **Чары удаляются целиком** (решение владельца). Семь чар датапака вместе с
`registrars/EnchantmentRegistrar` и `data/reignofnether/enchantment/*.json` уходят в
`docs/reference/enchantments.md` и удаляются. Удалить и читателей в каркасе: `breaching`
(`unit/UnitServerEvents.java:1000`), `fortifying` (`unit/interfaces/Unit.java:476`,
`util/MiscUtil.getMaxAbsorptionAmount`). Снять локализацию `enchantment.reignofnether.*`
и `hud.enchant.reignofnether.*`.
☐ E.2 **Атрибуты без потребителей** удаляются по решению 15: `critical_hit_chance`,
`explosive_hit_chance`, `lifesteal`, `mana_on_hit`, `scale`, `building_damage_bonus` —
вместе с `registrars/AttributeRegistrar` и геттерами в `AttackerUnit`/`HeroUnit`. Частицы
`floating_crit`, `floating_heart`, `mana` в `registrars/ParticleRegistrar` остаются.
☐ E.3 **Эффекты.** Пять из 28 использовались только удаляемым контентом и удаляются по решению
14: `zombie_infected`, `slime_infected`, `frost_damage`, `warm`, `villager_inspiration`.
Остальные 23 остаются.
☐ E.4 **Лимит армии и население.** Решение 13. Сейчас `getTotalPopulationSupply`
(`BuildingServerEvents.java:681-692`) суммирует `cost.population` построенных зданий, а
`ProductionItem.canAffordPopulation:74-85` сравнивает с этим. Требуется:
* базовый лимит — **1 юнит**, константой, не зависящий от зданий;
* прирост лимита даёт **ратуша фракции** — отдельный механизм, а не `cost.population`;
* снять поле `population` из `ResourceCost` либо оставить только для зданий-поставщиков.
Рекомендация: ввести `UnitServerEvents.maxPopulation = 1` как базу и
`UnitServerEvents.getPopulationBonusFromCapitols(ownerName)`, суммирующую по
`Building.isCapitol`.
☐ E.5 **Ленивое создание `SavedData`.** 8 файлов пишутся в любой мир. Проверить, создаёт ли
`DataStorage.save()` записи, в которые ничего не клали; иначе — удалять пустые файлы после
сохранения.
☐ E.6 **`ProductionItems` пуст.** `ReignOfNetherRegistries.PRODUCTION_ITEM` останется пустым —
проверить, что `CustomBuilding.checkAndAddProductionItems` и
`CustomBuildingPlacement.getProductionItem` это переживают.
☐ E.7 **`UnitItem.ENABLED = false`** и 7 из 30 предметов в реестре — если слой удалён (D.15),
проверить, что на `ItemUtil` больше ничего не висит.
☐ E.8 **`neutralAggro` без юнитов** (`NonUnitServerEvents:75-98`) станет бессмысленным:
он нацеливает ванильных мобов на юнитов. `attackSuppressedNonUnits` и `nonUnitMoveTargets`
работают на ванильной навигации и остаются полезными.
☐ E.9 **Скрытая связность `EntityRegistrar.getEntityType` — чинить** (решение 16).
`ProductionItems.getProductionItem` ищет юнит через
`EntityRegistrar.getEntityType(prodItem.getItemName())`, поэтому строка `itemName` в каждом
`*Prod` обязана совпадать с `case` в этом свитче; несовпадение не даёт ошибки, а молча ломает
обучение в кастомных строениях. Заменить на прямое сопоставление по `EntityType<?>` и убрать
зависимость от совпадения строк.

**Проверка:** гейты 1–4; `runClient` грузится без ошибок; в логе нет новых исключений при
входе в мир.

---

## 8. Этап F — `Faction`

Становится тривиальным после этапа D: при живых контентных классах enum ещё что-то значил.

☐ F.1 Удалить `faction/**` (уже в D.16).
☐ F.2 Убрать `Unit.getFaction()` (`unit/interfaces/Unit.java:172`) и
`Building.getFaction()` (`building/Building.java:100`) из контрактов. **Оба метода входят в
список «не трогать» §1 — здесь исключение осознанное**, их правка не ломает механику, а
убирает поле, которое после удаления контента не имеет носителей.
☐ F.3 Переписать `PlayerServerEvents.startRTS:463-474, 562-575` — свичи по фракции исчезают;
стартовый юнит и столица задаются параметром или конфигом.
☐ F.4 `building/custombuilding/CustomBuilding.java:93-95` — три флага
`buildableByVillagers/Monsters/Piglags` в NBT заменить одним булевым. **Сигнатура NBT
несовместима**, см. F.6.
☐ F.5 `util/MiscUtil.java:993-1014` — иконка и имя фракции.
☐ F.6 Миграция миров не нужна (решение владельца: мод для нового сервера). Старый тег
`faction` в `RTSPlayerSaveData` просто перестаёт читаться — важно, чтобы код не вызывал
`Faction.valueOf()` на старом NBT и не падал. Проверить, что `ResearchSaveData` и
`BuildingSaveData` тоже не содержат вызовов удаляемого перечисления.
☐ F.7 Локализация `hud.faction.reignofnether.*` — удалить из 22 файлов.

⚠ **Не делать** `FactionDefinition`. Это отдельная фича для нескольких играбельных рас, и
enum из шести значений ей не фундамент. Материалы уже есть в `HOWTO_FACTION.md` §2.

---

## 9. Этап G — камера

Требование владельца: менять высоту камеры и не рендерить уровни выше камеры.

Половина есть: `orthoviewPlayerBaseY = 100`, `orthoviewPlayerMaxY = 160`
(`orthoview/OrthoviewClientEvents.java:124-125`), пересчёт от рельефа (`:171-172`), зажим
телепорта по границе (`PlayerServerEvents.movePlayer`). Мёртвый геймрул `groundYLevel` влияет
только на плоскость тумана на клиенте — починить или удалить.

☐ G.1 **Сделать высоту настраиваемой и проверяемой, а не вычисляемой от рельефа.** Готово:
`OrthoviewClientEvents.setCameraHeight(double)` пинит высоту и зажимает её между
`minOrthoviewY` и новым `maxOrthoviewY` (320 — верх предела мира), `clearCameraHeightOverride()`
возвращает слежение за рельефом, `getCameraHeight()` читает текущее значение. Пока
`cameraHeightOverridden` не выставлен, поведение прежнее. Команду и хоткей для этого добавить
в этапе H.
☐ G.2 **Отсечение секций выше камеры — НЕ сделано, требует проверки в игре.** Замысел: камера
смотрит строго вниз, значит секции выше неё не видны ни при каком положении игрока по X/Z, и
ванилла зря их мешит и хранит. Очевидная точка — `cancellable`-инъекция в
`SectionRenderDispatcher.RenderSection#setSectionDirty`, но это внутренний вызов, и его отмена
рискует оставить секции с устаревшей геометрией. Проверяемое только в игре, поэтому вслепую не
сделано. Безопасные альтернативы для проверки: фильтр списка секций в
`SectionRenderDispatcher#setupRender` либо `LevelRenderer#renderSectionLayer`.
☐ G.3 Снять побочные эффекты входа: `PlayerServerEvents.enableOrthoview` вызывает
`removeAllEffects()` (стирает и ванильные зелья), а
`OrthoviewClientEvents.switchToEasyIfPeaceful()` молча повышает одиночный Peaceful до Easy.
**Обязательно**, иначе орторежим непригоден для обычной игры.
☐ G.4 `TopdownGuiClientEvents` форсит `guiScale = 3` и блокирует клавиши инвентаря и
достижений — снять или за флагом.
☐ G.5 После B.5.7 (возврат удушья) проверить, что камера на высоте не застревает в блоках.

**Проверка:** визуально — высота двигается, секции сверху не рисуются, эффекты игрока не
стираются при входе и выходе, Peaceful не меняется сам.

---

## 10. Этап H — РТС-режим для обычной игры

Задачи владельца, требующие своего кода. **Выполняются после того, как у каркаса появится
контент** — иначе проверять нечем.

☐ H.1 **Пропуск.** Пропуск = право оператора (уровень 2). Новой сущности не вводить. Добавить
проверку в оба пути входа: хоткей и команда.
☐ H.2 **Вход по команде.** Команда запускает тот же путь, что и хоткей. Без пропуска —
**ничего не происходит**: ни сообщения, ни ошибки.
☐ H.3 **Хоткей входа** остаётся как сейчас, но проходит ту же проверку пропуска.
☐ H.4 **Конец сессии.** Срабатывает потеря последнего здания, но **только если здания вообще
были** — иначе на старте, при нуле зданий, сессия рвётся сразу. После срабатывания:
юниты становятся нейтральными (`ownerName = ""`), владелец сессии перестаёт считаться
RTS-игроком, и при наличии пропуска он может войти снова.
☐ H.5 **Команда `force-loose`.** Симулирует поражение от лица игрока через
`execute as <player> run ...`. Для ручного прогона сценария поражения.
☐ H.6 Юнит умирает за границей мира — `unit/interfaces/Unit.java:388-389`. На сервере с world
border армия будет вымирать.
☐ H.7 Спавн рядом с игроком в РТС самовольно отдаёт ему юнита —
`unit/UnitServerEvents.java:847-857`. Для ГМа это ловушка: поспавнил врага рядом с союзником —
он стал его.
☐ H.8 Хук стартовой армии: сколько юнитов какого типа давать на старте. Сейчас `startRTS`
создаёт только воркера и скаута; спавн пачки уже умеет `UnitServerEvents.spawnMobs`
(`unit/UnitServerEvents.java:891-927`). Учитывать решение 13: базовый лимит 1 юнит, поэтому
стартовая армия — 1 юнит, пока не построена ратуша.
☐ H.9 Класс генератора ресурсов. **Не реализуется в этом плане** — владелец сделает его при
написании своей фракции. Форма, которую надо заложить:
`getTickInterval()`, `getResourceAmount()`, `getResourceType()`, необязательный `getCapacity()`,
чтобы генератор не давал бесконечно за тик. Описание расширения — в `_GUIDES/`.

---

## 11. Порядок и гейты

| Этап | Что | Зависит от | Гейт | Статус |
|---|---|---|---|---|
| **A** | Поломки | — | 1–4 + ручная проверка | **сделано** `daa270b1` |
| **B.1–B.2** | Датапак генерации и ванильные ассеты | — | новый мир, `diff` папки | **сделано** `2d806c30` |
| **B.3–B.4** | Туман войны, перекраска за границей | — | 1–4 | **сделано** `89984459` |
| **B.5** | Дефолты геймрулов, `doUnitGriefing` | — | 1–4 | **сделано** `da73d986` |
| **B.6–B.7** | Безусловные миксины | — | 1–4 | **сделано** `b354df4a` |
| **B.8** | Экология зданий + настраиваемый блок лесов | — | 1–4 | **сделано** |
| **C** | Режимы кроме CLASSIC | B.5 | 1–4 + `runClient` | **сделано** |
| **D** | Удаление контента | C | 1–4; **в игре не проверяемо** | **сделано**, см. §13 |
| **E** | Дыры каркаса | D | 1–4 | **сделано** |
| **F** | `Faction` | D | 1–4 | **сделано** |
| **G** | Камера | независим | визуальная проверка | **G.1/G.3/G.4 сделаны**, G.2/G.5 — только в игре |
| **H** | РТС для обычной игры | контент владельца | ручная проверка | **H.1–H.8 сделаны**, `runClient` не прогонялся |
| **§14** | Переделка «сущность + способности» | контент владельца | 1–4 + `runClient` | **§14.1–§14.4 сделаны**, §14.5 откачен; `runClient` прогнан — баги в `BUGS_RUNCLIENT.md`, см. §15 |

Гейты на каждом этапе: `compileJava`, `validateMixins`, `runData`, `runServer`, `runClient`
до титольного экрана.

**Что уже изменилось в цифрах:** миксинов было 49 серверных и 27 клиентских, стало 26 и 12.
Датапак генерации и namespace `assets/minecraft` удалены целиком.

**Проверка в игре ещё не выполнялась** после этапа B — она требует `runClient`, который сам не
завершается.

---

## 13. Состояние этапа D — измерено точно

Этап D начат и доведён до состояния «почти». Работа идёт на ветке
**`wip/stage-d-deletions`**; ветка `1.21.1-clean` остаётся зелёной.

### Что уже сделано

* Удалено **385 файлов контента**: `unit/units/**` (113), `building/buildings/**` (68),
  `ability/abilities/**` (55), `ability/heroAbilities/**` (27), `research/researchItems/**` (51),
  `resources/**` (15), сценарий, выживание, обучение, стартовые позиции, экран матча, `rtsmap`,
  `faction/**`, предметный слой юнитов, а также поддеревья рендереров, моделей, анимаций,
  снарядов, иконок чар и героев.
* Сохранено: `VillagerUnit` + `VindicatorUnit` и их `Prod`, `TownCentre` + `Barracks`,
  классы стоимости и пула ресурсов, `UnitInventory`.
* `ReignOfNetherCommonConfigs` сведён к пустому spec — 154 записи стоимостей удалены.
* `EntityRegistrar` → 2 типа юнитов, `ProductionItems` → 2, `Buildings` → 2,
  `BuildingSaveData` → 2 случая, `ItemRegistrar` переписан.
* Синтаксис после массового удаления восстановлен, компиляция проходит по синтаксису.

### Что осталось: 1235 ошибок, 1146 из них — «cannot find symbol»

Это ссылки на удалённые классы в ~60 файлах. Таксономия по убыванию:

| Что referenced | Файлов |
|---|---|
| `ResourceCosts.*` (стоимости юнитов/героев) | много |
| `PortalPlacement`, `FarmPlacement`, `StockpilePlacement`, `ItemShopPlacement`, `GraveyardPlacement`, `SculkCatalystPlacement`, `HealingFountainPlacement`, `BridgePlacement`, `BeaconPlacement` | ~25 |
| `HeroServerEvents` / `HeroClientEvents` | 8 |
| `SurvivalServerEvents`, `ScenarioClientEvents`, `StartPosClientEvents`, `RTSMapInfoServerEvents`, `TutorialServerEvents` | ~30 |
| `FactionRegistries`, `Faction` | ~15 |
| `UnitItems` / `UnitItem` / `items.unititems.*` | ~20 |
| Классы конкретных юнитов (`PillagerUnit`, `RavagerUnit`, `NecromancerUnit`, …) в instanceof-цепочках и свитчах | ~40 |

Почему так много: интерфейсы (`Unit`, `HeroUnit`, `AttackerUnit`, `WorkerUnit`) и цели
(`unit/goals/**`) содержат длинные цепочки `instanceof` по конкретным юнитам, а HUD и
`SandboxClientEvents` перечисляют их в свитчах. Каждая такая цепочка требует ручного решения:
удалить ветку, а не «закомментировать».

### ⚠ Ошибка в границах удаления, которую надо исправить первой

Каталог `building/buildings/placements/` содержал **12 подклассов размещения**, включая
`TownCentrePlacement` и `CustomBuildingPlacement`, которые нужны **сохранённым** строениям и
кастомным строениям. Он был удалён вместе с контентом целиком. `TownCentrePlacement` и
`CustomBuildingPlacement` уже восстановлены; остальные девять (`PortalPlacement`,
`ProductionPlacement` в этом каталоге, `FarmPlacement`, `StockpilePlacement`, `ItemShopPlacement`,
`GraveyardPlacement`, `SculkCatalystPlacement`, `HealingFountainPlacement`, `BridgePlacement`,
`BeaconPlacement`) удалены правильно — они соответствуют удалённым строениям.

⚠ При продолжении не удалять `placements/` целиком: там лежит общий `ProductionPlacement`-по-
не-разным путям и код, на который ссылается ядро.

### Порядок продолжения

1. Восстановить/проверить `placements/` (см. выше).
2. Удалить ссылки на `ResourceCosts.*` — заменить на константы или убрать условия.
3. Пройти `unit/interfaces/**` и `unit/goals/**` — вычистить `instanceof` по удалённым юнитам.
4. Пройти `hud/**`, `sandbox/**`, `minimap/**`, `player/**`.
5. Удалить регистрации удалённых пакетов в `PacketHandler`, `ClientEventRegistrar`,
   `ServerEventRegistrar`.
6. `ClientModEvents`, `CommonModEvents` — рендереры и атрибуты удалённых юнитов.
7. Гейт после каждого шага; `validateMixins` обязателен — удаление миксинов легко ломает
   цель инъекции.

### Рекомендация

Это ~10–15 часов работы. Начинать с чистой сессии. Проверка — только компиляцией;
`runServer`/`runClient` запускать в самом конце, когда всё зелёное, как и договорились.

**Этапы D–F делать подряд, одним прицеванием.** Между ними мод нерабочий, и держать
промежуточные коммиты с «почти пустым» содержимым незачем.

## 12. Решения владельца — все вопросы закрыты

Закрыто 2026-10-05. Ниже полный список; ни один открытый вопрос не остался.

### 12.1 Побочные эффекты размещения здания

**Решение: оставить всё.** Снос рельефа при `slantedBuilding`, леса, вытеснение животных,
запрет перехода между измерениями внутри строения, тушение пожаров, подавление роста культур —
всё остаётся механикой здания. «Порталом и нельзя пользоваться, если это строение» — так
и задумано.

**Добавить:** блок лесов настраивается. Либо универсальный блок (в примере — кобблстоун), либо
зависящий от биома: сверху трава, всё что ниже — земля. Значение по умолчанию — текущее
поведение. Реализуется в `building/BuildingServerEvents.placeScaffoldingUnder` плюс новое поле
на `Building`; для кастомных строений — соответствующий пункт в
`CustomBuildingAction`/`CustomBuilding`.

### 12.2 Вход, выход и границы сессии

**Решение:**

* **Вход — двумя способами.** Горячая клавиша остаётся как есть, плюс **команда** для входа.
  Если у игрока нет пропуска — **ничего не происходит**, ни ошибки, ни сообщения.
* **Пропуск** = право оператора (уровень 2). Пропуск выдаётся существующим механизмом прав,
  новой сущности не вводится.
* **Выход — как сейчас.**
* **Конец сессии — потеря последнего здания.** После этого юниты становятся нейтральными
  (`ownerName = ""`), а игрок, если пропуск ещё есть, может начать заново, заспавнив первый
  отряд.
* **Пока зданий ноль (в частности на старте), система не включается.** Терять нечего, и обрыв
  сессии не фиксируется. Условие «здание есть» проверяется до срабатывания поражения.
* **Команда `force-loose`** — симулирует проигрыш от лица игрока, реализуется через
  `execute as <player> run`. Нужна, чтобы прогонять сценарий поражения вручную.

### 12.3 Ресурсы и лимит армии

**Решение:** ресурсы остаются. `ResourceCost` сохраняется вместе с `ProductionItem.getCost()`,
но 154 записи конфига стоимостей удаляются. В будущем возможны свои ресурсы на фракцию — это
отдельная фича, закладывать её сейчас не нужно, но `ResourceCost` не должен быть привязан к
списку юнитов (то есть не строить стоимость по enum'у, а хранить её на `ProductionItem`).

**Лимит армии по умолчанию — 1 юнит.** Увеличение даёт ратуша каждой фракции (геймдизайн).
Значит: снять зависимость лимита от `cost.population` зданий и сделать базовый лимит
константой (1), а прирост от ратуши — отдельным механизмом.

### 12.4 Пять эффектов из удаляемого контента

**Решение:** удалить — `zombie_infected`, `slime_infected`, `frost_damage`, `warm`,
`villager_inspiration`. Возражений не было.

### 12.5 Шесть атрибутов без потребителей

**Решение: удалить.** `critical_hit_chance`, `explosive_hit_chance`, `lifesteal`,
`mana_on_hit`, `scale`, `building_damage_bonus`. Частицы `floating_crit`, `floating_heart`,
`mana` остаются (пригодятся владельцу), вызов не реализуется.

### 12.6 Скрытая связность `EntityRegistrar.getEntityType`

**Решение: починить.** Сопоставлять `ProductionItem` с `EntityType<?>` напрямую, без строкового
свитча по имени, и убрать зависимость от совпадения строк.

### 12.7 Остальные закрытые ранее

* Стена границы мира рисуется (B.4.3) — миксин удаляется.
* `doPlayerGriefing` → `false`; `doUnitGriefing` удаляется, перехват взрывов ограничивается
  уроном по зданиям (B.5.4, B.5.6).
* Миграция старых миров не нужна (F.6).
* Чары и способности — в документацию и удаляются (D.12, E.1).
* Туман войны удаляется целиком (B.3).
* Шаблон сохраняется: `VillagerUnit`, `TownCentre`, `Barracks`, `VindicatorUnit` (решение 7).
* Палитра спецблоков сохраняется (решение 8).
* Инвентарь юнита сохраняется (решение 9).

**Открытых вопросов нет.**

---

## 14. Новая задача владельца: система юнитов и строений «как в Warcraft 3»

Поставлена 2026-10-07, **вне этапов A–H**. Это переделка самого каркаса, а не удаление контента:
она опирается на тот же принцип «сущность + набор способностей» и должна лечь на уже
существующие `ability/**`, `UnitAction`, `ProductionItem` и `BuildingPlacement`. Делать после
того, как у каркаса появится свой контент (как этап H) — иначе нечем проверять.

### 14.1 Идея

* **Любой юнит — это просто юнит с определённым набором способностей.** Нет жёстких подклассов
  «рабочий / боец / строитель» на уровне поведения: поведение собирается из способностей, как в
  Warcraft 3.
* **Рабочий — это юнит** со способностями добычи ресурсов плюс способностью **«открыть меню»**: она
  раскрывает подменю других способностей (например «Построить …»), откуда выбирается здание. Само
  строительство — тоже способность, а не отдельная ветка кода.
* **Строения — аналогично:** каждый объект — сущность с набором способностей (производство,
  апгрейды, меню-в-меню), а не набор хардкодных полей и свитчей по классу.
* Цель — убрать специальные случаи по конкретному классу юнита/здания из HUD, целей и обработчиков.

### 14.2 Юниты

Сегодня поведение рабочего задано целями (`unit/goals/GatherResourcesGoal`,
`ReturnResourcesGoal`, `BuildGoal`) и перечислением профессий (`VillagerUnitProfession`:
`MASON`/`LUMBERJACK`/`MINER`), а способности (`ability/Ability`) — отдельный слой для боевых и
специальных умений. Требуется свести это к одной модели:

* [ ] Способность умеет: открыть подменю способностей (меню-в-меню), быть целью-приказом, иметь
      иконку/кулдаун/цену и (опционально) серверную проверку.
* [ ] «Добывать дерево/камень/еду», «возвращать ресурсы», «строить» — становятся способностями;
      `VillagerUnitProfession` и профессионные бонусы либо сворачиваются в способности, либо
      остаются как модификаторы способностей.
* [ ] HUD строит кнопки из списка способностей единообразно для юнита и здания.
* [ ] Определиться, что делать с `unit/goals/**`: цели остаются «низким» слоем (навигация,
      преследование, паника), а приказы игрока переезжают в способности.

### 14.3 Строения

* [ ] Строение — сущность со списком способностей (производство, апгрейд, аура, «открыть меню»).
* [ ] Производство (`ProductionItem`) и аддоны (`building/addon/**`) выразить в этой же модели,
      чтобы не было двух параллельных механизмов «что умеет здание».

### 14.4 Новые виды приказов (`UnitAction`)

* [ ] `DIG_BLOCK` — «вскопать блок»: выкопать/убрать один блок в указанной точке.
* [ ] `DIG_AREA` — «вскопать область»: то же по выделенной области (рамка как у box-select).
* [ ] Приказ = константа в `ability/UnitAction` + способность + обработчик в `unit/UnitActionItem`
      + (при необходимости) команда в `commands/rtsapi`. Расширение должно быть конфигурируемым:
      новый приказ не должен требовать правок в HUD.
* [ ] Что именно «вскапывается» (трава/земля/руда/лес), куда девается дроп, ограничения по
      инструменту и правам — **уточнить у владельца**.

### 14.5 Вскапывание строения — ОТКАЧЕНО (урон по HP отменён)

Замысел был: блок строения не копается, а удар по нему наносит зданию урон и сносит его сверху
вниз. Это было реализовано (коммиты `a422e885`→`21d511f3`, процентный урон
`BUILDING_DAMAGE_PERCENT_PER_HIT = 0.05`), но **первый `runClient` показал, что владелец этого не
хочет**, и фича откачена в коммите `445447ad`.

**Действующее поведение:** блок в составе любого модового строения
(`BuildingUtils.isPosInsideAnyBuilding`) **не вскапывается и не получает урона** — удар просто
откатывается: ни копания, ни дропа. Здания ломаются обычной атакой (`ATTACK_BUILDING` / мили).
`DigAbility.BUILDING_DAMAGE_PERCENT_PER_HIT`, `BUILDING_DAMAGE_PER_HIT` и
`BuildingPlacement.demolishTopDown` удалены.

Прежние решения владельца (2026-10-08) сохранены ниже только как история реализации:
1. Порядок удаления блоков — сверху вниз.
2. Модель урона — сначала фикс. `30`, затем процент от максимального HP (см. §15.5).
3. После сноса — ничего не остаётся.
4. Отдельной полосы прогресса сноса в HUD не было.
5. Правило применялось ко всем модовым строениям.

### 14.6 Порядок работ и связь с остальным планом

* Зависит от: контента владельца (как этап H) — переделывать нечего, пока нет юнитов/зданий.
* Каждый пункт 14.2–14.4 — отдельный коммит с гейтами 1–4 (`compileJava`, `validateMixins`,
  `runData`, `runServer`); фича 14.5 была сделана, затем откачена по результату `runClient` —
  см. §14.5 и §15.5.
* Не ломать швы `_GUIDES/00_обзор.md` §«Три точки» (производство, лимит армии, гарнизон).

---

## 15. Состояние §14 — первый инкремент

Сделано в ветке `wip/stage-d-deletions` (решения владельца: отбросить прежний сломанный WIP
и реализовать заново; вскапываются любые блоки, дроп идёт в инвентарь юнита; шаблоны
(`VillagerUnit`, `VindicatorUnit`, `TownCentre`, `Barracks`) используются как демо-контент).

### 15.1 Механика меню-в-меню (§14.1)

* `ability/Ability` получил `subAbilities` / `addSubAbility` / `isMenu()` / `getSubAbilities()`.
  Способность с непустым списком ведёт себя как кнопка меню.
* `ability/MenuAbility` — кнопка, которая открывает подменю (серверного эффекта нет).
* `HudClientEvents` держит стек `AbilityMenuFrame` (способность + владелец-юнит или -здание),
  открывает/закрывает меню и рисует подменю в левом верхнем углу с кнопкой «назад». Стек даёт
  меню-в-меню; меню закрывается при смене выделения. Добавлен `hud.reignofnether.ability_back`.

### 15.2 Приказы как способности (§14.2)

* `ability/OrderAbility` — обёртка над `UnitAction` для меню; `ability/CommandAbility` —
  кнопка с собственным хоткеем и динамической иконкой.
* `ability/CommandAbilities` — общие приказы (ATTACK, BUILD_REPAIR, GATHER, GARRISON,
  UNGARRISON, STOP, HOLD). `Unit.getCommandAbilities()` выдаёт их по интерфейсам юнита;
  `Unit.getAbilityButtons()` теперь их добавляет. Старый HUD-ный список `ActionButtons` удалён.
* Кнопки постройки зданий переехали в меню: `ability/BuildMenuAbility` открывает
  `VillagerUnit.getBuildingButtons()`; в `Ability` добавлен `getSubButtons` для меню с
  готовыми кнопками.

### 15.3 Вскапывание (§14.4) и снос строений (§14.5)

* `UnitAction.DIG_BLOCK` и `UnitAction.DIG_AREA`.
* `ability/DigAbility` — целевая способность: блок удаляется, дроп кладётся в шестислотовый
  `UnitInventory` юнита, а если тот полон — падает на землю.
* `DIG_AREA` — выделение области рамкой: нажатие запоминает первый угол
  (`CursorClientEvents.setDigAreaStartBp`), отпускание досылает прямоугольник вторым углом через
  `UnitActionItem` → `Ability.useArea`; суммарно не больше `MAX_AREA_BLOCKS = 256` блоков.
* §14.5 **откачен** (`445447ad`): блок в составе строения не вскапывается и урона не наносит —
  удар отменяется (блок не удаляется, дропа нет). Здания ломаются обычной атакой. Прежний
  процентный урон и `demolishTopDown` удалены; см. §14.5 и §15.5.
* Приказ идёт штатным путём способности: кнопка ставит `CursorClientEvents.setLeftClickAction`,
  клик по миру шлёт команду, сервер вызывает `DigAbility.use`/`useArea`.

### 15.4 Остаток по §14

* Дизайн-значения: `MAX_AREA_BLOCKS = 256`, дальность `DIG_RANGE = 12` — легко меняются
  константами.
* Повторный `runClient`: `DIG_AREA` не копал, потому что `enqueue` отбрасывал блоки дальше
  `DIG_RANGE`. Теперь в очередь идёт вся область, а недосягаемые блоки юнит обходит пешком
  (`DigAbility.serverTick`), первого угла и второго угла области ведёт `CursorClientEvents`
  (`digAreaStartBp`/`digAreaEndBp`).

### 15.5 Правка §14.5 (история): урон по строению был процентным, затем откачен

Коммит `21d511f3` сделал урон долей максимального HP: `BUILDING_DAMAGE_PERCENT_PER_HIT = 0.05`
(5 % за удар), поэтому время сноса не зависело от размера здания. Удар по блоку строения при этом
**отменялся**: блок не удалялся, дроп не выдавался. Всё это вместе с `DemolishTopDown` **удалено в
`445447ad`**: теперь копание по строению не делает вообще ничего (см. §14.5). Абзац сохранён как
история одного из откатов, а не как описание текущего поведения.

Гейты: `compileJava`, `validateMixins`, `runData`, `runServer` — зелёные после каждого шага.
`runClient` не прогонялся (интерактивный): клиентские пути (меню, рамка выделения, отрисовка
кнопок) гейтами не покрываются и требуют ручной проверки в игре.
