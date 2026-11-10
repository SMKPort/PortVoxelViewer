# Port Client Booster 0.2.0 | Perfil Somake Network

Mod experimental **somente cliente** para **Minecraft 1.21.1 / NeoForge 21.1.235 / Java 21**.

Baseado no manifesto e na lista de mods exportados do modpack **SSMP - Season 1 - Capitulo 2, v1.10** (213 mods e 1 pacote de recursos). Nao altera o servidor e **nao precisa ser instalado no servidor**.

## Alteracoes em relacao a v0.1.0

- Corrige a exigencia minima do NeoForge: **21.1.235**, alinhada ao manifesto do Somake. Alvo de compilacao tambem 21.1.235.
- Adiciona amostragem de tempos entre frames (HUD com media FPS e 1% low), e opcionalmente p95 frametime no log.
- Detecta mods de otimizacao e modelos conhecidos pelo nome apresentado (apenas informativo, sem alterar suas configuracoes).
- Troca o perfil padrao para modo conservador: distancia de entidades e particulas sem ajuste automatico ate autorizacao local do jogador.
- Preserva a configuracao de alvo de FPS e ajustes graduais opcionais da versao anterior.

## Por que defaults conservadores?

Somake lista Sodium, ImmediatelyFast, Entity Culling, Dynamic FPS, FerriteCore, ModernFix, AllTheLeaks, Armourer's Workshop, Emotecraft, playerAnimator, GeckoLib e Iris. Alterar o renderer dos outros mods sem profiling do ambiente poderia introduzir glitches. O recurso de distancia de entidades reduz culling de modelos distantes, incluindo armaduras de jogadores. O controle global de particulas pode ocultar magias. Ambas as funcoes ficam desligadas por padrao.

## Como ativar ganhos visuais opcionais

Apos primeira execucao, edite `config/portclientbooster-client.toml`:

- `adapt_entities = true`: reduz em passos pequenos o alcance visual de entidades se o FPS permanecer abaixo da meta. `min_entity_scale = 0.85` por padrao. Pode fazer armaduras distantes desaparecerem.
- `adapt_particles = true`: reduz a configuracao global de particulas se houver baixa persistente de FPS. Pode afetar magias.
- `target_fps = 60`: altera a meta de estabilidade.
- `log_diagnostics = true`: registra metricas de FPS e frametime no `latest.log` em janelas de 5 segundos.

## Como compilar

Exige JDK 21 e Gradle 9.2.1 instalados. Execute `gradle clean build` dentro desta pasta. Ou abra **Actions -> Port Client Booster JAR** no GitHub e baixe o JAR gerado pelo workflow. **O JAR ainda nao foi testado em jogo.** Instale o JAR somente na pasta `mods` do cliente.

## Nao implementado

Nao reduz draw calls de Armourer's Workshop, nao faz culling por osso, nao otimiza videos MCEF/WATERFRAMES, nao muda shaders do Iris, nao altera o TPS do servidor e nao corrige travamentos da rede. Esses pontos requerem profiling e testes direcionados.
