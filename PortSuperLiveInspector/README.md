# Port SuperLive Inspector

Aplicativo Android nativo para examinar **somente arquivos acessíveis** relacionados ao pacote \`com.superlive.liveapp\`.

## Recursos
- Identifica versão e caminhos de instalação via PackageManager.
- Lista entradas do APK principal e de split APKs.
- Verifica se as pastas públicas e privadas existem e estão acessíveis.
- Usa o seletor do Android para listar uma pasta autorizada pelo usuário.
- Permite filtrar e copiar o relatório.

**Limitação:** no Android 11+, pastas privadas de outros aplicativos são protegidas. O Inspector não contorna as proteções, não usa root, não altera saldo e não modifica o SuperLive.

## Compilar
Abra **Actions > Port SuperLive Inspector APK > Run workflow** no repositório GitHub. O artefato \`PortSuperLiveInspector-APK\` contém o APK de depuração.

Projeto: Java 17, Gradle 8.7, Android Gradle Plugin 8.5.2, compileSdk 35, minSdk 26, targetSdk 32.
