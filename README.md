# OrderLab

API experimental de pedidos e notificações para o TCC «Evolução de monólito para microsserviços: critérios quantitativos e qualitativos para extração de serviço».

## Estágio atual

A Variante A implementa a criação de pedidos e uma tentativa síncrona de notificação. O ambiente integrado inclui PostgreSQL, simulador HTTP, Prometheus e Grafana. As execuções atuais são pilotos de preparação; não constituem a coleta oficial do estudo.

| Variante | Organização | Situação |
| --- | --- | --- |
| A | Monólito com tentativa síncrona | Implementada no fluxo de criação |
| B | Processamento assíncrono persistido no mesmo processo | Planejada |
| C | Processamento assíncrono em serviço separado | Planejada |

A comparação A × B avaliará a assincronia; B × C avaliará a extração com recursos e concorrência controlados. O estudo não pressupõe vantagem de microsserviços. O banco compartilhado planejado para C continuará sendo uma dependência comum.

## Tecnologias e organização

Java 21, Spring Boot 4.1.1, Web MVC, Validation, Data JPA, PostgreSQL 18, Lombok, Actuator, Micrometer Prometheus, Maven, Docker Compose e Buildx/Bake.

O pacote base é `com.viratech`, organizado em `clients`, `controllers`, `dtos`, `enums`, `mappers`, `models`, `repositories` e `services`. A organização atual é em camadas; não há pacotes de módulos de negócio separados.

| Entidade | Responsabilidade |
| --- | --- |
| `Order` | Cliente, valor total, data e itens do pedido |
| `OrderItem` | Descrição, quantidade e preço unitário |
| `NotificationTask` | Destinatário, conteúdo, estado, tentativas, datas e último erro |

Os estados `PENDING`, `PROCESSING`, `COMPLETED` e `FAILED` pertencem à tarefa de notificação, não ao pedido.

## Fluxo implementado

1. Validar a entrada e calcular o total no servidor.
2. Gravar pedido, itens e tarefa `PENDING` na mesma transação.
3. Confirmar a transação antes da comunicação externa.
4. Registrar `PROCESSING`, incrementar tentativas e chamar o simulador.
5. Registrar `COMPLETED` ou `FAILED` conforme o resultado observado pelo cliente.
6. Retornar `201 Created` com os dados do pedido quando a criação e o tratamento da tentativa terminarem normalmente, inclusive nas falhas de comunicação tratadas.

A resposta não inclui o estado da tarefa. Não há retry automático nesta variante. Interrupções podem deixar tarefas intermediárias sem recuperação automática. O cliente usa timeout de conexão de 2000 ms e de leitura de 5000 ms; timeout não cancela necessariamente o processamento remoto.

## Execução integrada com Docker Compose

### 1. Pré-requisitos e pastas

Execute os comandos em Bash no Linux/WSL, com Docker Engine acessível, Compose com suporte a `build.entitlements` (2.27.1 ou posterior) e Buildx/Bake. A primeira construção precisa de internet para baixar imagens e dependências. JDK e Maven locais não são necessários para a execução inteiramente em Docker.

```bash
docker version
docker compose version
docker buildx version
```

O Compose fica na raiz do OrderLab. Seu `build.context: ../notification-simulator` pressupõe projetos em pastas irmãs. Não é uma exigência do Docker: ajuste o contexto se usar outra organização.

| Arquivo relativo à pasta do OrderLab | Função |
| --- | --- |
| `docker-compose.yaml` | Orquestra os cinco serviços |
| `Dockerfile` | Compila e executa o OrderLab |
| `../notification-simulator/Dockerfile` | Compila e executa o simulador |
| `monitoring/prometheus.yml` | Coleta das métricas |
| `monitoring/grafana/datasources.yml` | Provisiona a fonte Prometheus |

Em cada aplicação, `Dockerfile`, `pom.xml` e `src` devem estar na raiz do contexto correspondente. Os dois YAMLs de monitoramento devem ser arquivos, não diretórios.

### 2. Variáveis locais

Crie ou complete `.env` na pasta do OrderLab com valores literais, por exemplo:

```dotenv
POSTGRES_DB=orderlab
POSTGRES_USER=orderlab
POSTGRES_PASSWORD=defina_uma_senha_local
GRAFANA_ADMIN_PASSWORD=defina_outra_senha_local
SIMULATOR_DELAY_MS=1000
SIMULATOR_FAIL_ENABLED=false
```

Em banco existente, preserve as credenciais originais: mudar o `.env` não altera usuários de um volume já inicializado. Não versione senhas reais. O `.env-example` enviado ainda contém placeholders e não inclui Grafana e cenários; use o exemplo completo acima ao preencher seu `.env`.

### 3. Banco de dados: condição para a primeira execução

O projeto enviado utiliza `spring.jpa.hibernate.ddl-auto: validate`. Isso valida as tabelas, mas não as cria. O `pom.xml` atual não inclui Flyway, portanto scripts de migração não serão executados automaticamente por ele. O Compose também não configura scripts de inicialização do esquema.

O ambiente funcional utiliza um banco com esquema existente. Para um volume vazio, prepare o esquema compatível com as entidades antes de iniciar a API; a automatização dessa preparação ainda precisa ser concluída para uma reconstrução integral do zero. Não substitua `validate` por `create` sobre um banco com dados.

### 4. Construir as imagens

Encerre as aplicações no IntelliJ para liberar as portas. Na pasta do OrderLab:

```bash
docker compose config --quiet
docker buildx bake -f docker-compose.yaml --allow=network.host --load orderlab notification-simulator
docker image inspect orderlab-app:latest notification-simulator-app:latest --format '{{.RepoTags}}'
```

Os Dockerfiles compilam o JAR com Maven 3.9/JDK 21 e executam com JRE 21. A construção usa `-DskipTests`: não executa os testes, mas compila o código de teste.

O uso de `network: host`, `entitlements` e `--allow=network.host` registra a solução usada neste ambiente WSL para a falha de DNS durante o build. Não é requisito universal das aplicações. A rede dos serviços em execução continua sendo a rede padrão do Compose.

### 5. Iniciar e conferir

```bash
docker compose up -d --no-build
docker compose ps -a
docker compose logs -f orderlab notification-simulator
```

`Ctrl+C` encerra apenas a visualização dos logs. O OrderLab aguarda o banco saudável, mas apenas o início do contêiner do simulador; aguarde a inicialização das duas aplicações antes das requisições.

| Serviço | Acesso pelo computador |
| --- | --- |
| OrderLab | http://localhost:9090 |
| Simulador | http://localhost:9091 |
| Prometheus | http://localhost:9092 |
| Grafana | http://localhost:3000 |
| PostgreSQL | localhost:5432 |

Na rede Docker, a API usa `orderlab-mono-db:5432` e `http://notification-simulator:9091`. O Grafana usa `http://prometheus:9090`. As variáveis do Compose sobrescrevem os endereços locais do `application.yaml`.

## API e verificação funcional

O endpoint de negócio implementado no controller atual é `POST /api/v1/orders`. Consulta, paginação e consulta de tarefas ainda não possuem endpoints implementados.

```bash
curl -i -X POST http://localhost:9090/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerName":"piloto-manual-001","customerEmail":"cliente@example.com","items":[{"description":"Produto de teste","quantity":2,"unitPrice":100.00}]}'
```

O esperado no cenário de sucesso é HTTP 201, pedido com identificador, itens e total 200.00. O header `Idempotency-Key` é enviado pelo OrderLab ao simulador; não é necessário adicioná-lo ao POST de pedidos.

Confira a tarefa diretamente no banco:

```bash
docker compose exec orderlab-mono-db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

```sql
SELECT t.id, t.order_id, t.status, t.attempts, t.last_error
FROM notification_tasks t
JOIN orders o ON o.id = t.order_id
WHERE o.customer_name = 'piloto-manual-001'
ORDER BY t.id DESC;
```

Em condições normais, a tarefa termina `COMPLETED` com uma tentativa. HTTP 201 sozinho não comprova sucesso da notificação. Use `\q` para sair do psql.

## Prometheus e Grafana

Confira `/actuator/health` e `/actuator/prometheus` nas portas 9090 e 9091. Abra http://localhost:9092/targets: os dois alvos devem estar `UP`; a coleta é feita a cada 5 s.

No Grafana, entre com usuário `admin` e a senha `GRAFANA_ADMIN_PASSWORD` da primeira inicialização do volume. Alterar a variável depois não redefine automaticamente a senha já armazenada.

A fonte Prometheus é provisionada automaticamente, mas não há dashboards provisionados. Em Explore, selecione Prometheus e consulte:

```promql
up
```

Memória heap em MiB, por aplicação:

```promql
sum by (application) (jvm_memory_used_bytes{area="heap"}) / 1024 / 1024
```

CPU reportada pelo processo Java:

```promql
process_cpu_usage{application="orderlab"} * 100
```

A heap não representa a memória total do processo ou contêiner. Essa configuração não coleta métricas detalhadas do PostgreSQL ou de todos os contêineres. P95/P99 das requisições continuam sendo coletados pelo k6. Os pilotos anteriores à mudança de computador/ambiente devem ser identificados separadamente.

## Rotina de operação

Depois de alterar código, dependências ou `application.yaml`, reconstrua as imagens pelo comando Bake da etapa 4 e reaplique `docker compose up -d --no-build`.

Depois de alterar atraso/falha no `.env`:

```bash
docker compose up -d --no-build notification-simulator
```

Isso recria o simulador quando o ambiente muda e elimina sua memória de idempotência. `docker compose restart` sozinho não reaplica novas variáveis do `.env`.

Depois de editar o arquivo montado do Prometheus:

```bash
docker compose restart prometheus
```

Depois de editar o provisionamento do Grafana:

```bash
docker compose restart grafana
```

Para encerrar preservando os volumes:

```bash
docker compose down
```

Não use `down -v` para uma parada normal: essa opção remove os volumes. Preserve o nome do projeto Compose para reutilizar os volumes existentes.

## Execução pelo IntelliJ ou Maven

Como alternativa, mantenha apenas o PostgreSQL em Docker e execute as duas aplicações localmente, sem disputar portas com suas versões em contêineres. O OrderLab precisa de JDK 21, banco preparado e variáveis `POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD` na configuração de execução. O `.env` do Compose não é carregado automaticamente pela aplicação Java local.

```bash
docker compose stop orderlab notification-simulator prometheus grafana
docker compose up -d orderlab-mono-db
chmod +x mvnw
./mvnw spring-boot:run
```

Inicie também o simulador conforme seu README. No PowerShell, use `.\mvnw.cmd spring-boot:run`. O YAML local usa banco em `localhost:5432` e simulador em `localhost:9091`. A coleta Prometheus incluída aponta para nomes Docker e não atende automaticamente aplicações locais.

## Diagnóstico

| Sintoma | Verificação |
| --- | --- |
| `Dockerfile: no such file` | Conferir `build.context` e nome do Dockerfile |
| `Temporary failure in name resolution` no Maven | Verificar DNS do WSL, contêiner e construtor; a falha precede a compilação |
| `additional privileges requested` | Usar Bake com `--allow=network.host` conforme a etapa 4 |
| `No such image` | Confirmar sucesso da construção e do `--load` |
| `not a directory` em montagem | Conferir extensão `.yml`, caminho e tipo de arquivo com `ls -ld` |
| Serviço `Exited` | Consultar `docker compose logs --tail=100 NOME_DO_SERVICO` |
| Alvo Prometheus `DOWN` | Verificar logs, endpoint de métricas e endereço interno configurado |
| API falha em banco novo | Preparar esquema; `ddl-auto: validate` não cria tabelas |

## Limites e próximos passos

- Implementar consultas/paginação e tratamento de erros personalizado.
- Automatizar o preparo do esquema em banco vazio.
- Ampliar verificações automatizadas do fluxo e das transações; o build Docker não as executa.
- Preparar protocolo de aquecimento, repetições, reconciliação e registro de ambiente para a coleta oficial.
- Fixar versões/digests das imagens antes das comparações; o Compose atual usa `latest` para Prometheus/Grafana e outras tags móveis.
- Desenvolver B e C sem atribuir às falhas do banco compartilhado um isolamento que a extração não fornece.

Autenticação, pagamentos, estoque, catálogo e envio real de e-mail ficam fora do escopo atual. A preparação computacional adicional da notificação permanece planejada; não aparece implementada nos serviços enviados.

## Contexto acadêmico

Projeto de apoio ao TCC do MBA em Engenharia de Software da USP/Esalq, de Lourival Oliveira e Silva Júnior, sob orientação de Renato Godoi da Cruz. O escopo e o protocolo poderão receber ajustes após revisão acadêmica.
