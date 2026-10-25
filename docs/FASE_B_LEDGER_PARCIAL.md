# Fase B — Ledger econômico parcial

## Implementado

- Modelo de evento tipado em `EconomyLedger.kt`.
- Cada evento exige agente, tick não negativo, item/identificador, quantidade positiva e causa.
- O coordenador de mercado registra a transação no ledger para comprador e vendedor.
- A tela Eventos mostra a quantidade de liquidações e informa o escopo limitado do ledger.
- Testes cobrem validação de eventos e registro de liquidações.

## Limitações conhecidas

- O ledger agora é serializado em `SharedPreferences` do servidor e recuperado na abertura do app. Linhas inválidas são ignoradas durante a recuperação; ainda não existe reconciliação/replay com os saldos e inventários dos agentes.
- As rotinas de coleta, caça, pesca, refino, fabricação e tutorial ainda não estão todas ligadas ao ledger.
- Não existe um estoque natural compartilhado finito; por isso ainda há caminhos em que a produção pode gerar itens diretamente.
- A moeda inicial e algumas recompensas ainda não têm uma contabilidade global completa.
- A UI não deve apresentar o ledger atual como balanço econômico total.

## Próximos critérios de aceite

1. Cada mutação de inventário ou saldo deve emitir evento causal.
2. Transferências de comércio devem gerar eventos pareados sem contar transferência como criação de moeda.
3. Estoques do mundo devem ser finitos, compartilhados e atualizados de forma determinística.
4. **Parcialmente atendido:** o histórico sobrevive a reinício; ainda falta reconciliar o histórico com saldos/quantidades dos agentes e testar reinicialização do app no dispositivo.
5. Testes devem verificar conservação de itens e moeda em compra, fabricação, refino, recompensa, reparo e falha de ação.


## Finite shared resource nodes added

- `WorldResourceStore` holds finite shared stocks for T1-T3 gathering resources, creatures, and T1 fish.
- Gathering, hunting, and fishing consume from this shared store before adding resources to an agent inventory.
- A failed harvest does not partially reduce stock; depleted sources do not award items or hunting currency.
- The UI persists the shared stock snapshot separately and displays remaining units and depleted source types.
- Tests cover atomic harvest, exhausted sources, and restoring stock counts after serialization.

This is still not a full closed economy: scripted tutorial starter materials/rewards are explicitly recorded as such, and some recipes and currency changes still need full coverage. Resource regeneration, location-specific nodes, and ledger reconciliation with agent state remains future work.
