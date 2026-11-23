# Port Performance + Booster

## 3.0.0 (Minecraft 1.21.1 / NeoForge)

### Motor
- FrameTimeMonitor acompanha cada frame renderizado e calcula FPS médio, percentis p95/p99, 1% low e porcentagem de frames acima de 50 ms.
- BoosterEngine funciona continuamente em janelas de 5 s; só tenta reduzir carga após quedas persistentes de FPS.
- O primeiro ajuste reduz chunks em um por etapa, até 4 abaixo da distância inicial. Distância de entidades só com ativação explícita.
- Avaliação reversível: ganho de pelo menos 3,5% no FPS médio ou 8% em p95 mantém a alteração; caso contrário desfaz o último passo.
- Cooldowns e restauração ao sair do mundo, desligar o booster ou recuperar sessão anterior interrompida.

### HUD
- Painel de 225 x 102 unidades lógicas, com tamanho de 55% a 225%.
- Arrastar para mover. Canto inferior direito, scroll, botões +/- e comando para alterar tamanho.
- Tamanho e posição persistidos no arquivo local de propriedades.

### Integração
- API e dashboard aceitam métricas reais de frametime mantendo compatibilidade com telemetria antiga.
- Controle web opt-in e chave de acesso continuam exigidos.

### Limitações
- Somente ajustes do cliente. Não modifica TPS, CPU/GPU, renderizadores de mods, partículas de magia ou dados do servidor.
- Resultados devem ser comparados no Somake com cenas equivalentes, sem garantia de ganho.
