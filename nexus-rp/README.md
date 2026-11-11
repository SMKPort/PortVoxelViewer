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
