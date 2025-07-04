package io.a2a.server.requesthandlers;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.*;
import io.a2a.spec.InternalError;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mutiny.zero.ZeroPublisher;

import java.util.concurrent.Flow;

import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;

@ApplicationScoped
public class JSONRPCHandler {

    private AgentCard agentCard;
    private RequestHandler requestHandler;

    @Inject
    public JSONRPCHandler(@PublicAgentCard AgentCard agentCard, RequestHandler requestHandler) {
        this.agentCard = agentCard;
        this.requestHandler = requestHandler;
    }

    public SendMessageResponse onMessageSend(SendMessageRequest request) {
        try {
            EventKind taskOrMessage = requestHandler.onMessageSend(request.getParams());
            return new SendMessageResponse(request.getId(), taskOrMessage);
        } catch (JSONRPCError e) {
            return new SendMessageResponse(request.getId(), e);
        } catch (Throwable t) {
            return new SendMessageResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }


    public Flow.Publisher<SendStreamingMessageResponse> onMessageSendStream(SendStreamingMessageRequest request) {
        if (!agentCard.capabilities().streaming()) {
            return ZeroPublisher.fromItems(
                    new SendStreamingMessageResponse(
                            request.getId(),
                            new InvalidRequestError("Streaming is not supported by the agent")));
        }

        try {
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onMessageSendStream(request.getParams());
            // We can't use the convertingProcessor convenience method since that propagates any errors as an error handled
            // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
            return convertToSendStreamingMessageResponse(request.getId(), publisher);
        } catch (JSONRPCError e) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), e));
        } catch (Throwable throwable) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), new InternalError(throwable.getMessage())));
        }
    }

    public CancelTaskResponse onCancelTask(CancelTaskRequest request) {
        try {
            Task task = requestHandler.onCancelTask(request.getParams());
            if (task != null) {
                return new CancelTaskResponse(request.getId(), task);
            }
            return new CancelTaskResponse(request.getId(), new TaskNotFoundError());
        } catch (JSONRPCError e) {
            return new CancelTaskResponse(request.getId(), e);
        } catch (Throwable t) {
            return new CancelTaskResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public Flow.Publisher<SendStreamingMessageResponse> onResubscribeToTask(TaskResubscriptionRequest request) {
        if (!agentCard.capabilities().streaming()) {
            return ZeroPublisher.fromItems(
                    new SendStreamingMessageResponse(
                            request.getId(),
                            new InvalidRequestError("Streaming is not supported by the agent")));
        }

        try {
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onResubscribeToTask(request.getParams());
            // We can't use the convertingProcessor convenience method since that propagates any errors as an error handled
            // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
            return convertToSendStreamingMessageResponse(request.getId(), publisher);
        } catch (JSONRPCError e) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), e));
        } catch (Throwable throwable) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), new InternalError(throwable.getMessage())));
        }
    }

    public GetTaskPushNotificationConfigResponse getPushNotification(GetTaskPushNotificationConfigRequest request) {
        try {
            TaskPushNotificationConfig config = requestHandler.onGetTaskPushNotificationConfig(request.getParams());
            return new GetTaskPushNotificationConfigResponse(request.getId(), config);
        } catch (JSONRPCError e) {
            return new GetTaskPushNotificationConfigResponse(request.getId().toString(), e);
        } catch (Throwable t) {
            return new GetTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public SetTaskPushNotificationConfigResponse setPushNotification(SetTaskPushNotificationConfigRequest request) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new SetTaskPushNotificationConfigResponse(request.getId(),
                    new InvalidRequestError("Push notifications are not supported by the agent"));
        }
        try {
            TaskPushNotificationConfig config = requestHandler.onSetTaskPushNotificationConfig(request.getParams());
            return new SetTaskPushNotificationConfigResponse(request.getId().toString(), config);
        } catch (JSONRPCError e) {
            return new SetTaskPushNotificationConfigResponse(request.getId(), e);
        } catch (Throwable t) {
            return new SetTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public GetTaskResponse onGetTask(GetTaskRequest request) {
        try {
            Task task = requestHandler.onGetTask(request.getParams());
            return new GetTaskResponse(request.getId(), task);
        } catch (JSONRPCError e) {
            return new GetTaskResponse(request.getId(), e);
        } catch (Throwable t) {
            return new GetTaskResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public AgentCard getAgentCard() {
        return agentCard;
    }

    private Flow.Publisher<SendStreamingMessageResponse> convertToSendStreamingMessageResponse(
            Object requestId,
            Flow.Publisher<StreamingEventKind> publisher) {
            // We can't use the normal convertingProcessor since that propagates any errors as an error handled
            // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
            return ZeroPublisher.create(createTubeConfig(), tube -> {
                publisher.subscribe(new Flow.Subscriber<StreamingEventKind>() {
                    Flow.Subscription subscription;
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(StreamingEventKind item) {
                        tube.send(new SendStreamingMessageResponse(requestId, item));
                        subscription.request(1);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (throwable instanceof JSONRPCError jsonrpcError) {
                            tube.send(new SendStreamingMessageResponse(requestId, jsonrpcError));
                        } else {
                            tube.send(
                                    new SendStreamingMessageResponse(
                                            requestId, new
                                            InternalError(throwable.getMessage())));
                        }
                        onComplete();
                    }

                    @Override
                    public void onComplete() {
                        tube.complete();
                    }
                });
            });
    }
}
