# TecMotors

App Android nativo para controle de carro e moto, com foco em:
- tela de resumo: gasto do mes vs. orcamento, km/L, custo por km, o que esta vencendo
- registro rapido de abastecimento (valor pago + litros + odometro)
- consumo de combustivel e distancia percorrida
- manutencao preventiva com dashboard de saude dos componentes
- calculadora de combustivel
- lembretes condicionais com acao direta na notificacao
- login Google + sincronizacao na nuvem (opcional)

Versao atual: veja `app/version.properties` (incrementa a cada build).

## Stack
- Kotlin
- Jetpack Compose (Material 3)
- Core SplashScreen (abertura)
- Room (persistencia local)
- Firebase Auth + Firestore (sync opcional)
- AlarmManager + Notification
- KSP (codegen Room)

## Arquitetura
- `presentation` -> `domain` -> `data`
- DI manual com `AppContainer`
- telas por feature com `UiState` + `UiEvent`
- sem acesso direto da UI a persistencia/cloud
- navegacao por barra inferior (Resumo, Manutencao, Relatorios, Veiculos) + FAB de registro rapido

Detalhes: `docs/ARCHITECTURE.md`

## Requisitos
- JDK 17 ou superior (testado com 25)
- SDK Android com `platforms;android-36.1`, `build-tools;36.1.0` e `platform-tools`
- Gradle 9+ (wrapper incluso)
- Android Studio e opcional: o build funciona so com o SDK via linha de comando

### SDK sem Android Studio

```bash
# 1. cmdline-tools
mkdir -p ~/Android/Sdk/cmdline-tools
curl -fsSL -o /tmp/cmdtools.zip \
  https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
unzip -q /tmp/cmdtools.zip -d /tmp/cmdtools
mv /tmp/cmdtools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest

# 2. pacotes
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
yes | sdkmanager --licenses
sdkmanager --install "platform-tools" "platforms;android-36.1" "build-tools;36.1.0"

# 3. apontar o projeto (local.properties esta no .gitignore)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

## Configurar Firebase (opcional)
1. Crie o projeto no Firebase.
2. Habilite Google Sign-In em Authentication.
3. Crie Firestore.
4. Registre app Android com pacote `br.com.tec.tecmotors`.
5. Baixe `google-services.json`.
6. Copie para `app/google-services.json`.
7. Sincronize Gradle.

Sem esse arquivo o app funciona localmente, mas sem login/sync.

## Gerar APK

### APK Debug (desenvolvimento / teste rapido)

```bash
./gradlew assembleDebug
```

APK gerado em `app/build/outputs/apk/debug/`, com nome versionado:
```
TecMotors-v<versionName>-build<versionCode>-debug.apk
```

> O build incrementa `app/version.properties` automaticamente a cada
> `assembleDebug`/`assembleRelease`, entao esse arquivo aparece modificado no git.

### APK Release (distribuicao)

> Requer keystore configurada. Sem isso o build de release falha na assinatura.

**1. Criar keystore (uma vez so):**
```bash
keytool -genkey -v \
  -keystore tecmotors.jks \
  -alias tecmotors \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**2. Configurar credenciais em `app/build.gradle.kts`:**
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../tecmotors.jks")
            storePassword = "sua_senha"
            keyAlias = "tecmotors"
            keyPassword = "sua_senha"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

**3. Gerar:**
```bash
./gradlew assembleRelease
```

APK gerado em:
```
app/build/outputs/apk/release/app-release.apk
```

### Alternativa: Android Studio

`Build > Build Bundle(s) / APK(s) > Build APK(s)`

O APK fica em `app/build/outputs/apk/debug/`.

## Instalar via ADB

```bash
# Instalar (ou atualizar) no dispositivo conectado
adb install -r app/build/outputs/apk/debug/TecMotors-*-debug.apk

# Ver dispositivos conectados
adb devices
```

## Outros comandos Gradle

```bash
# Testes unitarios
./gradlew :app:testDebugUnitTest

# Compilar APK de testes instrumentados
./gradlew :app:assembleAndroidTest

# Limpar build
./gradlew clean

# Ver tarefas disponíveis
./gradlew tasks
```

## Migracoes de banco (Room)

Nao usamos Liquibase neste projeto.

Usamos migracao nativa do Room com `Migration(versaoAntiga, versaoNova)`:
- Versao do banco: `@Database(version = 3)` em `app/src/main/java/br/com/tec/tecmotors/data/local/TecMotorsDatabase.kt`
- Migracoes em `app/src/main/java/br/com/tec/tecmotors/data/local/migration/RoomMigrations.kt`
- Registro: `.addMigrations(...)` em `app/src/main/java/br/com/tec/tecmotors/core/di/AppContainer.kt`
- Snapshot de schema: `app/schemas/br.com.tec.tecmotors.data.local.TecMotorsDatabase/`

Importante:
- Migracao Room (schema do SQLite) e diferente da importacao legada de prefs.
- Importacao legada de `SharedPreferences` fica em `LegacyImportManager` e roda no primeiro boot para preservar dados antigos.

Quando alterar entidades/tabelas:
1. Atualize a versao em `@Database`.
2. Crie nova migracao em `RoomMigrations`.
3. Registre no `AppContainer` com `.addMigrations(...)`.
4. Rode build para gerar/atualizar schema em `app/schemas`.

## Estrutura atual (resumo)

```text
app/src/main/java/br/com/tec/tecmotors/
  MainActivity.kt
  core/
    di/AppContainer.kt
  data/
    local/
    remote/
    repository/
    CsvExporter.kt
  domain/
    model/
    repository/
    usecase/
  presentation/
    app/
    vehicles/
    refuels/
    maintenance/     <- inclui ComponentsDashboard (saude dos componentes)
    reports/
    account/
  reminder/
  ui/theme/
```

## Observacoes
- Ha migracao automatica de dados legados de `SharedPreferences` para Room.
- O app exibe versao no canto inferior direito.
- Icone customizado esta no manifest (`logo_launcher_app`).
- O dashboard de saude dos componentes aparece na aba Manutencao automaticamente quando ha registros com vencimento por km ou manutencoes concluidas.
