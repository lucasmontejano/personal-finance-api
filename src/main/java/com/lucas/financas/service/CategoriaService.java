package com.lucas.financas.service;

import com.lucas.financas.dto.AtualizarCategoriaDTO;
import com.lucas.financas.dto.CategoriaRespostaDTO;
import com.lucas.financas.dto.CriarCategoriaDTO;
import com.lucas.financas.exception.RecursoNaoEncontradoException;
import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.TipoCategoria;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.repository.CategoriaRepository;
import com.lucas.financas.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;
    private final TransacaoRepository transacaoRepository;

    public CategoriaService(CategoriaRepository repository, TransacaoRepository transacaoRepository) {
        this.repository = repository;
        this.transacaoRepository = transacaoRepository;
    }

    @Transactional
    public CategoriaRespostaDTO criar(Usuario usuario, CriarCategoriaDTO dto) {
        if (repository.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(usuario.getId(), dto.nome(), dto.tipo())) {
            throw new IllegalArgumentException("ja existe uma categoria com esse nome e tipo");
        }
        Categoria c = new Categoria(usuario, dto.nome(), dto.tipo(), dto.icone(), dto.cor());
        return CategoriaRespostaDTO.de(repository.save(c));
    }

    @Transactional(readOnly = true)
    public List<CategoriaRespostaDTO> listar(Usuario usuario, TipoCategoria tipo) {
        List<Categoria> categorias = (tipo == null)
                ? repository.findByUsuarioIdOrderByTipoAscNomeAsc(usuario.getId())
                : repository.findByUsuarioIdAndTipoOrderByNome(usuario.getId(), tipo);
        return categorias.stream().map(CategoriaRespostaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaRespostaDTO buscar(Usuario usuario, Long id) {
        return CategoriaRespostaDTO.de(achar(usuario, id));
    }

    @Transactional
    public CategoriaRespostaDTO atualizar(Usuario usuario, Long id, AtualizarCategoriaDTO dto) {
        Categoria c = achar(usuario, id);
        c.setNome(dto.nome());
        c.setIcone(dto.icone());
        c.setCor(dto.cor());
        return CategoriaRespostaDTO.de(c);
    }

    @Transactional
    public void deletar(Usuario usuario, Long id) {
        Categoria c = achar(usuario, id);
        // se ja tem transação usando essa categoria, nao deixa apagar
        if (transacaoRepository.existsByCategoriaIdAndDeletadoFalse(c.getId())) {
            throw new IllegalArgumentException("categoria está em uso por transações, nao pode ser apagada");
        }
        repository.delete(c);
    }

    @Transactional
    public void seedPadrao(Usuario usuario) {
        List<Categoria> padrao = List.of(
                new Categoria(usuario, "Salário", TipoCategoria.RECEITA, "salario", "#4CAF50"),
                new Categoria(usuario, "Freelance", TipoCategoria.RECEITA, "freelance", "#2196F3"),
                new Categoria(usuario, "Investimentos", TipoCategoria.RECEITA, "investimento", "#9C27B0"),
                new Categoria(usuario, "Outros", TipoCategoria.RECEITA, null, "#9E9E9E"),

                new Categoria(usuario, "Alimentação", TipoCategoria.DESPESA, "comida", "#FF5722"),
                new Categoria(usuario, "Transporte", TipoCategoria.DESPESA, "carro", "#607D8B"),
                new Categoria(usuario, "Moradia", TipoCategoria.DESPESA, "casa", "#795548"),
                new Categoria(usuario, "Saúde", TipoCategoria.DESPESA, "saude", "#F44336"),
                new Categoria(usuario, "Lazer", TipoCategoria.DESPESA, "lazer", "#E91E63"),
                new Categoria(usuario, "Educação", TipoCategoria.DESPESA, "educacao", "#3F51B5"),
                new Categoria(usuario, "Compras", TipoCategoria.DESPESA, "compras", "#FF9800"),
                new Categoria(usuario, "Contas", TipoCategoria.DESPESA, "contas", "#009688"),
                new Categoria(usuario, "Outros", TipoCategoria.DESPESA, null, "#9E9E9E")
        );
        repository.saveAll(padrao);
    }

    private Categoria achar(Usuario usuario, Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("categoria nao encontrada"));
    }
}
