# Edge Glow AOD (Moto Edge 50)

Aplicativo Android nativo de **Always On Display (AOD)** com efeito de **Edge Lighting (luzes vibrantes em curva)** projetado especificamente para o painel pOLED do **Motorola Moto Edge 50**, agora equipado com **Assistente de Voz Ininterrupto** e **Atualizações Automáticas**.

---

## 📱 Recursos Principais

1. **Iluminação de Borda Curva (Moto Edge 50)**:
   - Traçado adaptado às curvas laterais e cantos da tela pOLED (raio padrão de 42dp ajustável).
   - Efeito gradiente giratório contínuo com pulso neon e glow difuso exterior.

2. **Always On Display Real para Notificações**:
   - Acorda a tela instantaneamente quando chega uma notificação (WhatsApp, Telegram, Instagram, etc.).
   - Fundo 100% preto (`#000000`), aproveitando os pixels totalmente desligados da tela OLED para economia drástica de bateria.
   - Relógio minimalista e exibição do remetente/mensagem.
   - Timer configurável de desligamento automático (5s a 30s) para não drenar energia.

3. **Assistente de Voz Ininterrupto (Estilo Gemini Live & Alexa)**:
   - **Sem cortes ou timeouts de 3 segundos**: diferente do `SpeechRecognizer` tradicional do Android, utiliza streaming de baixo nível com `AudioRecord` (16kHz PCM) em um Foreground Service de microfone.
   - **Palavra de Ativação (Wake Word)**: Escuta contínua de baixa energia por palavras-chave (ex: *"Edge"*, *"Hey Edge"*).
   - **Conversa Contínua (Continuous Dialogue)**: Ao acionar a IA e receber a resposta, o microfone permanece aberto sem exigir repetição da palavra de ativação para novas réplicas.
   - **Feedback Visual Neon**: Acende as bordas curvas do Moto Edge 50 quando o assistente está ouvindo ativamente.
   - **Integração com Gemini API**: Suporte a respostas inteligentes e síntese de voz (TTS) em português.

4. **Sistema de Atualizações Automáticas (In-App Updater)**:
   - Checagem automática e silenciosa sempre que o app é aberto.
   - Diálogo nativo com resumo de novidades (changelog).
   - Opções: **"Instalar Agora"** (download direto com barra de progresso e instalador seguro com `FileProvider`) ou **"Lembrar Depois"** (adia lembrete por 24h).
   - Verificação manual disponível na tela principal a qualquer momento.

---

## 🛠️ Permissões Necessárias no Aparelho

Ao abrir o app pela primeira vez:
1. **Microfone (`RECORD_AUDIO`)**: Necessário para a escuta contínua e palavra de ativação.
2. **Acesso a Notificações**: Necessário para detectar mensagens recebidas (`NotificationListenerService`).
3. **Instalação de Fontes Desconhecidas**: Para permitir que o app atualize seu próprio APK automaticamente.
4. **Otimização de Bateria**: Desative para que o sistema não finalize o serviço de fundo.

---

## 🚀 Como Compilar e Instalar

### No Android Studio:
1. Abra esta pasta (`Edge`) no Android Studio.
2. Conecte o Motorola Moto Edge 50 via cabo USB (com Depuração USB ativada) ou Wi-Fi ADB.
3. Clique no botão verde de **Run (Shift + F10)**.

### Via Linha de Comando (Gradle):
```bash
./gradlew assembleDebug
```
O arquivo APK gerado ficará em:
`app/build/outputs/apk/debug/app-debug.apk`
