# Port Performance + Booster 3.0 🎮⚡

Mod **100% client-side** para Minecraft **1.21.1 / NeoForge 21.1.235 ou superior**, integrado ao site de telemetria opcional.

Código: https://github.com/SMKPort/PortVoxelViewer/tree/port-performance-monitor/performance-mod

Site: https://port-performance-monitor.onrender.com

## Novidades da 3.0

- Booster **contínuo e reversível**, com janelas de 5 segundos, detecção de quedas persistentes e cooldown para não ficar oscilando.
- Ajuste gradual da distância de renderização de chunks, limitado a até 4 chunks abaixo da configuração inicial e respeitando um mínimo configurável (6 por padrão).
- Mede diferença de FPS e p95 após cada ajuste; desfaz automaticamente a última alteração quando não observa melhoria suficiente.
- Restaura opções ao desligar, sair do mundo ou reiniciar após interrupção, sem sobrescrever escolhas manuais detectadas.
- Distância de entidades é **opcional e desligada por padrão**, preservando armaduras customizadas de outros jogadores.
- Não altera partículas, magias, shaders, renderizadores de terceiros ou configurações do servidor.
- Medição de tempos reais entre frames para **1% low e frametime p95/p99**, em vez de estimar por FPS amostrado uma vez por segundo.
- HUD **arrastável e redimensionável**, com tamanho entre 55% e 225%, botões +/-, roda do mouse, canto de redimensionamento, persistência de posição/escala e adaptação à resolução.

## Instalação

Abra [GitHub Actions](https://github.com/SMKPort/PortVoxelViewer/actions/workflows/port-performance-build.yml), selecione um build bem-sucedido da branch `port-performance-monitor` e baixe o artefato `Port-Performance-Monitor-1.21.1-NeoForge`. Coloque o JAR `portperf-3.0.0.jar` na pasta mods. Remova o JAR 2.1.0 e evite executar ao mesmo tempo outro PortClientBooster que ajuste as mesmas opções.

O JAR deve ser **compilado e testado no Somake** antes de considerar os ganhos confirmados.

## Comandos de desempenho

```
/portperf boost observe
/portperf boost auto
/portperf boost off
/portperf boost reset
/portperf boost status
/portperf boost target 60
/portperf boost chunks on
/portperf boost chunks off
/portperf boost entities on
/portperf boost entities off
```

O booster começa desligado. `auto` ativa a otimização contínua. Chunks ficam habilitados no perfil seguro, e entidades somente após optar explicitamente. O ajuste de chunks depende do FPS e pode reduzir a distância visível do cenário; quando a taxa estabiliza, o mod tenta recuperar qualidade aos poucos.

## HUD editável

```
/portperf hud edit
/portperf hud scale 125
/portperf hud reset
/portperf hud on
/portperf hud off
```

No editor, **arraste o quadro** para mover, **arraste o canto inferior direito** para dimensionar ou use `+`, `-` e a roda do mouse. Setas movimentam 1 pixel e Shift + setas movimentam 10. ESC salva. Posição e escala são salvas em `config/port-performance-monitor.properties`.

## Site (opcional)

```
/portperf key
/portperf stream on
/portperf webcontrol on
/portperf stream off
/portperf resetkey
/portperf status
```

O site recebe FPS, ping, memória e estado do booster, incluindo p95 e 1% low medidos. O envio HTTPS e o controle remoto permanecem **desligados após cada inicialização** e exigem autorização local. Guarde a chave em segredo.

## Limitações e teste recomendado

Isto não é um profiler completo de CPU ou GPU e não altera o TPS do servidor. Mudanças de cenário podem distorcer a comparação antes/depois. Em locais onde a queda é causada por CPU de simulação, renderizadores de outros mods, rede ou servidor, a redução de chunks pode não resolver o problema e será revertida quando não houver melhora consistente. Compare FPS médio, 1% low e p95 em spawn povoado, combates com muitas magias e bases Create/Mekanism. Confira se o braço/armaduras do Armourer's Workshop e os efeitos do Iron's Spells continuam normais.

Build: Java 21, `gradle -p performance-mod build --no-daemon`. Testes web: `node --test performance-web/tests/*.test.js`.
