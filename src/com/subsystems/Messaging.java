package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.repository.MessageRepository;
import com.repository.SQLiteMessageRepository;
import com.common.Database;
import com.managers.message.*;

public class Messaging implements Subsystems {

    private AsyncMessageBroker broker;
    private MessageRepository repo;
    private Database database;

    // Constructor to accept database
    public Messaging(Database database) {
        this.database = database;
    }

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;
        // Use SQLiteMessageRepository with the database instance
        this.repo = new SQLiteMessageRepository(database);

        new SendMessageManager(broker, repo);
        new ReplyMessageManager(broker, repo);
        new LoadConversationManager(broker, repo);
        new ConversationListManager(broker, repo);
        new UnreadMessageManager(broker, repo);

        System.out.println("[Messaging] Initialized.");
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
    }
}