# Nexus Aterna Web 2.0

Sistema web/PWA do universo de Port Morningstar. Site HTTPS atual: https://nexus-aterna-web.onrender.com

## Arquitetura

A versão 2.0 é **aditiva**. A aplicação V1 continua funcionando, com as oito telas originais (Núcleo, Link Tecnológico, Visão Arcana, Laboratório Ritualístico, Mapa Energético, Engenharia Arcana, Diagnóstico e Arquivos). Os módulos V2 são carregados por `v2-base.js`, `v2-3d.js`, `v2-ritual.js` e `v2-spectrum.js`. A camada `v2.css` complementa a identidade preta, vermelha e dourada.

O aplicativo está na branch `nexus-aterna-web` do repositório `SMKPort/PortVoxelViewer`. O serviço estático da Render publica o diretório `nexus-web` com auto-deploy ativado.

## Novos módulos

- **ATERNA AI**: chat, consulta a memórias locais, entrada por voz via Web Speech API e leitura de respostas pelo sintetizador do navegador. Modo local por regras, com backend Gemini **opcional e protegido**, caso configurado.
- **Gêmeo Digital 3D**: modelo geométrico articulado do braço, visualização orbitável por Three.js, separação de placas, captura PNG e importação GLB/GLTF/BBMODEL. BBMODEL importa cubos como geometria simplificada e não garante texturas/bones. GLB é o formato recomendado.
- **Rituais Avançados**: editor SVG de runas, drag-and-drop com mouse/toque, simulação narrativa de coesão e custo, exportação SVG e integração com o acervo de rituais da V1.
- **Tecnologia Real**: recursos opcionais Web Bluetooth BLE com leitura de Battery Service, Web Serial para dispositivos autorizados, seleção WebUSB e leitura Web NFC. Integração Home Assistant original permanece na V1.
- **Protocolo TS**: campanha narrativa de cinco fragmentos, escolhas, integridade fictícia e histórico persistente. Nenhum sistema real é atacado ou protegido pelo protocolo ficcional.
- **Espectrômetro**: gráfico temporal, DFT de espectro, controles de sinal sintético, importação CSV de medições, cálculo RMS/pico/média/frequência dominante e arquivo de amostras.

## Segurança e limites

O site é público, sem login para o portal. **Não armazene segredos nos campos de lore** em dispositivos compartilhados. Memória/rituais/TS/leituras V2 são salvos em `localStorage`, separado do legado V1. Backup JSON do módulo Arquivos agora inclui V1 + V2.

O protocolo TS, os rituais e a análise de energia arcana **são simulados**, sem medição sobrenatural ou proteção cibernética. Controle de dispositivos depende do navegador, permissões do usuário, protocolos e equipamentos reais compatíveis.

O assistente local **não é um LLM**. Para respostas generativas reais, configure o backend separado em `nexus-api/` com variáveis privadas na Render. Nunca coloque `GEMINI_API_KEY` em JavaScript do site. O token de acesso da API é fornecido ao navegador em cada sessão e não é salvo. O backend opcional utiliza CORS com origem específica, rate limit e validação simples de código de acesso. Para usos sensíveis, implante autenticação mais forte.

## Como executar

Em HTTPS, abra https://nexus-aterna-web.onrender.com e, em navegador compatível, use Instalar aplicativo. Para desenvolvimento, sirva `nexus-web/` em localhost por HTTP (PWA/Web APIs funcionam em contexto seguro). O renderizador 3D importa a versão fixa do Three.js por CDN e requer internet e WebGL.

Para integrar dados reais de sensores, forneça CSV ou um adaptador explícito. Não trate valores fictícios como medidas físicas.

## Backend de IA

Arquivo: `nexus-api/server.js` e `nexus-api/package.json`. Render Web Service, Node >=20, **plano free** se disponível, branch `nexus-aterna-web`. Build: `echo ready`. Start: `node nexus-api/server.js`.

Configure, nas variáveis **privadas** de ambiente: `GEMINI_API_KEY` (chave de API legítima da sua conta Gemini), `ATERNA_ACCESS_CODE` (código longo e aleatório para a sessão do operador), e opcionalmente `GEMINI_MODEL` (modelo disponível, inicialmente `gemini-2.5-flash`). O site usa por padrão `https://nexus-aterna-api.onrender.com`, ajustável em ATERNA AI.

A API retorna 503 até que a chave e o código sejam configurados. Isso é esperado e o chat continua funcionando em modo local.

## Backups

Exporte os dados em Arquivos e Segurança, após configurar rituais e memórias. O backup é um JSON local; restaurar substitui o conjunto de dados do navegador. Senhas de acesso não são exportadas.
