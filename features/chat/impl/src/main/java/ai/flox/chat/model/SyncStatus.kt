package ai.flox.chat.model

private const val SYNC_NEEDED_STATUS_CODE = -1
private const val COMPLETED_STATUS_CODE = 1
private const val FAILED_PERMANENTLY_STATUS_CODE = 2
private const val IN_PROGRESS_STATUS_CODE = 3

/**
 * If the message has been sent to the servers.
 */
enum class SyncStatus(val status: Int) {
    /**
     * When the entity is new or changed.
     */
    SYNC_NEEDED(SYNC_NEEDED_STATUS_CODE),

    /**
     * When the entity has been successfully synced.
     */
    COMPLETED(COMPLETED_STATUS_CODE),

    /**
     * After the retry strategy we still failed to sync this.
     */
    FAILED_PERMANENTLY(FAILED_PERMANENTLY_STATUS_CODE),

    /**
     * When sync is in progress.
     */
    IN_PROGRESS(IN_PROGRESS_STATUS_CODE);

    public companion object {
        private val map = values().associateBy(SyncStatus::status)
        public fun fromInt(type: Int): SyncStatus? = map[type]
    }
}
