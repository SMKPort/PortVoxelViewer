# ChuvaPort 1.1 🌧️

Aplicativo de previsão de precipitação em milímetros, probabilidade de chuva e acumulados horários/diários (16 dias). Visual preto, vinho e dourado.

## Abrir e instalar
Depois de configurar o GitHub Pages para **GitHub Actions** no repositório PortVoxelViewer, acesse:
https://smkport.github.io/PortVoxelViewer/ChuvaPort/

No Android, abra no Chrome e escolha **Instalar aplicativo** ou **Adicionar à tela inicial**, conforme a opção disponível.

## Arquivos
- `index.html`: aplicação completa (HTML, CSS e JavaScript incorporados, para evitar problemas com visualizadores de arquivos locais).
- `manifest.webmanifest`, `sw.js` e `assets/icon.svg`: configuração da versão web instalável.

## Dados e diagnósticos
A previsão vem da API pública Open-Meteo. O app precisa de conexão para buscar novos dados. Se a busca falhar, experimente abrir a Open-Meteo diretamente no navegador e desligar temporariamente VPN/DNS privado. A previsão não é uma medida oficial de chuva observada. Avisos por volume elevado aparecem apenas com o app aberto.

**Atenção:** o repositório é **privado**. GitHub Pages em repositórios privados exige plano compatível do GitHub. Não altere a visibilidade do repositório inteiro sem avaliar os demais projetos nele presentes.
