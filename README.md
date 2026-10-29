# Port Voxel Viewer v2.1 (beta 1)

Aplicativo Android offline para visualizar e editar parcialmente arquivos Blockbench (.bbmodel). Mantém as funções v1.3: importação, visualização 3D, animações, GIF, PNG, vista explodida e isolamento de peças.

## Skin Studio v2.1 beta 1

Abrir skins de Minecraft em PNG diretamente pelo botão **ABRIR .BBMODEL OU SKIN PNG**, pela seção **Skin Studio** no painel ou pelo gerenciador de arquivos Android quando o compartilhamento fornece o arquivo.

- Formatos: 64×64 moderno e 64×32 legado, além de resoluções proporcionais HD até 1024×1024 (ou 1024×512 para legado).
- Braços: modelo **Clássico/Steve (4 px)** e **Slim/Alex (3 px)**, escolha manual para skins modernas.
- Segundo plano: camada externa de cabeça, roupas, mangas e calças, com opção de ocultar ou mostrar.
- Prévia 3D em 360°, vistas frente/costas/lateral, animações simuladas de respiração e caminhada, capturas PNG e exportação de GIF via aba Animações.
- Baixar o PNG da skin original sem alterar seus pixels; o modelo 3D é gerado localmente e pode ser salvo como projeto .bbmodel na biblioteca.

**Limites:** a skin PNG não traz dados de animação, então caminhada e respiração são animações de demonstração. O tipo de braço Slim/Classic não é recuperável com certeza de uma imagem PNG; escolha manual. Skins HD têm prévia proporcional, mas são aceitas por serviços Minecraft conforme suas regras. Skins Bedrock com geometria personalizada ou pacotes .mcpack não estão incluídos nesta etapa. O APK ainda precisa compilar no GitHub Actions e ser testado em aparelho.

Testes automáticos em `tests/skin-format.test.cjs` validam atlas UV, número de cubos, camadas e ligação de grupos animados para clássico, slim, legado e HD.

---
## Novidades implementadas

- Renderização: cores de texturas preservadas, brilho de 40% a 160% e qualidade gráfica de 50% a 150%.
- Animações: interpolação Catmull-Rom com pontos vizinhos e suporte inicial a Bézier por alças numéricas, além de step e linear.
- Editor: rotação de cubos e grupos, centro e dimensões de cubos, desfazer e exportar cópia .bbmodel.
- Biblioteca local: IndexedDB com miniaturas e fallback de armazenamento básico.
- Estúdio: fundos alternativos, iluminação ajustável e até 24 câmeras salvas localmente.
- Exportações: GIF e PNG mantidos; imagem WebP, WebP animado com cancelamento e gravação MP4 condicionada ao suporte de MediaRecorder pelo WebView.
- Android: salvar WebP, .bbmodel e MP4 compatível pelo seletor de arquivos.

## Limitações da beta

O editor não modifica UV, vértices de malhas ou keyframes. Expressões MoLang e animações avançadas podem permanecer aproximadas. MP4 nativo universal ainda não foi implementado e a opção é desativada quando não houver suporte. A biblioteca é local, sem sincronização. O desempenho com 5.000 cubos e o APK precisam ser validados em dispositivo real.

## Gerar APK

O GitHub Actions valida a sintaxe JavaScript, configura Java 17, Android SDK 35, Gradle 8.9 e Android Gradle Plugin 8.7.3.
Abra https://github.com/SMKPort/PortVoxelViewer/actions , escolha a execução mais recente de Gerar APK Android, confirme o sucesso e baixe Port-Voxel-Viewer-Android-APK na seção Artifacts.

---

Aplicativo Android para abrir modelos do **Blockbench (.bbmodel)** no celular, visualizar peças e animações e exportar GIFs de forma offline.

## Funcionalidades
- Abrir projetos `.bbmodel` com o seletor de arquivos do Android ou pelo gerenciador de arquivos (quando o MIME é reconhecido).
- Girar, ampliar e reenquadrar modelos 3D; alternar entre vistas e destacar peças.
- Pesquisar, ocultar e isolar peças; vista explodida.
- Reproduzir animações e percorrer keyframes.
- Exportar capturas PNG e animações GIF (256, 384 ou 512 pixels; 8, 12 ou 20 FPS) pela opção **Salvar como**.
- Exemplos Braço Arcano e Grimório para experimentar sem arquivo externo.

## Instalar APK pelo GitHub
1. Abra a aba [Actions](https://github.com/SMKPort/PortVoxelViewer/actions) e escolha **Gerar APK Android**.
2. Aguarde uma execução aparecer com marcação de sucesso. Se não houver execução, clique **Run workflow** e escolha `main`.
3. Abra a execução e baixe **Port-Voxel-Viewer-Android-APK** em Artifacts.
4. Extraia o ZIP e abra o arquivo `app-debug.apk` no celular.

O APK gerado é de **teste (debug)**, não uma versão assinada de produção. A compilação é feita pelo GitHub Actions. Até que a execução termine com sucesso e o APK seja testado, a instalação e a compatibilidade com dispositivos Android físicos permanecem sem validação.

## Desenvolvimento local
Importe este repositório no Android Studio com Java 17, Android SDK 35 e Gradle 8.9. Execute `gradle :app:assembleDebug` ou use **Build > Generate APK(s)**. O arquivo sairá em `app/build/outputs/apk/debug/app-debug.apk`.

## Privacidade
Os modelos são processados no próprio aparelho. O app não solicita acesso amplo ao armazenamento nem permissão de internet. Ao exportar, você escolhe onde salvar o arquivo.

## Limitações
O visualizador não é o editor completo do Blockbench. Animações com scripts MoLang ou curvas complexas podem ser aproximadas. A exportação GIF tem limite de 240 quadros, usa até 256 cores e não preserva semitransparência. Algumas texturas externas precisam ser selecionadas separadamente.
