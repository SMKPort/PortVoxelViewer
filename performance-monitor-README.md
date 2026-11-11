# Port Performance Monitor 🎮

Projeto totalmente separado do Nexus Aterna: um mod client-side NeoForge 1.21.1 e um site próprio.

**Site:** https://port-performance-monitor.onrender.com

**Código:** https://github.com/SMKPort/PortVoxelViewer/tree/port-performance-monitor/performance-mod

## Instalação

1. Baixe o JAR `portperf-1.0.0.jar` no artefato da compilação do GitHub Actions: https://github.com/SMKPort/PortVoxelViewer/actions/runs/43111169682
2. Coloque na pasta `mods` de Minecraft Java **1.21.1 com NeoForge 21.1.x**.
3. Inicie o jogo. O HUD exibirá FPS, ping e memória Java.
4. Digite `/portperf key`. Uma chave privada será mostrada somente no seu Minecraft.
5. Cole a chave no site. É possível selecionar a opção de lembrá-la no navegador.
6. Digite `/portperf stream on` no Minecraft para autorizar a transmissão HTTPS.
7. Para interromper, use `/portperf stream off`. Por segurança, o streaming não é reativado automaticamente após reiniciar o jogo.

## Comandos do mod

- `/portperf key`: mostra a chave de pareamento
- `/portperf stream on`: ativa a transmissão ao site
- `/portperf stream off`: interrompe a transmissão
- `/portperf hud on` e `/portperf hud off`: mostram ou ocultam o HUD
- `/portperf status`: FPS, ping, memória e estado HTTPS
- `/portperf resetkey`: gera outra chave e desliga o streaming

## Métricas

O mod lê FPS do Minecraft, latência (ping da lista de jogadores), e heap usada, alocada e máxima da JVM. O site mostra dados atuais e histórico com gráficos.

O tempo de frame mostrado é **1000/FPS**, apenas estimativa matemática. Não corresponde a uma medição real de frametime. A versão 1.0 não mede CPU, GPU, TPS real do servidor, nem FPS 1% low.

## Segurança e privacidade

A chave hexadecimal de 32 caracteres é aleatória e gerada no computador do jogador. O site não requer a chave da Render, login do Nexus ou RCON. O servidor web usa apenas um hash SHA-256 para associar os dados recebidos a quem conhece a chave. **Quem obtiver a chave pode acessar as suas métricas**, então não compartilhe essa informação. Para revogar o acesso, use `/portperf resetkey`.

No Minecraft, o mod a salva em `config/port-performance-monitor.properties`. A chave aparece apenas em mensagens locais do cliente. O website pode lembrá-la no navegador mediante autorização. No servidor Render, o histórico fica em RAM, sem banco de dados, com limite de 720 amostras e expiração após seis horas. Reinícios limpam os históricos.

O streaming começa desligado e sempre volta a ficar desligado quando o jogo reinicia. Não envia conversa, posição do personagem, arquivos ou credenciais do Nexus. Não precisa de instalação no servidor Minecraft nem privilégios administrativos.

## Estrutura

- `performance-mod/`: código do mod NeoForge e build Gradle
- `performance-web/`: site responsivo, API Node.js e testes
- `.github/workflows/port-performance-build.yml`: build automatizado do JAR e testes de integração

## Compilar

Use Java 21 e Gradle 9.2.1. O comando é:

`gradle -p performance-mod build --no-daemon`

O JAR fica em `performance-mod/build/libs/portperf-1.0.0.jar`.

O site é executado com `node performance-web/server.js`. Foi criado um serviço próprio na Render, com branch `port-performance-monitor` e domínio https://port-performance-monitor.onrender.com. Nenhum serviço do Nexus foi modificado.

A integração precisa de teste final dentro do modpack real para validar compatibilidade com os demais mods.
