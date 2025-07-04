# A2A Java SDK HelloWorld 예제 실행 가이드

## 1. 사전 준비 사항

예제를 실행하기 전에 다음 도구들이 설치되어 있어야 합니다.

* **Java Development Kit (JDK) 11 이상**: Java 애플리케이션 실행 환경
* **Apache Maven**: Java 프로젝트 빌드 및 의존성 관리 도구
* **JBang**: Java 소스 코드를 직접 실행할 수 있게 해주는 도구 (설치 과정에 포함)
* **Git**: A2A Java SDK 저장소를 클론하기 위한 도구

---

## 2. A2A Java SDK 저장소 클론

먼저 A2A Java SDK 저장소를 로컬 환경으로 클론합니다.

```bash
git clone [https://github.com/a2aproject/a2a-java.git](https://github.com/a2aproject/a2a-java.git)
cd a2a-java # 클론된 저장소 디렉토리로 이동
````

-----

## 3\. Maven 설치 확인 (설치되어 있지 않다면 설치)

Maven이 설치되어 있지 않다면 다음 명령어를 사용하여 설치할 수 있습니다. (macOS 기준)

```bash
brew install maven
```

설치 후, 다음 명령어로 Maven 버전이 정상적으로 출력되는지 확인합니다.

```bash
mvn --version
```

-----

## 4\. A2A Server 실행

이제 A2A 서버 예제를 실행합니다.

1.  **서버 프로젝트 디렉토리로 이동:**

    ```bash
    cd examples/server
    ```

2.  **서버 실행:**

    ```bash
    mvn quarkus:dev
    ```

    **예상 출력 (일부):**

    ```
    ...
    __  ____  __  _____   ___  __ ____  ______
     --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
     -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
    --\___\_\____/_/ |_/_/|_/_/|_|\____/___/
    2025-07-03 22:21:30,972 INFO  [io.quarkus] (Quarkus Main Thread) a2a-java-sdk-examples-server 0.2.4-SNAPSHOT on JVM (powered by Quarkus 3.22.3) started in 4.811s. Listening on: http://localhost:9999
    ...
    ```

    * `Listening on: http://localhost:9999` 메시지가 보이면 서버가 정상적으로 시작된 것입니다.
    * 일부 `WARNING` 메시지가 출력될 수 있으나, 서버 동작에는 문제가 없습니다.

-----

## 5\. JBang 설치 및 확인

JBang은 Java 소스 파일을 직접 실행할 수 있게 해주는 편리한 도구입니다.

1.  **JBang 설치:**

    ```bash
    curl -Ls [https://sh.jbang.dev](https://sh.jbang.dev) | bash -s - app setup
    ```

    * Windows 사용자는 [JBang 공식 설치 가이드](https://www.jbang.dev/documentation/guide/latest/installation.html)를 참고하세요.

2.  **JBang 버전 확인:**

    ```bash
    jbang --version
    ```

    * JBang 버전 정보가 출력되면 정상적으로 설치된 것입니다.

-----

## 6\. A2A Client 실행

서버가 실행 중인 상태에서 새로운 터미널을 열고 A2A 클라이언트 예제를 실행합니다.

1.  **클라이언트 프로젝트 디렉토리로 이동:**

    ```bash
    cd ../client/src/main/java/io/a2a/examples/helloworld
    ```

    * 이전 단계에서 `examples/server`에 있었다면 `cd ../client/src/main/java/io/a2a/examples/helloworld`로 이동합니다.

2.  **클라이언트 실행:**

    ```bash
    jbang HelloWorldRunner.java
    ```

    **예상 출력:**

    ```
    Successfully fetched public agent card:
    {"name":"Hello World Agent","description":"Just a hello world agent","url":"http://localhost:9999","version":"1.0.0","documentationUrl":"[http://example.com/docs](http://example.com/docs)","capabilities":{"streaming":true,"pushNotifications":true,"stateTransitionHistory":true},"defaultInputModes":["text"],"defaultOutputModes":["text"],"skills":[{"id":"hello_world","name":"Returns hello world","description":"just returns hello world","tags":["hello world"],"examples":["hi","hello world"]}],"supportsAuthenticatedExtendedCard":false}
    Using public agent card for client initialization (default).
    Public card does not indicate support for an extended card. Using public card.
    Message sent with ID: af4ce0cc-9cbe-4d6e-94f6-646ebf36be6b
    Response: io.a2a.spec.SendMessageResponse@ebaa6cb
    ```

    * `Successfully fetched public agent card:` 메시지와 함께 서버의 에이전트 카드 정보가 출력됩니다.
    * `Message sent with ID:` 메시지와 함께 클라이언트가 서버에 메시지를 성공적으로 보냈음을 알 수 있습니다.
    * `Response:` 메시지는 서버로부터 받은 응답 객체입니다.

-----

## 7\. 실행 확인

클라이언트 실행 후, 서버 터미널로 돌아가면 클라이언트의 요청을 처리하는 과정에 대한 추가 로그가 출력된 것을 확인할 수 있습니다.

**서버 터미널 예상 추가 로그 (일부):**

```
...
2025-07-03 22:25:22,647 INFO  [io.a2a.ser.req.DefaultRequestHandler] (vert.x-worker-thread-1) onMessageSend - task: null; context null
2025-07-03 22:25:22,653 INFO  [io.a2a.ser.req.DefaultRequestHandler] (vert.x-worker-thread-1) Request context taskId: d4193365-a9e0-487e-ab90-f020da0df20c
...
```

이로써 A2A Java SDK의 `helloworld` 예제가 성공적으로 실행되었음을 확인할 수 있습니다. 이제 이 동작을 바탕으로 각 파일의 코드를 분석하며 A2A SDK의 내부 동작을 더 깊이 이해할 수 있습니다.

-----

**참고:**

* 클라이언트를 실행하기 전에 반드시 서버가 먼저 실행되어 있어야 합니다.
* `mvn quarkus:dev`는 개발 모드로, 코드를 변경하면 자동으로 재빌드 및 재시작됩니다.
