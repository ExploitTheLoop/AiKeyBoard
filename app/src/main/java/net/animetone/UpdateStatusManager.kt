package net.animetone

object UpdateStatusManager {
    private var isUpdateFound: Boolean = false

    // Getter
    fun isUpdateFound(): Boolean {
        return isUpdateFound
    }

    // Setter
    fun setUpdateFound(status: Boolean) {
        isUpdateFound = status
    }
}