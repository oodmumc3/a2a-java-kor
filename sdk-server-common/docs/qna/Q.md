## 제네릭을 이용한 상속 및 타입 분할
`SendMessageRequest` 을 보면 `NonStreamingJSONRPCRequest<MessageSendParams>`를 구현하고 있고 `NonStreamingJSONRPCRequest<?>`를 기반으로 generic을 통해 각 요청을 나눠주고 있음



