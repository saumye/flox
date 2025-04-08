package ai.flox.chat.data

class SharedResource {
    // Synchronized method for Thread 1 to wait for a signal with a timeout
    @Synchronized
    fun waitForSignalWithTimeout(timeoutMillis: Long): Boolean {
        val startTime = System.currentTimeMillis()

        try {
            (this as Object).wait(timeoutMillis) // Wait for the given timeout
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt() // Restore interrupt status
            return false // Thread interruption as timeout
        }

        val elapsedTime = System.currentTimeMillis() - startTime

        // Check if wait returned due to notify or timeout
        return if (elapsedTime < timeoutMillis) {
            true // Returned due to notify
        } else {
            false // Returned due to timeout
        }
    }

    // Synchronized method for Thread 2 to send a signal
    @Synchronized
    fun sendSignal() {
        (this as Object).notify() // Notifies the waiting thread
    }
}