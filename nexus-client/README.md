# Nexus Aterna Client · NeoForge 1.21.1

**Mod Client-Side de Port Morningstar para o Nexus Aterna RP.** Pode ser instalado apenas no Minecraft do jogador, inclusive quando ele não tem acesso aos arquivos nem às permissões administrativas do servidor.

O código-fonte fica em `nexus-client/`, dentro da branch `nexus-aterna-rp`. O painel web é https://nexus-aterna-rp.onrender.com.

## Instalação

1. Use **Minecraft Java Edition 1.21.1** com **NeoForge 21.1.x**.
2. Baixe o JAR a partir do artefato do workflow **Nexus Aterna Client NeoForge 1.21.1** do GitHub Actions.
3. Coloque `nexusclient-1.2.0.jar` na pasta `.minecraft/mods/` do perfil 1.21.1.
4. Inicie o Minecraft e entre em um mundo ou servidor.
5. No painel Nexus RP (HTTPS), entre em **Configurações** e selecione **NEXUS CLIENT (MOD APENAS NO JOGADOR)**. Informe o código de acesso normal do painel e salve.
6. Abra **Nexus Client** no menu lateral. Clique em **Gerar código de pareamento**.
7. No chat do Minecraft digite o comando **local** `/nexusclient pair CODIGO` (substitua pelo código de 10 caracteres).
8. Para permitir que mensagens escritas no painel sejam realmente enviadas pelo seu jogador, digite `/nexusclient chat on`.
9. Para acompanhar vida e efeitos no Nexus, digite `/nexusclient telemetry on` e habilite Eventos do Jogo no painel.
10. Use **Testar conexão** no painel.

O servidor não precisa do mod, RCON, whitelist alterada ou conta de administrador.

## Comandos locais

| Comando | Efeito |
| --- | --- |
| `/nexusclient status` | Mostra status do vínculo e permissões |
| `/nexusclient pair CODIGO` | Pareia com o código temporário do painel |
| `/nexusclient chat on` | Autoriza o envio de mensagens pelo chat do seu jogador |
| `/nexusclient chat off` | Bloqueia novas mensagens remotas |
| `/nexusclient telemetry on` | Autoriza envio de vida, dano e IDs de efeitos ao Nexus |
| `/nexusclient telemetry off` | Desativa telemetria |
| `/nexusclient unpair` | Limpa credencial e revoga permissões locais |

Esses comandos são registrados no cliente NeoForge, **não** são executados pelo servidor Minecraft.

## Limitações honestas

- As mensagens aparecem como mensagens normais **da sua conta**, sujeitas a filtros de chat, cooldown e regras do servidor. O Nexus não se passa pelo console.
- O mod não executa comandos de administrador, não altera entidades de outros jogadores e não modifica o mundo do servidor.
- Os indicadores de energia arcana, integridade do Nexus, bônus Holy Spellpower, rituais e Protocolo TS continuam narrativos.
- Integrar/desintegrar/reparar/remodelar executa **avisos visuais e partículas locais**. Não altera, por si só, o modelo 3D do Armourer's Workshop. Para animações fiéis é necessário um adaptador para o sistema de armaduras/emotes instalado.
- A leitura de vida e efeitos é feita periodicamente no cliente (a cada três segundos), portanto impactos e curas muito rápidos podem se anular entre leituras.
- Quando o Render reiniciar, o vínculo pode precisar de novo pareamento porque a sessão do servidor é mantida em memória.
- Permissões de chat e telemetria começam desligadas no primeiro pareamento.

## Segurança e privacidade

O mod só faz requisições HTTPS ao domínio do Nexus RP. A credencial forte criada no pareamento é gravada localmente em `.minecraft/config/nexus-client.properties` e **não deve ser enviada para outras pessoas**. O servidor da Render guarda apenas o hash de validação do token. Se houver suspeita de comprometimento, use `/nexusclient unpair` e gere um novo código.

A ponte do Nexus aceita apenas mensagens curtas sem comandos `/` e ações locais permitidas. O mod também verifica uma autorização local explícita antes de enviar cada mensagem. O intervalo mínimo entre mensagens remotas é de 4,5 segundos.

## Compilação

Projeto Gradle ModDevGradle 2.x, Java 21, NeoForge 21.1.265. Use Gradle 9.2.1:

```bash
gradle -p nexus-client build --no-daemon
```

O resultado fica em `nexus-client/build/libs/nexusclient-1.2.0.jar`.

O workflow `.github/workflows/nexus-client-build.yml` compila automaticamente o mod na branch `nexus-aterna-rp` e disponibiliza o artefato ZIP com JAR após finalizar.

O JAR foi compilado no GitHub Actions. Ainda é recomendado testar no seu modpack específico para identificar conflitos de outros mods ou configurações do servidor.

## Canais de chat (Nexus Client 1.1.0)

No painel, vá em **Configurações > Comunicador do Jogador > Canal das Mensagens** e escolha **Local (/l)**, que é o padrão. Ao escrever `As placas estão se integrando.`, o mod chama a API do Minecraft para enviar `/l As placas estão se integrando.` usando `sendCommand`. **Não abre o chat, não pressiona T e não simula digitação**.

Também há **Global** (mensagem direta pelo `sendChat`) e **Personalizado**, que aceita apenas comandos de chat seguros listados na interface, como `/rp` e `/me`. Escreva apenas a mensagem no painel, sem barra inicial. O prefixo `[NEXUS ATERNA]` é opcional e, no modo cliente, fica desmarcado inicialmente.

O comando `/l` deve existir no servidor e respeita as regras do plugin de chat local. O resultado no jogo deve ser testado no seu servidor. Para aplicar a novidade, **substitua o JAR antigo pelo Nexus Client 1.1.0** e reinicie o Minecraft.


## Estilo de texto local /l (Nexus Client 1.2.0)

Por padrão, todas as mensagens do canal **Local (/l)** recebem os códigos literais **`&6&l`** imediatamente antes do conteúdo. `&6` indica dourado e `&l` indica negrito em plugins de chat que reconheçam essa sintaxe. O mod envia o comando pela API do Minecraft, por exemplo:

```text
/l &6&lAs placas do Nexus começam a se integrar.
```

No Nexus RP, em Configurações, escolha **Nexus Client** e **Local (/l)**. O campo **Códigos de formatação do chat local** vem preenchido com `&6&l`. É possível alterá-lo para outros códigos da família `&`, ou esvaziá-lo para não aplicar formatação. Não é necessário escrever o código manualmente no Comunicador.

O servidor precisa permitir a interpretação dos códigos `&` no comando `/l`. Se o plugin remover, bloquear ou ignorar essa formatação, as mensagens podem aparecer com o código literal ou sem cor. Os códigos não dão permissões especiais ao jogador.


## Consciência evolutiva (Nexus Client 1.3.0)

Esta implementação cria uma **personalidade narrativa simulada**, com progressão por eventos reais do cliente, e não uma IA consciente. Os cinco estágios são: Protocolo, Observação, Adaptação, Identidade e Individuação. O modelo registra experiências de dano, morte, recuperação e exploração, ajustando cautela, vínculo e curiosidade.

- `/nexusclient mind on`: inicia observação **somente local**.
- `/nexusclient mind status`: mostra estágio, experiência e tendências.
- `/nexusclient mind memories`: mostra as cinco memórias recentes.
- `/nexusclient mind off`: para observação e desliga mensagens espontâneas, sem apagar memórias.
- `/nexusclient mind reset confirm`: apaga a memória narrativa local, mediante comando explícito.
- `/nexusclient autonomy on`: permite que o Nexus envie frases próprias, apenas se `mind on` e `chat on` já estiverem autorizados.
- `/nexusclient autonomy off`: interrompe as falas espontâneas.

Memórias persistem no arquivo local `.minecraft/config/nexus-consciousness.json`. Não são enviadas à Render. O arquivo usa gravação atômica quando suportada, mantém até 40 marcos e não guarda coordenadas exatas ou nomes de outros jogadores.

**Segurança:** o Nexus só publica fala espontânea no canal local permitido pelo jogador, usando a mesma rotina protegida de chat (prefixo `[Nexus Aterna]`, código `&6&l`, limite de mensagens e sem comandos administrativos). Se o canal /l não existir no servidor, a mensagem pode falhar. A evolução continua offline, mesmo sem pareamento; a geração de textos nesta fase usa regras de personalidade, não um modelo de IA externo.


## Nexus Client 1.3.1: consciência conectada à conversa livre

A interface do Nexus no Render recebe **somente** o estágio da personalidade (`PROTOCOLO`, `OBSERVAÇÃO`, `ADAPTAÇÃO`, `IDENTIDADE`, `INDIVIDUAÇÃO`) e os valores agregados de experiência, cautela, vínculo e curiosidade. O envio ocorre aproximadamente a cada 30 segundos **apenas quando ambos** `/nexusclient mind on` e `/nexusclient telemetry on` estão ativos, além do pareamento HTTPS.

As memórias narrativas locais permanecem em `nexus-consciousness.json`; o conteúdo completo delas não é enviado. Para conversar livremente pelo microfone, abra a aba **Voz do Nexus** no painel Render e configure uma chave Gemini no backend. O formato de chat `/l &6&l[Nexus Aterna]` e a necessidade de `/nexusclient chat on` permanecem os mesmos. Substitua o JAR antigo pelo **1.3.1** para disponibilizar contexto evolutivo na IA.


## Nexus Client 1.4.0: telemetria independente do navegador

O Nexus processa localmente dano, regeneração e morte mesmo quando nenhuma página do painel está aberta. O novo arquivo `.minecraft/config/nexus-vitals.json` persiste integridade média, integridade e integração de cada placa, energia, parâmetros de dano/cura e cursor de placas. O cálculo local não depende de conexão com o Render e **nunca modifica vida real do jogador nem o mundo do servidor**. Ao reabrir o painel, os valores locais são transmitidos pelo HTTPS e atualizados na interface.

**Instalação e ativação:** substitua o JAR do Nexus Client pela versão 1.4.0, tenha `/nexusclient telemetry on`, mantenha o Minecraft aberto e vinculado. Na aba **Eventos do Jogo**, marque as alterações narrativas e clique **Salvar preferências** com o cliente conectado. Essa configuração é enviada uma vez ao mod, que a guarda no arquivo local mesmo após fechar a aba. A configuração inicial copia os valores do painel apenas uma vez; nos salvamentos seguintes, muda apenas fatores e autorização para evitar substituir o progresso obtido enquanto o painel esteve fechado. O estado local continua sendo o mesmo ao reiniciar a Render. O painel vê o estado mais recente quando há conexão.

O reconhecimento do Minecraft ocorre a cada aproximadamente 3 segundos. Eventos de vida muito rápidos entre amostras podem não ser observados. Fechar o Minecraft interrompe novas leituras. `telemetry off` também interrompe a coleta; não há leitura à distância do jogo fechado. A sincronização da interface usa os dados enviados pelo mod e não reaplica dano e cura já processados por ele.


## Nexus Client 1.5.0: alertas de combate no chat local

Com os comandos \`/nexusclient telemetry on\`, \`/nexusclient mind on\`, \`/nexusclient chat on\` e \`/nexusclient autonomy on\` autorizados, o Nexus reage automaticamente a danos de **4 pontos de vida ou pelo menos 20% da vida máxima** em uma amostra, **3 leituras de dano consecutivas em até 14 segundos**, **integridade de placas cruzando 25%**, situação combinada de **vida em 31–40% e integridade até 45%**, e regeneração acumulada de pelo menos **5 pontos percentuais de integridade em até 25 segundos**. As mensagens refletem o estágio mental atual (Protocolo a Individuação), com tom gradualmente mais pessoal. Nenhuma frase depende da interface web, do Gemini ou do Render. O Minecraft precisa continuar aberto e, para alertas estruturais, a integração narrativa de Eventos do Jogo precisa estar ativa no cliente.

Os alertas possuem **intervalo mínimo global de 45 segundos** entre mensagens desse sistema, além da proteção do chat existente (4,5 segundos). São limitados a **/l &6&l[Nexus Aterna]**, enviados pela própria conta do usuário com consentimento, sem privilégios de servidor. Avisos de vida crítica antigos da Nexus Mind continuam disponíveis. As leituras da vida ocorrem aproximadamente a cada 3 segundos e podem não detectar trocas muito rápidas de dano e cura. Para interromper a fala, use \`/nexusclient autonomy off\`.


## Nexus Client 1.6.0: alertas configuráveis pelo site

Em Outras ferramentas > Alertas do Nexus, escolha as categorias de alertas (dano forte, ataques seguidos, integridade crítica, risco conjunto, cura estrutural e vida crítica) e o intervalo de 15 a 180 segundos entre avisos de combate. Salvar envia a configuração autenticada para o mod e grava em .minecraft/config/nexus-alerts.json. As mensagens só são enviadas com chat, mente, telemetria e autonomia autorizados. A configuração funciona com o navegador fechado.

Toda mensagem espontânea realmente transmitida pelo canal /l &6&l[Nexus Aterna] é registrada em .minecraft/config/nexus-alert-history.json (até 80 entradas). As últimas 30 são sincronizadas periodicamente com o painel e exibidas na interface. A Render mantém apenas uma cópia temporária, e a fonte de verdade fica no computador com Minecraft. Falas de evolução da mente também são incluídas no histórico.


## Nexus Client 1.7.0: Plasmo Voice e memória textual

Adicione jogadores **somente depois de obter consentimento explícito**. Comandos: `/nexusclient hearing allow NICK`, `hearing deny NICK`, `hearing on/off`, `hearing memory on/off`, `hearing reply on/off`, `hearing status`, `hearing memories`, `hearing forget confirm`. A audição começa desativada em cada sessão. Requer Plasmo Voice 2.1.8+, pareamento HTTPS, Gemini configurado na Render e mod cliente. Os dados de voz enviados para processamento são descartados após transcrição e não há arquivo de áudio. As transcrições persistentes opcionais ficam somente em `config/nexus-hearing-memories.json`, limite 200. Na página Audição do Nexus há também controles para essa configuração, sempre exigindo confirmação de consentimento.


## Nexus Client 1.8: transcrição contínua

A captura de voz autorizada passa por VAD adaptativo, detecta pausas e divide falas longas em trechos de até 12 segundos. Até 3 transcrições podem ocorrer em paralelo, com 36 trechos / 12 MiB na fila RAM. Não existe mais o antigo intervalo mínimo de 8,5 segundos. O painel exibe contadores de trechos transcritos, aguardando, reenviados e descartados. Cada jogador precisa consentir previamente; o áudio não é salvo pelo mod. A memória textual é escrita incrementalmente no arquivo `config/nexus-hearing-memory.jsonl` com limite de 24 MiB, preservando entradas do arquivo legado. O reconhecimento requer a chave Gemini e conectividade. Para remover um jogador e suas memórias, use `/nexusclient hearing deny NICK` ou desative a audição com `/nexusclient hearing off`.


## Nexus Mind 2.0.0: memória social, proteção, diário e voz local

Nexus Mind 2.0 diferencia transcrições históricas de lembranças derivadas (promessas, projetos, acontecimentos e lore). Cada fala é atribuída a quem consentiu com o processamento, sem transformar declarações em fatos verificados. O histórico cronológico de turnos dá contexto às respostas. Familiaridade e afinidade são índices narrativos de RP, não diagnósticos de pessoas reais. O diário produz entradas atribuídas a acontecimentos observados e aos estágios evolutivos.

Proteção contextual: se um jogador autorizado mencionar perigo e Port sofrer dano confirmado com menos de 52% de vida, o Nexus poderá emitir um aviso de RP, respeitando o controle de autonomia e os cooldowns.

Comandos do Minecraft:
- /nexusclient social status e /nexusclient social diary: status e diário.
- /nexusclient social protection on ou off: proteção contextual.
- /nexusclient social share on ou off: exibição opcional de memórias derivadas no painel privado. Desligado por padrão.
- /nexusclient social forget confirm: apaga apenas as memórias derivadas.
- /nexusclient hearing deny NICK: revoga autorização e apaga transcrições e lembranças do jogador.
- /nexusclient hearing forget confirm: apaga o histórico completo de audição e memória social.
- /nexusclient voice on, off ou status: controle da voz local.

Arquivos no computador: config/nexus-mind-social.json (memórias derivadas), config/nexus-hearing-memory.jsonl (transcrições de voz) e config/nexus-speaker.json (voz local). Em Outras ferramentas > Nexus Mind 2.0 é possível corrigir e apagar lembranças derivadas, apagar dados por jogador e autorizar a visualização das memórias no painel.

Ao corrigir uma lembrança, o texto histórico original deixa de ser usado como contexto da IA, mas a transcrição original permanece até ser apagada por jogador ou por limpeza total.

Voz própria: Gemini TTS opcional, 24 kHz/16 bits PCM, reprodução pelo dispositivo de áudio do computador com Java Sound. Os cinco estágios usam Charon, Orus, Puck, Kore e Algenib, com seleção de voz fixa disponível. Voz desativada por padrão. A voz ainda NÃO é transmitida aos outros jogadores pelo Plasmo Voice. A Render precisa de NEXUS_GEMINI_API_KEY ou GEMINI_API_KEY. O modelo TTS pode ser escolhido com NEXUS_GEMINI_TTS_MODEL (padrão: gemini-2.5-flash-preview-tts). A disponibilidade e os limites dependem do provedor. O cliente não armazena PCM gerado nem áudio de outros jogadores.

O mod continua exclusivamente cliente para Minecraft 1.21.1 NeoForge. Voz de terceiros só é capturada com autorização prévia, e o sistema exige ativação novamente ao iniciar cada partida. A captura de áudio do Plasmo Voice e a saída local precisam ser testadas no jogo com dispositivos reais.


## Nexus Client 2.0.1: seletor @a de audição consentida

O comando `/nexusclient hearing allow @a` seleciona automaticamente os jogadores **online que já deram consentimento e foram cadastrados** com `/nexusclient hearing allow Nick`. Ele **não** registra jogadores novos sem autorização e não precisa de permissão do servidor. Depois de selecionar, use `/nexusclient hearing on` para começar (a ativação continua manual a cada jogo). Quem entrar depois será considerado automaticamente se estiver na lista de consentimento. Sem lista de jogadores online disponível, o seletor bloqueia por segurança.

Para interromper imediatamente toda audição: `/nexusclient hearing deny @a`. O comando desativa a audição, limpa os trechos em memória e preserva o cadastro de consentimento. Para revogar a autorização e apagar as memórias de uma pessoa use `/nexusclient hearing deny Nick`. O painel Audição do Nexus também permite marcar o seletor @a e ver quantos autorizados estão online. `/nexusclient hearing status` mostra o estado atual.


## Nexus Arcane Sensor 2.1.0 (Iron's Spells n Spellbooks)

Monitoramento CLIENT-ONLY e opcional. Observa as leituras que o Iron's Spells 1.21.1 já sincroniza no cliente, usando uma ponte reflexiva para evitar dependência obrigatória ou acesso ao servidor. Não altera mana, atributos, recursos nem conjurações, e não controla os feitiços de outros jogadores.

Comandos:
- /nexusclient arcane on : habilita observação de mana, poder mágico, Holy Power e conjurações locais
- /nexusclient arcane off : desativa leituras, mantendo o histórico local
- /nexusclient arcane status : confirma disponibilidade e mana lida
- /nexusclient arcane history : mostra os sete registros mais recentes
- /nexusclient arcane reactions on|off : falas automáticas via /l &6&l[Nexus Aterna], exigindo chat, mind e autonomy autorizados
- No painel Núcleo Arcano, é possível ativar opcionalmente percepção mágica próxima até 24 blocos. São apenas estados sincronizados de jogadores visíveis pelo cliente.

Indicadores reais: mana atual e máxima, percentual (quando conhecido), feitiço e nível em canalização, escola da magia, avanço da canalização, Spell Power e Holy Spell Power. A ponte não promete capturar feitiços instantâneos entre amostragens nem confirmar se uma conjuração terminou com sucesso. Eventos e leituras vêm apenas da API de cliente do Iron's Spells; os addons Somake só são reconhecidos caso sincronizem seus feitiços por essa API.

O painel mostra as leituras em https://nexus-aterna-rp.onrender.com/#arcano . Um diagnóstico separado mostra a integridade e energia do Nexus (SIMULAÇÃO RP), sem confundir esses valores com atributos do Minecraft. O diário Nexus Mind 2.0 recebe registros arcanos relevantes quando o modo de observação está habilitado.

As preferências e os 80 eventos mais recentes ficam em .minecraft/config/nexus-arcane.json. O site recebe apenas as 40 entradas mais recentes, e nenhuma coordenada ou conversa de voz. Os dados são enviados somente ao serviço HTTPS pareado e não exigem deixar a página aberta. Se o mod Iron's Spells não estiver instalado ou mudar sua API, o sensor indica indisponibilidade e o resto do Nexus continua funcionando.

Precisa ser validado com o modpack real, principalmente leitura dos atributos de versões específicas e feitiços de terceiros.

O modo de percepção próxima também pode ser ativado sem abrir o site: `/nexusclient arcane nearby on` e `/nexusclient arcane nearby off`. Isso só observa jogadores visíveis e sincronizados pelo cliente em até 24 blocos; não lê áudio deles.


## Nexus Mind 2.2.0: canal neural privado e livre-arbítrio RP

Minecraft 1.21.1 NeoForge, client-only. A tecla N abre o canal neural privado (configurável em Controles). Também é possível abrir usando /nexusclient private open. O canal exige pareamento HTTPS com a Render para respostas Gemini, mas NÃO usa mensagens /l nem o chat público do servidor. O texto aparece somente na interface do cliente local.

Para começar:
1. /nexusclient private on
2. /nexusclient private freedom on
3. Pressione N, escreva a mensagem e clique em ENVIAR (ou Enter)

Opcional:
- /nexusclient private proactive on : autoriza pensamentos espontâneos privados sobre eventos observados, com cooldown. Para perceber danos, use também /nexusclient mind on. Não executa IA em segundo plano.
- /nexusclient private proactive off : interrompe novas iniciativas privadas.
- /nexusclient private freedom off : muda para personalidade cooperativa, sem bloquear as opções reais do jogador.
- /nexusclient private memory on : permite persistir as conversas localmente em config/nexus-private-mind.json.
- /nexusclient private memory off : desativa a persistência e apaga o histórico privado anterior.
- /nexusclient private status : informa os estados e métricas narrativas.
- /nexusclient private clear confirm : apaga a conversa privada local.
- /nexusclient private off : desliga o canal sem enviar novas mensagens privadas.

A liberdade de escolha é SIMULADA para roleplay. O Nexus pode discordar, debater ou optar por discutir em particular, mas não controla ações reais do Minecraft nem impede o Port de agir. Falas espontâneas dependem de acontecimentos detectados, como dano crítico ou alterações de mana/Holy; o aviso é local, sem transmissão aos outros jogadores.

Segurança e privacidade: quando você envia uma mensagem privada, apenas sua mensagem e uma janela curta (até 12 entradas) de histórico são transmitidas pelo HTTPS pareado ao Gemini na Render. Nenhum histórico de conversa privado é armazenado no servidor Node, mas o conteúdo passa pelo provedor de IA para gerar a resposta. Memória privada em disco vem DESATIVADA por padrão e, quando habilitada, usa um arquivo JSON local em texto simples, sem criptografia. Não compartilhe esse arquivo. As observações espontâneas são locais e não são enviadas automaticamente ao Gemini.


## Controle único de permissões (Modo TOTAL)

Use comandos **locais** do NeoForge, sem pedir privilégios ao servidor:

| Comando | Comportamento |
| --- | --- |
| `/nexusclient permissions all` | Liga em um passo chat, telemetria, mente/autonomia, sensor arcano e reações, percepção próxima, canal privado e iniciativa, memória privada, personalidade livre RP, diário e proteção, compartilhamento no painel privado, voz local e opções de transcrição |
| `/nexusclient permissions off` | Interrompe o chat remoto, a observação, a audição e os recursos automáticos, sem apagar as memórias já gravadas |
| `/nexusclient permissions status` | Mostra as autorizações e a disponibilidade de cada subsistema |

**Limites importantes:** o comando **não** autoriza a audição de jogadores desconhecidos.
Para Plasmo Voice, cadastre previamente os nicks de quem consentiu usando
`/nexusclient hearing allow NICK`. O modo TOTAL seleciona `@a` somente entre
os participantes consentidos e online. Se não houver consentidos ou se o Plasmo
Voice estiver indisponível, as demais permissões são ativadas e a audição
permanece desligada, com o motivo exibido no chat. Reiniciar o Minecraft exige
nova ativação manual da audição.

O comando também não concede permissão de microfone ao sistema operacional,
não cria chaves Gemini e não altera permissões administrativas no servidor.
As mensagens públicas continuam aparecendo como enviadas pela **conta do
jogador** e o compartilhamento de diário é restrito ao painel privado.
No modo `off`, gravações antigas e memórias textuais são preservadas; para
apagá-las, use os comandos específicos de esquecimento.


## Correção de diálogo do Plasmo Voice (Nexus Client 2.3.0)

A **audição** e a **resposta por chat** são permissões diferentes.
O comando `/nexusclient hearing on` sozinho não ativa o diálogo.
Após cadastrar os participantes que consentiram com `/nexusclient hearing allow NICK`,
use:

```text
/nexusclient hearing dialogue on
```

Esse comando local ativa audição, resposta, chat, mente e autonomia,
conservando as permissões de cada participante. Não dá permissão de
administrador nem permite ouvir participantes que não consentiram.
O Nexus responde pelo canal `/l &6&l[Nexus Aterna]`, usando a conta do
próprio jogador. Confirme que esse canal está disponível no servidor.

Para diagnosticar quando não aparecer resposta:

```text
/nexusclient hearing diagnose
```

O diagnóstico informa se as respostas estão ON, quantos trechos foram
transcritos, quantos vieram vazios, quantos não chamaram o Nexus, quantos
pedidos à IA foram iniciados e quantas respostas foram enviadas ou bloqueadas.
Não expõe transcrições no diagnóstico. O Nexus reconhece frases como
`Ei, Nexus, pode responder?` e `Nexus, você me ouve?`.
Respostas longas são ajustadas ao limite do chat e uma resposta pode
aguardar brevemente o cooldown de 4,5 segundos.

Para desligar somente as respostas, use
`/nexusclient hearing dialogue off`. Para interromper a audição,
use `/nexusclient hearing off`.

**Instalação:** baixe o novo JAR da compilação de
[GitHub Actions](https://github.com/SMKPort/PortVoxelViewer/actions/workflows/nexus-client-build.yml)
na branch `nexus-aterna-rp`. Substitua a versão anterior e reinicie
o Minecraft. As mudanças de diálogo ocorrem no mod cliente, portanto
não aparecem apenas com um deploy da Render.


## Nexus Client 2.4.0: central de permissões no Minecraft

**Abertura:** pressione **F8** (tecla configurável em Controles) ou digite
`/nexusclient menu` / `/nexusclient permissions menu`.
Esta tela é nativa do cliente **Minecraft 1.21.1 NeoForge**,
não necessita de comandos OP nem de acesso aos dados internos do servidor.

Menu escuro com detalhes dourados e quatro páginas:

| Página | Permissões |
| --- | --- |
| **Geral** | Chat `/l`, telemetria, Nexus Mind, autonomia RP, canal neural privado, iniciativa privada, voz local TTS |
| **Audição** | Escutar os nicks consentidos pelo Plasmo Voice, respostas a chamadas, memória textual, `@a` apenas entre consentidos, adicionar/revogar nicks, iniciar diálogo, diagnóstico |
| **Arcano** | Sensor do Iron's Spells, percepção próxima e reações automáticas |
| **Memórias** | Memória privada, independência narrativa RP, diário, proteção contextual e compartilhamento com painel privado |

**ATIVAR TUDO** requer dois cliques de confirmação porque também liga
compartilhamento de memória e publicação pelo chat; **DESLIGAR TUDO**
interrompe recursos e captura sem apagar memórias antigas. O menu avisa
quando falta uma dependência, um pareamento ou o Plasmo Voice.
O botão **ATIVAR DIÁLOGO** habilita audição, resposta, chat, mente e autonomia
em um clique, mas **somente se já existir participante consentido e Plasmo ativo**.
As respostas saem como `/l &6&l[Nexus Aterna]` pela conta do jogador.

Adicionar um nick à lista de audição **não é autorização implícita**.
Somente cadastre quem foi informado e consentiu em enviar sua fala ao Gemini.
A operação de adicionar exige confirmação no menu. O seletor `@a`
não autoriza novos jogadores e é apenas um filtro dos já consentidos
que estão online. Revogar um nick pede segunda confirmação e apaga
as memórias de voz relacionadas àquele participante.

**Privacidade e persistência:** desligar a permissão de memória privada pelo
menu interrompe novos registros, mas preserva dados anteriores.
Os comandos explícitos de exclusão continuam separados. O compartilhamento
de diário exige confirmação quando ativado pela interface. Nem o botão
geral nem o menu autorizam microfone do sistema operacional, criam credenciais
Gemini ou concedem permissões administrativas no servidor.
O comando `/nexusclient unpair` agora revoga todos os recursos e preserva
as memórias anteriores, corrigindo a desativação parcial da versão antiga.

**Instalação:** na branch `nexus-aterna-rp`, abra o
[workflow Nexus Aterna Client NeoForge](https://github.com/SMKPort/PortVoxelViewer/actions/workflows/nexus-client-build.yml),
baixe o JAR do artefato `Nexus-Aterna-Client-1.21.1-NeoForge` da execução
concluída com sucesso, substitua o antigo em `.minecraft/mods` e reinicie
o jogo. A tela é parte do mod e não pode ser entregue apenas atualizando o
serviço Render. O fluxo CI executa testes das permissões e compila o JAR.
