---
name: reignofnether-add-unit
description: Как добавить юнита в Reign of Nether — класс, атрибуты, цели, тик, NBT, регистрация, рендер. Использовать при создании нового юнита.
---

# Как добавить юнита

Полный разбор — `_GUIDES/02_unit.md`. Шаблон для копирования — `unit/units/villagers/VillagerUnit`
(воркер) или `VindicatorUnit` (боевой).

## Шаги

1. **Класс** `unit/units/<Name>Unit.java` — наследует `PathfinderMob` и реализует нужные
   интерфейсы: `Unit` обязательно; `AttackerUnit` (боевой), `WorkerUnit` (строитель),
   `RangedAttackerUnit`, `HeroUnit`.
2. **Атрибуты** — `public static AttributeSupplier.Builder createAttributes()`, начинать с
   `Unit.createDefaultAttributes()`. В билдер сущности атрибуты **не** передаются.
3. **Синхронизация** — `defineSynchedData` (через `Unit.defineSynchedData`).
4. **Цели** — `initialiseGoals`/`registerGoals`; используй `unit/goals/**`.
5. **Тик** — `tick()` + `Unit.tick()`.
6. **NBT** — `addUnitSaveData`/`readUnitSaveData` (+ `Unit.addUnitSaveData`).
7. **Регистрация** — `registrars/EntityRegistrar`:
   `DeferredRegister<EntityType<?>>`, `EntityType.Builder.of(...).sized(w,h).clientTrackingRange(100)`.
8. **Производство** — `<Name>Prod extends ProductionItem` с `getEntityType()` (см.
   `reignofnether-add-production`).
9. **Рендер** — `unit/modelling/renderers/**` + регистрация в клиентском событии.
10. **Ассеты и переводы** — `reignofnether-add-assets`.

## Грабли

* `Unit.createDefaultAttributes()` — точка входа; не изобретай базовые атрибуты.
* `ProductionItems.getProductionItem` сопоставляет по `EntityType<?>` напрямую (E.9), не по имени.
* Лимит армии базово 1; прирост даёт ратуша.
