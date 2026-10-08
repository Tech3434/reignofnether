---
name: reignofnether-add-research
description: Как добавить исследование и гейт контента по нему в Reign of Nether. Использовать, когда новая способность/юнит/здание должны открываться после исследования.
---

# Как добавить исследование

Действующая система — `_GUIDES/07_research.md` и `docs/RESEARCH_AND_EXTENSIBILITY_PLAN.md`.
Прежняя (`research/researchItems/**`, `ResearchServerEvents`, клиентский флаг) удалена; ниже —
новая.

## Определение — данными (датапак)

`data/<namespace>/research/<name>.json`, id = `<namespace>:<name>`. Перечитывается на `/reload`
(`ResearchJsonLoader`).

```json
{
  "name": "research.<ns>.<id>",
  "icon": "<ns>:textures/icons/items/...",
  "type": "unlock",
  "cost": { "food": 0, "wood": 100, "ore": 0, "seconds": 20 },
  "prerequisites": [ { "research": "<ns>:other", "invert": false } ],
  "attributes": [ { "attribute": "minecraft:generic.attack_damage", "amount": 1.0, "unit": "<ns>:<unit>" } ]
}
```

## Гейт — код, серверный

У `Ability`, `ProductionItem`, `Building` есть `requiredResearch` (список `ResearchCondition`) и
флюент-сеттер `requireResearch(...)`. Условие **инвертируемо**: `ResearchCondition.of(id)` /
`ResearchCondition.not(id)`.

```java
new SomeAbility(...).requireResearch(ResearchCondition.of(ResourceLocation.parse("<ns>:tech")));
```

Проверки выполняются на сервере (использование способности, старт производства, постановка
здания; GM-команды постановки обходят) и в HUD (кнопки серые, пока условие не выполнено).

## Состояние и команды

Состояние — на игрока (`ResearchSaveData`, персистентно, лениво), зеркало на клиенте —
`ResearchClientEvents`/`ResearchUtils.meetsClient`. **Поражение обнуляет** исследования.
Команды (право 2): `/research grant|revoke|clear|list <player> [<ns:id>]`.

## Гейты

`compileJava` → `validateMixins` → `runData`. `runServer` не поднимать (см. `AGENTS.md`).
