# A2A Hello World 예시

이 예시는 A2A Java SDK를 사용하여 A2A 클라이언트와 통신하는 방법을 보여줍니다. 이 예시에는 Python A2A 클라이언트로부터 일반 메시지와 스트리밍 메시지를 모두 수신하는 자바 서버가 포함되어 있습니다.

## 전제 조건

- Java 11 이상
- Python 3.8 이상
- [uv](https://github.com/astral-sh/uv)
- Git

## Java A2A 서버 실행

자바 서버는 `mvn`을 사용하여 다음과 같이 시작할 수 있습니다.

```bash
cd examples/server
mvn quarkus:dev
```

## Python A2A 클라이언트 설정 및 실행

Python A2A 클라이언트는 [a2a-samples](https://github.com/google-a2a/a2a-samples) 프로젝트의 일부입니다. 설정 및 실행 방법은 다음과 같습니다.

1.  a2a-samples 저장소 복제:

    ```bash
    git clone https://github.com/google-a2a/a2a-samples.git
    cd a2a-samples/samples/python/agents/helloworld
    ```

2.  **권장 방법**: uv를 사용하여 종속성 설치 (훨씬 빠른 Python 패키지 설치 프로그램):

    ```bash
    # uv가 아직 설치되어 있지 않다면 설치합니다.
    # macOS 및 Linux
    curl -LsSf https://astral.sh/uv/install.sh | sh
    # Windows
    powershell -c "irm https://astral.sh/uv/install.ps1 | iex"

    # uv를 사용하여 패키지 설치
    uv venv
    source .venv/bin/activate  # Windows: .venv\Scripts\activate
    uv pip install -e .
    ```

3.  uv를 사용하여 클라이언트 실행 (권장):

    ```bash
    uv run test_client.py
    ```

클라이언트는 `http://localhost:9999`에서 실행 중인 자바 서버에 연결됩니다.

## 예시의 작동 방식

Python A2A 클라이언트(`test_client.py`)는 다음 작업을 수행합니다.

1.  서버의 공개 에이전트 카드(public agent card)를 가져옵니다.
2.  서버에서 지원하는 경우 서버의 확장 에이전트 카드(extended agent card)를 가져옵니다 (참고: [https://github.com/a2aproject/a2a-java/issues/81](https://github.com/a2aproject/a2a-java/issues/81)).
3.  `http://localhost:9999`에서 Python 서버에 연결하는 확장 에이전트 카드를 사용하여 A2A 클라이언트를 생성합니다.
4.  "10 USD는 INR로 얼마입니까?"라는 일반 메시지를 보냅니다.
5.  서버의 응답을 출력합니다.
6.  동일한 메시지를 스트리밍 요청으로 보냅니다.
7.  서버의 스트리밍 응답 각 청크가 도착할 때마다 출력합니다.

## 참고 사항

- Python 클라이언트를 시작하기 전에 자바 서버가 실행 중인지 확인하세요.
- 클라이언트는 종료하기 전에 스트리밍 응답을 수집하기 위해 10초 동안 기다립니다.
- 필요한 경우 `AgentExecutorProducer.java`에서 서버 응답을 수정할 수 있습니다.
- 필요한 경우 `AgentCardProducer.java`에서 서버 에이전트 카드를 수정할 수 있습니다.
- 필요한 경우 `application.properties` 및 `AgentCardProducer.java`에서 서버 URL을 수정할 수 있습니다.
