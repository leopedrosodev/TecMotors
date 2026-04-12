# Plano de Implementacao de Refatoracao

Status geral: `Proposto`
Atualizado em: `12/04/2026`

## Objetivo
Transformar os pontos de melhoria identificados no app em um backlog executavel, priorizado por impacto/esforco e alinhado com o estado atual do codigo.

## Resumo executivo
- Prioridade imediata: `feedback tipado`, `cor de destaque no tema`, `componentes Compose compartilhados` e `event channel para snackbars`.
- Itens com melhor retorno na base atual: `1`, `2`, `3` e `6`.
- Itens que pedem migracao de schema ou decisao de produto: `4` e `5`.
- Itens parcialmente iniciados: `8` e `9`.

## Observacoes sobre o estado atual
- O app hoje usa `feedback: String?` em `vehicles`, `refuels` e `maintenance`, e `exportFeedback: String?` em `reports`.
- O `SnackbarHost` central fica em [TecMotorsRoot.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/presentation/app/TecMotorsRoot.kt).
- A cor `Color(0xFF2F81F7)` ainda aparece hardcoded em pontos de UI, apesar de o tema ja ter `AutoBlueDark`.
- Ja existe uma base de testes unitarios no modulo `app` e testes de calculo no modulo `shared`, entao o item de cobertura deve focar em lacunas, nao em partir do zero.
- A migracao KMP ja comecou. O modulo `shared` existe e parte das regras de calculo ja foi extraida.

## Ordem recomendada
1. Item `1` - Feedback tipado
2. Item `2` - Cor de destaque no tema
3. Item `3` - Componentes Compose compartilhados
4. Item `6` - Event channel para snackbars
5. Item `9` - Fechar lacunas de testes
6. Itens `4`, `5`, `7` e `8` conforme roadmap e folego de arquitetura

## Tier 1 - Alta prioridade

### 1. Sistema de feedback tipado
Impacto: `Alto`
Esforco: `Baixo`

#### Dor atual
- Mensagens de erro e sucesso compartilham o mesmo transporte e o mesmo tratamento visual.
- `LaunchedEffect(feedback)` depende de `String?`, o que limita a evolucao da UX.

#### Estado atual no codigo
- `feedback: String?` em [VehiclesContract.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/presentation/vehicles/VehiclesContract.kt), [RefuelsContract.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/presentation/refuels/RefuelsContract.kt) e [MaintenanceContract.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/presentation/maintenance/MaintenanceContract.kt).
- `exportFeedback: String?` em [ReportsContract.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/presentation/reports/ReportsContract.kt).
- Coleta e exibicao centralizadas em [TecMotorsRoot.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/presentation/app/TecMotorsRoot.kt).

#### Proposta
- Criar `sealed class UiFeedback` em `presentation.common`.
- Estrutura sugerida:

```kotlin
sealed class UiFeedback(
    open val message: String
) {
    data class Success(override val message: String) : UiFeedback(message)
    data class Error(override val message: String) : UiFeedback(message)
    data class Info(override val message: String) : UiFeedback(message)
}
```

- Substituir `String?` por `UiFeedback?` nos contratos de feature.
- Atualizar ViewModels para emitirem `UiFeedback.Success` e `UiFeedback.Error`.
- No `SnackbarHost`, aplicar cor por tipo:
  - `Error`: vermelho semantico
  - `Success`: verde semantico
  - `Info`: cor neutra ou `inverseSurface`

#### Arquivos-alvo
- `presentation/common/UiFeedback.kt`
- `presentation/vehicles/*`
- `presentation/refuels/*`
- `presentation/maintenance/*`
- `presentation/reports/*`
- `presentation/app/TecMotorsRoot.kt`

#### Criterios de aceite
- Erros e sucessos aparecem com tratamento visual distinto.
- Nenhuma feature fica dependente de string solta para semantica de feedback.
- O app continua compilando sem regressao de fluxo de snackbar.

#### Observacao
- Este item prepara o terreno para o item `6`. Se quisermos fatiar pequeno, primeiro tipamos o feedback mantendo state; depois trocamos o transporte para eventos.

### 2. Extrair cor de destaque para o tema
Impacto: `Medio`
Esforco: `Baixo`

#### Dor atual
- O tom azul de destaque aparece repetido em componentes fora do tema.

#### Estado atual no codigo
- Hardcoded em [VehiclesScreen.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/presentation/vehicles/VehiclesScreen.kt) e [VehicleSelectors.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/presentation/common/VehicleSelectors.kt).
- O tema ja define `AutoBlueDark` e `AutoBlueLight` em [Color.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/ui/theme/Color.kt) e aplica `primary` em [Theme.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/ui/theme/Theme.kt).

#### Proposta
- Padronizar o destaque em uma unica fonte.
- Opcao preferida: reutilizar `MaterialTheme.colorScheme.primary` ou `tertiary` onde o papel visual for de destaque.
- Opcao alternativa: expor uma extensao semantica, por exemplo `accentHighlight`, se a cor tiver funcao diferente de `primary`.

#### Criterios de aceite
- Nenhum componente de destaque usa `Color(0xFF2F81F7)` diretamente.
- A mudanca de tom passa a exigir ajuste em um unico ponto.

### 3. Componentes Compose compartilhados
Impacto: `Alto`
Esforco: `Baixo/Medio`

#### Dor atual
- Campos numericos e de data se repetem em varias telas com configuracoes muito parecidas.
- Isso aumenta custo de manutencao e dificulta padronizar comportamento visual.

#### Estado atual no codigo
- Repeticao de `OutlinedTextField` com `KeyboardType.Decimal` em `vehicles`, `refuels`, `maintenance`, `reports` e `fuel calculator`.
- Campos de data com label `label_date_br` tambem se repetem.

#### Proposta
- Criar componentes em `presentation.common`:
  - `DecimalField`
  - `MoneyField`
  - `DateBrField`
- Manter API simples e sem acoplamento a feature.

#### Assinaturas sugeridas

```kotlin
@Composable
fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
)

@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
)

@Composable
fun DateBrField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

#### Criterios de aceite
- Pelo menos `vehicles`, `refuels` e `reports` passam a usar os componentes compartilhados.
- Configuracoes de teclado e estilo deixam de ser copiadas entre telas.
- O comportamento visual permanece consistente.

## Tier 2 - Media prioridade

### 4. Factory de ID centralizada no repositorio
Impacto: `Medio`
Esforco: `Medio`

#### Dor atual
- O padrao `(dao.maxId() ?: 0L) + 1L` aparece repetido em quatro repositorios.

#### Estado atual no codigo
- Repeticao em [RoomRepositories.kt](/home/leonardo/codes/github/TecMotors/app/src/main/java/br/com/tec/tecmotors/data/repository/RoomRepositories.kt).

#### Proposta
- Passo curto: extrair helper local para reduzir duplicacao.
- Passo longo: avaliar migracao para `@PrimaryKey(autoGenerate = true)`.

#### Recomendacao
- Executar primeiro apenas o helper.
- So partir para `autoGenerate` se houver motivacao concreta de sync, importacao ou interoperabilidade.

#### Riscos
- Migracao de schema e revisao de importacao/snapshot.
- Potencial impacto em restore, sync remoto e compatibilidade com dados legados.

### 5. Desacoplar `VehicleType.OTHER` de budget e relatorios
Impacto: `Medio/Alto`
Esforco: `Medio/Alto`

#### Dor atual
- O suporte a `OTHER` ficou parcial e depende de `return` ou fallback `0.0`.

#### Estado atual no codigo
- `VehicleType.OTHER -> return` em `SettingsRepositoryImpl.setMonthlyBudget`.
- `VehicleType.OTHER, null -> 0.0` em `ReportsViewModel`.

#### Proposta
- Migrar de colunas fixas para representacao extensivel:
  - opcao A: tabela `vehicle_budgets(vehicle_type, amount)`
  - opcao B: estrutura serializada no settings

#### Recomendacao
- Preferir tabela dedicada se `OTHER` realmente for virar classe de primeira linha.
- Nao fazer agora sem alinhamento de produto.

#### Dependencias
- Migracao Room
- Ajuste de mapper local/remoto
- Revisao de snapshot e importacao legada

### 6. Event channel para snackbars
Impacto: `Alto`
Esforco: `Medio`

#### Dor atual
- O fluxo `feedback no state -> LaunchedEffect -> showSnackbar -> ClearFeedback` e verboso.
- Repetir a mesma mensagem pode nao disparar nova exibicao.

#### Proposta
- Expor `SharedFlow<UiFeedback>` nos ViewModels de feature.
- Coletar eventos no `TecMotorsRoot`.
- Remover `ClearFeedback` dos contratos onde ele existir apenas para snackbar.

#### Estrategia sugerida
- Fazer depois do item `1`, reaproveitando `UiFeedback`.
- Migrar por feature, nao tudo de uma vez.

#### Criterios de aceite
- Mensagens iguais podem ser exibidas em sequencia.
- Eventos transitivos nao poluem mais o `UiState`.
- `ClearFeedback` deixa de existir nas features migradas.

## Tier 3 - Baixa prioridade

### 7. Migrar ViewModels para Hilt ou Koin
Impacto: `Medio`
Esforco: `Medio/Alto`

#### Dor atual
- `TecMotorsRoot` concentra bastante boilerplate de fabrica de ViewModel.

#### Proposta
- Adotar Hilt se houver intencao de seguir stack oficial Android.

#### Recomendacao
- Nao tratar como urgente.
- Antes disso, vale considerar um passo menor: extrair funcoes de construcao ou um provider local para reduzir verbosidade.

### 8. Mover logica compartilhada para `:shared`
Impacto: `Alto`
Esforco: `Alto`

#### Estado atual
- Este trabalho ja esta em andamento.
- O modulo `shared` existe e ja concentra parte dos casos de uso e testes.
- O documento [KMP_POC_PLAN.md](/home/leonardo/codes/github/TecMotors/docs/KMP_POC_PLAN.md) mostra a trilha atual.

#### Proposta atualizada
- Continuar a extracao em fatias pequenas.
- Prioridade natural: modelos de dominio e wrappers Android restantes.

#### Recomendacao
- Tratar como frente separada da refatoracao de UI.
- Evitar misturar essa migracao com itens de Compose no mesmo PR.

### 9. Cobertura de testes
Impacto: `Alto`
Esforco: `Baixo/Medio`

#### Ajuste importante
- O projeto ja possui testes unitarios relevantes no `app` e no `shared`.
- A oportunidade aqui e fechar lacunas de regra e regressao, nao iniciar a suite.

#### Lacunas candidatas
- Casos-limite adicionais para `CalculatePeriodReportUseCase`
- Repeticao de mensagem/evento apos migracao do item `6`
- Feedback tipado nas ViewModels apos item `1`
- Campos compartilhados de input, se receberem logica propria
- Regras de budget para `OTHER`, se o item `5` entrar no roadmap

#### Criterios de aceite
- Cada refatoracao de comportamento vem acompanhada de teste no mesmo ciclo.
- Casos de regressao dos itens `1`, `3` e `6` ficam cobertos.

## Fatiamento recomendado por PR

### PR 1 - Feedback tipado
- Criar `UiFeedback`
- Atualizar contratos e ViewModels
- Ajustar `SnackbarHost` com cor semantica

### PR 2 - Tema e componentes compartilhados
- Remover hardcodes de cor
- Introduzir `DecimalField`, `MoneyField` e `DateBrField`
- Aplicar em telas prioritarias

### PR 3 - Snackbars por evento
- Introduzir `SharedFlow`
- Remover `ClearFeedback`
- Cobrir repeticao de eventos com teste

### PR 4 - Lacunas de teste
- Consolidar cobertura das refatoracoes anteriores

## Decisoes em aberto
- `UiFeedback` deve carregar apenas `message` ou tambem `actionLabel`, `duration` e `cause`?
- A cor de sucesso/erro ficara em `colorScheme` padrao ou em extensoes semanticas do tema?
- O item `5` realmente faz parte do roadmap de produto ou pode continuar como suporte parcial?
- O item `7` vale o custo agora ou o projeto ainda se beneficia de DI manual?

## Recomendacao pratica para o proximo passo
Comecar pelo item `1`.

Motivos:
- baixo risco
- ganho visual imediato
- reduz ambiguidade semantica
- prepara o item `6`
- toca poucos arquivos e tem rollback simples
