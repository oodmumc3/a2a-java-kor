## **A2A Java SDK: `sdk-quarkus` 모듈 심층 분석**

이 문서에서는 A2A Java SDK의 `sdk-quarkus` 모듈을 자세히 살펴봄 이전에 `core` 및 `sdk-server-common` 모듈에 대한 문서를 보셨다면 `sdk-quarkus`가 어떻게 이들을 활용하여 Quarkus 애플리케이션에서 A2A 서버 기능을 제공하는지 이해하는 데 큰 도움이 될 것임

### 1. `sdk-quarkus` 모듈 개요

`sdk-quarkus` 모듈은 A2A 프로토콜 기반의 서버 애플리케이션을 **Quarkus 프레임워크 위에서 쉽고 효율적으로 개발할 수 있도록 통합 기능과 컴포넌트를 제공함** 이는 `sdk-server-common` 모듈에서 정의된 핵심 비즈니스 로직과 공통 컴포넌트들을 Quarkus의 강력한 CDI(Contexts and Dependency Injection) 메커니즘을 통해 연결하고 활용함

즉 `sdk-server-common`이 특정 프레임워크에 독립적인 핵심 서버 로직을 담당한다면 `sdk-quarkus`는 그 로직을 Quarkus 환경에 맞게 "어댑팅"하여 HTTP 엔드포인트를 노출하고 종속성을 관리하는 역할을 수행함

### 2. 핵심 컴포넌트 및 Quarkus 통합

`sdk-quarkus` 모듈의 중심에는 클라이언트의 HTTP 요청을 받아 JSON-RPC 요청을 처리하고 응답하는 `A2AServerRoutes` 클래스가 있음 이 클래스는 Quarkus의 Reactive Routes를 사용하여 RESTful API 엔드포인트를 정의함

`A2AServerRoutes`는 다음과 같은 핵심 컴포넌트들을 `@Inject` 어노테이션을 통해 주입받아 사용함:

* **`JSONRPCHandler jsonRpcHandler`**:
  `sdk-server-common` 모듈에 정의된 `JSONRPCHandler` 인터페이스의 구현체(기본적으로 `DefaultRequestHandler`)를 주입받음 이 핸들러는 모든 JSON-RPC 요청의 최상위 진입점이며 실제 비즈니스 로직 처리를 `DefaultRequestHandler`에 위임함

* **`@ExtendedAgentCard Instance<AgentCard> extendedAgentCard`**:
    - `AgentCard`는 에이전트의 메타데이터를 담는 인터페이스임
    - `@ExtendedAgentCard`는 CDI `@Qualifier` 어노테이션으로 `AgentCard` 타입 중 **확장된 정보(예: 내부 설정 디버그 정보 관리 기능 목록 등)를 포함하는 특정 `AgentCard` 구현체**를 구분하여 주입받기 위해 사용됨
    - `Instance` 타입을 사용하는 것은 이 확장된 에이전트 카드가 **선택적인 컴포넌트**임을 나타냄
    - 해당 빈이 존재하지 않더라도 애플리케이션 오류를 발생시키지 않고 런타임에 `isResolvable()` 메서드를 통해 사용 가능 여부를 확인할 수 있도록 함
    - **최종 서비스 애플리케이션에서 `@ExtendedAgentCard`와 함께 `AgentCard`의 구현체를 직접 빈으로 등록해야 함**

* **`@Internal Executor executor`**:
  비동기 작업을 처리하는 데 사용되는 스레드 풀(`Executor`)을 주입받음 이 `Executor` 구현체는 `sdk-server-common` 모듈의 `AsyncExecutorProducer` 클래스에서 `@Produces` 어노테이션을 통해 제공되므로 **별도로 구현하거나 설정할 필요가 없음**

* **`MultiSseSupport` (내부 클래스)**:
  이 클래스는 `Flow.Publisher`에서 생성되는 객체(JSON-RPC 응답)를 SSE(Server-Sent Events) 형식으로 HTTP 응답 스트림에 효율적으로 쓰는 역할을 담당함 스트리밍 JSON-RPC 응답을 클라이언트에 실시간으로 전달하는 데 활용됨

### 3. 노출되는 주요 API 엔드포인트

`A2AServerRoutes`는 주로 다음 세 가지 HTTP 엔드포인트를 통해 외부와 통신함

1.  **JSON-RPC 메인 핸들러**

    * **경로**: `/`

    * **HTTP 메서드**: `POST`

    * **Content-Type**: `application/json`

    * **설명**:
        - 이 엔드포인트는 A2A 프로토콜의 모든 JSON-RPC 요청을 처리하는 중앙 게이트웨이임 요청 본문을 분석하여 스트리밍 요청(예: `sendStreamingMessage` `resubscribeToTask`)인지 아닌지 (`sendMesage` `getTask` 등)를 판단함
        - `JSONRPCHandler`의 해당 메서드를 호출하여 비즈니스 로직을 위임하며 JSON 파싱 오류나 JSON-RPC 관련 에러 발생 시 표준화된 에러 응답을 반환함 스트리밍 요청의 경우 `MultiSseSupport`를 통해 SSE 형식으로 이벤트를 스트리밍함

    ```plantuml
        @startuml
        actor Client
        participant A2AServerRoutes
        participant JSONRPCHandler
        participant DefaultRequestHandler
        
        Client -> A2AServerRoutes: POST / (JSON-RPC Request)
        activate A2AServerRoutes
        A2AServerRoutes -> JSONRPCHandler: JSON-RPC 메서드 실행
        activate JSONRPCHandler
        JSONRPCHandler -> DefaultRequestHandler: 비즈니스 로직 처리
        activate DefaultRequestHandler
        DefaultRequestHandler --> JSONRPCHandler: 결과 또는 에러 반환
        deactivate DefaultRequestHandler
        JSONRPCHandler --> A2AServerRoutes: JSON-RPC 응답 반환 (또는 스트리밍을 위한 Publisher)
        deactivate A2AServerRoutes
        @enduml
    ```

2.  **공용 에이전트 카드 조회**

    * **경로**: `/.well-known/agent.json`

    * **HTTP 메서드**: `GET`

    * **Produces**: `application/json`

    * **설명**:
        - 에이전트의 기본적인 공용 메타데이터(`AgentCard`)를 JSON 형식으로 제공하는 표준 엔드포인트임
        - 에이전트의 이름 설명 버전 및 지원 기능(스트리밍 푸시 알림 등)과 같은 정보가 포함되어 클라이언트가 에이전트의 기능을 동적으로 파악할 수 있도록 함
        - **이 `AgentCard` 구현체는 `@PublicAgentCard` Qualifier를 사용하여 최종 서비스 애플리케이션에서 직접 빈으로 등록해야 함**

    ```plantuml
    @startuml
    actor Client
    participant A2AServerRoutes
    participant JSONRPCHandler
    participant "AgentCard (Public)" as PublicAgentCard
    
    Client -> A2AServerRoutes: GET /.well-known/agent.json
    activate A2AServerRoutes
    A2AServerRoutes -> JSONRPCHandler: getAgentCard() 호출
    activate JSONRPCHandler
    JSONRPCHandler -> PublicAgentCard: 공개 AgentCard 조회
    activate PublicAgentCard
    PublicAgentCard --> JSONRPCHandler: AgentCard 객체 반환
    deactivate PublicAgentCard
    JSONRPCHandler --> A2AServerRoutes: AgentCard 객체 반환
    deactivate JSONRPCHandler
    A2AServerRoutes --> Client: HTTP 200 OK (AgentCard JSON)
    deactivate A2AServerRoutes
    @enduml
    ```

3.  **인증된 확장 에이전트 카드 조회**

    * **경로**: `/agent/authenticatedExtendedCard`

    * **HTTP 메서드**: `GET`

    * **Produces**: `application/json`

    * **설명**:
        - 이 엔드포인트는 일반 `AgentCard`보다 더 상세하거나 민감한 정보를 포함할 수 있는 확장된 에이전트 카드 정보를 제공함
        - `A2AServerRoutes` 코드에는 아직 인증 로직이 TODO로 남아있지만 일반적으로는 특정 권한이 있는 사용자나 시스템에만 접근이 허용되는 정보가 포함됨
        - 에이전트가 이 기능을 지원하고 (`jsonRpcHandler.getAgentCard().supportsAuthenticatedExtendedCard()`) 서버에 확장 `AgentCard` 구현체가 설정되어 있는 경우에만 응답을 반환함
        - **이 확장 `AgentCard` 구현체는 `@ExtendedAgentCard` Qualifier를 사용하여 최종 서비스 애플리케이션에서 직접 빈으로 등록해야 함**

    ```plantuml
    @startuml
    actor Client
    participant A2AServerRoutes
    participant JSONRPCHandler
    participant "AgentCard (Extended)" as ExtendedAgentCard

    Client -> A2AServerRoutes: GET /agent/authenticatedExtendedCard
    activate A2AServerRoutes
    A2AServerRoutes -> JSONRPCHandler: getAgentCard().supportsAuthenticatedExtendedCard() 호출
    activate JSONRPCHandler
    JSONRPCHandler --> A2AServerRoutes: boolean 값 반환
    deactivate JSONRPCHandler
    alt 지원되고 해석 가능한 경우
        A2AServerRoutes -> ExtendedAgentCard: 확장 AgentCard 조회
        activate ExtendedAgentCard
        ExtendedAgentCard --> A2AServerRoutes: 확장 AgentCard 객체 반환
        deactivate ExtendedAgentCard
        A2AServerRoutes --> Client: HTTP 404 Not Found
    end
    deactivate A2AServerRoutes
    @enduml
    ```

### 4. `AgentCard` 구현 및 CDI 스코프 (`@ApplicationScoped`)

`sdk-quarkus`는 `AgentCard` 인터페이스의 구체적인 구현체를 직접 제공하지 않음 대신 `@PublicAgentCard` 및 `@ExtendedAgentCard`와 같은 `@Qualifier` 어노테이션을 사용하여 **최종 서비스 애플리케이션에서 직접 `AgentCard`의 구현체를 CDI 빈으로 등록**하도록 유도함

이때 사용되는 `@ApplicationScoped` 스코프 어노테이션은 해당 빈이 **애플리케이션 전체에서 단 하나의 인스턴스만 생성되어 애플리케이션의 생명주기 동안 유지되고 모든 클라이언트가 공유**함을 의미함 `JSONRPCHandler`나 `InMemoryQueueManager`와 같이 애플리케이션 전반에 걸쳐 상태를 공유하고 효율적으로 관리해야 하는 핵심 컴포넌트에 이 스코프가 적합하게 사용됨

### 7. `sdk-quarkus`와 `sdk-jakarta` 모듈의 공통점 및 차이점

`sdk-quarkus`와 `sdk-jakarta` 모듈은 모두 `sdk-server-common` 모듈의 핵심 기능을 활용하여 A2A 서버를 구현함 두 모듈은 각각 Quarkus와 Jakarta EE라는 다른 프레임워크를 기반으로 하지만 기본적인 목적과 핵심 로직은 동일함

#### 공통점

* **핵심 의존성**: 두 모듈 모두 `JSONRPCHandler` `@ExtendedAgentCard Instance<AgentCard>` `@Internal Executor`와 같은 핵심 A2A 서버 컴포넌트들을 CDI를 통해 주입받아 사용함 이는 `sdk-server-common`에 정의된 공통 비즈니스 로직을 재사용함을 의미함

* **API 엔드포인트**:
    - JSON-RPC 요청을 처리하는 `POST /` 엔드포인트
    - 공용 에이전트 카드 조회 `GET /.well-known/agent.json` 엔드포인트
    - 인증된 확장 에이전트 카드 조회 `GET /agent/authenticatedExtendedCard` 엔드포인트
      이 세 가지 주요 API 엔드포인트를 동일하게 노출함

* **JSON-RPC 처리 위임**: 클라이언트로부터 받은 JSON-RPC 요청을 `JSONRPCHandler`에게 위임하여 실제 비즈니스 로직을 처리함

* **스트리밍 지원**: 두 모듈 모두 `Flow.Publisher`를 사용하여 스트리밍 응답을 지원하며 이는 반응형 프로그래밍 패러다임을 따름

* **에러 처리**: JSON 파싱 및 매핑 예외에 대한 에러 처리 로직을 포함하며 `JSONRPCErrorResponse` 형태로 표준화된 에러 응답을 반환함

#### 차이점

* **기반 프레임워크**:
    - `sdk-quarkus`는 Quarkus를 사용하고 `sdk-jakarta`는 Jakarta EE를 사용함 각 프레임워크는 웹 애플리케이션을 만드는 방식이 다름

* **스트리밍 방식**:
    - `sdk-quarkus`는 자체적인 SSE(Server-Sent Events) 지원 방식을 사용해 데이터를 실시간으로 보냄
    - `sdk-jakarta`는 Jakarta EE의 표준 SSE 기능을 활용해 데이터를 실시간으로 보냄

* **요청 처리 방식**:
    - `sdk-quarkus`는 요청을 받으면 본문을 직접 읽어서 스트리밍인지 아닌지 판단하고 처리함
    - `sdk-jakarta`는 요청이 들어오기 전에 필터(Filter)를 사용해 요청 타입을 미리 구분하고 각각 다른 방식으로 처리하도록 넘겨줌

* **에러 처리 방식**:
    - `sdk-quarkus`는 코드 안에서 `try-catch`로 예외를 직접 잡아서 에러 응답을 만듦
    - `sdk-jakarta`는 `ExceptionMapper`라는 표준 기능을 사용해 예외가 발생하면 자동으로 에러 응답으로 변환함

두 모듈은 A2A 서버 기능이라는 동일한 목표를 가지고 있지만 각 프레임워크의 특성과 기능을 최대한 활용하여 구현 방식에서 차이를 보임

#### `sdk-quarkus`와 `sdk-jakarta` 중 선택 가이드

두 모듈 중 어떤 것을 선택할지는 주로 팀의 기술 스택 경험 그리고 프로젝트의 요구사항에 따라 결정됨

* **기존 프로젝트와의 통합**:
    - 만약 이미 Quarkus 기반의 프로젝트를 운영 중이거나 Quarkus에 대한 팀의 숙련도가 높다면 `sdk-quarkus`를 선택하는 것이 자연스러움
    - 기존에 Jakarta EE (예: WildFly GlassFish Open Liberty 등) 기반의 애플리케이션 서버를 사용하고 있거나 JAX-RS CDI 등 Jakarta EE 표준 기술에 익숙하다면 `sdk-jakarta`를 선택하는 것이 유리함

* **성능 및 리소스 효율성**:
    - Quarkus는 빠른 시작 시간 낮은 메모리 사용량 높은 처리량 등 클라우드 네이티브 환경에 최적화된 성능을 제공함 따라서 마이크로서비스나 서버리스 환경에서 높은 성능과 효율성이 중요하다면 `sdk-quarkus`가 더 적합할 수 있음
    - Jakarta EE는 오랜 기간 검증된 안정성과 폭넓은 기능 세트를 제공하지만 일반적으로 Quarkus보다 더 많은 리소스를 요구할 수 있음

* **개발자 경험**:
    - Quarkus는 개발 생산성을 높이는 데 중점을 둠 개발 중 코드 변경 시 빠른 재시작(Live Coding) 기능과 간결한 설정 등 개발자 친화적인 기능을 제공함
    - Jakarta EE는 표준 스펙을 따르므로 다양한 벤더의 구현체를 선택할 수 있는 유연성이 있음 하지만 Quarkus와 같은 최신 프레임워크의 개발 편의 기능은 부족할 수 있음

* **커뮤니티 및 생태계**:
    - Quarkus는 비교적 신흥 프레임워크이지만 빠르게 성장하는 커뮤니티와 활발한 생태계를 가지고 있음
    - Jakarta EE는 오랜 역사와 방대한 커뮤니티를 가지고 있으며 엔터프라이즈 환경에서 폭넓게 사용됨

* **Spring Boot 사용 서비스의 경우**:
    - 현재 Spring Boot를 사용하고 있다면 두 가지 주요 접근 방식을 고려할 수 있음
    - **Spring Boot 유지**: A2A Java SDK는 Spring Boot를 직접 지원하는 모듈을 제공하지 않음 이 경우 `sdk-server-common` 모듈의 핵심 로직을 직접 가져와 Spring Boot 애플리케이션에 통합하는 방안을 고려할 수 있음 이는 추가적인 통합 작업이 필요하지만 기존 Spring Boot 생태계를 그대로 활용할 수 있다는 장점이 있음
    - **Quarkus 또는 Jakarta EE로 전환**: 마이크로서비스 아키텍처로의 전환이나 클라우드 네이티브 환경 최적화가 주된 목표라면 Quarkus로의 전환을 고려할 수 있음 Quarkus는 Spring Boot와 유사한 개발 경험을 제공하면서도 더 빠른 시작 시간과 낮은 메모리 사용량을 강점으로 내세움 만약 Jakarta EE 표준을 따르는 엔터프라이즈 환경에 더 중점을 둔다면 `sdk-jakarta` 모듈을 통한 전환도 가능함 이 경우 점진적인 마이그레이션 전략을 수립하는 것이 중요함

**요약**:

| 기준 | `sdk-quarkus` | `sdk-jakarta` |
|---|---|---|
| **기존 스택** | Quarkus 기반 프로젝트 또는 Quarkus 선호 팀 | Jakarta EE 기반 프로젝트 또는 JAX-RS CDI 선호 팀 |
| **성능/효율성** | 마이크로서비스 클라우드 네이티브 환경에 최적화된 고성능 | 안정적이지만 상대적으로 더 많은 리소스 요구 |
| **개발 경험** | 빠른 재시작 간결한 설정 등 높은 개발 생산성 | 표준 스펙 기반 다양한 벤더 선택 가능 |
| **커뮤니티** | 빠르게 성장하는 활발한 커뮤니티 | 오랜 역사와 방대한 엔터프라이즈 커뮤니티 |

최종 선택은 프로젝트의 구체적인 요구사항과 팀의 전문성을 종합적으로 고려하여 결정하는 것이 가장 중요함
