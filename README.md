-----

# A2A Java SDK

[](https://www.google.com/search?q=LICENSE)

\<html\>
\<h2 align="center"\>
\<img src="[https://raw.githubusercontent.com/google-a2a/A2A/refs/heads/main/docs/assets/a2a-logo-black.svg](https://raw.githubusercontent.com/google-a2a/A2A/refs/heads/main/docs/assets/a2a-logo-black.svg)" width="256" alt="A2A 로고"/\>
\</h2\>
\<h3 align="center"\>\<a href="[https://google-a2a.github.io/A2A](https://google-a2a.github.io/A2A)"\>Agent2Agent (A2A) 프로토콜\</a\>에 따라 에이전트 애플리케이션을 A2AServer로 실행하는 데 도움이 되는 자바 라이브러리입니다.\</h3\>
\</html\>

-----

## 설치

`mvn`을 사용하여 A2A Java SDK를 빌드할 수 있습니다:

```bash
mvn clean install
```

-----

## 예시

A2A Java SDK 사용 방법에 대한 예시는 [여기](https://github.com/fjuma/a2a-samples/tree/java-sdk-example/samples/multi_language/python_and_java_multiagent/weather_agent)에서 찾을 수 있습니다.

더 많은 예시가 곧 추가될 예정입니다.

-----

## A2A 서버

A2A Java SDK는 [Agent2Agent (A2A) 프로토콜](https://google-a2a.github.io/A2A)의 자바 서버 구현을 제공합니다. 에이전트 자바 애플리케이션을 A2A 서버로 실행하려면 아래 단계를 따르세요.

- [프로젝트에 A2A Java SDK Core Maven 종속성 추가](https://www.google.com/search?q=%231-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8%EC%97%90-a2a-java-sdk-core-maven-%EC%A2%85%EC%86%8D%EC%84%B1-%EC%B6%94%EA%B0%80)
- [A2A 에이전트 카드(Agent Card)를 생성하는 클래스 추가](https://www.google.com/search?q=%232-a2a-%EC%97%90%EC%9D%B4%EC%A0%84%ED%8A%B8-%EC%B9%B4%EB%93%9C-agent-card%EB%A5%BC-%EC%83%9D%EC%84%B1%ED%95%98%EB%8A%94-%ED%95%98%EB%8A%94-%ED%81%B4%EB%9E%98%EC%8A%A4-%EC%B6%94%EA%B0%80)
- [A2A 에이전트 실행기(Agent Executor)를 생성하는 클래스 추가](https://www.google.com/search?q=%233-a2a-%EC%97%90%EC%9D%B4%EC%A0%84%ED%8A%B8-%EC%8B%A4%ED%96%89%EA%B8%B0-agent-executor%EB%A5%BC-%EC%83%9D%EC%84%B1%ED%95%98%EB%8A%94-%ED%81%B4%EB%9E%98%EC%8A%A4-%EC%B6%94%EA%B0%80)
- [프로젝트에 A2A Java SDK Server Maven 종속성 추가](https://www.google.com/search?q=%234-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8%EC%97%90-a2a-java-sdk-server-maven-%EC%A2%85%EC%86%8D%EC%84%B1-%EC%B6%94%EA%B0%80)

-----

### 1\. 프로젝트에 A2A Java SDK Core Maven 종속성 추가

> **참고**: A2A Java SDK는 아직 Maven Central에서 사용할 수 없지만 곧 제공될 예정입니다. 지금은 최신 태그(태그는 [여기](https://github.com/a2aproject/a2a-java/tags)에서 확인할 수 있음)를 확인하고, 해당 태그에서 빌드한 다음 아래에 해당 버전을 참조해야 합니다. 예를 들어, 최신 태그가 `0.2.3`인 경우 다음 종속성을 사용할 수 있습니다.

```xml
<dependency>
    <groupId>io.a2a.sdk</groupId>
    <artifactId>a2a-java-sdk-core</artifactId>
    <version>0.2.3</version>
</dependency>
```

-----

### 2\. A2A 에이전트 카드(Agent Card)를 생성하는 클래스 추가

```java
import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
...

@ApplicationScoped
public class WeatherAgentCardProducer {
    
    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name("Weather Agent")
                .description("Helps with weather")
                .url("http://localhost:10001")
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                        .id("weather_search")
                        .name("Search weather")
                        .description("Helps with weather in city, or states")
                        .tags(Collections.singletonList("weather"))
                        .examples(List.of("weather in LA, CA"))
                        .build()))
                .build();
    }
}
```

-----

### 3\. A2A 에이전트 실행기(Agent Executor)를 생성하는 클래스 추가

```java
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
...

@ApplicationScoped
public class WeatherAgentExecutorProducer {

    @Inject
    WeatherAgent weatherAgent;

    @Produces
    public AgentExecutor agentExecutor() {
        return new WeatherAgentExecutor(weatherAgent);
    }

    private static class WeatherAgentExecutor implements AgentExecutor {

        private final WeatherAgent weatherAgent;

        public WeatherAgentExecutor(WeatherAgent weatherAgent) {
            this.weatherAgent = weatherAgent;
        }

        @Override
        public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            TaskUpdater updater = new TaskUpdater(context, eventQueue);

            // 작업을 제출 상태로 표시하고 작업 시작
            if (context.getTask() == null) {
                updater.submit();
            }
            updater.startWork();

            // 메시지에서 텍스트 추출
            String userMessage = extractTextFromMessage(context.getMessage());

            // 사용자 메시지로 날씨 에이전트 호출
            String response = weatherAgent.chat(userMessage);

            // 응답 파트 생성
            TextPart responsePart = new TextPart(response, null);
            List<Part<?>> parts = List.of(responsePart);

            // 응답을 아티팩트로 추가하고 작업 완료
            updater.addArtifact(parts, null, null, null);
            updater.complete();
        }

        @Override
        public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            Task task = context.getTask();

            if (task.getStatus().state() == TaskState.CANCELED) {
                // 이미 취소된 작업
                throw new TaskNotCancelableError();
            }

            if (task.getStatus().state() == TaskState.COMPLETED) {
                // 이미 완료된 작업
                throw new TaskNotCancelableError();
            }

            // 작업 취소
            TaskUpdater updater = new TaskUpdater(context, eventQueue);
            updater.cancel();
        }

        private String extractTextFromMessage(Message message) {
            StringBuilder textBuilder = new StringBuilder();
            if (message.getParts() != null) {
                for (Part part : message.getParts()) {
                    if (part instanceof TextPart textPart) {
                        textBuilder.append(textPart.getText());
                    }
                }
            }
            return textBuilder.toString();
        }
    }
}
```

-----

### 4\. 프로젝트에 A2A Java SDK Server Maven 종속성 추가

> **참고**: A2A Java SDK는 아직 Maven Central에서 사용할 수 없지만 곧 제공될 예정입니다. 지금은 최신 태그(태그는 [여기](https://github.com/a2aproject/a2a-java/tags)에서 확인할 수 있음)를 확인하고, 해당 태그에서 빌드한 다음 아래에 해당 버전을 참조해야 합니다. 예를 들어, 최신 태그가 `0.2.3`인 경우 다음 종속성을 사용할 수 있습니다.

A2A Java SDK 서버에 종속성을 추가하면 에이전트 자바 애플리케이션을 A2A 서버로 실행할 수 있습니다.

A2A Java SDK는 두 가지 A2A 서버 엔드포인트 구현을 제공합니다. 하나는 Jakarta REST(`a2a-java-sdk-server-jakarta`) 기반이고 다른 하나는 Quarkus Reactive Routes(`a2a-java-sdk-server-quarkus`) 기반입니다. 애플리케이션에 가장 적합한 것을 선택할 수 있습니다.

다음 종속성 중 **하나**를 프로젝트에 추가하세요.

```xml
<dependency>
    <groupId>io.a2a.sdk</groupId>
    <artifactId>a2a-java-sdk-server-jakarta</artifactId>
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

또는

```xml
<dependency>
    <groupId>io.a2a.sdk</groupId>
    <artifactId>a2a-java-sdk-server-quarkus</artifactId>
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

-----

## A2A 클라이언트

A2A Java SDK는 [Agent2Agent (A2A) 프로토콜](https://google-a2a.github.io/A2A)의 자바 클라이언트 구현을 제공하여 A2A 서버와 통신할 수 있도록 합니다.

-----

### 사용 예시

#### A2A 클라이언트 생성

```java
// A2AClient 생성 (지정된 URL은 서버 에이전트의 URL이며, 연결하려는 A2A 서버의 실제 URL로 대체해야 합니다)
A2AClient client = new A2AClient("http://localhost:1234");
```

#### A2A 서버 에이전트에 메시지 전송

```java
// A2A 서버 에이전트에 텍스트 메시지 전송
Message message = A2A.toUserMessage("tell me a joke"); // 메시지 ID는 자동으로 생성됩니다
MessageSendParams params = new MessageSendParams.Builder()
        .message(message)
        .build();
SendMessageResponse response = client.sendMessage(params);        
```

`A2A#toUserMessage`는 `Message` 생성 시 명시적으로 지정하지 않으면 메시지 ID를 자동으로 생성합니다. 다음과 같이 메시지 ID를 명시적으로 지정할 수도 있습니다.

```java
Message message = A2A.toUserMessage("tell me a joke", "message-1234"); // messageId는 message-1234
```

#### 작업의 현재 상태 가져오기

```java
// "task-1234" ID를 가진 작업 검색
GetTaskResponse response = client.getTask("task-1234");

// 응답에 포함할 작업 이력의 최대 항목 수도 지정할 수 있습니다.
GetTaskResponse response = client.getTask(new TaskQueryParams("task-1234", 10));
```

#### 진행 중인 작업 취소

```java
// 이전에 제출한 "task-1234" ID를 가진 작업 취소
CancelTaskResponse response = client.cancelTask("task-1234");

// 맵을 사용하여 추가 속성을 지정할 수도 있습니다.
Map<String, Object> metadata = ...        
CancelTaskResponse response = client.cancelTask(new TaskIdParams("task-1234", metadata));
```

#### 작업의 푸시 알림 구성 가져오기

```java
// 작업 푸시 알림 구성 가져오기
GetTaskPushNotificationConfigResponse response = client.getTaskPushNotificationConfig("task-1234");

// 맵을 사용하여 추가 속성을 지정할 수도 있습니다.
Map<String, Object> metadata = ...
GetTaskPushNotificationConfigResponse response = client.getTaskPushNotificationConfig(new TaskIdParams("task-1234", metadata));
```

#### 작업의 푸시 알림 구성 설정

```java
// 작업 푸시 알림 구성 설정
PushNotificationConfig pushNotificationConfig = new PushNotificationConfig.Builder()
        .url("https://example.com/callback")
        .authenticationInfo(new AuthenticationInfo(Collections.singletonList("jwt"), null))
        .build();
SetTaskPushNotificationResponse response = client.setTaskPushNotificationConfig("task-1234", pushNotificationConfig);
```

#### 스트리밍 메시지 전송

```java
// 원격 에이전트에 텍스트 메시지 전송
Message message = A2A.toUserMessage("tell me some jokes"); // 메시지 ID는 자동으로 생성됩니다
MessageSendParams params = new MessageSendParams.Builder()
        .message(message)
        .build();

// Task, Message, TaskStatusUpdateEvent, TaskArtifactUpdateEvent에 대해 호출될 핸들러 생성
Consumer<StreamingEventKind> eventHandler = event -> {...};

// 오류 수신 시 호출될 핸들러 생성
Consumer<JSONRPCError> errorHandler = error -> {...};

// 실패 시 호출될 핸들러 생성
Runnable failureHandler = () -> {...};

// 원격 에이전트에 스트리밍 메시지 전송
client.sendStreamingMessage(params, eventHandler, errorHandler, failureHandler);
```

#### 작업 재구독

```java
// Task, Message, TaskStatusUpdateEvent, TaskArtifactUpdateEvent에 대해 호출될 핸들러 생성
Consumer<StreamingEventKind> eventHandler = event -> {...};

// 오류 수신 시 호출될 핸들러 생성
Consumer<JSONRPCError> errorHandler = error -> {...};

// 실패 시 호출될 핸들러 생성
Runnable failureHandler = () -> {...};

// "task-1234" ID를 가진 진행 중인 작업 재구독
TaskIdParams taskIdParams = new TaskIdParams("task-1234");
client.resubscribeToTask("request-1234", taskIdParams, eventHandler, errorHandler, failureHandler);
```

#### 이 클라이언트 에이전트가 통신하는 서버 에이전트에 대한 세부 정보 검색

```java
AgentCard serverAgentCard = client.getAgentCard();
```

에이전트 카드는 `A2A#getAgentCard` 메서드를 사용하여 검색할 수도 있습니다.

```java
// http://localhost:1234는 카드 정보를 검색하려는 에이전트의 기본 URL입니다.
AgentCard agentCard = A2A.getAgentCard("http://localhost:1234");
```

-----

## 추가 예시

### Hello World 예시

A2A 클라이언트가 Python A2A 서버와 통신하는 완전한 예시는 [examples/helloworld](https://www.google.com/search?q=src/main/java/io/a2a/examples/helloworld) 디렉토리에서 확인할 수 있습니다. 이 예시는 다음을 보여줍니다.

- A2A 자바 클라이언트 설정 및 사용
- 일반 및 스트리밍 메시지 전송
- 응답 수신 및 처리

이 예시에는 JBang을 사용하여 Python 서버와 자바 클라이언트를 모두 실행하는 방법에 대한 자세한 지침이 포함되어 있습니다. 자세한 내용은 [예시의 README](https://www.google.com/search?q=examples/client/src/main/java/io/a2a/examples/helloworld/README.md)를 참조하세요.

-----

## 라이선스

이 프로젝트는 [Apache 2.0 라이선스](https://www.google.com/search?q=LICENSE) 조건에 따라 라이선스가 부여됩니다.

-----

## 기여

기여 가이드라인은 [CONTRIBUTING.md](https://www.google.com/search?q=CONTRIBUTING.md)를 참조하세요.
