package uk.co.skills4all.adultsafe.data

import uk.co.skills4all.adultsafe.model.AdultSafeAlert
import uk.co.skills4all.adultsafe.model.CapabilityTrack
import uk.co.skills4all.adultsafe.model.ConnectedSite
import uk.co.skills4all.adultsafe.model.DashboardTile
import uk.co.skills4all.adultsafe.model.DbsStatus
import uk.co.skills4all.adultsafe.model.DocumentRequirement
import uk.co.skills4all.adultsafe.model.DocumentState
import uk.co.skills4all.adultsafe.model.EvidenceItem
import uk.co.skills4all.adultsafe.model.PersonFilter
import uk.co.skills4all.adultsafe.model.PersonSummary
import uk.co.skills4all.adultsafe.model.TileTone

object DemoRepository {
    val demoSite = ConnectedSite(
        name = "AdultSafe Demo",
        url = "https://demo.invalid",
        apiVersion = "demo-v1",
        demoMode = true
    )

    fun dashboardTiles(): List<DashboardTile> = listOf(
        DashboardTile("profiles", "Awaiting profile completion", 5, "People", TileTone.Warning, PersonFilter.NeedsAttention),
        DashboardTile("no_email", "No email", 4, "People", TileTone.Attention, PersonFilter.NoEmail),
        DashboardTile("dbs_requested", "DBS requested", 2, "DBS / ID", TileTone.Attention, PersonFilter.AwaitingId),
        DashboardTile("dbs_expiring", "DBS expiring within 90 days", 1, "DBS / ID", TileTone.Danger, PersonFilter.ExpiringSoon),
        DashboardTile("documents", "Documents still to give", 6, "Documents", TileTone.Warning, PersonFilter.Documents),
        DashboardTile("follow_up", "Signing follow-up needed", 2, "Documents", TileTone.Danger, PersonFilter.Documents),
        DashboardTile("retention", "Retention review due", 1, "Retention", TileTone.Attention, PersonFilter.NeedsAttention)
    )

    fun people(): List<PersonSummary> = listOf(
        PersonSummary(
            id = "jamie",
            initials = "JB",
            name = "Jamie Bray",
            role = "AdultSafe Manager · Cubs, Scouts",
            email = "jamie@example.org",
            dbsStatus = DbsStatus.ResultReturned,
            dbsExpiry = "2026-11-25",
            warning = "DBS expires in 42 days",
            evidence = evidence(3),
            documents = listOf(
                DocumentRequirement("Yellow Card 2026", DocumentState.SignedGiven, "Signed today"),
                DocumentRequirement("Purple Card", DocumentState.SignedGiven, "Signed today"),
                DocumentRequirement("Green Card", DocumentState.NotRequired, "Not required for role")
            ),
            history = listOf("DBS result returned", "ID evidence checked", "Yellow Card 2026 signed")
        ),
        PersonSummary(
            id = "alex",
            initials = "AS",
            name = "Alex Smith",
            role = "Volunteer · Beavers",
            email = null,
            dbsStatus = DbsStatus.AwaitingIdCheck,
            dbsExpiry = null,
            warning = "Documents still to give",
            evidence = evidence(2),
            documents = listOf(
                DocumentRequirement("Yellow Card 2026", DocumentState.Outstanding),
                DocumentRequirement("Purple Card", DocumentState.Outstanding),
                DocumentRequirement("Green Card", DocumentState.NotSet)
            ),
            history = listOf("Profile created", "DBS requested")
        ),
        PersonSummary(
            id = "riley",
            initials = "RP",
            name = "Riley Patel",
            role = "Parent/carer · Cubs",
            email = "riley@example.org",
            dbsStatus = DbsStatus.AwaitingProfileCompletion,
            dbsExpiry = null,
            warning = "Confirmation follow-up needed",
            evidence = evidence(0),
            documents = listOf(
                DocumentRequirement("Yellow Card 2026", DocumentState.FollowUpNeeded, "Signer queried receipt"),
                DocumentRequirement("Purple Card", DocumentState.SigningLinkPending, "Link sent", "https://example.org/adultsafe/sign/demo")
            ),
            history = listOf("Documents confirmation requested", "Signer reported uncertainty")
        ),
        PersonSummary(
            id = "morgan",
            initials = "MT",
            name = "Morgan Taylor",
            role = "ID Checker",
            email = "morgan@example.org",
            dbsStatus = DbsStatus.ResultReturned,
            dbsExpiry = "2029-04-01",
            warning = "",
            evidence = evidence(3),
            documents = listOf(
                DocumentRequirement("Yellow Card 2026", DocumentState.SignedGiven, "Signed"),
                DocumentRequirement("Purple Card", DocumentState.NotRequired),
                DocumentRequirement("Green Card", DocumentState.NotRequired)
            ),
            history = listOf("Record reviewed", "No actions due")
        )
    )

    fun alerts(): List<AdultSafeAlert> = listOf(
        AdultSafeAlert("DBS expiry", "Jamie Bray expires within 90 days", TileTone.Danger, "jamie"),
        AdultSafeAlert("ID evidence incomplete", "Alex Smith has 2 of 3 ID evidence items", TileTone.Warning, "alex"),
        AdultSafeAlert("Document follow-up", "Riley Patel queried document receipt", TileTone.Danger, "riley")
    )

    fun capabilityTracks(): List<CapabilityTrack> = listOf(
        CapabilityTrack("Camera/photo/file picker", "Prepared", "V1-critical. Needs device picker and private upload wiring before first serious field test."),
        CapabilityTrack("Biometric/PIN unlock", "Prepared", "Dependency and lock route present. Needs Android biometric prompt and fallback PIN screen."),
        CapabilityTrack("Real upload wiring", "Prepared", "API client has endpoint shape. Needs multipart request and plugin auth token."),
        CapabilityTrack("QR invite scanner", "Prepared", "Connect flow has invite route reserved. Needs scanner dependency and parsing."),
        CapabilityTrack("Push notifications", "Planned", "Requires Firebase or another push provider plus plugin-side notification events."),
        CapabilityTrack("Release signing", "Prepared", "Gradle reads signing secrets from environment variables. No secrets stored in repo.")
    )

    fun filterPeople(filter: PersonFilter, people: List<PersonSummary> = people()): List<PersonSummary> = when (filter) {
        PersonFilter.All -> people
        PersonFilter.NeedsAttention -> people.filter { it.needsAttention }
        PersonFilter.ExpiringSoon -> people.filter { it.dbsExpiry?.startsWith("2026") == true }
        PersonFilter.Documents -> people.filter { person -> person.documents.any { it.state == DocumentState.Outstanding || it.state == DocumentState.FollowUpNeeded || it.state == DocumentState.SigningLinkPending } }
        PersonFilter.AwaitingId -> people.filter { it.dbsStatus == DbsStatus.AwaitingIdCheck || it.evidenceCount < 3 }
        PersonFilter.NoEmail -> people.filter { it.email.isNullOrBlank() }
    }

    private fun evidence(count: Int): List<EvidenceItem> = listOf(
        EvidenceItem("ID document 1", count >= 1, if (count >= 1) "Camera/photo" else "Not uploaded yet"),
        EvidenceItem("ID document 2", count >= 2, if (count >= 2) "Gallery/file" else "Not uploaded yet"),
        EvidenceItem("ID document 3", count >= 3, if (count >= 3) "PDF/file" else "Not uploaded yet")
    )
}
