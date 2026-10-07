# Туман войны

> Удалено на этапе D. Любой файл восстанавливается командой `git show 5079004e:<путь>`.

Удалён целиком (решение 6). Туман **сокрывал данные**, а не рисовал: `ChunkMapMixin` выкидывал затемнённых игроков из получателей чанка, `TrackedEntityMixin` отменял синхронизацию сущностей, `ClientChunkCacheMixin` держал устаревший снимок. Именно поэтому он был несовместим с любым модом рендера. Разбор — `INTRUSION_AUDIT.md` §4.2.

## Инвентарь (из `5079004e`)

- BiomeColorsMixin.java
- BlockColorsAccessor.java
- ChunkHolderMixin.java
- ChunkMapInitialSendMixin.java
- ChunkMapMixin.java
- ClientChunkCacheMixin.java
- ClientPacketListenerLightMixin.java
- CompiledChunkMixin.java
- EntityRenderDispatcherMixin.java
- EntityShadowMixin.java
- ItemEntityRendererMixin.java
- LevelRendererMixin.java
- LiquidBlockRendererMixin.java
- ServerLevelParticleMixin.java
- SingleQuadParticleMixin.java
- TrackedEntityMixin.java
- WorldBorderMixin.java
- WorldBorderRenderMixin.java

