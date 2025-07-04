package io.a2a.server.requesthandlers;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.agentexecution.SimpleRequestContextBuilder;
import io.a2a.server.events.*;
import io.a2a.server.tasks.PushNotifier;
import io.a2a.server.tasks.ResultAggregator;
import io.a2a.server.tasks.TaskManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.util.TempLoggerWrapper;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.*;
import io.a2a.spec.InternalError;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static io.a2a.server.util.async.AsyncUtils.*;

@ApplicationScoped
public class DefaultRequestHandler implements RequestHandler {

    private static final Logger log = new TempLoggerWrapper(LoggerFactory.getLogger(DefaultRequestHandler.class));

    private final AgentExecutor agentExecutor;
    private final TaskStore taskStore;
    private final QueueManager queueManager;
    private final PushNotifier pushNotifier;
    private final Supplier<RequestContext.Builder> requestContextBuilder;

    private final Map<String, CompletableFuture<Void>> runningAgents = Collections.synchronizedMap(new HashMap<>());

    private final Executor executor;

    @Inject
    public DefaultRequestHandler(AgentExecutor agentExecutor, TaskStore taskStore,
                                 QueueManager queueManager, PushNotifier pushNotifier, @Internal Executor executor) {
        this.agentExecutor = agentExecutor;
        this.taskStore = taskStore;
        this.queueManager = queueManager;
        this.pushNotifier = pushNotifier;
        this.executor = executor;
        // TODO In Python this is also a constructor parameter defaulting to this SimpleRequestContextBuilder
        //  implementation if the parameter is null. Skip that for now, since otherwise I get CDI errors, and
        //  I am unsure about the correct scope.
        //  Also reworked to make a Supplier since otherwise the builder gets polluted with wrong tasks
        this.requestContextBuilder = () -> new SimpleRequestContextBuilder(taskStore, false);
    }

    @Override
    public Task onGetTask(TaskQueryParams params) throws JSONRPCError {
        log.debug("onGetTask {}", params.id());
        Task task = taskStore.get(params.id());
        if (task == null) {
            log.debug("No task found for {}. Throwing TaskNotFoundError", params.id());
            throw new TaskNotFoundError();
        }
        if (params.historyLength() != null && task.getHistory() != null && params.historyLength() < task.getHistory().size()) {
            List<Message> history;
            if (params.historyLength() <= 0) {
                history = new ArrayList<>();
            } else {
                history = task.getHistory().subList(
                        task.getHistory().size() - params.historyLength(),
                        task.getHistory().size() - 1);
            }

            task = new Task.Builder(task)
                    .history(history)
                    .build();
        }

        log.debug("Task found {}", task);
        return task;
    }

    @Override
    public Task onCancelTask(TaskIdParams params) throws JSONRPCError {
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }
        TaskManager taskManager = new TaskManager(
                task.getId(),
                task.getContextId(),
                taskStore,
                null);

        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);

        EventQueue queue = queueManager.tap(task.getId());
        if (queue == null) {
            queue = EventQueue.create();
        }
        agentExecutor.cancel(
                requestContextBuilder.get()
                        .setTaskId(task.getId())
                        .setContextId(task.getContextId())
                        .setTask(task)
                        .build(),
                queue);

        CompletableFuture<Void> cf = runningAgents.get(task.getId());
        if (cf != null) {
            cf.cancel(true);
        }

        EventConsumer consumer = new EventConsumer(queue);
        EventKind type = resultAggregator.consumeAll(consumer);
        if (type instanceof Task tempTask) {
            return tempTask;
        }

        throw new InternalError("Agent did not return a valid response");
    }

    @Override
    public EventKind onMessageSend(MessageSendParams params) throws JSONRPCError {
        log.debug("onMessageSend - task: {}; context {}", params.message().getTaskId(), params.message().getContextId());
        TaskManager taskManager = new TaskManager(
                params.message().getTaskId(),
                params.message().getContextId(),
                taskStore,
                params.message());

        Task task = taskManager.getTask();
        if (task != null) {
            log.debug("Found task updating with message {}", params.message());
            task = taskManager.updateWithMessage(params.message(), task);

            if (shouldAddPushInfo(params)) {
                log.debug("Adding push info");
                pushNotifier.setInfo(task.getId(), params.configuration().pushNotification());
            }
        }

        RequestContext requestContext = requestContextBuilder.get()
                .setParams(params)
                .setTaskId(task == null ? null : task.getId())
                .setContextId(params.message().getContextId())
                .setTask(task)
                .build();

        String taskId = requestContext.getTaskId();
        log.debug("Request context taskId: {}", taskId);

        EventQueue queue = queueManager.createOrTap(taskId);
        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);

        EnhancedRunnable producerRunnable = registerAndExecuteAgentAsync(taskId, requestContext, queue);

        EventConsumer consumer = new EventConsumer(queue);

        producerRunnable.addDoneCallback(consumer.createAgentRunnableDoneCallback());

        boolean interrupted = false;
        ResultAggregator.EventTypeAndInterrupt etai = resultAggregator.consumeAndBreakOnInterrupt(consumer);

        try {
            if (etai == null) {
                log.debug("No result, throwing InternalError");
                throw new InternalError("No result");
            }
            interrupted = etai.interrupted();
            log.debug("Was interrupted: {}", interrupted);

            EventKind kind = etai.eventType();
            if (kind instanceof Task taskResult && !taskId.equals(taskResult.getId())) {
                throw new InternalError("Task ID mismatch in agent response");
            }

        } finally {
            if (interrupted) {
                // TODO Make this async
                cleanupProducer(taskId);
            } else {
                cleanupProducer(taskId);
            }
        }

        log.debug("Returning: {}", etai.eventType());
        return etai.eventType();
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onMessageSendStream(MessageSendParams params) throws JSONRPCError {
        TaskManager taskManager = new TaskManager(
                params.message().getTaskId(),
                params.message().getContextId(),
                taskStore,
                params.message());

        Task task = taskManager.getTask();
        if (task != null) {
            task = taskManager.updateWithMessage(params.message(), task);

            if (shouldAddPushInfo(params)) {
                pushNotifier.setInfo(task.getId(), params.configuration().pushNotification());
            }
        }

        RequestContext requestContext = requestContextBuilder.get()
                .setParams(params)
                .setTaskId(task == null ? null : task.getId())
                .setContextId(params.message().getContextId())
                .setTask(task)
                .build();

        AtomicReference<String> taskId = new AtomicReference<>(requestContext.getTaskId());
        EventQueue queue = queueManager.createOrTap(taskId.get());
        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);

        EnhancedRunnable producerRunnable = registerAndExecuteAgentAsync(taskId.get(), requestContext, queue);

        EventConsumer consumer = new EventConsumer(queue);

        producerRunnable.addDoneCallback(consumer.createAgentRunnableDoneCallback());

        try {
            Flow.Publisher<Event> results = resultAggregator.consumeAndEmit(consumer);

            Flow.Publisher<Event> eventPublisher =
                    processor(createTubeConfig(), results, ((errorConsumer, event) -> {
                if (event instanceof Task createdTask) {
                    if (!Objects.equals(taskId.get(), createdTask.getId())) {
                        errorConsumer.accept(new InternalError("Task ID mismatch in agent response"));
                    }

                    // TODO the Python implementation no longer has the following block but removing it causes
                    //  failures here
                    try {
                        queueManager.add(createdTask.getId(), queue);
                        taskId.set(createdTask.getId());
                    } catch (TaskQueueExistsException e) {
                        // TODO Log
                    }
                    if (pushNotifier != null &&
                            params.configuration() != null &&
                            params.configuration().pushNotification() != null) {

                        pushNotifier.setInfo(
                                createdTask.getId(),
                                params.configuration().pushNotification());
                    }

                }
                if (pushNotifier != null && taskId.get() != null) {
                    EventKind latest = resultAggregator.getCurrentResult();
                    if (latest instanceof Task latestTask) {
                        pushNotifier.sendNotification(latestTask);
                    }
                }

                return true;
            }));

            return convertingProcessor(eventPublisher, event -> (StreamingEventKind) event);
        } finally {
            cleanupProducer(taskId.get());
        }
    }

    @Override
    public TaskPushNotificationConfig onSetTaskPushNotificationConfig(TaskPushNotificationConfig params) throws JSONRPCError {
        if (pushNotifier == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.taskId());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        pushNotifier.setInfo(params.taskId(), params.pushNotificationConfig());

        return params;
    }

    @Override
    public TaskPushNotificationConfig onGetTaskPushNotificationConfig(TaskIdParams params) throws JSONRPCError {
        if (pushNotifier == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        PushNotificationConfig pushNotificationConfig = pushNotifier.getInfo(params.id());
        if (pushNotificationConfig == null) {
            throw new InternalError("No push notification config found");
        }

        return new TaskPushNotificationConfig(params.id(), pushNotificationConfig);
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onResubscribeToTask(TaskIdParams params) throws JSONRPCError {
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        TaskManager taskManager = new TaskManager(task.getId(), task.getContextId(), taskStore, null);
        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);
        EventQueue queue = queueManager.tap(task.getId());

        if (queue == null) {
            throw new TaskNotFoundError();
        }

        EventConsumer consumer = new EventConsumer(queue);
        Flow.Publisher<Event> results = resultAggregator.consumeAndEmit(consumer);
        return convertingProcessor(results, e -> (StreamingEventKind) e);
    }

    private boolean shouldAddPushInfo(MessageSendParams params) {
        return pushNotifier != null && params.configuration() != null && params.configuration().pushNotification() != null;
    }

    private EnhancedRunnable registerAndExecuteAgentAsync(String taskId, RequestContext requestContext, EventQueue queue) {
        EnhancedRunnable runnable = new EnhancedRunnable() {
            @Override
            public void run() {
                agentExecutor.execute(requestContext, queue);
                try {
                    queueManager.awaitQueuePollerStart(queue);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        CompletableFuture<Void> cf = CompletableFuture.runAsync(runnable, executor)
                .whenComplete((v, err) -> {
                    if (err != null) {
                        runnable.setError(err);
                    }
                    queue.close();
                    runnable.invokeDoneCallbacks();
                });
        runningAgents.put(taskId, cf);
        return runnable;
    }

    private void cleanupProducer(String taskId) {
        // TODO the Python implementation waits for the producerRunnable
        CompletableFuture<Void> cf = runningAgents.get(taskId);
        if (cf != null) {
            cf.whenComplete((v, t) -> {
                queueManager.close(taskId);
                runningAgents.remove(taskId);
            });
        }
    }

}
