# LumiPort 0.2.0 (branch de desenvolvimento) 🔴🟡⚫

Aplicativo Android em Java com tema preto, vermelho-rubi e dourado.

## Integrações

- **Tuya / Smart Life:** controle por API local ou HTTPS do **Home Assistant**, já configurado com a integração oficial Tuya.
- **HappyLighting:** Bluetooth LE direto **experimental**, exclusivo para controladores reconhecidos LEDBLE/LEDCAR com característica FFE1. Nem toda fita HappyLighting usa esse protocolo.
- Controle conjunto: ligar, desligar, cores RGB, brilho e cenas.
- O token do Home Assistant fica **apenas na memória enquanto o app está aberto**, sem entrar no GitHub ou ser salvo pelo app.
- O app não espelha automaticamente alterações feitas fora dele. No BLE direto, é necessário permanecer perto da fita.

## Como usar

1. Configure Home Assistant e a integração Tuya.
2. Insira a URL do Home Assistant e o identificador da lâmpada, por exemplo light.quarto.
3. Insira um token de longa duração do Home Assistant no celular. Não publique o token em issues ou commits.
4. Ative o Bluetooth e toque em **Conectar fita**. O app procura controladores LEDBLE/LEDCAR próximos.
5. Escolha cores ou cenas para enviar comandos aos dois dispositivos.

## APK

Na aba **Actions** do GitHub, execute/acompanhe **Compilar LumiPort Android**. Em caso de sucesso, o APK de teste estará disponível no artefato **LumiPort-debug-apk**.

## Estado

Protótipo Android. Fontes e fluxo de compilação publicados para testes. A compilação e a comunicação com a fita específica ainda precisam ser verificadas. Esta branch é **isolada** da branch principal do PortVoxelViewer. O ideal é migrar depois para um repositório próprio **SMKPort/LumiPort**.
