package uk.co.skills4all.adultsafe.model

enum class AppScreen { Dashboard, People, Alerts, Documents, Settings }
enum class AppMode { Disconnected, Connecting, ConnectedDemo, ConnectedLive, Locked, Error }
enum class TileTone { Good, Attention, Warning, Danger }
enum class DbsStatus { AwaitingProfileCompletion, DbsRequested, AwaitingIdCheck, ResultReturned }
enum class DocumentState { NotSet, Outstanding, SignedGiven, NotRequired, FollowUpNeeded, SigningLinkPending }
enum class PersonFilter { All, NeedsAttention, ExpiringSoon, Documents, AwaitingId, NoEmail }

data class ConnectedSite(
    val name: String,
    val url: String,
    val apiVersion: String = "unknown",
    val demoMode: Boolean = false,
    val active: Boolean = true
)

data class DashboardTile(
    val slug: String,
    val title: String,
    val count: Int,
    val group: String,
    val tone: TileTone = TileTone.Attention,
    val filter: PersonFilter? = null
)

data class DocumentRequirement(
    val name: String,
    val state: DocumentState,
    val lastUpdated: String = "",
    val signingLink: String? = null
)

data class EvidenceItem(
    val label: String,
    val present: Boolean,
    val source: String = "Not uploaded yet"
)

data class PersonSummary(
    val id: String,
    val initials: String,
    val name: String,
    val role: String,
    val email: String?,
    val dbsStatus: DbsStatus,
    val dbsExpiry: String?,
    val warning: String,
    val evidence: List<EvidenceItem>,
    val documents: List<DocumentRequirement>,
    val history: List<String>
) {
    val needsAttention: Boolean
        get() = warning.isNotBlank() || documents.any { it.state == DocumentState.Outstanding || it.state == DocumentState.FollowUpNeeded }

    val evidenceCount: Int
        get() = evidence.count { it.present }
}

data class AdultSafeAlert(
    val title: String,
    val body: String,
    val tone: TileTone,
    val personId: String? = null
)

data class CapabilityTrack(
    val name: String,
    val status: String,
    val note: String
)
