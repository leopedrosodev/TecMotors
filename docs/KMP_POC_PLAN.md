# KMP POC Plan - TecMotors

Status geral: `Em andamento`
Iniciado em: `03/03/2026`

## Objetivo
Validar compartilhamento de regras de negocio entre Android e iOS com Kotlin Multiplatform, sem quebrar o app Android atual.

## Escopo da POC
- Compartilhar modelos de dominio e calculos centrais (relatorios/manutencao).
- Definir contratos de repositorio no modulo compartilhado.
- Manter Android com Room/Firebase nesta fase.
- Preparar base para iOS consumir o modulo `shared`.

## Estrutura alvo
- `app/` Android atual (Compose/Room/Firebase)
- `shared/` novo modulo Kotlin Multiplatform
- `shared/src/commonMain/` dominio compartilhado
- `shared/src/androidMain/` adaptacoes Android (se necessario)
- `shared/src/iosMain/` adaptacoes iOS (fase seguinte)
- `iosApp/` (fase seguinte)

## Checklist de etapas

### Etapa 1 - Bootstrap KMP
- [x] Criar modulo `shared`.
- [x] Configurar Gradle (plugins e include no projeto).
- [x] Adicionar source sets iniciais (`commonMain`, `commonTest`, `androidMain`, `iosMain`).
- [x] Validar compilacao do modulo compartilhado.

### Etapa 2 - Domain shared
- [ ] Mover `models` de dominio para `shared`.
- [ ] Mover use cases de calculo (periodo/mensal/r$/km/manutencao).
- [ ] Ajustar app Android para consumir classes de dominio do `shared`.

### Etapa 3 - Contratos de dados
- [ ] Definir interfaces de repositorio no `shared`.
- [ ] Adaptar implementacoes Android existentes para os contratos.
- [ ] Garantir testes unitarios dos use cases no `shared`.

### Etapa 4 - POC iOS minima
- [ ] Subir `iosApp` com SwiftUI.
- [ ] Integrar `shared` no iOS.
- [ ] Implementar fluxo minimo: lista veiculos, abastecimentos e relatorio mensal.

### Etapa 5 - Validacao final da POC
- [ ] Comparar resultados Android vs iOS para o mesmo dataset.
- [ ] Executar smoke test Android sem regressao.
- [ ] Registrar decisoes e proxima fase no README/docs.

## Riscos e mitigacoes
- Risco: divergencia de modelos durante migracao.
  - Mitigacao: migrar por blocos pequenos e manter testes de calculo.
- Risco: build iOS exige ambiente macOS.
  - Mitigacao: nesta maquina Linux focar bootstrap e dominio comum; integrar iOS em ambiente Mac na etapa 4.
- Risco: regressao no Android atual.
  - Mitigacao: manter app funcional e validar com `:app:compileDebugKotlin` e testes a cada etapa.

## Definicao de pronto da POC
- Modulo `shared` ativo no projeto.
- Calculadoras de dominio compartilhadas e cobertas por teste.
- Android consumindo shared sem regressao.
- iOS consumindo shared com fluxo minimo funcional (em ambiente Mac).

## Log de andamento
- 03/03/2026: Etapa 1 concluida.
  - Modulo `shared` criado e incluido no projeto.
  - Build validado com `:shared:compileDebugKotlinAndroid`, `:shared:testDebugUnitTest` e `:app:assembleDebug`.
  - Observacao tecnica: aplicado workaround temporario no `gradle.properties` para compatibilidade AGP 9 + KMP (`android.builtInKotlin=false` e `android.newDsl=false`).
- 06/03/2026: Etapa 2 em andamento (incremental).
  - Use cases de calculo no Android passaram a delegar regras para `shared` (periodo, r$/km, status de manutencao e resumo do veiculo).
  - `CalculatePeriodReportUseCase` agora aceita `maintenanceRecords` e usa o mesmo nucleo de calculo compartilhado.
  - Adicionado `CalculateSharedVehicleSummaryUseCase` no modulo KMP para reduzir duplicacao de regra no app.
  - Proximo passo da etapa 2: mover modelos de dominio para `shared` e reduzir wrappers Android.
