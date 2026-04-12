package br.com.tec.tecmotors.data.repository

import android.content.Context
import br.com.tec.tecmotors.data.remote.RemoteSnapshotMapper
import br.com.tec.tecmotors.domain.model.SyncResult
import br.com.tec.tecmotors.domain.repository.SnapshotRepository
import br.com.tec.tecmotors.domain.repository.SyncRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SyncRepositoryImpl(
    private val context: Context,
    private val snapshotRepository: SnapshotRepository
) : SyncRepository {
    private val firebaseAvailable: Boolean by lazy {
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
    }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun currentUserEmail(): String? {
        if (!firebaseAvailable) return null
        return auth.currentUser?.email
    }

    override fun isSignedIn(): Boolean {
        if (!firebaseAvailable) return false
        return auth.currentUser != null
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> {
        return runCatching {
            ensureFirebaseConfiguredOrThrow()
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            auth.currentUser?.email ?: "Conta Google conectada"
        }
    }

    override fun signOut() {
        auth.signOut()
    }

    override suspend fun uploadLocalState(): Result<SyncResult> {
        return runCatching {
            ensureSignedInOrThrow()
            val snapshot = snapshotRepository.getSnapshot()
            writeRemoteSnapshot(snapshot)
            SyncResult(
                message = "Dados locais enviados para a nuvem.",
                localUpdated = false
            )
        }
    }

    override suspend fun downloadRemoteState(): Result<SyncResult> {
        return runCatching {
            ensureSignedInOrThrow()
            val remote = readRemoteSnapshot()
                ?: throw IllegalStateException("Nenhum backup remoto encontrado para esta conta.")
            snapshotRepository.restoreSnapshot(remote)
            SyncResult(
                message = "Dados baixados da nuvem para o aparelho.",
                localUpdated = true
            )
        }
    }

    override suspend fun syncNow(): Result<SyncResult> {
        return runCatching {
            ensureSignedInOrThrow()

            val local = snapshotRepository.getSnapshot()
            val remote = readRemoteSnapshot()

            if (remote == null) {
                writeRemoteSnapshot(local)
                return@runCatching SyncResult(
                    message = "Primeiro backup criado na nuvem.",
                    localUpdated = false
                )
            }

            when {
                remote.updatedAtMillis > local.updatedAtMillis -> {
                    snapshotRepository.restoreSnapshot(remote)
                    SyncResult(
                        message = "Conflito resolvido: nuvem estava mais recente e substituiu o local.",
                        localUpdated = true
                    )
                }

                remote.updatedAtMillis < local.updatedAtMillis -> {
                    writeRemoteSnapshot(local)
                    SyncResult(
                        message = "Conflito resolvido: local estava mais recente e foi enviado para a nuvem.",
                        localUpdated = false
                    )
                }

                else -> {
                    SyncResult(
                        message = "Dados ja estao sincronizados.",
                        localUpdated = false
                    )
                }
            }
        }
    }

    private suspend fun writeRemoteSnapshot(snapshot: br.com.tec.tecmotors.domain.model.LocalStateSnapshot) {
        val uid = requireNotNull(auth.currentUser?.uid)
        firestore.collection(COLLECTION)
            .document(uid)
            .set(RemoteSnapshotMapper.toRemoteMap(snapshot))
            .await()
    }

    private suspend fun readRemoteSnapshot(): br.com.tec.tecmotors.domain.model.LocalStateSnapshot? {
        val uid = requireNotNull(auth.currentUser?.uid)
        val doc = firestore.collection(COLLECTION)
            .document(uid)
            .get()
            .await()

        if (!doc.exists()) return null
        val map = doc.data ?: return null
        return RemoteSnapshotMapper.fromRemoteMap(map)
    }

    private fun ensureSignedInOrThrow() {
        ensureFirebaseConfiguredOrThrow()
        if (auth.currentUser == null) {
            throw IllegalStateException("Faca login com Google antes de sincronizar.")
        }
    }

    private fun ensureFirebaseConfiguredOrThrow() {
        if (FirebaseApp.getApps(context).isNotEmpty()) return
        val initialized = FirebaseApp.initializeApp(context)
        if (initialized == null) {
            throw IllegalStateException(
                "Firebase nao configurado. Adicione o arquivo app/google-services.json e sincronize o projeto."
            )
        }
    }

    companion object {
        private const val COLLECTION = "tec_motors_users"
    }
}
