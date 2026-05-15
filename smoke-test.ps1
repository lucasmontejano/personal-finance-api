$ErrorActionPreference = 'Continue'
$base = "http://localhost:8080"
$rand = Get-Random -Minimum 100000 -Maximum 999999
$email = "smoke_$rand@teste.com"
$senha = "senha123"

$passou = 0
$total  = 0
$falhas = @()

function Json($obj) { $obj | ConvertTo-Json -Depth 10 -Compress }

function HttpReq {
    param([string]$Method, [string]$Path, [hashtable]$Headers = @{}, $Body = $null)
    $params = @{ Method = $Method; Uri = "$base$Path"; Headers = $Headers; ErrorAction = 'Stop' }
    if ($Body -ne $null) {
        $params.Body = (Json $Body)
        $params.ContentType = 'application/json'
    }
    try {
        $resp = Invoke-WebRequest @params -UseBasicParsing
        $body = if ($resp.Content) { $resp.Content | ConvertFrom-Json -ErrorAction SilentlyContinue } else { $null }
        return [PSCustomObject]@{ ok = $true; status = [int]$resp.StatusCode; body = $body }
    } catch {
        $code = 0; $body = $null
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $stream.Position = 0
                $reader = New-Object System.IO.StreamReader($stream)
                $raw = $reader.ReadToEnd()
                if ($raw) { $body = $raw | ConvertFrom-Json -ErrorAction SilentlyContinue }
            } catch {}
        }
        return [PSCustomObject]@{ ok = $false; status = $code; body = $body; err = $_.Exception.Message }
    }
}

function Check {
    param([string]$nome, [int[]]$esperados, [scriptblock]$expr)
    $script:total++
    $r = & $expr
    $passouAgora = $esperados -contains $r.status
    if ($passouAgora) {
        $script:passou++
        Write-Host ("[{0,2}] OK   " -f $script:total) -ForegroundColor Green -NoNewline
        Write-Host "$nome (status $($r.status))"
    } else {
        Write-Host ("[{0,2}] FAIL " -f $script:total) -ForegroundColor Red -NoNewline
        Write-Host "$nome (status $($r.status), esperado: $($esperados -join '/'))"
        if ($r.body) { Write-Host "        body: $(Json $r.body)" -ForegroundColor DarkGray }
        $script:falhas += $nome
    }
    return $r
}

Write-Host "`n=== SMOKE TEST - API FINANCAS ===" -ForegroundColor Cyan
Write-Host "Email de teste: $email`n"

# --------- AUTH ---------
$r = Check "POST /auth/cadastro" @(200) { HttpReq POST /auth/cadastro @{} @{ nome = "Smoke"; email = $email; senha = $senha } }
$token = $r.body.token
$auth = @{ Authorization = "Bearer $token" }

Check "POST /auth/login" @(200) { HttpReq POST /auth/login @{} @{ email = $email; senha = $senha } } | Out-Null

$r = Check "GET /me com token" @(200) { HttpReq GET /me $auth }
if ($r.body.email -ne $email) { Write-Host "        ALERTA: email no /me nao bate" -ForegroundColor Yellow }

Check "GET /me sem token (deve barrar)" @(401, 403) { HttpReq GET /me } | Out-Null

Check "POST /auth/cadastro com email repetido" @(400) { HttpReq POST /auth/cadastro @{} @{ nome = "X"; email = $email; senha = $senha } } | Out-Null

Check "POST /auth/login com senha errada" @(401) { HttpReq POST /auth/login @{} @{ email = $email; senha = "errada" } } | Out-Null

Check "POST /auth/cadastro com email invalido" @(400) { HttpReq POST /auth/cadastro @{} @{ nome = "X"; email = "naoeemail"; senha = "senha123" } } | Out-Null

# --------- CATEGORIAS (seed automatico) ---------
$r = Check "GET /categorias (esperando 13 do seed)" @(200) { HttpReq GET /categorias $auth }
if ($r.body.Count -ne 13) { Write-Host "        ALERTA: esperado 13, veio $($r.body.Count)" -ForegroundColor Yellow }
$catReceita = $r.body | Where-Object { $_.tipo -eq 'RECEITA' -and $_.nome -eq 'Salário' } | Select-Object -First 1
$catDespesa = $r.body | Where-Object { $_.tipo -eq 'DESPESA' -and $_.nome -eq 'Alimentação' } | Select-Object -First 1
if (-not $catReceita -or -not $catDespesa) { Write-Host "        ALERTA: nao achou Salario/Alimentacao no seed" -ForegroundColor Yellow }

Check "GET /categorias?tipo=DESPESA" @(200) { HttpReq GET '/categorias?tipo=DESPESA' $auth } | Out-Null

Check "POST /categorias duplicada (mesmo nome+tipo)" @(400) { HttpReq POST /categorias $auth @{ nome = "Salário"; tipo = "RECEITA" } } | Out-Null

# --------- CONTAS ---------
$r = Check "POST /contas (corrente, saldo 1000)" @(201) { HttpReq POST /contas $auth @{ nome = "Nubank"; tipo = "CORRENTE"; saldoInicial = 1000.00; cor = "#820AD1" } }
$conta1Id = $r.body.id

$r = Check "POST /contas (poupanca, saldo 500)" @(201) { HttpReq POST /contas $auth @{ nome = "Poupanca"; tipo = "POUPANCA"; saldoInicial = 500.00 } }
$conta2Id = $r.body.id

$r = Check "GET /contas (esperando 2)" @(200) { HttpReq GET /contas $auth }
if ($r.body.Count -ne 2) { Write-Host "        ALERTA: esperado 2, veio $($r.body.Count)" -ForegroundColor Yellow }

# --------- TRANSACOES ---------
$idemp = [guid]::NewGuid().ToString()

$r = Check "POST /transacoes RECEITA (idempotency)" @(201) {
    HttpReq POST /transacoes ($auth + @{ 'Idempotency-Key' = $idemp }) @{
        tipo = "RECEITA"; valor = 4500.00; descricao = "Salario"
        data = (Get-Date -Format "yyyy-MM-dd"); contaId = $conta1Id; categoriaId = $catReceita.id
    }
}
$txReceitaId = $r.body.id

$r2 = Check "POST /transacoes RECEITA mesma Idempotency-Key (nao deve duplicar)" @(201) {
    HttpReq POST /transacoes ($auth + @{ 'Idempotency-Key' = $idemp }) @{
        tipo = "RECEITA"; valor = 9999; descricao = "duplicada"
        data = (Get-Date -Format "yyyy-MM-dd"); contaId = $conta1Id; categoriaId = $catReceita.id
    }
}
if ($r2.body.id -ne $txReceitaId) { Write-Host "        ALERTA: idempotency falhou, id diferente!" -ForegroundColor Yellow }
if ($r2.body.valor -ne 4500.00 -and [decimal]$r2.body.valor -ne 4500.00) { Write-Host "        ALERTA: idempotency falhou, valor diferente!" -ForegroundColor Yellow }

Check "POST /transacoes DESPESA" @(201) {
    HttpReq POST /transacoes $auth @{
        tipo = "DESPESA"; valor = 150.50; descricao = "Mercado"
        data = (Get-Date -Format "yyyy-MM-dd"); contaId = $conta1Id; categoriaId = $catDespesa.id
    }
} | Out-Null

Check "POST /transacoes TRANSFERENCIA conta1 -> conta2 (R$ 200)" @(201) {
    HttpReq POST /transacoes $auth @{
        tipo = "TRANSFERENCIA"; valor = 200.00; descricao = "Reserva"
        data = (Get-Date -Format "yyyy-MM-dd"); contaId = $conta1Id; contaDestinoId = $conta2Id
    }
} | Out-Null

# --------- SALDOS RECALCULADOS ---------
# conta1: 1000 + 4500 - 150.50 - 200 = 5149.50
# conta2: 500 + 200 = 700
$r = Check "GET /contas (saldos recalculados)" @(200) { HttpReq GET /contas $auth }
$c1 = $r.body | Where-Object { $_.id -eq $conta1Id }
$c2 = $r.body | Where-Object { $_.id -eq $conta2Id }
if ([decimal]$c1.saldo -ne 5149.50) { Write-Host "        ALERTA: conta1 saldo $($c1.saldo), esperado 5149.50" -ForegroundColor Yellow }
if ([decimal]$c2.saldo -ne 700.00)  { Write-Host "        ALERTA: conta2 saldo $($c2.saldo), esperado 700.00"  -ForegroundColor Yellow }

# --------- FILTROS / PAGINACAO ---------
$r = Check "GET /transacoes (paginado)" @(200) { HttpReq GET /transacoes $auth }
if ($r.body.itens.Count -lt 3) { Write-Host "        ALERTA: esperado >=3 transacoes, veio $($r.body.itens.Count)" -ForegroundColor Yellow }

$r = Check "GET /transacoes?tipo=DESPESA" @(200) { HttpReq GET '/transacoes?tipo=DESPESA' $auth }
$soDespesa = ($r.body.itens | Where-Object { $_.tipo -ne 'DESPESA' }).Count
if ($soDespesa -ne 0) { Write-Host "        ALERTA: filtro tipo retornou nao-DESPESA" -ForegroundColor Yellow }

Check "GET /transacoes?contaId=$conta2Id" @(200) { HttpReq GET "/transacoes?contaId=$conta2Id" $auth } | Out-Null

# --------- UPDATE + DELETE TRANSACAO ---------
$r = Check "PUT /transacoes/{id} (muda valor da receita pra 5000)" @(200) {
    HttpReq PUT "/transacoes/$txReceitaId" $auth @{
        tipo = "RECEITA"; valor = 5000.00; descricao = "Salario corrigido"
        data = (Get-Date -Format "yyyy-MM-dd"); contaId = $conta1Id; categoriaId = $catReceita.id
    }
}

Check "DELETE /transacoes/{id} (soft delete)" @(204) { HttpReq DELETE "/transacoes/$txReceitaId" $auth } | Out-Null
Check "GET /transacoes/{id} apos delete (deve dar 404)" @(404) { HttpReq GET "/transacoes/$txReceitaId" $auth } | Out-Null

# --------- VALIDACOES NEGATIVAS ---------
Check "POST DESPESA com categoria de RECEITA (400)" @(400) {
    HttpReq POST /transacoes $auth @{
        tipo = "DESPESA"; valor = 10; descricao = "x"; data = (Get-Date -Format "yyyy-MM-dd")
        contaId = $conta1Id; categoriaId = $catReceita.id
    }
} | Out-Null

Check "POST TRANSFERENCIA origem == destino (400)" @(400) {
    HttpReq POST /transacoes $auth @{
        tipo = "TRANSFERENCIA"; valor = 10; descricao = "x"; data = (Get-Date -Format "yyyy-MM-dd")
        contaId = $conta1Id; contaDestinoId = $conta1Id
    }
} | Out-Null

Check "POST RECEITA sem categoriaId (400)" @(400) {
    HttpReq POST /transacoes $auth @{
        tipo = "RECEITA"; valor = 10; descricao = "x"; data = (Get-Date -Format "yyyy-MM-dd")
        contaId = $conta1Id
    }
} | Out-Null

Check "POST TRANSFERENCIA com categoriaId (400)" @(400) {
    HttpReq POST /transacoes $auth @{
        tipo = "TRANSFERENCIA"; valor = 10; descricao = "x"; data = (Get-Date -Format "yyyy-MM-dd")
        contaId = $conta1Id; contaDestinoId = $conta2Id; categoriaId = $catDespesa.id
    }
} | Out-Null

Check "POST /transacoes com valor zero (400)" @(400) {
    HttpReq POST /transacoes $auth @{
        tipo = "DESPESA"; valor = 0; descricao = "x"; data = (Get-Date -Format "yyyy-MM-dd")
        contaId = $conta1Id; categoriaId = $catDespesa.id
    }
} | Out-Null

Check "DELETE categoria em uso (400)" @(400) { HttpReq DELETE "/categorias/$($catDespesa.id)" $auth } | Out-Null

# --------- ISOLAMENTO ENTRE USUARIOS ---------
$email2 = "smoke2_$rand@teste.com"
$r = Check "POST /auth/cadastro (segundo usuario)" @(200) { HttpReq POST /auth/cadastro @{} @{ nome = "Smoke2"; email = $email2; senha = $senha } }
$auth2 = @{ Authorization = "Bearer $($r.body.token)" }

Check "GET conta do outro usuario (404)" @(404) { HttpReq GET "/contas/$conta1Id" $auth2 } | Out-Null
Check "GET transacao do outro usuario (404)" @(404) { HttpReq GET "/transacoes/$txReceitaId" $auth2 } | Out-Null

# --------- DELETE CONTA (soft) ---------
Check "DELETE /contas/{id} (soft)" @(204) { HttpReq DELETE "/contas/$conta2Id" $auth } | Out-Null
$r = Check "GET /contas (deletada nao aparece)" @(200) { HttpReq GET /contas $auth }
$deletadaAparece = ($r.body | Where-Object { $_.id -eq $conta2Id }).Count
if ($deletadaAparece -ne 0) { Write-Host "        ALERTA: conta soft-deletada apareceu na listagem" -ForegroundColor Yellow }

# --------- ORCAMENTOS ---------
$anoAtual = (Get-Date).Year
$mesAtual = (Get-Date).Month

$r = Check "POST /orcamentos (Alimentação, limite 1000)" @(201) {
    HttpReq POST /orcamentos $auth @{ categoriaId = $catDespesa.id; ano = $anoAtual; mes = $mesAtual; valorLimite = 1000.00 }
}
$orcId = $r.body.id

Check "POST /orcamentos duplicado (mesma cat+mes) (400)" @(400) {
    HttpReq POST /orcamentos $auth @{ categoriaId = $catDespesa.id; ano = $anoAtual; mes = $mesAtual; valorLimite = 500.00 }
} | Out-Null

Check "POST /orcamentos com categoria de RECEITA (400)" @(400) {
    HttpReq POST /orcamentos $auth @{ categoriaId = $catReceita.id; ano = $anoAtual; mes = $mesAtual; valorLimite = 500.00 }
} | Out-Null

Check "GET /orcamentos" @(200) { HttpReq GET /orcamentos $auth } | Out-Null

Check "GET /orcamentos?ano=$anoAtual&mes=$mesAtual" @(200) { HttpReq GET "/orcamentos?ano=$anoAtual&mes=$mesAtual" $auth } | Out-Null

$r = Check "GET /orcamentos/comparativo (DENTRO, gasto 150.50/1000)" @(200) {
    HttpReq GET "/orcamentos/comparativo?ano=$anoAtual&mes=$mesAtual" $auth
}
$item = $r.body.itens | Where-Object { $_.orcamentoId -eq $orcId }
if (-not $item)                          { Write-Host "        ALERTA: orcamento criado nao apareceu no comparativo" -ForegroundColor Yellow }
elseif ([decimal]$item.gasto -ne 150.50) { Write-Host "        ALERTA: gasto $($item.gasto), esperado 150.50" -ForegroundColor Yellow }
elseif ($item.status -ne 'DENTRO')       { Write-Host "        ALERTA: status $($item.status), esperado DENTRO" -ForegroundColor Yellow }

$r = Check "PUT /orcamentos/{id} (limite vira 100, vai ESTOUROU)" @(200) {
    HttpReq PUT "/orcamentos/$orcId" $auth @{ valorLimite = 100.00 }
}

$r = Check "GET /orcamentos/comparativo (agora ESTOUROU)" @(200) {
    HttpReq GET "/orcamentos/comparativo?ano=$anoAtual&mes=$mesAtual" $auth
}
$item = $r.body.itens | Where-Object { $_.orcamentoId -eq $orcId }
if ($item -and $item.status -ne 'ESTOUROU') { Write-Host "        ALERTA: status $($item.status), esperado ESTOUROU" -ForegroundColor Yellow }

Check "GET /orcamentos/{id} de outro usuario (404)" @(404) { HttpReq GET "/orcamentos/$orcId" $auth2 } | Out-Null

Check "DELETE /orcamentos/{id}" @(204) { HttpReq DELETE "/orcamentos/$orcId" $auth } | Out-Null
Check "GET /orcamentos/{id} apos delete (404)" @(404) { HttpReq GET "/orcamentos/$orcId" $auth } | Out-Null

# --------- RELATORIOS ---------
# estado esperado no mes atual (depois do que ja foi feito):
#   - RECEITA foi soft-deletada, nao deve contar
#   - DESPESA 150.50 (Alimentação) ativa
#   - TRANSFERENCIA 200 (nao entra em relatorio de receita/despesa)
$inicio = (Get-Date -Year $anoAtual -Month $mesAtual -Day 1).ToString("yyyy-MM-dd")
$fim = (Get-Date -Year $anoAtual -Month $mesAtual -Day 1).AddMonths(1).AddDays(-1).ToString("yyyy-MM-dd")

$r = Check "GET /relatorios/resumo-mensal" @(200) {
    HttpReq GET "/relatorios/resumo-mensal?ano=$anoAtual&mes=$mesAtual" $auth
}
if ([decimal]$r.body.totalReceitas -ne 0)        { Write-Host "        ALERTA: totalReceitas $($r.body.totalReceitas), esperado 0" -ForegroundColor Yellow }
if ([decimal]$r.body.totalDespesas -ne 150.50)   { Write-Host "        ALERTA: totalDespesas $($r.body.totalDespesas), esperado 150.50" -ForegroundColor Yellow }
if ([decimal]$r.body.saldoMes -ne -150.50)       { Write-Host "        ALERTA: saldoMes $($r.body.saldoMes), esperado -150.50" -ForegroundColor Yellow }
if ($r.body.quantidadeReceitas -ne 0)            { Write-Host "        ALERTA: qtdReceitas $($r.body.quantidadeReceitas), esperado 0" -ForegroundColor Yellow }
if ($r.body.quantidadeDespesas -ne 1)            { Write-Host "        ALERTA: qtdDespesas $($r.body.quantidadeDespesas), esperado 1" -ForegroundColor Yellow }

$r = Check "GET /relatorios/por-categoria?tipo=DESPESA" @(200) {
    HttpReq GET "/relatorios/por-categoria?inicio=$inicio&fim=$fim&tipo=DESPESA" $auth
}
if ([decimal]$r.body.total -ne 150.50)           { Write-Host "        ALERTA: total $($r.body.total), esperado 150.50" -ForegroundColor Yellow }
if ($r.body.itens.Count -ne 1)                   { Write-Host "        ALERTA: $($r.body.itens.Count) itens, esperado 1" -ForegroundColor Yellow }
elseif ([decimal]$r.body.itens[0].percentual -ne 100.00) { Write-Host "        ALERTA: percentual $($r.body.itens[0].percentual), esperado 100.00" -ForegroundColor Yellow }

$r = Check "GET /relatorios/por-categoria?tipo=RECEITA (vazio)" @(200) {
    HttpReq GET "/relatorios/por-categoria?inicio=$inicio&fim=$fim&tipo=RECEITA" $auth
}
if ($r.body.itens.Count -ne 0) { Write-Host "        ALERTA: $($r.body.itens.Count) itens, esperado 0" -ForegroundColor Yellow }

$r = Check "GET /relatorios/fluxo-caixa?meses=12" @(200) {
    HttpReq GET "/relatorios/fluxo-caixa?meses=12" $auth
}
if ($r.body.meses.Count -ne 12) { Write-Host "        ALERTA: $($r.body.meses.Count) meses, esperado 12" -ForegroundColor Yellow }
$mesAtualNoFluxo = $r.body.meses | Where-Object { $_.ano -eq $anoAtual -and $_.mes -eq $mesAtual }
if (-not $mesAtualNoFluxo)                                       { Write-Host "        ALERTA: mes atual nao apareceu no fluxo" -ForegroundColor Yellow }
elseif ([decimal]$mesAtualNoFluxo.despesas -ne 150.50)           { Write-Host "        ALERTA: despesas do mes $($mesAtualNoFluxo.despesas), esperado 150.50" -ForegroundColor Yellow }
elseif ([decimal]$mesAtualNoFluxo.receitas -ne 0)                { Write-Host "        ALERTA: receitas do mes $($mesAtualNoFluxo.receitas), esperado 0" -ForegroundColor Yellow }

Check "GET /relatorios/fluxo-caixa?meses=100 (400 fora de range)" @(400) { HttpReq GET "/relatorios/fluxo-caixa?meses=100" $auth } | Out-Null
Check "GET /relatorios/fluxo-caixa?meses=0 (400)" @(400) { HttpReq GET "/relatorios/fluxo-caixa?meses=0" $auth } | Out-Null

$r = Check "GET /relatorios/resumo-mensal do outro usuario (zerado)" @(200) {
    HttpReq GET "/relatorios/resumo-mensal?ano=$anoAtual&mes=$mesAtual" $auth2
}
if ([decimal]$r.body.totalReceitas -ne 0 -or [decimal]$r.body.totalDespesas -ne 0) {
    Write-Host "        ALERTA: relatorio do outro user nao zerado!" -ForegroundColor Yellow
}

# --------- TRANSACOES RECORRENTES ---------
# saldo da conta1 antes da recorrente: 1000 + 0 (receita deletada) - 150.50 - 200 = 649.50
$r = Check "POST /transacoes-recorrentes (DESPESA, dia 15, R$ 350)" @(201) {
    HttpReq POST /transacoes-recorrentes $auth @{
        tipo = "DESPESA"; valor = 350.00; descricao = "Internet"
        contaId = $conta1Id; categoriaId = $catDespesa.id
        diaDoMes = 15; dataInicio = "$anoAtual-01-01"
    }
}
$recId = $r.body.id

$r = Check "GET /transacoes-recorrentes (lista)" @(200) { HttpReq GET /transacoes-recorrentes $auth }
if (-not ($r.body | Where-Object { $_.id -eq $recId })) {
    Write-Host "        ALERTA: recorrencia criada nao apareceu na lista" -ForegroundColor Yellow
}

Check "GET /transacoes-recorrentes/{id}" @(200) { HttpReq GET "/transacoes-recorrentes/$recId" $auth } | Out-Null

Check "GET /transacoes-recorrentes/{id} de outro user (404)" @(404) {
    HttpReq GET "/transacoes-recorrentes/$recId" $auth2
} | Out-Null

# validacoes negativas
Check "POST recorrente diaDoMes=32 (400)" @(400) {
    HttpReq POST /transacoes-recorrentes $auth @{
        tipo = "DESPESA"; valor = 10; descricao = "x"
        contaId = $conta1Id; categoriaId = $catDespesa.id
        diaDoMes = 32; dataInicio = "$anoAtual-01-01"
    }
} | Out-Null

Check "POST recorrente dataFim < dataInicio (400)" @(400) {
    HttpReq POST /transacoes-recorrentes $auth @{
        tipo = "DESPESA"; valor = 10; descricao = "x"
        contaId = $conta1Id; categoriaId = $catDespesa.id
        diaDoMes = 5; dataInicio = "$anoAtual-06-01"; dataFim = "$anoAtual-05-01"
    }
} | Out-Null

Check "POST recorrente DESPESA com categoria de RECEITA (400)" @(400) {
    HttpReq POST /transacoes-recorrentes $auth @{
        tipo = "DESPESA"; valor = 10; descricao = "x"
        contaId = $conta1Id; categoriaId = $catReceita.id
        diaDoMes = 5; dataInicio = "$anoAtual-01-01"
    }
} | Out-Null

# update — muda valor pra 280
$r = Check "PUT /transacoes-recorrentes/{id}" @(200) {
    HttpReq PUT "/transacoes-recorrentes/$recId" $auth @{
        tipo = "DESPESA"; valor = 280.00; descricao = "Internet"
        contaId = $conta1Id; categoriaId = $catDespesa.id
        diaDoMes = 15; dataInicio = "$anoAtual-01-01"
        ativa = $true
    }
}
if ([decimal]$r.body.valor -ne 280.00) {
    Write-Host "        ALERTA: valor depois do update $($r.body.valor), esperado 280.00" -ForegroundColor Yellow
}

# executar agora — deve gerar a transacao e atualizar saldo
$r = Check "POST /transacoes-recorrentes/{id}/executar-agora (1a vez)" @(200) {
    HttpReq POST "/transacoes-recorrentes/$recId/executar-agora" $auth
}
$txGeradaId = $r.body.id
if ([decimal]$r.body.valor -ne 280.00)        { Write-Host "        ALERTA: tx gerada com valor $($r.body.valor), esperado 280" -ForegroundColor Yellow }
if ($r.body.tipo -ne 'DESPESA')                { Write-Host "        ALERTA: tipo $($r.body.tipo), esperado DESPESA" -ForegroundColor Yellow }

# segundo disparo — idempotente, devolve a mesma transacao
$r2 = Check "POST executar-agora 2a vez (idempotente)" @(200) {
    HttpReq POST "/transacoes-recorrentes/$recId/executar-agora" $auth
}
if ($r2.body.id -ne $txGeradaId) {
    Write-Host "        ALERTA: segunda execucao gerou transacao diferente! id $($r2.body.id) vs $txGeradaId" -ForegroundColor Yellow
}

# saldo da conta1 depois: 649.50 - 280 = 369.50
$r = Check "GET /contas (saldo c1 depois da recorrente)" @(200) { HttpReq GET /contas $auth }
$c1 = $r.body | Where-Object { $_.id -eq $conta1Id }
if ([decimal]$c1.saldo -ne 369.50) {
    Write-Host "        ALERTA: conta1 saldo $($c1.saldo), esperado 369.50" -ForegroundColor Yellow
}

# ultimaExecucao deve estar setado
$r = Check "GET recorrencia depois de executar (ultimaExecucao setado)" @(200) {
    HttpReq GET "/transacoes-recorrentes/$recId" $auth
}
if (-not $r.body.ultimaExecucao) {
    Write-Host "        ALERTA: ultimaExecucao nao foi setada" -ForegroundColor Yellow
}

# pausa a recorrencia e tenta executar — deve dar 400
Check "PUT recorrente ativa=false (pausa)" @(200) {
    HttpReq PUT "/transacoes-recorrentes/$recId" $auth @{
        tipo = "DESPESA"; valor = 280.00; descricao = "Internet"
        contaId = $conta1Id; categoriaId = $catDespesa.id
        diaDoMes = 15; dataInicio = "$anoAtual-01-01"
        ativa = $false
    }
} | Out-Null

Check "POST executar-agora numa pausada (400)" @(400) {
    HttpReq POST "/transacoes-recorrentes/$recId/executar-agora" $auth
} | Out-Null

# delete
Check "DELETE /transacoes-recorrentes/{id}" @(204) { HttpReq DELETE "/transacoes-recorrentes/$recId" $auth } | Out-Null
Check "GET recorrente apos delete (404)" @(404) { HttpReq GET "/transacoes-recorrentes/$recId" $auth } | Out-Null

# --------- CARTOES (8a) ---------
$r = Check "POST /cartoes (Nubank, limite 5000, fech dia 5, venc dia 12)" @(201) {
    HttpReq POST /cartoes $auth @{
        nome = "Nubank Roxinho"; bandeira = "MASTER"; limite = 5000.00
        diaFechamento = 5; diaVencimento = 12
        contaPadraoPagamentoId = $conta1Id; cor = "#820AD1"
    }
}
$cartao1Id = $r.body.id
if ([decimal]$r.body.limiteUsado -ne 0)         { Write-Host "        ALERTA: limiteUsado $($r.body.limiteUsado), esperado 0" -ForegroundColor Yellow }
if ([decimal]$r.body.limiteDisponivel -ne 5000) { Write-Host "        ALERTA: limiteDisponivel $($r.body.limiteDisponivel), esperado 5000" -ForegroundColor Yellow }

$r = Check "POST /cartoes (sem conta padrao, sem bandeira)" @(201) {
    HttpReq POST /cartoes $auth @{
        nome = "Inter Gold"; limite = 3000.00
        diaFechamento = 20; diaVencimento = 28
    }
}
$cartao2Id = $r.body.id

$r = Check "GET /cartoes (esperando 2)" @(200) { HttpReq GET /cartoes $auth }
if ($r.body.Count -ne 2) { Write-Host "        ALERTA: esperado 2, veio $($r.body.Count)" -ForegroundColor Yellow }

$r = Check "GET /cartoes/{id}" @(200) { HttpReq GET "/cartoes/$cartao1Id" $auth }
if ($r.body.nome -ne "Nubank Roxinho")           { Write-Host "        ALERTA: nome veio errado: $($r.body.nome)" -ForegroundColor Yellow }
if ($r.body.contaPadraoPagamentoId -ne $conta1Id) { Write-Host "        ALERTA: contaPadraoId $($r.body.contaPadraoPagamentoId), esperado $conta1Id" -ForegroundColor Yellow }

$r = Check "PUT /cartoes/{id} (limite vira 8000)" @(200) {
    HttpReq PUT "/cartoes/$cartao1Id" $auth @{
        nome = "Nubank Roxinho"; bandeira = "MASTER"; limite = 8000.00
        diaFechamento = 5; diaVencimento = 12
        contaPadraoPagamentoId = $conta1Id; cor = "#820AD1"
    }
}
if ([decimal]$r.body.limite -ne 8000) { Write-Host "        ALERTA: limite depois do PUT $($r.body.limite), esperado 8000" -ForegroundColor Yellow }

# validacoes negativas
Check "POST /cartoes limite negativo (400)" @(400) {
    HttpReq POST /cartoes $auth @{ nome = "X"; limite = -100; diaFechamento = 5; diaVencimento = 12 }
} | Out-Null

Check "POST /cartoes diaFechamento=32 (400)" @(400) {
    HttpReq POST /cartoes $auth @{ nome = "X"; limite = 1000; diaFechamento = 32; diaVencimento = 12 }
} | Out-Null

Check "POST /cartoes diaVencimento=0 (400)" @(400) {
    HttpReq POST /cartoes $auth @{ nome = "X"; limite = 1000; diaFechamento = 5; diaVencimento = 0 }
} | Out-Null

Check "POST /cartoes nome em branco (400)" @(400) {
    HttpReq POST /cartoes $auth @{ nome = ""; limite = 1000; diaFechamento = 5; diaVencimento = 12 }
} | Out-Null

Check "POST /cartoes conta padrao inexistente (400)" @(400) {
    HttpReq POST /cartoes $auth @{
        nome = "X"; limite = 1000; diaFechamento = 5; diaVencimento = 12
        contaPadraoPagamentoId = 99999999
    }
} | Out-Null

# isolamento
Check "GET cartao do outro usuario (404)" @(404) { HttpReq GET "/cartoes/$cartao1Id" $auth2 } | Out-Null

# delete (soft)
Check "DELETE /cartoes/{id} (soft)" @(204) { HttpReq DELETE "/cartoes/$cartao2Id" $auth } | Out-Null
$r = Check "GET /cartoes (deletado nao aparece)" @(200) { HttpReq GET /cartoes $auth }
$deletadoApareceu = ($r.body | Where-Object { $_.id -eq $cartao2Id }).Count
if ($deletadoApareceu -ne 0) { Write-Host "        ALERTA: cartao soft-deletado apareceu" -ForegroundColor Yellow }

# --------- CARTOES - COMPRAS + FATURAS (8b/8c) ---------
# cartao com fechamento=31 vence=15 -> case B, qualquer compra cai na fatura do mes seguinte (deterministico)
$r = Check "POST /cartoes (Cartao Compras, fech 31, venc 15)" @(201) {
    HttpReq POST /cartoes $auth @{
        nome = "Cartao Compras"; limite = 5000.00
        diaFechamento = 31; diaVencimento = 15
        contaPadraoPagamentoId = $conta1Id
    }
}
$cartaoComprasId = $r.body.id

# saldo da conta1 antes das compras
$rConta = Check "GET /contas (saldo antes compras cartao)" @(200) { HttpReq GET /contas $auth }
$saldoAntes = ($rConta.body | Where-Object { $_.id -eq $conta1Id }).saldo

$dataCompra = (Get-Date -Format "yyyy-MM-dd")

# compra a vista R$ 200
$r = Check "POST /transacoes DESPESA no cartao (a vista R$200)" @(201) {
    HttpReq POST /transacoes $auth @{
        tipo = "DESPESA"; valor = 200.00; descricao = "Mercado cartao"
        data = $dataCompra; cartaoId = $cartaoComprasId; categoriaId = $catDespesa.id
    }
}
$compraAVistaId = $r.body.id
$faturaAtualId = $r.body.faturaId
if (-not $faturaAtualId) { Write-Host "        ALERTA: faturaId nao veio na resposta" -ForegroundColor Yellow }
if ($r.body.cartaoId -ne $cartaoComprasId) { Write-Host "        ALERTA: cartaoId nao bate" -ForegroundColor Yellow }
if ($r.body.contaId -ne $null) { Write-Host "        ALERTA: contaId deveria ser null pra compra no cartao" -ForegroundColor Yellow }

# saldo da conta NAO pode mudar
$r = Check "GET /contas (saldo NAO muda apos compra no cartao)" @(200) { HttpReq GET /contas $auth }
$c1 = $r.body | Where-Object { $_.id -eq $conta1Id }
if ([decimal]$c1.saldo -ne [decimal]$saldoAntes) {
    Write-Host "        ALERTA: saldo mudou: $saldoAntes -> $($c1.saldo)" -ForegroundColor Yellow
}

# limite usado = 200
$r = Check "GET /cartoes/{id} (limite usado 200)" @(200) { HttpReq GET "/cartoes/$cartaoComprasId" $auth }
if ([decimal]$r.body.limiteUsado -ne 200)        { Write-Host "        ALERTA: limiteUsado $($r.body.limiteUsado), esperado 200" -ForegroundColor Yellow }
if ([decimal]$r.body.limiteDisponivel -ne 4800)  { Write-Host "        ALERTA: limiteDisponivel $($r.body.limiteDisponivel), esperado 4800" -ForegroundColor Yellow }

# compra parcelada 3x R$300 -> 3 parcelas de R$100
$r = Check "POST /transacoes DESPESA parcelada 3x (R$300)" @(201) {
    HttpReq POST /transacoes $auth @{
        tipo = "DESPESA"; valor = 300.00; descricao = "TV nova"
        data = $dataCompra; cartaoId = $cartaoComprasId; categoriaId = $catDespesa.id
        parcelas = 3
    }
}
$primeiraParcelaId = $r.body.id
if ([decimal]$r.body.valor -ne 100)   { Write-Host "        ALERTA: parcela com valor $($r.body.valor), esperado 100" -ForegroundColor Yellow }
if ($r.body.totalParcelas -ne 3)      { Write-Host "        ALERTA: totalParcelas $($r.body.totalParcelas)" -ForegroundColor Yellow }
if ($r.body.numeroParcela -ne 1)      { Write-Host "        ALERTA: numeroParcela $($r.body.numeroParcela)" -ForegroundColor Yellow }

# limite usado = 200 + 300 = 500
$r = Check "GET /cartoes/{id} (limite usado 500)" @(200) { HttpReq GET "/cartoes/$cartaoComprasId" $auth }
if ([decimal]$r.body.limiteUsado -ne 500) { Write-Host "        ALERTA: limiteUsado $($r.body.limiteUsado), esperado 500" -ForegroundColor Yellow }

# fatura atual deve ter compra avista (200) + 1a parcela (100) = 300
$r = Check "GET /cartoes/{id}/faturas/atual (valor 300)" @(200) { HttpReq GET "/cartoes/$cartaoComprasId/faturas/atual" $auth }
if ([decimal]$r.body.valorTotal -ne 300) { Write-Host "        ALERTA: valorTotal $($r.body.valorTotal), esperado 300" -ForegroundColor Yellow }
if ($r.body.status -ne 'ABERTA')         { Write-Host "        ALERTA: status $($r.body.status), esperado ABERTA" -ForegroundColor Yellow }

# parcelas 2 e 3 sao em meses diferentes — listagem traz >= 3 faturas
$r = Check "GET /cartoes/{id}/faturas (>= 3 faturas)" @(200) { HttpReq GET "/cartoes/$cartaoComprasId/faturas" $auth }
if ($r.body.Count -lt 3) { Write-Host "        ALERTA: $($r.body.Count) faturas, esperado >= 3" -ForegroundColor Yellow }

# estorno R$50 (RECEITA no cartao)
Check "POST RECEITA no cartao (estorno R$50)" @(201) {
    HttpReq POST /transacoes $auth @{
        tipo = "RECEITA"; valor = 50.00; descricao = "Estorno"
        data = $dataCompra; cartaoId = $cartaoComprasId; categoriaId = $catReceita.id
    }
} | Out-Null

# fatura atual: 300 - 50 = 250
$r = Check "GET fatura atual apos estorno (250)" @(200) { HttpReq GET "/cartoes/$cartaoComprasId/faturas/atual" $auth }
if ([decimal]$r.body.valorTotal -ne 250) { Write-Host "        ALERTA: valorTotal $($r.body.valorTotal), esperado 250" -ForegroundColor Yellow }

# validacoes negativas
Check "POST DESPESA com contaId + cartaoId (400)" @(400) {
    HttpReq POST /transacoes $auth @{
        tipo = "DESPESA"; valor = 10; descricao = "x"; data = $dataCompra
        contaId = $conta1Id; cartaoId = $cartaoComprasId; categoriaId = $catDespesa.id
    }
} | Out-Null

Check "POST DESPESA sem contaId nem cartaoId (400)" @(400) {
    HttpReq POST /transacoes $auth @{
        tipo = "DESPESA"; valor = 10; descricao = "x"; data = $dataCompra
        categoriaId = $catDespesa.id
    }
} | Out-Null

Check "POST TRANSFERENCIA com cartaoId (400)" @(400) {
    HttpReq POST /transacoes $auth @{
        tipo = "TRANSFERENCIA"; valor = 10; descricao = "x"; data = $dataCompra
        cartaoId = $cartaoComprasId; contaDestinoId = $conta1Id
    }
} | Out-Null

Check "POST RECEITA cartao parcelado (400, parcelas só em DESPESA)" @(400) {
    HttpReq POST /transacoes $auth @{
        tipo = "RECEITA"; valor = 100; descricao = "x"; data = $dataCompra
        cartaoId = $cartaoComprasId; categoriaId = $catReceita.id; parcelas = 3
    }
} | Out-Null

Check "POST com 100 parcelas (400)" @(400) {
    HttpReq POST /transacoes $auth @{
        tipo = "DESPESA"; valor = 100; descricao = "x"; data = $dataCompra
        cartaoId = $cartaoComprasId; categoriaId = $catDespesa.id; parcelas = 100
    }
} | Out-Null

# update da compra a vista — pode mudar valor
$r = Check "PUT transacao no cartao (valor 200 -> 180)" @(200) {
    HttpReq PUT "/transacoes/$compraAVistaId" $auth @{
        tipo = "DESPESA"; valor = 180.00; descricao = "Mercado corrigido"
        data = $dataCompra; categoriaId = $catDespesa.id
    }
}
if ([decimal]$r.body.valor -ne 180) { Write-Host "        ALERTA: valor depois do PUT $($r.body.valor)" -ForegroundColor Yellow }

# nao pode mudar a data
Check "PUT transacao cartao mudando data (400)" @(400) {
    HttpReq PUT "/transacoes/$compraAVistaId" $auth @{
        tipo = "DESPESA"; valor = 180; descricao = "x"
        data = "$anoAtual-01-01"; categoriaId = $catDespesa.id
    }
} | Out-Null

# DELETE primeira parcela -> cascade nas outras 2
Check "DELETE parcela 1/3 (cascade nas outras)" @(204) { HttpReq DELETE "/transacoes/$primeiraParcelaId" $auth } | Out-Null

# limite usado: 180 (a vista corrigida) - 50 (estorno) + 0 parcelas = 130
$r = Check "GET /cartoes/{id} (limite usado 130 apos cascade)" @(200) { HttpReq GET "/cartoes/$cartaoComprasId" $auth }
if ([decimal]$r.body.limiteUsado -ne 130) { Write-Host "        ALERTA: limiteUsado $($r.body.limiteUsado), esperado 130" -ForegroundColor Yellow }

# delete cartao com fatura aberta -> 400
Check "DELETE cartao com fatura aberta (400)" @(400) { HttpReq DELETE "/cartoes/$cartaoComprasId" $auth } | Out-Null

# isolamento
Check "GET fatura de outro usuario (404)" @(404) { HttpReq GET "/faturas/$faturaAtualId" $auth2 } | Out-Null

# --------- CARTOES - FECHAMENTO + PAGAMENTO (8d/8e) ---------
# forca fechamento da fatura atual (simula scheduler)
$r = Check "POST /faturas/{id}/forcar-fechamento" @(200) { HttpReq POST "/faturas/$faturaAtualId/forcar-fechamento" $auth }
if ($r.body.status -ne 'FECHADA') { Write-Host "        ALERTA: status $($r.body.status), esperado FECHADA" -ForegroundColor Yellow }

$rConta = Check "GET /contas (saldo antes pagamento)" @(200) { HttpReq GET /contas $auth }
$saldoAntesPag = ($rConta.body | Where-Object { $_.id -eq $conta1Id }).saldo

# pagamento parcial R$50
Check "POST /faturas/{id}/pagar (parcial R$50)" @(200) {
    HttpReq POST "/faturas/$faturaAtualId/pagar" $auth @{ contaId = $conta1Id; valor = 50.00 }
} | Out-Null

# status ainda FECHADA (parcial)
$r = Check "GET fatura apos pagamento parcial (FECHADA, pago 50)" @(200) { HttpReq GET "/faturas/$faturaAtualId" $auth }
if ($r.body.status -ne 'FECHADA')           { Write-Host "        ALERTA: status $($r.body.status), esperado FECHADA" -ForegroundColor Yellow }
if ([decimal]$r.body.valorPago -ne 50)      { Write-Host "        ALERTA: valorPago $($r.body.valorPago), esperado 50" -ForegroundColor Yellow }

# saldo caiu 50
$r = Check "GET /contas (saldo -50 apos pagamento)" @(200) { HttpReq GET /contas $auth }
$c1 = $r.body | Where-Object { $_.id -eq $conta1Id }
$esperado = [decimal]$saldoAntesPag - 50
if ([decimal]$c1.saldo -ne $esperado) {
    Write-Host "        ALERTA: saldo $($c1.saldo), esperado $esperado" -ForegroundColor Yellow
}

# paga o restante (130 - 50 = 80)
Check "POST /faturas/{id}/pagar (R$80 restante)" @(200) {
    HttpReq POST "/faturas/$faturaAtualId/pagar" $auth @{ contaId = $conta1Id; valor = 80.00 }
} | Out-Null

# agora PAGA
$r = Check "GET fatura apos pagamento total (PAGA)" @(200) { HttpReq GET "/faturas/$faturaAtualId" $auth }
if ($r.body.status -ne 'PAGA') { Write-Host "        ALERTA: status $($r.body.status), esperado PAGA" -ForegroundColor Yellow }

# pagar fatura ja paga -> 400
Check "POST pagar fatura ja paga (400)" @(400) {
    HttpReq POST "/faturas/$faturaAtualId/pagar" $auth @{ contaId = $conta1Id; valor = 10 }
} | Out-Null

# --------- RESUMO ---------
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESULTADO: $passou / $total testes passaram" -ForegroundColor $(if ($passou -eq $total) { 'Green' } else { 'Yellow' })
if ($falhas.Count -gt 0) {
    Write-Host "`nFalhas:" -ForegroundColor Red
    $falhas | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
}
Write-Host "========================================`n" -ForegroundColor Cyan
