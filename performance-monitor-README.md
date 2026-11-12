# Port Performance + Booster 2.0 🎮⚡

Projeto totalmente separado do Nexus Aterna: um mod integrado client-side NeoForge 1.21.1, com medição e otimização experimental reversível, além de um site próprio. O código antigo do PortClientBooster não estava acessível no GitHub conectado; por isso o motor booster foi implementado diretamente no Monitor, sem alterar o repositório original.

**Site:** https://port-performance-monitor.onrender.com

**Código:** https://github.com/SMKPort/PortVoxelViewer/tree/port-performance-monitor/performance-mod

## Instalação

1. Baixe o JAR `portperf-2.0.0.jar` no artefato da compilação do GitHub Actions: https://github.com/SMKPort/PortVoxelViewer/actions/runs/43111169682
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

## Integração Port Client Booster 2.0

O Motor Booster agora está **dentro do JAR Port Performance + Booster**. Não é necessário instalar um segundo mod para usar estas funções. Se a versão antiga do PortClientBooster estiver instalada, desative suas otimizações adaptativas ou remova o JAR antigo para evitar ajustes concorrentes.

- `/portperf boost observe`: apenas coleta e observa o FPS, sem modificar configurações.
- `/portperf boost auto`: inicia um experimento opcional. Mede **30 amostras de referência**, testa uma diminuição pequena e **reversível** na distância de visualização de entidades e coleta mais **30 amostras**. Mantém o ajuste apenas se medir pelo menos +4 FPS e +6% na média do teste; em caso contrário, restaura a configuração original.
- `/portperf boost off`: desativa o motor e restaura a opção original se foi ele que a modificou.
- `/portperf boost reset`: desfaz alterações e zera o histórico de experimentação local.
- `/portperf boost target 60`: define a meta de FPS (30 a 240).
- `/portperf boost status`: mostra o modo atual, a fase e o último resultado.
- `/portperf webcontrol on`: autoriza temporariamente que o site controle o booster. Também precisa de `/portperf stream on`. O site **não pode ativar essa autorização por conta própria**.
- `/portperf webcontrol off`: bloqueia alterações vindas do site.

**Segurança e reversão:** por padrão o motor não altera configurações. Ao iniciar o Minecraft, o booster volta desligado e a transmissão HTTPS e o controle remoto precisam de nova autorização. Se o jogo sair da sessão e detectar um ajuste anterior, procura restaurar a opção original. Há um registro persistente temporário para recuperação após uma interrupção inesperada. Se o usuário modificar manualmente a mesma opção, o motor não sobrescreve essa escolha durante a reversão.

O experimento **não altera distância de renderização de chunks, partículas, gráficos, VSync, memória, arquivos de configuração de outros mods nem configurações do servidor**. Ajusta apenas a distância de visualização de entidades, após autorização explícita. Isso pode reduzir a visibilidade de mobs e jogadores distantes. O ganho de FPS não é garantido e depende do que está visível no mundo.

A comparação **não é um benchmark controlado**: mudanças de cenário, quantidade de entidades e atividade do servidor podem influenciar os resultados. Para comparar corretamente, faça as medições em condições semelhantes. O `1% low estimado` utiliza FPS coletados uma vez por segundo, e **não substitui uma medição de frametimes reais**.

## Métricas

O mod lê FPS do Minecraft, latência (ping da lista de jogadores), e heap usada, alocada e máxima da JVM. O site mostra dados atuais e histórico com gráficos, além da fase e do resultado da otimização.

O tempo de frame mostrado é **1000/FPS**, apenas estimativa matemática. Não corresponde a uma medição real de frametime. A versão integrada mede uma estimativa de 1% low com amostras de FPS a cada segundo, mas não mede CPU, GPU, TPS real do servidor ou o verdadeiro 1% low por frametimes.

## Segurança e privacidade

A chave hexadecimal de 32 caracteres é aleatória e gerada no computador do jogador. **Quem obtiver a chave pode solicitar alterações do booster quando você tiver habilitado o controle remoto no jogo.** O site não requer a chave da Render, login do Nexus ou RCON. O servidor web usa apenas um hash SHA-256 para associar os dados recebidos a quem conhece a chave. **Quem obtiver a chave pode acessar as suas métricas**, então não compartilhe essa informação. Para revogar o acesso, use `/portperf resetkey`.

No Minecraft, o mod a salva em `config/port-performance-monitor.properties`. A chave aparece apenas em mensagens locais do cliente. O website pode lembrá-la no navegador mediante autorização. No servidor Render, o histórico fica em RAM, sem banco de dados, com limite de 720 amostras e expiração após seis horas. Reinícios limpam os históricos.

O streaming e o controle remoto começam desligados e sempre voltam a ficar desligados quando o jogo reinicia. Não envia conversa, posição do personagem, arquivos ou credenciais do Nexus. Não precisa de instalação no servidor Minecraft nem privilégios administrativos.

## Estrutura

- `performance-mod/`: código do mod NeoForge e build Gradle
- `performance-web/`: site responsivo, API Node.js e testes
- `.github/workflows/port-performance-build.yml`: build automatizado do JAR e testes de integração

## Compilar

Use Java 21 e Gradle 9.2.1. O comando é:

`gradle -p performance-mod build --no-daemon`

O JAR fica em `performance-mod/build/libs/portperf-2.0.0.jar`.

O site é executado com `node performance-web/server.js`. Foi criado um serviço próprio na Render, com branch `port-performance-monitor` e domínio https://port-performance-monitor.onrender.com. Nenhum serviço do Nexus foi modificado.

A integração precisa de teste final dentro do modpack real para validar compatibilidade com os demais mods.
