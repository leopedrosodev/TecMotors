# Tec Motors

App Android nativo para controle de carro e moto, com foco em:
- consumo de combustivel
- gasto semanal/mensal
- distancia percorrida
- quantidade de abastecimentos
- manutencao preventiva
- calculadora de combustivel
- lembretes no inicio e fim do mes
- login Google + sincronizacao na nuvem (opcional)

Versao atual: `1.3.0 (8)`

## Stack
- Kotlin
- Jetpack Compose (Material 3)
- Room (persistencia local)
- Firebase Auth + Firestore (sync opcional)
- AlarmManager + Notification
- KSP (codegen Room)

## Arquitetura
- `presentation` -> `domain` -> `data`
- DI manual com `AppContainer`
- telas por feature com `UiState` + `UiEvent`
- sem acesso direto da UI a persistencia/cloud

Detalhes: `docs/ARCHITECTURE.md`

## Migracoes de banco (Room)
Nao usamos Liquibase neste projeto.

Usamos migracao nativa do Room com `Migration(versaoAntiga, versaoNova)`:
- Versao do banco: `@Database(version = 2)` em `app/src/main/java/br/com/tec/tecmotors/data/local/TecMotorsDatabase.kt`
- Migracao atual: `MIGRATION_1_2` em `app/src/main/java/br/com/tec/tecmotors/data/local/migration/RoomMigrations.kt`
- Registro da migracao: `.addMigrations(RoomMigrations.MIGRATION_1_2)` em `app/src/main/java/br/com/tec/tecmotors/core/di/AppContainer.kt`
- Snapshot de schema: `app/schemas/br.com.tec.tecmotors.data.local.TecMotorsDatabase/`

Importante:
- Migracao Room (schema do SQLite) e diferente da importacao legada de prefs.
- Importacao legada de `SharedPreferences` fica em `LegacyImportManager` e roda no primeiro boot para preservar dados antigos.

Quando alterar entidades/tabelas:
1. Atualize a versao em `@Database`.
2. Crie nova migracao em `RoomMigrations`.
3. Registre no `AppContainer` com `.addMigrations(...)`.
4. Rode build para gerar/atualizar schema em `app/schemas`.

## Requisitos
- Android Studio atualizado
- JDK 17
- SDK Android instalado

## Configurar Firebase (opcional)
1. Crie o projeto no Firebase.
2. Habilite Google Sign-In em Authentication.
3. Crie Firestore.
4. Registre app Android com pacote `br.com.tec.tecmotors`.
5. Baixe `google-services.json`.
6. Copie para `app/google-services.json`.
7. Sincronize Gradle.

Sem esse arquivo o app funciona localmente, mas sem login/sync.

## Comandos principais
```bash
cd /home/leonardoti03/codes/github/LeoMotors

# Build debug
./gradlew :app:assembleDebug

# Testes unitarios
./gradlew :app:testDebugUnitTest

# Compilar APK de testes instrumentados
./gradlew :app:assembleAndroidTest
```

## APK
Gerado em:
- `app/build/outputs/apk/debug/app-debug.apk`

Renomear para `Tec-motors.apk`:
```bash
cp app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/debug/Tec-motors.apk
```

## Instalar via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

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
    maintenance/
    reports/
    account/
  reminder/
  ui/theme/
```

## Observacoes
- Ha migracao automatica de dados legados de `SharedPreferences` para Room.
- O app exibe versao no canto inferior direito.
- Icone customizado esta no manifest (`logo_launcher_app`).
