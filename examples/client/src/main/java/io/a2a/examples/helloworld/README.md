# A2A Hello World 예시

이 예시는 A2A Java SDK를 사용하여 A2A 서버와 통신하는 방법을 보여줍니다. 이 예시에는 Python A2A 서버에 일반 메시지와 스트리밍 메시지를 모두 보내는 자바 클라이언트가 포함되어 있습니다.

## 전제 조건

- Java 11 이상
- [JBang](https://www.jbang.dev/documentation/guide/latest/installation.html) (빠른 설치 방법은 [INSTALL\_JBANG.md](https://www.google.com/search?q=INSTALL_JBANG.md) 참조)
- Python 3.8 이상
- [uv](https://github.com/astral-sh/uv) (권장) 또는 pip
- Git

## Python A2A 서버 설정 및 실행

Python A2A 서버는 [a2a-samples](https://github.com/google-a2a/a2a-samples) 프로젝트의 일부입니다. 설정 및 실행 방법은 다음과 같습니다.

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

3.  uv를 사용하여 서버 실행 (권장):

    ```bash
    uv run .
    ```

서버는 `http://localhost:9999`에서 실행됩니다.

## JBang으로 Java A2A 클라이언트 실행

자바 클라이언트는 JBang을 사용하여 실행할 수 있습니다. JBang을 사용하면 수동 컴파일 없이 자바 소스 파일을 직접 실행할 수 있습니다.

### A2A Java SDK 빌드

먼저 `a2a-java` 프로젝트를 빌드했는지 확인하세요.

```bash
cd /path/to/a2a-java
mvn clean install
```

### JBang 스크립트 사용

클라이언트 실행을 쉽게 할 수 있도록 예시 디렉토리에 JBang 스크립트가 제공됩니다.

1.  JBang이 설치되어 있는지 확인합니다. 설치되어 있지 않다면 [JBang 설치 가이드](https://www.jbang.dev/documentation/guide/latest/installation.html)를 따르세요.

2.  예시 디렉토리로 이동합니다.

    ```bash
    cd examples/client/src/main/java/io/a2a/examples/helloworld
    ```

3.  JBang 스크립트를 사용하여 클라이언트를 실행합니다.

    ```bash
    jbang HelloWorldRunner.java
    ```

이 스크립트는 종속성 및 소스를 자동으로 처리합니다.

## 예시의 작동 방식

자바 클라이언트(`HelloWorldClient.java`)는 다음 작업을 수행합니다.

1.  서버의 공개 에이전트 카드(public agent card)를 가져옵니다.
2.  서버의 확장 에이전트 카드(extended agent card)를 가져옵니다.
3.  `http://localhost:9999`에서 Python 서버에 연결하는 확장 에이전트 카드를 사용하여 A2A 클라이언트를 생성합니다.
4.  "10 USD는 INR로 얼마입니까?"라는 일반 메시지를 보냅니다.
5.  서버의 응답을 출력합니다.
6.  동일한 메시지를 스트리밍 요청으로 보냅니다.
7.  서버의 스트리밍 응답 각 청크가 도착할 때마다 출력합니다.

## 참고 사항

- 자바 클라이언트를 시작하기 전에 Python 서버가 실행 중인지 확인하세요.
- 클라이언트는 종료하기 전에 스트리밍 응답을 수집하기 위해 10초 동안 기다립니다.
- 필요한 경우 `HelloWorldClient.java` 파일에서 메시지 텍스트 또는 서버 URL을 수정할 수 있습니다.
