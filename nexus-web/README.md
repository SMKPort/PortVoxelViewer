# Nexus Aterna (Web v1.0)

Painel web/PWA do universo Port Morningstar. Interface responsiva preta, vermelha e dourada. Projeto em ramo separado `nexus-aterna-web` para preservar os outros aplicativos.

## Executar

Abra a URL HTTPS da implantação ou sirva este diretório em um servidor local HTTP (exemplo: `python3 -m http.server 8000` dentro de `nexus-web`). O acesso ao hardware exige origem segura, normalmente HTTPS ou localhost.

## Módulos

Núcleo, Link Tecnológico, Visão Arcana, Laboratório Ritualístico, Mapa Energético, Engenharia Arcana, Diagnóstico, Arquivos e Segurança. Inclui terminal de comandos, modo holográfico, logs, editor de rituais, registros de projeto, simulações, exportação/importação JSON e PWA instalável.

## Integrações

- **Web Bluetooth (BLE)**: escolha de dispositivos com consentimento do usuário; o controle específico requer implementação de serviços GATT compatíveis.
- **Web Serial / WebUSB**: seleção de portas ou dispositivos com permissão. A versão atual ainda não envia comandos aos protocolos desses equipamentos.
- **Home Assistant**: URL HTTPS acessível e token informado na sessão. Lista entidades e executa `homeassistant.toggle` em entidades compatíveis, mediante confirmação. O servidor deve aceitar CORS da origem da aplicação. Não armazenamos o token em localStorage.

## Segurança, limitações e privacidade

- Site público HTTPS não significa conteúdo privado. Não há login, autorização multiusuário ou banco remoto.
- Dados de lore, rituais, equipamentos e relatórios são salvos **somente em localStorage** no navegador. Exportar um backup JSON é recomendado.
- O protocolo de isolamento TS é **fictício**. Não constitui isolamento de redes ou proteção de dispositivos.
- O terminal interpreta comandos localmente por regras. **Não** usa um modelo de IA remoto. IA conversacional real requer backend protegido e credenciais no servidor, nunca em JavaScript público.
- Energia arcana, análise de maldições, rituais e entidades são **simulações narrativas**, sem detecção física ou previsão científica.
- Nenhum acesso indiscriminado a equipamentos existe; somente tecnologias com API compatível e autorização.
- Não insira segredos ou dados sensíveis nos campos de lore de um computador compartilhado.

## Extensões sugeridas

Backend de IA com autenticação, armazenamento criptografado e sincronização, integração MQTT/CLP com permissões, importação Blockbench, leitura de dados de sensores reais com unidades físicas e controle de acesso.

Feito para desenvolvimento incremental, mantendo a fronteira entre tecnologia real e lore.
