# Nexus Aterna RP

**Novo painel web separado do Nexus Aterna Web 2.0.**

- Site próprio: https://nexus-aterna-rp.onrender.com
- Projeto: `nexus-rp/`
- GitHub: branch `nexus-aterna-rp`
- Render: serviço Node `nexus-aterna-rp`, hospedado com HTTPS

Este projeto não modifica os arquivos do antigo `nexus-web/` nem o serviço `nexus-aterna-web`.

## Funções do painel

O Nexus RP foi desenhado para Minecraft narrativo, com cinco abas:

1. **Núcleo:** estado atual, registros, energia e integridade (valores de interpretação).
2. **Comunicador:** caixa de mensagens e mensagens rápidas com envio ao chat do Minecraft Java via RCON, usando `tellraw`. O prefixo e a cor são configuráveis.
3. **Armadura Arcana:** animação visual de integrar/desintegrar placas, remodelagem de formas e reparo narrativo. Pode enviar narração ao chat e executar funções de um datapack.
4. **Rituais:** rituais canônicos e personalizados com consumo ficcional de energia, resultados e mensagens.
5. **Configurações:** host, porta, senha RCON, código do painel, destino global/individual, cor, prefixo, automações e IDs de funções.

Os dados narrativos (energia, integridade, registros e configurações não secretas) ficam no armazenamento local do navegador. Senhas não são salvas no armazenamento local nem no backup.

## Configuração do servidor Minecraft Java

**Pré-requisito:** servidor Java Edition com RCON habilitado. Por exemplo, em `server.properties`:

```properties
enable-rcon=true
rcon.port=25575
rcon.password=COLOQUE_UMA_SENHA_FORTE_AQUI
broadcast-rcon-to-ops=false
```

Reinicie o servidor para aplicar as alterações. Tenha cuidado com firewall e restrições de rede.

No **Nexus RP > Configurações**, informe:

- Host público acessível pela Render (exemplo `mc.seudominio.com`, sem `https://`)
- Porta RCON, normalmente `25575`
- Senha `rcon.password`
- Código de acesso da ponte HTTPS, configurado em **Render > nexus-aterna-rp > Environment > NEXUS_RP_ACCESS_KEY**
- Prefixo, cor e destino do chat

Use **Testar RCON** antes de transmitir mensagens. Ao enviar, o backend valida o código e conecta por TCP ao servidor RCON, executando **somente** o comando `tellraw`, `list` ou uma `function namespace:nome` autorizada pela interface.

### Segurança de rede

**RCON não cifra o próprio tráfego TCP**. Idealmente, hospede a ponte perto do servidor ou use um túnel privado/VPN adequado e regras de firewall. A ponte Render exige host com DNS/IP público e rejeita endereços privados/loopback, portanto **não conecta diretamente a um Minecraft acessível somente na LAN doméstica**. Não abra a porta RCON ao mundo sem uma estratégia de segurança. O código HTTPS protege o endpoint, mas não substitui o firewall de rede do Minecraft.

Para um servidor sem RCON exposto, uma extensão de servidor com conexão HTTPS de saída é a alternativa mais segura para desenvolvimento futuro.

### Funções de jogo opcionais

As mensagens no chat funcionam sem datapack. Para acrescentar sons/partículas, instale um datapack compatível com **Minecraft Java 1.21.1** e configure:

- Integrar: `nexus:armadura/integrar`
- Desintegrar: `nexus:armadura/desintegrar`
- Remodelar: `nexus:armadura/remodelar`
- Ritual: `nexus:rituais/ativar`

Um datapack ilustrativo ZIP foi fornecido na conversa. Ele usa `pack_format:48` e o diretório singular `data/nexus/function/` conforme o Minecraft 1.21.1. Marque o personagem no servidor com `/tag SeuNick add nexus_port`.

**Limitação:** comandos vanilla só podem produzir efeitos que o servidor suporta, como partículas, sons e textos. Não animam automaticamente placas de modelos personalizados no Armourer's Workshop ou Blockbench. Isso precisa de mod/extensão integrada.

## Hospedagem e desenvolvimento

Render Web Service Node >=20. Não são necessárias dependências npm.

- Build: `node --check nexus-rp/server.js && node --check nexus-rp/public/views.js && node --check nexus-rp/public/app.js`
- Start: `node nexus-rp/server.js`
- Environment: `NEXUS_RP_ACCESS_KEY` (código privado de 20+ caracteres)
- Health: `GET /api/health`

Acessos reais usam HTTPS do navegador até a ponte. A comunicação da ponte com o RCON é TCP conforme o protocolo do Minecraft.

## Backup e privacidade

Os registros narrativos são locais ao navegador e podem ser exportados via JSON em Configurações. Não existe sincronização de dados entre aparelhos nesta versão. Informações de conexão (host, porta e preferências) são salvas localmente. **Senha RCON e código da ponte ficam apenas na memória da aba**, desaparecendo ao recarregar.

## Verificações

Os scripts JavaScript passam por validação sintática. O deployment é confirmado pela Render após o build. A conexão efetiva RCON, os datapacks e as funções do servidor exigem testes no mundo do usuário depois de fornecer as credenciais e instalar os componentes correspondentes.

## Novidades da versão RP 1.1

A nova atualização mantém todas as funcionalidades e cinco abas originais. Acrescenta **RP ao Vivo** e **Protocolo TS** no menu, além de novos recursos integrados às abas existentes. Não há Narrador Arcano automático.

- **Armadura por segmentos:** ombreira, braço superior, articulação do cotovelo, antebraço e manopla com integridade individual, dano, reparo e acoplamento. A integridade geral é recalculada pela média. As placas desprendidas aparecem separadas na representação visual.
- **Energia dinâmica e alertas:** ações consomem ou restauram energia narrativa, e o sistema registra avisos em níveis críticos de energia e integridade. O modo Ao Vivo inclui recuperação de energia.
- **Ritual em quatro fases:** preparação, canalização, estabilização e conclusão. O custo é aplicado na conclusão. Ações podem anunciar cada etapa via chat se a conexão RCON e os anúncios automáticos estiverem ativos. Cancelar antes da conclusão não consome energia.
- **RP ao Vivo:** tela compacta para celular com envio direto ao chat, botões de armadura, energia, status e acesso rápido aos rituais.
- **Eventos TS:** uma campanha de cinco fragmentos narrativos controlados pelo operador. Desativada automaticamente por padrão. Quando o aviso por ações é habilitado, a cada quatro ações relevantes surge um fragmento *pendente*, que só é revelado ao entrar na tela e autorizar.
- **Funções de segmento opcionais:** em Configurações > Integração de segmentos, habilite e defina um prefixo como `nexus:segmentos`. O Nexus tenta executar `function nexus:segmentos/<segmento>/<acao>`, para `ombreira`, `braco`, `cotovelo`, `antebraco` ou `manopla`, com `integrar`, `desintegrar`, `danificar` e `reparar`. Isso só tem efeito no jogo se o datapack ou mod implementar as funções.

Existe um **datapack opcional 1.1** para Minecraft Java 1.21.1, com as 20 funções segmentadas e as quatro funções globais originais. Os efeitos vanilla são partículas, sons e mensagens. A movimentação real de modelos Blockbench / Armourer's Workshop exige mod específico.

### Código HTTPS e segurança

O código de acesso anteriormente compartilhado foi rotacionado. Ele **não funciona mais**. Abra o serviço `nexus-aterna-rp` na Render, vá em Environment, configure um valor forte de sua preferência em `NEXUS_RP_ACCESS_KEY` e use exatamente esse mesmo código em Configurações do Nexus RP. Não compartilhe o código nem a senha RCON no chat. A senha RCON não deve ser exposta publicamente. Use firewall e túnel privado sempre que possível.

---

## Atualização 1.2: configuração de armaduras e eventos

A versão RP 1.2 mantém todas as telas anteriores e adiciona:

- **Perfis personalizados:** Celestial, Combate, Ritual e Repouso, além de perfis novos criados pelo operador. Cada um tem custo de remodelagem, bônus narrativo de Holy Spellpower, estilo e cor.
- **Sequenciador:** etapas de desintegração, remodelagem, integração, reparo, recuperação de energia, emote ou espera, com tempos editáveis. Só começa quando o operador clicar em Executar, com cancelamento manual.
- **Emergência:** limiar configurável de integridade, bloqueio temporário de integrações/remodelagens, recuperação e liberação manual.
- **Emotes:** mapeamento opcional para funções namespaced de um datapack/mod. O painel solicita a execução no servidor, mas não garante animações nativas do Armourer's Workshop sem adaptador.
- **Eventos Minecraft:** dano, cura, morte, renascimento e efeitos aproximados pelo agente RCON local. A aplicação reage a eventos do personagem e atualiza os segmentos/indicadores narrativos. O usuário precisa habilitar a função, que fica desligada por padrão.

### Agente HTTPS local (recomendado)

O novo módulo `nexus-rp/agent/bridge_agent.py` resolve o problema de expor uma porta RCON pública. Ele consulta o RCON em `localhost` e faz apenas requisições **de saída** ao serviço Render. O site disponibiliza a opção `AGENTE HTTPS LOCAL` em Configurações. As mensagens e funções são enfileiradas até o agente confirmar a execução. Para instruções completas, consulte [agent/README.md](agent/README.md).

Na Render, a variável privada `NEXUS_RP_AGENT_KEY` foi configurada para autenticar o agente e é diferente de `NEXUS_RP_ACCESS_KEY`, usada no painel. O segredo do agente não deve ser colocado no JavaScript público nem no GitHub.

### Eventos, limites e segurança

O agente RCON consulta periodicamente a vida do jogador para inferir dano e cura. Essas estimativas não são equivalentes a eventos NeoForge de baixo nível e podem perder oscilações rápidas, especialmente entre duas leituras. Detectores de efeitos dependem do NBT exposto pelo servidor. Para cobertura exata seria necessário um mod NeoForge específico.

O bônus de Holy Spellpower, a energia arcana e a integridade do Nexus são **atributos narrativos**, não medições do Minecraft. O usuário pode personalizá-los conforme as regras do RPG. Registros ficam no navegador; o histórico de eventos da ponte é temporário e pode ser perdido quando a Render reiniciar.

Para animações reais com Blockbench ou Armourer's Workshop, instale/implemente o datapack ou mod que responda aos comandos configurados. A interface não altera automaticamente a malha 3D no cliente Minecraft.

---

## Nexus Aterna Client (versão RP 1.3)

Para jogadores **sem acesso administrativo ao servidor**, o Nexus pode se conectar através de um mod **Client-Side NeoForge 1.21.1**. Isso dispensa RCON público, agente Python no servidor e acesso aos arquivos da hospedagem.

O projeto está em [../nexus-client/README.md](../nexus-client/README.md) e é compilado pelo workflow `.github/workflows/nexus-client-build.yml`. O JAR `nexusclient-1.0.0.jar` pode ser baixado do GitHub Actions.

Abra o painel web em Configurações e escolha **Nexus Client (mod apenas no jogador)**. Em seguida, abra a aba Nexus Client e gere um código temporário. Execute no Minecraft:

```text
/nexusclient pair CODIGO
/nexusclient chat on
/nexusclient telemetry on
```

O mod transmite mensagens como a **própria conta do jogador**, respeitando as restrições do chat. Monitora vida, dano, cura, morte e mudanças em efeitos ativos quando telemetria estiver autorizada. As ações de armadura, emotes e rituais produzem avisos e partículas **somente no cliente**. Modelos 3D do Armourer's Workshop não são alterados sem um adaptador próprio.

O Nexus usa pareamento temporário e uma credencial privada no cliente para solicitar ações via HTTPS. O mod bloqueia o envio remoto de mensagens até o jogador autorizar explicitamente. Credenciais de pareamento não são armazenadas no código fonte nem expostas em páginas públicas. Um novo pareamento pode ser necessário após reinicialização do serviço Render, porque o estado da sessão é temporário.

Para instalação completa, consulte [Nexus Client README](../nexus-client/README.md).

## Canais de chat no modo Nexus Client (RP 1.4)

A opção **Local (/l)** é o padrão para o servidor de roleplay. O Comunicador não precisa digitar `/l`: o mod chama a API `ClientPacketListener.sendCommand("l " + mensagem)` diretamente, sem simulação de teclado. Também é possível selecionar **Global** ou um comando **Personalizado** permitido (somente canais de chat, não comandos de administração). O prefixo do Nexus é opcional no modo cliente. O JAR mínimo é Nexus Client 1.1.0.

## Formatação local automática (RP 1.5)

Por padrão, o canal **Local (/l)** acrescenta `&6&l` imediatamente antes da mensagem para solicitar **dourado e negrito** ao plugin de chat do servidor. O mod passa o comando ao Minecraft como `/l &6&lMensagem`, sem emular teclas. No painel, Configurações > Comunicador do Jogador, você pode editar os códigos ou deixá-los vazios. O recurso exige o JAR Nexus Client **1.2.0**. Se o plugin de chat não interpretar códigos `&`, a formatação pode não aparecer.


## Comunicador automático em modo técnico (RP 1.6)

As ações do Nexus enviam agora **somente mensagens curtas de sistema**, sem descrições em terceira pessoa: `Desintegrando armadura`, `Integrando armadura`, `Remodelando armadura: Celestial`, `Reparando armadura` e `Executando ritual: Bênção Divina`. Os segmentos individuais e as fases dos rituais também usam estados objetivos como `Reparando segmento: Ombreira` e `Canalizando ritual: ...`. Mensagens escritas manualmente pelo operador não são alteradas. O chat continua usando `/l &6&l` como canal/estilo padrão no **Nexus Client 1.2.0**. Não é preciso gerar um novo JAR: esta alteração é exclusivamente do painel web.


## Mensagens com identificação do Nexus (RP 1.7)

Todas as mensagens que saem do painel pelo modo Nexus Client são identificadas com **`[Nexus Aterna]`**, mesmo se o navegador tiver salvo a configuração antiga com prefixo desligado. Exemplo de comando transmitido: `/l &6&l[Nexus Aterna] Desintegrando armadura`. A identificação é aplicada uma única vez, incluindo mensagens digitadas manualmente. O servidor Minecraft ainda identifica tecnicamente a conta do jogador que envia a mensagem, pois o Nexus Client usa a conexão do próprio jogador.

A opção de ocultar a identificação foi desativada apenas no modo Nexus Client para evitar mensagens confundidas com falas do Port. O campo do prefixo aparece somente para consulta. Os avisos de armadura e rituais permanecem objetivos, no estilo de sistema. **Nenhum JAR novo é necessário:** a mudança foi implementada exclusivamente no painel e na ponte HTTPS, compatíveis com Nexus Client 1.2.0.


## RP 1.8 | Nexus Client sem senha RCON

Por padrão, o painel começa em **Nexus Client**. Ao entrar na aba Nexus Client, a conexão é ajustada automaticamente para o modo do jogador; perfis antigos configurados em RCON direto sem host são migrados para Nexus Client. A tela Configurações agora mostra apenas **NEXUS_RP_ACCESS_KEY** e preferências de chat quando o modo Client é escolhido, ocultando host, porta e senha RCON.

Procedimento: na Render, encontre o valor configurado em `NEXUS_RP_ACCESS_KEY` (não confunda com `NEXUS_RP_AGENT_KEY`), entre em **Configurações**, informe em **Código de acesso HTTPS do painel**, clique em **Salvar código e configurações**, abra a aba **Nexus Client** e toque em **Gerar código de pareamento**. No Minecraft instalado com Nexus Client 1.2.0, use `/nexusclient pair CODIGO` e autorize chat e telemetria separadamente. O código HTTPS fica só em memória na aba, não na biblioteca/localStorage. Não é necessário senha RCON nem acesso ao servidor Minecraft. O teste de conexão apenas confirma se o Minecraft já está pareado e online.

O painel agora inclui teste de regressão para impedir que o modo Client volte a exigir RCON. Os scripts possuem marcação de versão na URL para atualizar corretamente em navegadores de celular.


## RP 2.1 · lembrar chave HTTPS apenas no dispositivo

No modo Nexus Client, a chave `NEXUS_RP_ACCESS_KEY` da Render continua privada e **nunca é enviada embutida no HTML nem no código-fonte público**. Para evitar ter de digitá-la em cada visita, marque **Lembrar a chave HTTPS apenas neste dispositivo** ao informá-la pela primeira vez. A partir da próxima visita, ela é recuperada do armazenamento local desse navegador para autorizar as ações da ponte. O campo de senha fica vazio para não exibir o segredo no HTML. O status **Chave lembrada neste navegador** informa que está pronta. É possível revogar o armazenamento pelo botão **Esquecer chave salva**. Em um dispositivo compartilhado, mantenha a opção desmarcada.

Isso não dispensa a autenticação inicial: um website público não pode preencher de maneira segura o valor secreto da variável de ambiente da Render para qualquer visitante. A chave também continua separada do código de pareamento de dez caracteres usado dentro do Minecraft. Nenhuma modificação do JAR NeoForge foi necessária.


## Microfone para chat (Nexus RP 2.2)

A nova aba **Voz do Nexus** usa `SpeechRecognition` (Web Speech API) no navegador. Funciona sem reinstalar o mod cliente 1.3.0, pois usa a fila HTTPS existente. Em alguns navegadores, como Chrome, o processamento de áudio ocorre no serviço de reconhecimento do próprio navegador, portanto requer conexão com a internet e **não é reconhecimento offline privado**. O painel não armazena gravações nem grava arquivos de áudio.

1. Instale e pareie o Nexus Client. Execute `/nexusclient chat on` no Minecraft.
2. Abra o painel HTTPS no Chrome ou Edge em um computador com microfone, informe a chave em Configurações e acesse **Voz do Nexus**.
3. Clique em **ATIVAR MICROFONE** e permita o acesso ao microfone solicitado pelo navegador.
4. Diga **Nexus, como estamos?** ou apenas **Nexus**, seguido de uma pergunta em até dez segundos.
5. A resposta textual é colocada na fila e transmitida pelo mod via `/l &6&l[Nexus Aterna] ...`. O envio aparece como seu jogador, não como mensagem do servidor.

O microfone é desligado pelo botão **DESLIGAR** e ao fechar a aba. A página deve permanecer aberta; navegadores podem interromper reconhecimento em segundo plano. As respostas desta primeira etapa são **contextuais por regras**, não conversa aberta via IA. Vida recente só aparece se `/nexusclient telemetry on` estiver ativado e o mod enviar os eventos. Se o reconhecimento não estiver disponível, o botão de teste textual permite verificar a fila. O endpoint `/api/client/voice` exige a chave privada do painel, limita comprimento, exige a palavra de ativação e preserva o cooldown de chat do mod. Por segurança, o microfone só é habilitado para a própria origem HTTPS do painel.


## Nexus Aterna RP 2.3: conversação fluida com IA

A aba **Voz do Nexus** agora conversa livremente por Gemini, com contexto das últimas 16 falas, continuação automática por até 2 minutos depois de chamar **Nexus** (quando o modo contínuo está marcado) e respostas em até duas mensagens pelo chat local. O formato continua `/l &6&l[Nexus Aterna]`. Não são acionados comandos de servidor por fala. O Microfone depende da Web Speech API e pode ter interrupções por navegador, permissões ou segundo plano.

Na **Render > nexus-aterna-rp > Environment**, configure **NEXUS_GEMINI_API_KEY** como variável privada com sua chave do Google AI Studio. Opcionalmente ajuste **NEXUS_GEMINI_MODEL**, cujo padrão é `gemini-3.5-flash-lite`. O backend chama o endpoint fixo Gemini `generateContent`. Não coloque a chave no navegador, código-fonte, configuração do mod ou chat.

A memória da conversa é mantida na página enquanto ela estiver aberta. Para persistir entre visitas, marque **Lembrar conversa neste navegador** na aba Voz. O botão **Limpar conversa** apaga esse histórico local. O servidor do Nexus não mantém a transcrição persistentemente, mas envia à API do Gemini a pergunta e o histórico recente para gerar resposta. A telemetria só entra quando o usuário autoriza os comandos apropriados. Se o Gemini não estiver configurado ou retornar erro, o backend informa o problema, sem fingir que a resposta veio de IA.


## Interface Nexus RP 2.4: barra compacta
A navegação principal foi reduzida a **Visão geral**, **Voz do Nexus**, **Comunicador**, **Nexus Client** e **Configurações**. Todas as abas avançadas anteriores, incluindo armadura, rituais, eventos, sequenciador, protocolo TS, emotes e emergência, continuam disponíveis dentro de **Outras ferramentas**. O topo da página ganhou um atalho fixo para `#voz`, inclusive em telas estreitas. O menu lateral permite rolagem no celular. Assets do painel usam versão de cache `2.4` para invalidar scripts antigos.


## Recuperação da integridade por cura (Eventos do Jogo 2.6)

Com a opção **Permitir que eventos alterem a integridade e energia narrativas** ativada, cada evento `heal` recebido pelo painel restaura integridade física das placas danificadas em proporção à vida recuperada. O multiplicador `healIntegrity` vem em 5 pontos por ponto de cura (ajustável entre 0 e 10, inclusive 0 para desativar recuperação estrutural). As placas mais danificadas são recuperadas primeiro, respeitando o limite de 100%. A energia do RP continua recuperando o valor configurado por evento, independente do novo reparo. Uma placa previamente desintegrada não se reintegra automaticamente ao curar. Eventos de `respawn` não representam cura convencional e não restauram integridade. O painel agora mostra a integridade atual ao lado dos eventos, atualizada automaticamente.


## Monitoramento independente do painel, cliente 1.4

Com o Nexus Client 1.4 instalado e o Minecraft aberto, vá em **Eventos do Jogo**, marque **Permitir que eventos alterem a integridade e energia narrativas** e clique **Salvar preferências**. O navegador envia os fatores e, na primeira sincronização, a integridade/energia iniciais e o estado das placas ao mod. O cliente persiste tudo em `.minecraft/config/nexus-vitals.json` e aplica dano, cura e morte mesmo com o navegador fechado. Nas sincronizações posteriores, o painel trata o estado do mod como autoridade e não aplica novamente os eventos vistos no histórico. Se o mod não confirmar a configuração, a interface informa que somente o navegador foi salvo. Telemetria deve permanecer ligada com `/nexusclient telemetry on`. O Render mantém apenas uma cópia efêmera do estado, publicada regularmente pelo mod.


## Alertas do Nexus no site (painel 2.8)

Nova opção em Outras ferramentas > Alertas do Nexus, com seis chaves independentes (dano elevado, ataques consecutivos, integridade crítica, risco combinado, cura estrutural e vida crítica), intervalo mínimo entre falas de combate de 15 a 180 segundos, histórico sincronizado de até 30 mensagens realmente enviadas. O painel só aceita salvar com o mod 1.6 conectado e com sua configuração recebida. A atualização dos registros acontece automaticamente enquanto a aba está visível. O mod salva o histórico completo de até 80 falas e as preferências localmente. Não grava tokens nem falas em HTML. O backend da Render mantém somente cópia temporária.


## Audição do Nexus (client 1.7 / painel 2.9)

O mod usa a API de cliente do Plasmo Voice 2.1.8 para transcrever trechos curtos de jogadores que autorizaram explicitamente o processamento. Nenhuma captura é feita por padrão. A audição exige ativação manual em cada partida, lista individual de participantes consentidos e pareamento HTTPS. Com memória ativa, salva até 200 transcrições em `.minecraft/config/nexus-hearing-memories.json`; nenhum WAV é salvo. Pode apagar todas usando `/nexusclient hearing forget confirm`.

A página Outras ferramentas > Audição do Nexus permite ativar a audição, escolher participantes, memória e respostas. Exige confirmação de consentimento ao ativar. O site exibe só a contagem de memórias, sem copiá-las. Os trechos WAV de 16 kHz/mono são enviados temporariamente à Render para transcrição Gemini; as falas apenas são respondidas por chat se `reply on`, a personalidade e o chat estiverem ativos, e a frase começar com 'Nexus'.


## Audição contínua 1.8

O Nexus Client 1.8 remove o intervalo fixo de 8,5 s. Segmentação por atividade/silêncio em quadros de ~20 ms, trechos de no máximo 12 s com sobreposição curta, fila RAM de até 36 trechos e 12 MiB, e até 3 requisições simultâneas de ASR. A API aceita 3 transcrições paralelas por sessão e tem um teto de proteção de 90/minuto. Erros temporários podem ser tentados novamente (até duas vezes); contadores de filas, transcrições e perdas aparecem em Outras ferramentas > Audição do Nexus. O cliente não persiste WAV. Não se promete transcrição integral sem perdas quando a rede ou o Gemini estiverem indisponíveis.

Memória nova em arquivo JSONL incremental `config/nexus-hearing-memory.jsonl`, com limite de 24 MiB para não consumir o disco indefinidamente. Ao atingir esse espaço, o aplicativo avisa e não sobrescreve as memórias antigas; pode apagar todas ou por jogador. Memórias legadas do arquivo `nexus-hearing-memories.json` continuam sendo carregadas. Para responder, o Nexus seleciona localmente trechos relevantes do jogador autorizado e os envia ao Gemini como contexto, sem enviar o arquivo completo. O áudio é transmitido ao Gemini para transcrição, sujeito à política de tratamento de dados do provedor. A audição precisa ser ativada de novo a cada inicialização, e remover um participante limpa sua memória.
