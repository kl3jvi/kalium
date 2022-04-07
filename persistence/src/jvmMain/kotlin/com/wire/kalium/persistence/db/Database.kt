package com.wire.kalium.persistence.db

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.wire.kalium.persistence.dao.*
import com.wire.kalium.persistence.dao.asset.AssetDAO
import com.wire.kalium.persistence.dao.asset.AssetDAOImpl
import com.wire.kalium.persistence.dao.client.ClientDAO
import com.wire.kalium.persistence.dao.client.ClientDAOImpl
import com.wire.kalium.persistence.dao.message.MessageDAO
import com.wire.kalium.persistence.dao.message.MessageDAOImpl
import java.io.File
import java.util.Properties

actual class Database(private val storePath: File) {

    private val database: AppDatabase

    init {
        val databasePath = storePath.resolve(DATABASE_NAME)
        val databaseExists = databasePath.exists()

        // Make sure all intermediate directories exist
        storePath.mkdirs()

        val driver: SqlDriver = JdbcSqliteDriver(
            "jdbc:sqlite:${databasePath.absolutePath}",
            Properties(1).apply { put("foreign_keys", "true") })

        if (!databaseExists) {
          AppDatabase.Schema.create(driver)
        }

        database = AppDatabase(
            driver,
            Client.Adapter(user_idAdapter = QualifiedIDAdapter()),
            Conversation.Adapter(qualified_idAdapter = QualifiedIDAdapter(), typeAdapter = EnumColumnAdapter()),
            Member.Adapter(userAdapter = QualifiedIDAdapter(), conversationAdapter = QualifiedIDAdapter()),
            Message.Adapter(
                conversation_idAdapter = QualifiedIDAdapter(),
                sender_user_idAdapter = QualifiedIDAdapter(),
                statusAdapter = EnumColumnAdapter(),
                asset_image_widthAdapter = IntColumnAdapter,
                asset_image_heightAdapter = IntColumnAdapter,
                asset_sizeAdapter = IntColumnAdapter,
                content_typeAdapter = ContentTypeAdapter(),
                visibilityAdapter = EnumColumnAdapter()
            ),
            User.Adapter(qualified_idAdapter = QualifiedIDAdapter(), IntColumnAdapter)
        )
    }

    actual val userDAO: UserDAO
        get() = UserDAOImpl(database.usersQueries)

    actual val conversationDAO: ConversationDAO
        get() = ConversationDAOImpl(database.converationsQueries, database.usersQueries, database.membersQueries)

    actual val metadataDAO: MetadataDAO
        get() = MetadataDAOImpl(database.metadataQueries)

    actual val clientDAO: ClientDAO
        get() = ClientDAOImpl(database.clientsQueries)

    actual val messageDAO: MessageDAO
        get() = MessageDAOImpl(database.messagesQueries)

    actual val assetDAO: AssetDAO
        get() = AssetDAOImpl(database.assetsQueries)

    actual val teamDAO: TeamDAO
        get() = TeamDAOImpl(database.teamsQueries)

    actual fun nuke(): Boolean {
        return storePath.resolve(DATABASE_NAME).delete()
    }

    private companion object {
        const val DATABASE_NAME = "main.db"
    }
}
