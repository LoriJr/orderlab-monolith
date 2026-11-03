# OrderLab

API experimental de pedidos e notificações desenvolvida como apoio a um Trabalho de Conclusão de Curso em Engenharia de Software.

O projeto investigará em quais condições a extração do processamento de notificações oferece benefícios adicionais ao processamento assíncrono dentro de um monólito, considerando desempenho, consumo de recursos e esforço operacional.

## Estágio atual

O projeto está na fase de definição e implementação inicial do monólito base. A documentação da pesquisa está em avaliação pelo orientador; o escopo e o protocolo experimental poderão receber ajustes.

Este README descreve o planejamento atual. Endpoints, componentes e procedimentos apresentados abaixo são previstos e não representam funcionalidades já verificadas no repositório.

## Variantes previstas

| Variante | Organização | Processamento da notificação |
| --- | --- | --- |
| A — ponto de partida | Monólito modular | Síncrono, antes da resposta à requisição |
| B | Monólito modular | Assíncrono, no mesmo processo da API |
| C | API e serviço de notificações em processos separados | Assíncrono, no serviço extraído |

A comparação entre A e B avaliará os efeitos da assincronia. A comparação principal entre B e C avaliará os efeitos adicionais da extração, mantendo recursos totais e limite de processamento simultâneo equivalentes.

A pesquisa não pressupõe que a extração será vantajosa. Resultados favoráveis, desfavoráveis e inconclusivos serão considerados.

## Tecnologias previstas

- Java 21 e Spring Boot;
- Spring Web, Bean Validation e Spring Data JPA;
- PostgreSQL e migrações versionadas de banco;
- Maven;
- Docker e Docker Compose;
- JUnit, Mockito e Testcontainers;
- k6 para geração de carga na etapa experimental.

As versões e dependências efetivamente utilizadas serão registradas nos arquivos de configuração do projeto.

## Escopo inicial

- Criar pedidos com itens e calcular o valor total;
- consultar pedidos por identificador;
- listar pedidos com paginação;
- persistir e consultar tarefas de notificação;
- executar uma tentativa síncrona de notificação na variante A;
- integrar com um simulador HTTP de envio.

Autenticação, pagamentos, estoque, catálogo de produtos, cadastro de clientes e interface gráfica ficam fora do escopo inicial. Os testes utilizarão dados sintéticos e não haverá envio de e-mails reais.

## Modelo de dados previsto

| Entidade | Responsabilidade | Campos iniciais |
| --- | --- | --- |
| `Pedido` | Registrar o pedido e seu destinatário | `id`, `nomeDestinatario`, `emailDestino`, `valorTotal`, `criadoEm` |
| `ItemPedido` | Registrar os itens de um pedido | `id`, `pedidoId`, `descricao`, `quantidade`, `valorUnitario` |
| `TarefaNotificacao` | Registrar o processamento da notificação | `id`, `pedidoId`, `destinatario`, `conteudo`, `status`, `tentativas`, `criadaEm`, `iniciadaEm`, `concluidaEm`, `ultimoErro` |

Valores monetários serão representados por `BigDecimal`, e os instantes dos eventos por `Instant`.

A tarefa armazenará uma cópia do destinatário e do conteúdo necessário ao envio. Isso permitirá processá-la sem consultar os dados de negócio do pedido.

### Estados da tarefa

| Estado | Significado |
| --- | --- |
| `PENDING` | Tarefa registrada, aguardando processamento |
| `PROCESSING` | Tentativa em execução |
| `COMPLETED` | Simulador confirmou a conclusão |
| `FAILED` | Tentativa falhou |

Esses estados pertencem à tarefa de notificação. Uma falha no envio não desfaz um pedido já confirmado.

## Fluxo da variante A

1. Receber e validar a solicitação de criação do pedido.
2. Calcular o valor total no servidor.
3. Gravar pedido, itens e tarefa `PENDING` na mesma transação.
4. Confirmar a transação.
5. Executar a tentativa de notificação no mesmo processo da API, registrando seu resultado.
6. Responder à requisição após a tentativa, informando o pedido criado e o estado da notificação.

A transação de criação do pedido não permanecerá aberta durante a chamada HTTP ao simulador. O comportamento previsto é retornar `201 Created` quando o pedido tiver sido confirmado, inclusive se a tentativa de notificação falhar.

A versão inicial terá uma tentativa síncrona por criação de pedido. O processamento automático de pendências e a recuperação após interrupções serão desenvolvidos na variante B. A versão A ainda poderá deixar tarefas pendentes ou em processamento se houver interrupção do processo; esse comportamento deverá ser documentado.

## Endpoints planejados

| Método | Endpoint | Finalidade |
| --- | --- | --- |
| `POST` | `/pedidos` | Criar pedido e executar a tentativa síncrona |
| `GET` | `/pedidos/{id}` | Consultar um pedido |
| `GET` | `/pedidos?page=0&size=20` | Listar pedidos com paginação |
| `GET` | `/pedidos/{id}/notificacao` | Consultar o estado da notificação |

Os contratos de entrada, saída e erro serão documentados após a implementação.

## Organização prevista

Pacote base: `br.com.viratech`.

A aplicação será organizada nos módulos `pedido` e `notificacao`, com controllers, DTOs, serviços, modelos e repositórios. O cliente HTTP do simulador ficará no módulo de notificações. Configurações e tratamento de erros comuns ficarão em `compartilhado`.

A interação entre módulos ocorrerá por serviços ou interfaces públicas. O módulo de pedidos não deverá alterar diretamente o repositório de notificações.

## Simulador de notificações

Um serviço HTTP de apoio representará o provedor externo de envio. Ele permitirá configurar atraso e falhas, registrar recebimentos e conclusões e reconhecer solicitações repetidas pelo identificador da tarefa.

O mesmo simulador será utilizado nas três variantes. Ele será infraestrutura de apoio ao experimento; o processamento de notificações da variante A continuará dentro do monólito.

A preparação computacional configurável da notificação será executada pela aplicação responsável pelo processamento, antes da chamada ao simulador.

## Persistência e controle de duplicações

O identificador da tarefa será enviado ao simulador como chave de idempotência. O experimento distinguirá chamadas repetidas de efeitos duplicados e não presumirá garantia de envio único em provedores reais.

O PostgreSQL será compartilhado entre a API e o serviço extraído na variante C, representando uma etapa intermediária de migração. O serviço extraído terá acesso apenas aos dados de notificação.

Essa escolha limita o esforço de implementação e concentra a comparação na separação dos processos. A dependência do banco permanecerá comum: a extração não será interpretada como isolamento contra falhas do PostgreSQL.

## Avaliação experimental planejada

- Tempo de resposta das operações, com foco no percentil 95;
- vazão e taxa de erros;
- consumo de CPU e memória;
- tempo entre a criação do pedido e a conclusão da notificação;
- tamanho da fila, perdas e duplicações;
- comportamento diante de falhas de processo;
- esforço documentado de preparação, implantação e alterações padronizadas.

Taxas de carga, duração, aquecimento e limites de avaliação serão definidos no piloto, antes da coleta definitiva. Os resultados serão restritos às condições observadas na aplicação experimental.

## Plano do primeiro mês

- [ ] Configurar projeto, dependências e ambiente de desenvolvimento.
- [ ] Criar migrações e entidades.
- [ ] Implementar criação, consulta e paginação de pedidos.
- [ ] Implementar validações e tratamento de erros.
- [ ] Persistir a tarefa na mesma transação do pedido.
- [ ] Criar o simulador de notificações.
- [ ] Implementar a tentativa síncrona após a confirmação da transação.
- [ ] Registrar identificadores, tentativas, estados e tempos.
- [ ] Verificar o fluxo completo com testes de integração.
- [ ] Preparar o script inicial de carga e executar o piloto.
- [ ] Documentar a reconstrução do ambiente.
- [ ] Preservar uma versão de referência da variante A no Git.

## Critérios de conclusão do monólito base

- Pedido, itens e tarefa são gravados ou revertidos juntos.
- A resposta de criação aguarda a tentativa de notificação.
- Falha no simulador preserva o pedido e registra o resultado da tentativa.
- O simulador identifica chamadas repetidas com a mesma chave.
- Cálculo dos valores, consultas e paginação são verificados.
- O ambiente pode ser reconstruído com configurações e versões documentadas.

## Execução

As instruções de execução serão adicionadas após a configuração e verificação do projeto. Elas deverão incluir pré-requisitos, variáveis de ambiente, inicialização do banco e do simulador, migrações, execução da aplicação e dos testes.

## Contexto acadêmico

Projeto de apoio ao TCC do MBA em Engenharia de Software da USP/Esalq, de Lourival Oliveira e Silva Júnior, sob orientação de Renato Godoi da Cruz.

A aplicação, os scripts e os dados experimentais servirão como instrumentos para avaliar a extração gradual de serviços.
