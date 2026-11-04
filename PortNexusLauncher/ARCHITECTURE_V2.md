# Port Nexus Launcher 2.0 — arquitetura

## Objetivo
Launcher Android sem root, com home própria, núcleo animado, temas/protocolos, favoritos, app drawer em três visualizações, personalização local, painéis laterais e comandos locais. A versão 2.0 prioriza aspecto autoral e recursos estáveis de Android.

## Estrutura inicial

- `MainActivity.kt`: ciclo de vida, consulta a apps com activity LAUNCHER, execução explícita, status da bateria, abertura de configurações oficiais, brilho somente da janela.
- `NexusV2Models.kt`: modos Nexus, visualizações do Arsenal, categorias heurísticas e seus classificadores.
- `NexusV2.kt`: shell Compose, persistência local, home, anéis do núcleo, Arsenal, hub de comandos, painel Command Deck, Archive, configurações de aparência.
- `NexusUi.kt`: implementação legada 0.1 preservada por enquanto para referência/migração.
- `AndroidManifest.xml`: declaração de HOME e visibilidade limitada a apps com launcher.

## Dados
O estado de favoritos, apps ocultos, últimas atividades iniciadas **pelo próprio Nexus**, protocolo ativo, notas e parâmetros visuais é mantido em SharedPreferences. Não são coletadas estatísticas de uso externas. Nenhum dado é enviado para servidor.

## Entradas
- Aba Início: núcleo navegável, modos, dock mutável, botões e gestos horizontais (Archive à esquerda, Deck à direita).
- Aba Arsenal: grade, filtro Arsenal por categorias, Nexus AI (ordenação local pelas aberturas recentes), busca, favoritos e ocultação.
- Aba Nexus: console de comandos locais para modos, abrir aplicativos e atalhos do Android.
- Aba Visual: tema, animação, tamanho dos ícones e configuração do launcher padrão.

## Limitações atuais e roadmap técnico
Sem root, o Android não permite substituir a central de notificações, as telas recentes ou alternar todos os ajustes globais diretamente. Os botões de Wi-Fi, Bluetooth, áudio e tela abrem as configurações oficiais. Brilho é alterado apenas no Nexus. O hub de comandos é um interpretador local e não é IA remota.

Futuras etapas (não implementadas na base atual): personalização de pastas por arrastar e soltar, hospedagem real de widgets de terceiros, serviço de mídia com MediaSession e consentimento, Assistente com IA opcional e API configurável, perfis contextuais automáticos com permissões adequadas, temas exportáveis, backup e integração direta com dispositivos de terceiros.

## Compilação
Abrir como projeto raiz a pasta `PortNexusLauncher` em Android Studio ou executar `gradle -p PortNexusLauncher :app:assembleDebug` com JDK 17 e Android SDK 35. O workflow `.github/workflows/nexus-launcher.yml` do repositório compila e disponibiliza o APK debug no GitHub Actions.
