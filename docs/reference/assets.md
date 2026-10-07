# Ассеты

На этапе D вместе с контентом удалены ассеты трёх фракций. Восстанавливать их по одному обычно
не нужно — проще взять нужное из истории:

```
git show 5079004e:src/main/resources/assets/reignofnether/<путь> > <файл>
git ls-tree -r --name-only 5079004e -- src/main/resources | grep <подстрока>
```

## Что было снесено

| Категория | Путь | Примеры |
|---|---|---|
| Текстуры юнитов | `assets/reignofnether/textures/entity/**` | зомби, пиглины, нежить |
| Текстуры/иконки зданий | `assets/reignofnether/textures/block/**`, `textures/icons/blocks/**` | ратуши, казармы фракций |
| Спавн-яйца | `assets/reignofnether/models/item/*` + `textures/item/*` | по одному на юнита |
| Звуки юнитов | `assets/reignofnether/sounds/**` | шаги, атаки, смерти |
| Иконки способностей | `assets/reignofnether/textures/icons/abilities/**` | 93 способности |
| Переводы контента | `assets/reignofnether/lang/*.json` | ключи `unit*.reignofnether.*`, `buildings.*`, `abilities.*` |

## Что осталось (не трогать)

Ключи локализации каркаса: `hud.*`, `abilities.*`, `unitstats.*`, `commands.*`, `creativetab.*`,
`resources.*`, `server.*`, `unititemtype.*`.

## Что нужно для нового юнита/здания

Полный чек-лист — `_GUIDES/09_assets.md`: модель, текстура, `mobheads`, звуки, lang-ключи,
регистрация в `SoundRegistrar`/`EntityRegistrar`/`ItemRegistrar`.
