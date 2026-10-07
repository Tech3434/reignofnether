---
name: reignofnether-add-hero
description: Как добавить героя в Reign of Nether — уровни, мана, ранги, воскрешение, HeroUnit. Использовать при создании геройского юнита.
---

# Как добавить героя

Полный разбор — `_GUIDES/05_hero.md`. Контракт — `unit/interfaces/HeroUnit` (широко используется
HUD, healthbars и целями; **оставлен** после этапа D).

## Шаги

1. **Юнит** — обычный юнит (`reignofnether-add-unit`), дополнительно реализует `HeroUnit`.
2. **Уровни и опыт** — опыт через предмет (`HeroExperienceBottleItem` — предметный слой удалён,
   при необходимости восстанови из `5079004e` или сделай свой источник).
3. **Мана** — атрибут маны и её реген; см. `AttackerUnit`/`HeroUnit` (атрибут `mania_*`
   и подобные остались в `AttributeRegistrar`).
4. **Ранги/скиллы** — меню повышения ранга рисует `HudClientEvents` через
   `HeroAbility.allSkillsLearnt` / `isRankUpMenuOpen`.
5. **Воскрешение** — свои правила смерти героя (старые `HeroUnitSave`/`HeroUnitSaveData` удалены
   на этапе D и **не восстанавливаются** как есть — сделай хранение под свою фракцию).
6. **Локализация/иконки** — `reignofnether-add-assets`.

## Грабли

* Не тяни обратно `HeroUnitSave`/`HeroUnitSaveData` целиком: их вызовы вычищены из
  `UnitServerEvents`, и они завязаны на удалённую фракционную логику.
* Гейт `ALLOWED_HEROES` ограничивает число героев (геймрул `allowedHeroes`).
