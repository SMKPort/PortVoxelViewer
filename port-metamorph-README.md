# 🧬 Port Metamorph 0.1.0

**Minecraft Java 1.21.1, NeoForge 21.1.x, Java 21.**
Mod cosmético **100% client-side** com renderizador voxel próprio, formas modulares, presets, transições e controle remoto via navegador. Não requer permissões no servidor.

## Links

- Painel de controle: https://port-metamorph.onrender.com
- Código fonte: https://github.com/SMKPort/PortVoxelViewer/tree/port-metamorph
- Build JAR: https://github.com/SMKPort/PortVoxelViewer/actions/workflows/port-metamorph-build.yml
- Testes web: https://github.com/SMKPort/PortVoxelViewer/actions/workflows/port-metamorph-web.yml

## Primeira execução

1. Baixe o artefato **Port-Metamorph-NeoForge-1.21.1** no GitHub Actions e extraia **portmetamorph-0.1.0.jar**.
2. Coloque o JAR na pasta **mods** da instância Minecraft 1.21.1 com NeoForge.
3. Entre em um mundo e execute **/metamorph** para abrir o painel do jogo. Escolha uma forma e ative-a.
4. Para controlar pelo navegador, execute **/metamorph pair**. O chat exibirá um código hexadecimal de **8 caracteres** válido por **5 minutos**.
5. Abra **https://port-metamorph.onrender.com**, digite esse código e clique em **Vincular dispositivo**.
6. Edite a forma, módulos e peças e clique em **Aplicar no Minecraft**. O cliente busca as alterações a cada **4 segundos**.

Comandos:
- **/metamorph** ou **/metamorph open**: interface do jogo
- **/metamorph form normal|nexus|angel|hybrid|custom**: aplicar forma
- **/metamorph on** e **/metamorph off**: ativar/desativar transição visual
- **/metamorph pair**: gerar código temporário de pareamento com site
- **/metamorph unpair**: revogar a conexão local (site ficará offline; para invalidar a sessão do site, use Desvincular nele)
- **/metamorph web**: mostrar endereço do site no chat
- **/metamorph status**: mostrar forma e estado do painel
- **/metamorph reset**: restaurar configuração local básica

## Escopo real da versão 0.1

- Renderiza asas voxel articuladas com movimento senoidal, placas voxel no braço direito e chifres opcionais, usando blocos padrão como materiais.
- Possui presets Normal, Nexus, Angelical, Híbrido e forma Custom.
- Modificações de escala e movimento de asas, escala do braço e tempo de ativação.
- Aceita até **24 cubos extras** com posição e tamanho em coordenadas relativas ao personagem.
- Salva as configurações em **config/port-metamorph.json**.
- Oferece uma interface básica no jogo e um painel web em preto/vermelho/dourado.
- A visualização no site é **conceitual em 2D**, não reflete a renderização exata no Minecraft.

**Ainda não implementado:** importação direta de modelos do Blockbench (**.bbmodel**), texturas personalizadas, rigs arbitrários, troca integral do corpo base do jogador, física de hitbox, sincronização visual automática com outros jogadores, integração com Armourer's Workshop ou animações complexas. O mod é um **protótipo funcional de acessórios**, não o sistema Metamorph completo.

O renderizador atual usa blocos vanilla para as peças. Não é aconselhável tratar essas peças como modelos finais do Nexus. Os modelos verdadeiros virão após o pipeline de importação e bones.

**Dica:** teste em 3ª pessoa ou F5. O jogador normalmente não visualiza seu próprio corpo inteiro em primeira pessoa. Alguns shaders e mods que substituem o renderizador de jogadores podem exigir adaptações.

## Privacidade e segurança

O site não aceita comandos de servidor, scripts arbitrários ou arquivos executáveis. Aceita apenas alterações cosméticas validadas. O mod se conecta por HTTPS **de saída**, sem abrir portas locais. Uma chave aleatória do dispositivo é gravada em **config/port-metamorph-device.json**. O site gera uma sessão separada, guardada somente na aba do navegador (**sessionStorage**).

O código de pareamento só funciona uma vez e expira em 5 minutos. Mantenha o código privado. A comunicação controla **apenas sua própria renderização local** e não altera a aparência vista pelos demais jogadores.

**Observação sobre hospedagem gratuita:** o relay guarda os pareamentos em memória. Quando o serviço Render reinicia ou adormece e perde o estado, será necessário gerar **um novo código** no jogo e parear novamente. Isso será resolvido com armazenamento persistente numa evolução futura.

## Arquitetura

- **port-metamorph-mod/**: Java 21, NeoForge, armazenamento local, renderização 3D voxel e polling HTTPS.
- **port-metamorph-web/server.js**: Node.js sem dependências, autenticando sessões do painel e dispositivos separadamente.
- **port-metamorph-web/public/**: HTML/CSS/JS responsivos sem dependências externas.
- **port-metamorph-web/test.js**: teste de integração do fluxo de pareamento, autenticação, envio, polling e revogação.
- **.github/workflows/**: compilação JAR e validação do relay web.

## Build local

Com Gradle 9.2.1 e Java 21:

\`\`\`bash
gradle -p port-metamorph-mod build --no-daemon
\`\`\`

Para rodar o site localmente com Node 22+:

\`\`\`bash
PORT=3000 node port-metamorph-web/server.js
node --test port-metamorph-web/test.js
\`\`\`

A compilação automatizada confirma compatibilidade de APIs, mas **não substitui testes no modpack real com shaders/Armourer's Workshop**.

## Roteiro posterior

1. Formato de peças importáveis com materiais texturizados e bones reais.
2. Importação de modelos Blockbench e validação de hierarquia.
3. Timeline de transformação com fases, curvas e transições reversíveis.
4. Compatibilidade opcional com Figura e Armourer's Workshop (sem dependência obrigatória).
5. Compartilhamento de formas com outros clientes de maneira opt-in, respeitando regras do servidor.
