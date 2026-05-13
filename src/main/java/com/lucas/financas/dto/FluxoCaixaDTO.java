package com.lucas.financas.dto;

import java.util.List;

public record FluxoCaixaDTO(
        int mesesIncluidos,
        List<MesFluxoDTO> meses
) {
}
