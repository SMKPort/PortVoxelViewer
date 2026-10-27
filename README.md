# Port Voxel Viewer v1.3

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
