package models;

/**
 * DRAFT: Saved but not published
 * PUBLISHED: Published waiting someone to find out
 * WAITING_APPROVAL_PROVIDER: Requester has approved the service and we are waiting provider to approve
 * WAITING_APPROVAL_REQUESTER: Vice versa, waiting for requester to approve
 * IN_PROGRESS: Everyone approved, waiting service to be finished
 * WAITING_FINISH_PROVIDER: Requester said that the job is done. Waiting for provider to say it's done
 * WAITING_FINISH_REQUESTER: Vice versa
 * FINISHED: That's all folks!
 */
public enum ServiceStatus {
    DRAFT,
    PUBLISHED,
    WAITING_APPROVAL_PROVIDER,
    WAITING_APPROVAL_REQUESTER,
    IN_PROGRESS,
    WAITING_FINISH_PROVIDER,
    WAITING_FINISH_REQUESTER,
    FINISHED
}

