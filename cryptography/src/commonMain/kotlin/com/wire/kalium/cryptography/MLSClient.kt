package com.wire.kalium.cryptography

import kotlin.jvm.JvmInline

typealias WelcomeMessage = ByteArray
typealias HandshakeMessage = ByteArray
typealias ApplicationMessage = ByteArray
typealias PlainMessage = ByteArray
typealias MLSKeyPackage = ByteArray

enum class PublicGroupStateEncryptionType {
    PLAINTEXT,
    JWE_ENCRYPTED
}

enum class RatchetTreeType {
    FULL,
    DELTA,
    BY_REF
}

open class PublicGroupStateBundle(
    var encryptionType: PublicGroupStateEncryptionType,
    var ratchetTreeType: RatchetTreeType,
    var payload: ByteArray
)

open class CommitBundle(
    val commit: ByteArray,
    val welcome: ByteArray?,
    val publicGroupStateBundle: PublicGroupStateBundle
)

class DecryptedMessageBundle(
    val message: ByteArray?,
    val commitDelay: Long?,
    val senderClientId: CryptoQualifiedClientId?
)
@JvmInline
value class Ed22519Key(
    val value: ByteArray
)

@Suppress("TooManyFunctions")
interface MLSClient {

    /**
     * Release any locks the C code have on the MLS resources and
     * delete local MLS DB and files
     *
     * @return true when delete is successful and false otherwise
     */
    fun clearLocalFiles(): Boolean

    /**
     * Public key of the client's identity.
     *
     * @return public key of the client
     */
    fun getPublicKey(): ByteArray

    /**
     * Generate a fresh set of key packages.
     *
     * @return list of generated key packages. NOTE: can be more than the requested amount.
     */
    fun generateKeyPackages(amount: Int): List<ByteArray>

    /**
     * Number of valid key packages which haven't been consumed
     *
     * @return valid key package count
     */
    fun validKeyPackageCount(): ULong

    /**
     * Update your keying material for an existing conversation you're a member of.
     *
     * @param groupId MLS group ID for an existing conversation
     *
     * @return commit bundle, which needs to be sent to the distribution service.
     */
    fun updateKeyingMaterial(groupId: MLSGroupId): CommitBundle

    /**
     * Request to join an existing conversation
     *
     * @param groupId MLS group ID for an existing conversation
     * @param epoch current epoch for the conversation
     *
     * @return proposal, which needs to be sent to the distribution service.
     */
    fun joinConversation(
        groupId: MLSGroupId,
        epoch: ULong
    ): HandshakeMessage

    /**
     * Query if a conversation exists
     *
     * @param groupId MLS group ID provided by BE
     *
     * @return true if conversation exists in store
     */
    fun conversationExists(groupId: MLSGroupId): Boolean

    /**
     * Query the current epoch of a conversation
     *
     * @return conversation epoch
     */
    fun conversationEpoch(groupId: MLSGroupId): ULong

    /**
     * Create a new MLS conversation
     *
     * @param groupId MLS group ID provided by BE
     */
    fun createConversation(
        groupId: MLSGroupId,
        externalSenders: List<Ed22519Key> = emptyList()
    )

    fun wipeConversation(groupId: MLSGroupId)

    /**
     * Process an incoming welcome message
     *
     * @param message the incoming welcome message
     * @return MLS group ID
     */
    fun processWelcomeMessage(message: WelcomeMessage): MLSGroupId

    /**
     * Signal that last sent commit was accepted by the distribution service
     */
    fun commitAccepted(groupId: MLSGroupId)

    /**
     * Create a commit for any pending proposals
     *
     * @return commit bundle, which needs to be sent to the distribution service. If there are no
     * pending proposals null is returned.
     */
    fun commitPendingProposals(groupId: MLSGroupId): CommitBundle?

    /**
     * Clear a pending commit which has not yet been accepted by the distribution service
     */
    fun clearPendingCommit(groupId: MLSGroupId)

    /**
     * Encrypt a message for distribution in a group
     *
     * @param groupId MLS group ID provided by BE
     * @param message plain text message
     *
     * @return encrypted ApplicationMessage
     */
    fun encryptMessage(
        groupId: MLSGroupId,
        message: PlainMessage
    ): ApplicationMessage

    /**
     * Decrypt an application message or a handshake message
     *
     * **NOTE**: handshake messages doesn't return any decrypted message.
     *
     * @param groupId MLS group where the message was received
     * @param message application message or handshake message
     *
     * @return decrypted message bundle, which contains the decrypted message.
     */
    fun decryptMessage(
        groupId: MLSGroupId,
        message: ApplicationMessage
    ): DecryptedMessageBundle

    /**
     * Add a user/client to an existing MLS group
     *
     * @param groupId MLS group
     * @param members list of clients with a claimed key package for each client.
     *
     * * @return commit bundle, which needs to be sent to the distribution service.
     */
    fun addMember(
        groupId: MLSGroupId,
        members: List<Pair<CryptoQualifiedClientId, MLSKeyPackage>>
    ): CommitBundle?

    /**
     * Remove a user/client from an existing MLS group
     *
     * @param groupId MLS group
     * @param members list of clients
     *
     * @return commit bundle, which needs to be sent to the distribution service.
     */
    fun removeMember(
        groupId: MLSGroupId,
        members: List<CryptoQualifiedClientId>
    ): CommitBundle
}

expect class MLSClientImpl(rootDir: String, databaseKey: MlsDBSecret, clientId: CryptoQualifiedClientId) : MLSClient