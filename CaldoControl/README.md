# CaldoControl 2.6 para Android

## Novo na versão 2.6: hidratação de cal e polímero (manual)

A aba **Registrar** agora contém dois campos independentes, opcionais, de texto livre, com até **1.200 caracteres**:

- **Hidratação de cal**, para informações inseridas manualmente.
- **Hidratação de polímero**, para informações inseridas manualmente.

O usuário deve informar as respectivas unidades e contexto no próprio texto, conforme seu procedimento. É possível salvar um registro contendo **apenas hidratação**, sem pH ou Brix, desde que o operador esteja identificado. Os dados ficam vinculados à data, linha, processo e turno do registro.

Os campos estão disponíveis ao criar, editar, pesquisar e consultar o **histórico**; são incluídos no **relatório PDF/impressão**, no **CSV**, no **Excel XLSX** (aba `Analises`) e no **backup/restauração JSON v6**, mantendo compatibilidade com backups antigos. A auditoria de inclusões e alterações preserva os campos.

**Hidratação é somente manual:** não entra nas médias de pH/Brix, não faz cálculos de dosagem nem recebe automaticamente dados do gateway ou de fotos OCR. Não houve reintrodução de medidores de temperatura, Baumé, decantadores ou vazões na interface.

**Importante:** exporte backup JSON da versão anterior antes de atualizar. Um APK de testes pode exigir reinstalação se a assinatura for diferente.

## Histórico e recursos anteriores

A partir da **versão 2.5**, o CaldoControl concentra-se exclusivamente nos seguintes parâmetros:
- pH caleado/dosado (mesma etapa), clarificado, xarope e torre de resfriamento.
- °Brix caleado/dosado, clarificado e xarope.

**Foram removidos da interface, cálculos, relatórios, Excel, OCR, histórico visível e mapeamento de gateway:** temperaturas, aquecimento, Baumé, decantadores, lodo, turbidez e todas as vazões (caldo, leite de cal, polímero e retirada de lodo). O diagnóstico calcula apenas tendências de pH e Brix. Fechamentos de turno calculam somente médias, mínimos, máximos e ocorrências de pH/Brix.

**Preservação dos dados anteriores:** a chave de armazenamento local continua a mesma. Medições antigas retiradas do escopo seguem preservadas internamente em registros já existentes e no **backup JSON**, inclusive quando se edita uma análise antiga ou restaura uma cópia de versões anteriores. Elas não entram nas médias ou exportações CSV/Excel da versão 2.5. Faça backup ANTES de atualizar.

Permanece: médias e fechamento de turnos com conferência, gráficos, OCR de fotos com revisão, importação tabular em lote, relatórios PDF/Excel/CSV, operadores com trilha local, gateway seguro de leitura WSS/HTTPS, e backup/restore JSON. Nenhuma escrita ou comando ao CLP.

## Instalar o APK

Acesse **Actions > CaldoControl APK Android > execução mais recente > Artifacts > CaldoControl-2.6-debug-APK**. Extraia `app-debug.apk` do ZIP gerado. O APK debug requer backup anterior dos dados se for necessário reinstalar o app por diferença de assinatura.

## Versões anteriores e limitações


Aplicativo de acompanhamento do **tratamento de caldo para açúcar e etanol**, com calagem, pH do caldo misto e caleado/dosado (mesma etapa), decantadores e lodo, aquecedores, relatórios por turno, histórico e conexão **somente de leitura** com gateway WebSocket autorizado.

## Novo na versão 2.3: pH do xarope

Adicionado o campo independente **pH do xarope** com validação de 0 a 14 e duas casas decimais. Ele aparece na ficha de registro, histórico, gráfico de tendência de pH, médias por turno (incluindo o número de amostras), relatórios, exportação CSV, backup JSON, restauração, leitura de fotografias por OCR e configuração/mapeamento das tags do gateway (campo `phXarope`, alias `ph_xarope`).

Registros anteriores continuam válidos; medições antigas que não tinham pH de xarope ficam sem esse valor, sem afetar as demais médias. Não é definida uma faixa operacional automática: configure os limites segundo os procedimentos da usina.

## Melhorias da versão 2.4

**Ao vivo:** mostra amostras pontuais recebidas pela interface WSS/HTTPS do supervisório e um gráfico local (até 240 leituras). Mostra se os dados estão antigos ou o gateway não está conectado. Não trata amostras do gateway como análises salvas e nunca escreve em CLPs.

**Fechamento de turnos:** horários A, B e C configuráveis. Enquanto o aplicativo estiver aberto, cria **rascunhos automáticos** para turnos com registros após o fim do turno + 15 minutos, com médias, quantidades de amostras e ocorrências fora dos limites. Um operador precisa revisar e marcar **Conferido**. Se os registros mudarem, o fechamento é marcado como desatualizado. O fechamento não é executado em segundo plano quando o aplicativo estiver fechado. Configure também o início do dia operacional, principalmente para o turno da madrugada.

**OCR de folhas inteiras:** fotografe uma tabela de análises pelo APK Android. O reconhecimento offline ML Kit retorna as posições de linhas e células. Associe manualmente cada coluna, confira e corrija valores, horários e linhas selecionadas antes de salvar. Importe até 70 linhas por lote. A foto reduzida acompanha o primeiro registro do lote. Fotografias de tabelas difíceis podem exigir digitação e mapeamento manual. A leitura tabular automática **não funciona no HTML isolado**.

**Calagem e diagnóstico:** cálculo de L/m³ de leite de cal/caldo, gráficos por análise, médias e correlação descritiva com o pH caleado. Alertas de últimas medições fora das faixas configuradas e mudanças monotônicas. Não inferir causa nem recomendar dosagem automaticamente.

**Operadores:** identificação local de operador e turno habitual. Registro de autor, horário, justificativa e resumo antes/depois nas criações, edições e exclusões. A trilha é armazenada localmente, não há autenticação corporativa, assinatura digital ou proteção contra alteração intencional.

**Relatórios:** aba Médias por turno com exportação XLSX contendo planilhas Analises, Medias por turno, Fechamentos e Auditoria, sem biblioteca externa ou internet. A exportação CSV e impressão ou **Salvar como PDF** na tela Relatórios permanecem disponíveis. Para preservar imagens, faça o backup JSON completo.

**Backup e restauração:** backup JSON v4 inclui, além dos registros e fotos, auditoria, fechamentos e configuração de horários. Ao restaurar, o app pergunta antes de adicionar eventos de auditoria e fechamentos. Dados antigos são mantidos. Faça backup ANTES de trocar de APK ou desinstalar: APK debug pode ter assinatura diferente e exigir reinstalação.

## Testes de build

O GitHub Actions valida sintaxe JavaScript, campos críticos da interface, exportador XLSX com ZIP/OpenXML válido e compila Android. O fato de compilar não substitui testes no dispositivo, na sua folha de análises real e na rede industrial autorizada.

## APK

No GitHub, acesse **Actions > CaldoControl APK Android > execução mais recente > Artifacts > CaldoControl-2.6-debug-APK**. Baixe o ZIP e extraia o arquivo **app-debug.apk** para instalar no Android. Este é um APK debug para teste, não assinado para distribuição pela Play Store.

O workflow `.github/workflows/caldocontrol-apk.yml` compila o projeto com Java 17, Gradle 8.9 e Android SDK 35. Também pode ser executado manualmente via **Run workflow**.


## Novidades da versão 2.1

- Aba **Médias por turno**, com médias aritméticas separadas de pH caleado/dosado, pH clarificado, pH da torre de resfriamento e Brix caleado, clarificado e xarope. Cada resultado mostra `n`, o número de amostras utilizadas. É necessário indicar o turno ao registrar a análise; as médias são recalculadas quando novos registros são salvos.
- Campo de **caldo misto removido** da ficha. Dados legados continuam no histórico e backup, quando já existentes.
- Três campos de **°Brix separados**. Um Brix genérico de versões anteriores permanece no backup sem ser atribuído automaticamente a uma etapa.
- **Importação de fotos por OCR offline** no APK Android com Google ML Kit. Escolha a imagem, revise as sugestões e selecione quais números serão atribuídos a cada campo antes de aplicá-los. Nem todos os formatos ou letras manuscritas serão interpretados corretamente. No navegador HTML, a foto pode ser anexada, mas o OCR exige o APK Android.
- **Exportação** de registros em CSV, médias por turno em CSV e **backup JSON completo, incluindo miniaturas de fotos anexadas e texto reconhecido**. A importação/restauração JSON conserva o histórico de arquivos compatíveis, com prevenção de IDs duplicados.

A versão é de apoio ao acompanhamento e exige conferência humana. O OCR pode confundir leituras e **não deve gerar correções operacionais automáticas** no CLP.


## Conexão com gateway do supervisório (2.2)

1. Na aba **Sensores/CLP**, informe um nome e selecione o tipo: **WebSocket WSS (JSON)** ou **API HTTPS JSON via GET**. Para gateways exclusivamente **OPC UA, Modbus TCP ou MQTT**, solicite ao setor de automação um adaptador autorizado que disponibilize leitura por WSS/HTTPS. Não há conexão direta com CLPs.
2. Cole o endereço completo HTTPS/WSS fornecido pela TI. O app **não armazena senha nem aceita usuário/senha na URL** e não desativa verificação de certificados. Para certificados internos, use uma cadeia de confiança instalada/gerenciada corretamente.
3. Clique em **Testar / descobrir tags**. O sistema exibe os nomes das tags encontradas em uma mensagem JSON. Selecione a tag correspondente a cada campo.
4. Se a tag de **vazão de cal** for utilizada, escolha explicitamente a unidade **L/h ou m³/h**. Clique em **Salvar configuração** e **Conectar**. Uma API HTTPS é consultada a cada 15, 30 ou 60 segundos.
5. Confira os valores na área **Última leitura**; somente depois clique em **Copiar leitura para análise** e grave o registro manualmente. Leituras sem timestamp ou com hora divergente são recusadas.

Formato recomendado:
```json
{
  "timestamp": "2026-10-31T12:00:00Z",
  "tags": {
    "TAG_PH_DOSADO": {"value":7.10,"quality":"GOOD"},
    "TAG_BRIX_CLARIFICADO": {"value":14.8,"quality":"GOOD"},
    "TAG_VAZAO_CAL": {"value":1400,"quality":"GOOD"}
  }
}
```

Também são aceitos objetos com `tags` simples ou listas de leituras `data`, `readings` e `values` contendo `name`/`tag`/`id` e `value`. O mapeamento é salvo localmente e pode ser exportado/importado como arquivo JSON. A versão web pode esbarrar na política CORS da API; o APK possui leitura HTTPS nativa sem exigir CORS. Dados só são lidos de endpoints HTTPS/WSS explicitamente inseridos pelo operador. **Sem credenciais, não se conecta a gateways que exigem autenticação.** Para isso, configure uma API de leitura sob a supervisão da equipe de TI/automação.

**Segurança industrial:** não exponha o gateway ou o CLP diretamente à internet ou a redes não confiáveis. Separe a interface de leitura do sistema de controle, mantenha autorização e segmentação de rede, use TLS válido e valide a configuração com a equipe responsável.

## Organização

- `app/src/main/assets/index.html`: CaldoControl 2.2 completo, com suporte Android a CSV/JSON e impressão.
- `app/src/main/java/br/com/caldocontrol/app/MainActivity.java`: WebView com origem local HTTPS de recursos, seleção de arquivos, exportação e impressão.
- `app/build.gradle`: configurações de compilação Android.
- `../.github/workflows/caldocontrol-apk.yml`: compilação automatizada e artefato APK.

## Dados e segurança

Os dados ficam no armazenamento privado do WebView do Android. **O APK não recebe automaticamente o histórico salvo no Chrome**: exporte Backup JSON na versão web e importe no APK em Ajustes. Faça backups periódicos, pois desinstalar o APK ou limpar seus dados pode apagar os registros.

A integração com CLPs permanece **somente leitura** e requer gateway autorizado em `wss://` (TLS/certificado confiável); não há acesso direto a Modbus/OPC UA nem envio de comandos. A origem do app é HTTPS local via AndroidX WebViewAssetLoader. Evite conectar o APK a redes industriais sem validação pela equipe de automação/cibersegurança.

Protótipo para apoio a registros. Não substitui SCADA/SDCD, proteções/intertravamentos ou procedimentos homologados da usina. Faixas operacionais exigem aprovação da engenharia.

## Compilação local

Com Android SDK 35, JDK 17 e Gradle 8.9:

```bash
cd CaldoControl
gradle :app:assembleDebug
```

O resultado fica em `app/build/outputs/apk/debug/app-debug.apk`.
