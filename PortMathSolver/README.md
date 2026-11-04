# Port Math Solver 1.0 📐

Aplicativo educacional offline de Álgebra Linear em Android e navegador. Parte do repositório de aplicativos SMKPort/PortVoxelViewer, isolado em `PortMathSolver/`, sem alterar os demais apps.

## Módulos
- Matrizes: operações, determinante, inversa, transposta, posto e redução Gauss-Jordan.
- Sistemas lineares: solução única, infinitas soluções, incompatíveis, Cramer e mínimos quadrados.
- Vetores, produto escalar/vetorial, projeções, ângulos e ortogonalização.
- Mudança de base, espaço linha, espaço coluna, núcleo, imagem e independência linear.
- Autovalores, autovetores, diagonalização e polinômio característico.
- LU, QR, Cholesky e SVD econômica.
- Transformações lineares, biblioteca de estudo, exercícios, histórico e exportação PDF.
- OCR de texto por câmera/galeria no Android, com conferência manual do reconhecimento.

## Como abrir
No navegador, abra `PortMathSolver/PortMathSolver_Demo.html`. Em Android, baixe o artefato da ação **Port Math Solver - Gerar APK** na aba Actions do GitHub.

## Gerar APK manualmente
```bash
cd PortMathSolver
node --test tests/engine.test.js
gradle --no-daemon assembleDebug
```
Requer Java 17, Gradle 8.11.1 e Android SDK 35. O instalador debug fica em `PortMathSolver/app/build/outputs/apk/debug/app-debug.apk`.

## Notas
- Cálculos numéricos em ponto flutuante (não há álgebra simbólica universal).
- Matrizes gerais não simétricas acima de 2×2 ainda não têm algoritmo completo de autovalores.
- Forma de Jordan e autovetores complexos gerais não estão implementados.
- OCR é reconhecimento de texto e pode errar números ou sinais, que precisam ser conferidos.
- A compilação pela Actions depende do SDK e downloads externos; confira a execução antes de instalar.
- Licença MIT.
