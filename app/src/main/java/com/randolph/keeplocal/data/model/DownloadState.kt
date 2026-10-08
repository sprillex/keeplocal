package com.randolph.keeplocal.data.model

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState
    data object Verifying : DownloadState
    data object Success : DownloadState
    data class Error(val message: String) : DownloadState
}
