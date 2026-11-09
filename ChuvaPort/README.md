# ChuvaPort 2.0 🌧️🌪️

Amplia o ChuvaPort 1.1 mantendo todas as funções de chuva e adicionando: temperatura, sensação térmica, umidade, ponto de orvalho, vento e rajadas, direção, pressão, nuvens, visibilidade, UV, nascer e pôr do sol, previsões de temperatura e ventos para 16 dias, índices CAPE, LI, CIN e nível de congelamento.

## Atenção à interpretação

O painel **Tempestades** é informativo, com indicadores de instabilidade do modelo Open-Meteo. A existência ou ausência de um indicador **não confirma nem exclui** tempestades severas. Códigos WMO 96 e 99 sinalizam trovoadas com granizo no modelo, mas não substituem medições de radar/observação. **Não existe previsão pontual ou probabilidade de tornado neste app.** Acesse [INMET](https://avisos.inmet.gov.br/) para avisos oficiais do Brasil.

## Fonte

Open-Meteo Forecast API: https://open-meteo.com/en/docs. Variáveis podem ser nulas dependendo do modelo e da região. Previsão até 16 dias; incerteza crescente com o prazo.

## Android

O build Android continua usando WebViewAssetLoader e assets gerados a partir de `ChuvaPort/index.html`. A compilação automática disponibiliza APK de teste nos artifacts do GitHub Actions; não requer GitHub Pages.

## Testes

`node tests/weather-utils.test.mjs` (regressão da chuva) e `node tests/test_extended.mjs` (ampliação meteorológica).