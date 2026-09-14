# Arquitetura Tec Motors

## Objetivo
Aplicar separacao por camadas e feature para reduzir acoplamento e melhorar testabilidade.

## Camadas
- `presentation`: Compose UI + ViewModels + contratos de estado/evento por feature.
- `domain`: modelos de dominio, interfaces de repositorio e casos de uso.
- `data`: Room (local), Firebase (remoto), mapeadores e implementacoes de repositorio.
- `core`: DI manual (`AppContainer`), preparo de abertura (`AppStartup`) e utilitarios compartilhados.

## Estrutura principal
- `MainActivity`: bootstrap Android e splash do sistema (`installSplashScreen`).
- `core/di/AppContainer.kt`: composicao de dependencias da aplicacao.
- `core/startup/AppStartup.kt`: importacao legada + veiculos padrao; a splash fica em pe ate `ready`.
- `data/local/`: banco Room (`TecMotorsDatabase`), DAOs, entities e snapshot local.
- `data/local/migration/`: migracoes de schema Room e importacao legada de `SharedPreferences`.
- `data/repository/`: implementacoes de `VehicleRepository`, `RefuelRepository`, `MaintenanceRepository`, `SettingsRepository`, `SnapshotRepository` e `SyncRepository`.
- `data/remote/RemoteSnapshotMapper.kt`: compatibilidade de schema remoto (incluindo documentos antigos sem manutencao).
- `domain/repository/Repositories.kt`: contratos de abstracao.
- `domain/usecase/`: regras de negocio por feature.
- `presentation/app/TecMotorsRoot.kt`: shell da aplicacao (barra inferior, FAB, toolbar, tema, login Google).
- `presentation/app/AppContract.kt`: `AppDestination` - quatro destinos primarios na barra inferior,
  os demais (Calculadora, historico de abastecimentos) sao secundarios e tem botao de voltar.
- `presentation/home/`: tela de resumo. Nao tem regra propria: compoe `CalculatePeriodReportUseCase`
  e `DecideRemindersUseCase`.
- `presentation/refuels/QuickRefuelSheet.kt`: registro rapido (valor pago + litros + odometro).
  Preco por litro e derivado; `RefuelsScreen` continua como historico e lancamento completo.
- `presentation/vehicles|refuels|maintenance|reports|account/`: ViewModels e telas especificas.

## Fluxo de dados
1. UI dispara `UiEvent`.
2. ViewModel orquestra casos de uso.
3. Caso de uso chama interface de repositorio (`domain`).
4. Repositorio concreto (`data`) le/escreve em Room e/ou Firebase.
5. `Flow` retorna para ViewModel e atualiza `UiState`.

## Persistencia local
- Banco: Room SQLite.
- Classe de banco: `data/local/TecMotorsDatabase.kt`.
- Versao atual do schema: `4`.
- Schemas exportados: `app/schemas/br.com.tec.tecmotors.data.local.TecMotorsDatabase/`.

## Abertura do app
- Tema `Theme.TecMotors.Starting` (Core SplashScreen) desenha desde o primeiro frame.
- `setKeepOnScreenCondition` segura a splash enquanto `AppStartup.ready` for falso, entao a
  duracao e o tempo real de preparo - nao um `delay` fixo.
- `windowBackground` do tema pos-splash usa `@color/brand_background`, o mesmo tom do
  `DarkBackground` do Compose, para nao haver flash branco.

## Lembretes
- `DecideRemindersUseCase` (dominio, testavel) decide o que merece notificacao:
  manutencao perto de vencer, virada de mes sem odometro, N dias sem abastecimento.
- Veiculo sem historico nunca gera alerta.
- Um unico alarme diario (`ReminderReceiver`) avalia; sem motivo, nada e publicado.
- `ReminderActionReceiver` atende os botoes da notificacao ("Ja troquei", "Adiar").
- A mesma decisao alimenta o card de atencao do Resumo, com limiar de km maior.

## Migracoes de banco (Room)
Nao usamos Liquibase.

Estratégia:
- Cada mudanca de schema incrementa `@Database(version = X)`.
- Para cada incremento, criamos uma migracao explicita em `RoomMigrations`.
- O `AppContainer` registra as migracoes com `.addMigrations(...)`.

Implementacao atual:
- `MIGRATION_1_2`, `MIGRATION_2_3` e `MIGRATION_3_4` em `data/local/migration/RoomMigrations.kt`.
- `RoomMigrations.ALL` e a fonte unica: `AppContainer` e os receivers de lembrete usam a mesma lista.

Objetivos da migracao:
- manter integridade do schema esperado pelo Room
- preservar dados locais no update do APK
- manter compatibilidade com schema antigo ja instalado no celular

## Importacao legada (SharedPreferences -> Room)
Isso nao e migracao de schema SQLite, e migracao de fonte de dados legada.

- Responsavel: `LegacyImportManager`.
- Le dados antigos de `SharedPreferences` via `LegacyPreferencesReader`.
- Executa no primeiro boot apos update.
- Preserva IDs e evita duplicidade com flag `legacyImportDone`.

## Sincronizacao
- `SyncRepositoryImpl` encapsula Firebase Auth + Firestore.
- Estrategia de conflito atual: timestamp `updatedAtMillis` mais recente vence.
- Compatibilidade mantida para snapshots antigos (sem `maintenanceRecords`).

## Testes
- Unit tests:
  - regras de relatorio
  - decisao de lembretes (`DecideRemindersUseCaseTest`)
  - derivacoes da tela de resumo (`HomeViewModelTest`)
  - status de manutencao
  - exportacao CSV
  - compatibilidade de mapper remoto
  - comportamento de ViewModels
- Instrumentation tests:
  - migracao legada para Room
  - smoke de navegacao/salvamento/persistencia

## Decisoes de arquitetura
- 1 modulo Android (`app`) com separacao forte por pacotes.
- DI manual (sem Hilt/Koin) para manter simplicidade do projeto.
- UI sem dependencia direta de storage/cloud.
