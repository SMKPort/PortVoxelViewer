# Port Card Scanner 1.0

Aplicativo nativo Android para identificar **cartões e etiquetas NFC compatíveis** por aproximação, exibindo os metadados acessíveis pelo sistema.

## Funções

- Leitura de NFC-A, NFC-B, NFC-F e NFC-V, de acordo com o controlador do telefone.
- Mostra UID quando disponível, lista de tecnologias, ATQA/SAK (NFC-A), parâmetros de NFC-B/F/V e informações do NDEF.
- Histórico das últimas 30 leituras **em memória**, sem enviar dados para servidores e sem salvá-los permanentemente.
- Detecção de NFC desativado ou ausente.
- Compartilhamento e cópia intencionais de um relatório técnico.
- Interface preta, vermelha e dourada, com abas Leitura, Histórico e Sobre.

## Como gerar APK pelo GitHub

1. Crie um repositório e envie o **conteúdo** desta pasta para a raiz (incluindo `.github`).
2. Acesse **Actions > Gerar APK Android > Run workflow**.
3. Ao terminar, abra a execução e baixe o artefato `PortCardScanner-v1-debug-apk`.
4. Extraia o ZIP do artefato e instale `app-debug.apk` no seu Android (autorize instalação externa se necessário).

Também pode abrir a pasta no Android Studio com Java 17 e SDK 35 e executar o módulo `app`.

## Uso

Ative NFC nos ajustes do Android. Abra o aplicativo, deixe a aba **Leitura** aberta, selecione **Ativar leitura** se necessário e encoste o cartão NFC na parte traseira do aparelho. O Android só detecta tags compatíveis e responde quando a tag se aproxima. A tela acende o estado de leitura e exibe os metadados públicos encontrados.

## Limites técnicos e privacidade

- **Não clona, emula, autentica nem reproduz cartões de ponto ou acesso.** É uma ferramenta de leitura e diagnóstico de tags próprias ou autorizadas.
- O NFC do telefone trabalha em **13,56 MHz**. Não lê RFID **125 kHz** sem um leitor externo adequado (não implementado nesta versão).
- Não captura ondas brutas, RSSI/intensidade de sinal, chaves criptográficas, dados protegidos ou tráfego entre o relógio de ponto e outro cartão.
- Algumas tags expõem um ID aleatório, não permanente, ou nenhum ID útil. Ler um UID não garante que um sistema possa aceitá-lo como uma credencial digital.
- A ausência de leitura não identifica com certeza a frequência do cartão; pode ser por incompatibilidade, blindagem, localização da antena ou NFC desligado.
- Os dados ficam somente na RAM durante a sessão. O botão de compartilhar abre o seletor de apps do Android e o de copiar coloca o relatório na área de transferência por solicitação do usuário.
- O app não solicita INTERNET.

Este projeto inclui código-fonte e workflow de compilação. O APK não está pré-compilado neste pacote.
