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
