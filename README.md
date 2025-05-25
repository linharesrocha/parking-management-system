# Estapar - Sistema de Gerenciamento de Estacionamento (Desafio Técnico)

Backend para o desafio técnico da Estapar, implementando um sistema de gerenciamento de estacionamento com Java 21 e Spring Boot `3.5.0`.

**Link do Repositório:** [https://github.com/linharesrocha/parking-management-system](https://github.com/linharesrocha/parking-management-system)

---

## Índice

1.  [Visão Geral e Funcionalidades](#1-visão-geral-e-funcionalidades)
2.  [Stack Tecnológico e Arquitetura](#2-stack-tecnológico-e-arquitetura)
3.  [Regras de Negócio Implementadas](#3-regras-de-negócio-implementadas)
4.  [APIs e Documentação (Swagger)](#4-apis-e-documentação-swagger)
5.  [Como Rodar o Projeto](#5-como-rodar-o-projeto)
6.  [Testes](#6-testes)
7.  [Melhorias Propostas](#7-melhorias-propostas)

---

## 1. Visão Geral e Funcionalidades

Este sistema gerencia as operações de um estacionamento, controlando entrada/saída de veículos, ocupação de vagas, faturamento e aplicando regras de negócio dinâmicas.

**Principais Funcionalidades:**
* Importação da configuração da garagem via API de um simulador externo (`GET /garage`).
* Processamento em tempo real de eventos de webhook (`ENTRY`, `PARKED`, `EXIT`) do simulador.
* Implementação de **Preço Dinâmico** baseado na lotação do setor.
* Implementação de **Controle de Lotação Máxima** por setor.
* Cálculo de tarifa na saída do veículo.
* APIs REST para consulta de status de placa, status de vaga e faturamento diário por setor.
* Documentação interativa da API com OpenAPI (Swagger).
* Tratamento global e padronizado de exceções.
* Ambiente completo orquestrado com Docker Compose (Aplicação, PostgreSQL, Simulador).
* Testes unitários para a lógica de negócio.

## 2. Stack Tecnológico e Arquitetura

* **Linguagem/Framework:** Java 21, Spring Boot `3.5.0`
* **Banco de Dados:** PostgreSQL (Dockerizado)
* **Build:** Maven
* **Containerização:** Docker, Docker Compose
* **API Docs:** Springdoc OpenAPI
* **Testes:** JUnit 5, Mockito
* **Arquitetura:** Inspirada na Clean Architecture, com separação em camadas (`domain`, `application`, `infrastructure`) para promover baixo acoplamento, testabilidade e manutenibilidade.
    * **Domain:** Entidades de negócio (`Sector`, `Spot`, `Vehicle`, `ParkingRecord`) e exceções customizadas.
    * **Application:** DTOs para comunicação e Services (`ParkingEventService`, `GarageSetupService`) para orquestrar os casos de uso.
    * **Infrastructure:** REST Controllers (`ParkingQueryController`, `WebhookController`), Repositórios Spring Data JPA, Cliente HTTP para o simulador, e o Tratador Global de Exceções.

## 3. Regras de Negócio Implementadas

* **Preço Dinâmico:** Calculado no evento `PARKED` com base na taxa de ocupação do setor:
    * ```< 25% lotação: -10% no preço base.```
    * ```25% a < 50% lotação: Preço base.```
    * ```50% a < 75% lotação: +10% no preço base.```
    * ```>= 75% lotação: +25% no preço base.```
* **Controle de Lotação do Setor:** No evento `PARKED`, o sistema verifica se o setor está lotado (`vagas_ocupadas >= capacidade_maxima`). Se sim, uma exceção é lançada, impedindo o estacionamento.
* **Cálculo de Tarifa:** No evento `EXIT`, a tarifa é `duração_em_horas * pricePerHour` (o `pricePerHour` é o preço dinâmico fixado na entrada).

## 4. APIs e Documentação (Swagger)

A documentação interativa e completa de todos os endpoints da API, schemas de DTOs e exemplos está disponível via **Swagger UI**.

* **Acesso:** [`http://localhost:3003/swagger-ui.html`](http://localhost:3003/swagger-ui.html) (após iniciar o ambiente com `docker-compose up`).
* **Endpoints Principais:**
    * `POST /webhook`: Recebe eventos do simulador (`ENTRY`, `PARKED`, `EXIT`).
    * `POST /api/v1/plate-status`: Consulta status de um veículo pela placa.
    * `POST /api/v1/spot-status`: Consulta status de uma vaga por coordenadas.
    * `GET /api/v1/revenue`: Consulta faturamento por setor e data.
* **Respostas de Erro:** Padronizadas usando `ApiErrorResponseDTO` e tratadas globalmente.

## 5. Como Rodar o Projeto

### Pré-requisitos

* Docker Desktop (Windows/Mac com "Enable host networking" ativado) ou Docker Engine + Docker Compose (Linux).

### Passos
1.  Clone o repositório:
    ```bash
    git clone https://github.com/linharesrocha/parking-management-system.git
    cd parking-management-system
    ```
2.  Suba o ambiente completo (PostgreSQL, Aplicação, Simulador):
    ```bash
    docker-compose up --build
    ```
3.  Acesse a documentação da API/Swagger UI: [`http://localhost:3003/swagger-ui.html`](http://localhost:3003/swagger-ui.html)
4.  A API do simulador (para ver config da garagem): [`http://localhost:3000/garage`](http://localhost:3000/garage)

## 6. Testes
Testes unitários foram implementados para a camada de serviço (`ParkingEventService`, `GarageSetupService`) usando JUnit 5 e Mockito, focando na validação da lógica de negócio, cálculos e diferentes cenários de eventos.
Execute com:
```bash
./mvnw test
```

## 7. Melhorias Propostas

Para evoluir o sistema e prepará-lo para um ambiente de produção ainda mais robusto, as seguintes melhorias são sugeridas:

1.  **Segurança da API:** Implementar autenticação e autorização (ex: Spring Security com JWT) para proteger os endpoints.
2.  **Testes de Integração:** Adicionar testes que validem os fluxos da API de ponta a ponta, incluindo a interação com o banco de dados (ex: usando Testcontainers).
3.  **Resiliência:** Incorporar padrões como Retry ou Circuit Breaker para a comunicação com serviços externos, como o simulador.
4.  **Configurabilidade das Regras de Negócio:** Permitir que parâmetros chave (ex: percentuais de preço dinâmico) sejam configuráveis externamente.
5.  **Monitoramento Avançado:** Expandir o uso do Actuator e considerar integração com ferramentas como Prometheus/Grafana para métricas de negócio e técnicas.