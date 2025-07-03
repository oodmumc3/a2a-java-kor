# **A2A Java SDK 및 JSON-RPC 이해를 위한 가이드**

## **1. JSON-RPC 기본 개념**

* **정의**: JSON(JavaScript Object Notation)을 사용하여 원격으로 함수(Procedure)를 호출하는 프로토콜
* **특징**:
    * **경량성**: JSON 기반의 가벼운 데이터 형식으로, 사람이 읽고 쓰기 용이
    * **간단함**: 프로토콜 구조가 직관적이고 단순하여 구현 및 이해 용이
    * **함수 중심**: 특정 비즈니스 로직을 수행하는 함수(메서드)를 직접 호출하는 것에 중점
    * **단일 엔드포인트**: 일반적으로 모든 요청이 단일 URL로 `POST` HTTP 메서드를 사용하여 전송
    * **JSON 규격 (JSON-RPC 2.0)**:
        * **요청 객체**:
            * `jsonrpc`: 프로토콜 버전 ("2.0"으로 고정)
            * `method`: 호출할 메서드의 이름 (문자열)
            * `params`: 메서드에 전달할 매개변수 (구조화된 값 또는 배열)
            * `id`: 요청을 식별하는 고유 값 (응답과 매칭)
        * **응답 객체**:
            * `jsonrpc`: 프로토콜 버전 ("2.0"으로 고정)
            * `result`: 메서드 호출 성공 시 반환되는 값 (요청에 `params`가 없는 경우 `null`일 수 있음)
            * `error`: 메서드 호출 실패 시 오류 정보 (오류 코드, 메시지 등)
            * `id`: 요청과 동일한 `id` 값 (오류 발생 시 `null`일 수 있음)
* **동작 방식**:
    * **요청**: 클라이언트가 서버의 특정 함수를 호출하기 위해 정의된 JSON 형식의 요청 메시지를 전송
        ```json
        // JSON-RPC 요청 예시: 날씨 정보 요청
        {
          "jsonrpc": "2.0",
          "method": "getWeather",
          "params": { "city": "Seoul" },
          "id": 123
        }
        ```
    * **처리**: 서버는 수신된 요청을 파싱하여 요청된 함수와 매개변수를 식별하고 해당 함수를 실행
    * **응답**: 서버는 함수 실행 결과 또는 발생한 오류를 정의된 JSON 형식의 응답 메시지로 클라이언트에게 반환
        ```json
        // JSON-RPC 응답 예시: 날씨 정보 결과
        {
          "jsonrpc": "2.0",
          "result": { "temperature": 25, "condition": "Sunny" },
          "id": 123
        }
        ```

## **2. JSON REST API와 JSON-RPC 비교**

* **목적 차이**:
    * **JSON REST API**: 웹상의 '자원(Resource)'(예: 사용자, 게시물, 상품)을 생성(Create), 조회(Read), 수정(Update), 삭제(Delete)하는 CRUD 작업에 중점
    * **JSON-RPC**: 특정 '함수(Method)' 또는 '프로시저'를 원격으로 실행하는 것에 중점
* **엔드포인트 활용**:
    * **JSON REST API**: 각 자원 또는 자원 컬렉션마다 고유한 여러 URL 경로를 가지며, `GET`, `POST`, `PUT`, `DELETE` 등 다양한 HTTP 메서드를 사용하여 작업을 구분
        ```
        // REST API 엔드포인트 예시:
        GET /users/1         // 1번 사용자 조회
        POST /products       // 새 상품 생성
        PUT /orders/ABC      // ABC 주문 수정
        ```
    * **JSON-RPC**: 모든 함수 호출에 일반적으로 `/rpc`와 같은 단일 URL 엔드포인트를 사용하며, 대부분 `POST` HTTP 메서드를 통해 요청을 전송
        ```
        // JSON-RPC 엔드포인트 예시:
        POST /rpc            // 모든 RPC 요청은 이 단일 엔드포인트로 전송
        ```
* **요청 내용 구성**:
    * **JSON REST API**: HTTP 메서드와 URL 경로 자체가 자원과 수행할 행위를 명시하며, 요청 본문의 JSON은 자원의 현재 또는 변경될 상태를 표현. 특정 필드에 대한 엄격한 제약보다는 자원 모델을 따름
        ```json
        // REST API POST 요청 본문 예시: 새 사용자 생성
        POST /users
        {
          "name": "홍길동",
          "email": "hong@example.com"
        }
        ```
    * **JSON-RPC**: 요청 본문의 JSON 내부에 `method`라는 필드를 통해 호출할 함수의 이름을 명시하고, `params` 필드로 해당 함수에 전달할 매개변수를 정의하는 명확한 형식을 따름
        ```json
        // JSON-RPC 요청 본문 예시: 사용자 생성 함수 호출
        {
          "jsonrpc": "2.0",
          "method": "createUser",
          "params": { "name": "홍길동", "email": "hong@example.com" },
          "id": 456
        }
        ```
* **URL 구조**:
    * **JSON REST API**: 자원의 계층 구조를 반영하고 의미를 파악하기 쉬운 계층적이고 RESTful한 URL 구조를 지향
    * **JSON-RPC**: 일반적으로 단일 엔드포인트에 모든 요청이 집중되므로, URL 자체보다는 요청 본문의 `method` 필드가 핵심적인 역할을 함

* **주요 차이점 요약**:

| 특징 | JSON REST API 호출 | JSON-RPC 호출 |
| :---- | :-------------------- | :--------------- |
| **중점** | 자원(Resource) 조작 | 함수(Method/Procedure) 호출 |
| **HTTP 메서드** | `GET`, `POST`, `PUT`, `DELETE` 등 다양하게 활용 | 주로 `POST`만 사용 (단일 엔드포인트) |
| **엔드포인트** | 자원마다 고유한 URL (예: `/users`, `/products/1`) | 모든 RPC 요청이 단일 URL로 전송 (예: `/rpc`) |
| **요청 내용** | HTTP 메서드와 URL 경로로 자원과 행위 명시 | `method` 이름과 `params`로 함수와 인자 명시 |
| **URL 구조** | 계층적이고 의미 있는 URL 사용 | 주로 단일 엔드포인트 사용 |

* **동작 방식 시각화 (PlantUML)**:
    * **JSON REST API 동작 방식 (게시물 조회)**
        ```plantuml
        @startuml
        title JSON REST API 동작 방식 (게시물 조회)

        actor Client
        participant "REST API Server" as Server
        database "Database (Posts)" as DB

        Client -> Server: GET /posts/1 \n(게시물 1번 조회 요청)
        activate Server
        Server -> DB: SELECT * FROM posts WHERE id = 1
        activate DB
        DB --> Server: 게시물 1번 데이터 반환
        deactivate DB
        Server --> Client: HTTP 200 OK \n{ "id": 1, "title": "첫 게시물", "content": "..." } \n(게시물 데이터 응답)
        deactivate Server

        @enduml
        ```
      *설명*: 클라이언트가 `GET` 메서드와 `/posts/1` URL을 통해 특정 게시물 자원의 조회를 요청하고, 서버는 데이터베이스에서 해당 자원을 조회하여 JSON 형태로 응답

    * **JSON-RPC 동작 방식 (날씨 정보 요청)**
        ```plantuml
        @startuml
        title JSON-RPC 동작 방식 (날씨 정보 요청)

        actor Client
        participant "JSON-RPC Server" as Server
        participant "Weather Service" as WS

        Client -> Server: POST /rpc \n{\n  "jsonrpc": "2.0",\n  "method": "getWeather",\n  "params": { "city": "Seoul" },\n  "id": 123\n} \n(날씨 조회 함수 호출 요청)
        activate Server
        Server -> WS: getWeather(city="Seoul") \n(내부 서비스 호출)
        activate WS
        WS --> Server: 날씨 데이터 반환
        deactivate WS
        Server --> Client: HTTP 200 OK \n{\n  "jsonrpc": "2.0",\n  "result": { "temperature": 25, "condition": "Sunny" },
          "id": 123
        } \n(함수 호출 결과 응답)
        deactivate Server

        @enduml
        ```
      *설명*: 클라이언트가 단일 엔드포인트 `/rpc`로 `POST` 요청을 보내고, 요청 본문의 `method` 필드를 통해 `getWeather` 함수를 호출하며, 서버는 해당 함수를 실행한 결과를 JSON 응답으로 반환

## **3. A2A 프레임워크의 JSON-RPC 활용**

* **A2A 프로토콜 정의**: A2A(Agent2Agent) 프로토콜은 서로 다른 에이전트(서비스) 간의 표준화된 통신을 위해 JSON-RPC를 기반으로 활용
* **주요 활용 목적**:
    * **메시지 및 작업 교환**: 에이전트 간에 메시지를 주고받거나 특정 작업을 요청하고 그 상태를 관리하는 데 사용
    * **원격 기능 호출**: 다른 에이전트가 제공하는 특정 기능을 원격으로 호출하는 데 활용
    * **실시간 상호작용**: 스트리밍 메시지나 푸시 알림과 같이 실시간으로 정보를 주고받는 메커니즘에 적용
