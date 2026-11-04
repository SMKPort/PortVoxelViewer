# ⚡ Port Elétrica 1.0

Aplicativo Android offline para exercícios de eletricidade básica. Interface preta, vermelha e dourada.

## Recursos
- Resolver enunciados simples (Lei de Ohm, potência, energia e resistores em série/paralelo) com etapas.
- Calculadora guiada de tensão, corrente, resistência, potência, consumo, carga e resistividade.
- Circuitos mistos aninhados, por exemplo `S(10;P(20;30))`.
- Treino com correção, formulário de fórmulas e histórico local.

## Gerar APK
Abra [Actions do repositório](https://github.com/SMKPort/PortVoxelViewer/actions/workflows/port-eletrica-apk.yml), selecione **Port Elétrica | Gerar APK** e clique em **Run workflow**, ou atualize a pasta `PortEletrica/` para disparar automaticamente. No resultado, baixe o artefato **PortEletrica-Android-APK** e extraia o `app-debug.apk`.

Projeto Android independente. Android Gradle Plugin 8.9.2, Gradle 8.11.1, Java 17 e SDK 35. Também pode abrir no Android Studio a pasta `PortEletrica`.

## Testes
`cd PortEletrica && node --test tests/solver.test.js`

## Observações
Não requer internet ou login. O reconhecedor de texto é por padrões e não entende fotos nem todos os enunciados. Circuitos são resistivos ideais em CC. APK de debug para teste, sem assinatura de release.
