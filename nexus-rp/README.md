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
